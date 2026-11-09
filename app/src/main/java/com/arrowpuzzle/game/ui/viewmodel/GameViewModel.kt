package com.arrowpuzzle.game.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arrowpuzzle.game.data.ProgressRepository
import com.arrowpuzzle.game.data.SavedGame
import com.arrowpuzzle.game.data.ScoreRepository
import com.arrowpuzzle.game.data.SettingsRepository
import com.arrowpuzzle.game.data.model.LevelConfig
import com.arrowpuzzle.game.data.model.ScoreRecord
import com.arrowpuzzle.game.game.arrowExitDurationMillis
import com.arrowpuzzle.game.game.ArrowGeometry
import com.arrowpuzzle.game.game.ArrowLogic
import com.arrowpuzzle.game.game.DefeatReason
import com.arrowpuzzle.game.game.GameFeedback
import com.arrowpuzzle.game.game.GameState
import com.arrowpuzzle.game.game.GeneratedLevel
import com.arrowpuzzle.game.game.LevelGenerator
import com.arrowpuzzle.game.scoring.ScoreCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameViewModel(
    private val config: LevelConfig,
    private val levelGenerator: LevelGenerator,
    private val settingsRepository: SettingsRepository,
    private val scoreRepository: ScoreRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel() {
    private val clickLock = Any()
    private val _state = MutableStateFlow<GameState>(GameState.Loading)
    val state: StateFlow<GameState> = _state.asStateFlow()

    /**
     * Running clock of the level. Separate from [state] on purpose: the timer
     * ticks ten times a second, and only the time display should react to it,
     * not the whole board.
     */
    private val _elapsed = MutableStateFlow(0.0)
    val elapsed: StateFlow<Double> = _elapsed.asStateFlow()

    private val _feedback = MutableSharedFlow<GameFeedback>(extraBufferCapacity = 12)
    val feedback: SharedFlow<GameFeedback> = _feedback.asSharedFlow()

    private var timerJob: Job? = null
    private var finishJob: Job? = null
    private var startedNanos = 0L

    init {
        // Only restore when the home screen explicitly asked for it and the
        // saved level matches the one we were navigated to.
        val resumeCandidate = if (progressRepository.consumeResumeRequest()) {
            progressRepository.savedGame.value?.takeIf { it.config == config }
        } else {
            null
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                val seed = if (config.isCustom) {
                    System.nanoTime()
                } else {
                    1_000_003L * config.levelId
                }
                runCatching { withTimeLimit(levelGenerator.generate(config, seed)) }
            }
            result.onSuccess { level ->
                synchronized(clickLock) {
                    if (resumeCandidate != null) {
                        val validRemoved = resumeCandidate.removedIds
                            .filter { level.arrowById(it) != null }
                            .toSet()
                        // Continue the clock where the saved game stopped, so a
                        // resumed run is scored like the same uninterrupted run.
                        startedNanos = System.nanoTime() -
                            (resumeCandidate.elapsedSeconds * NANOS_PER_SECOND).toLong()
                        _elapsed.value = resumeCandidate.elapsedSeconds
                        _state.value = GameState.Playing(
                            level = level,
                            removedIds = validRemoved,
                            failures = resumeCandidate.failures,
                            elapsedSeconds = resumeCandidate.elapsedSeconds,
                        )
                    } else {
                        // A fresh start replaces whatever level was pending.
                        progressRepository.clear()
                        startedNanos = System.nanoTime()
                        _state.value = GameState.Playing(
                            level = level,
                            removedIds = emptySet(),
                            failures = 0,
                            elapsedSeconds = 0.0,
                        )
                    }
                }
                startTimer()
            }.onFailure { error ->
                _state.value = GameState.Error(
                    error.message ?: "Level generation failed",
                )
            }
        }
    }

    fun onArrowClick(arrowId: Int) {
        var feedback: GameFeedback? = null
        var persist = false
        var clearProgress = false

        synchronized(clickLock) {
            val current = _state.value as? GameState.Playing ?: return@synchronized
            val limit = current.level.config.timeLimitSeconds
            if (limit != null) {
                val elapsed = elapsedSeconds()
                if (elapsed >= limit) {
                    timerJob?.cancel()
                    _state.value = GameState.Defeat(
                        level = current.level,
                        reason = DefeatReason.TIME_EXPIRED,
                        failures = current.failures,
                        elapsedSeconds = elapsed,
                    )
                    clearProgress = true
                    feedback = defeatFeedback()
                    return@synchronized
                }
            }

            when (
                val result = ArrowLogic.evaluateClick(
                    level = current.level,
                    removedIds = current.removedIds,
                    arrowId = arrowId,
                    failures = current.failures,
                )
            ) {
                is ArrowLogic.ClickResult.Correct -> {
                    val removedIds = current.removedIds + arrowId
                    if (removedIds.size >= current.level.arrowCount) {
                        timerJob?.cancel()
                        val elapsed = elapsedSeconds()
                        val breakdown = ScoreCalculator.calculate(
                            arrowCount = current.level.arrowCount,
                            timeElapsedSeconds = elapsed,
                            failures = current.failures,
                        )
                        val record = ScoreRecord(
                            levelId = config.levelId,
                            points = breakdown.total,
                            arrowsUsed = current.level.arrowCount,
                            timeElapsed = elapsed,
                            failures = current.failures,
                            timestamp = System.currentTimeMillis(),
                        )
                        val isNewHighScore = scoreRepository.recordScore(record)
                        clearProgress = true
                        _state.value = current.copy(
                            removedIds = removedIds,
                            failures = current.failures,
                            elapsedSeconds = elapsed,
                            correctFlashId = arrowId,
                            correctFlashToken = current.correctFlashToken + 1,
                            wrongFlashId = null,
                        )
                        // Wait for the exit animation of the final arrow. The
                        // duration depends on that arrow's length, so it is
                        // derived the same way GameScreen derives it.
                        val exitMillis = arrowExitDurationMillis(
                            current.level.arrowById(arrowId)?.lengthCells ?: 1,
                        )
                        finishJob?.cancel()
                        finishJob = viewModelScope.launch {
                            delay(exitMillis + 40L)
                            _state.value = GameState.Victory(current.level, record, isNewHighScore)
                        }
                        feedback = victoryFeedback()
                    } else {
                        _state.value = current.copy(
                            removedIds = removedIds,
                            correctFlashId = arrowId,
                            correctFlashToken = current.correctFlashToken + 1,
                            wrongFlashId = null,
                        )
                        persist = true
                        feedback = correctFeedback()
                    }
                }

                is ArrowLogic.ClickResult.Wrong -> {
                    if (result.failures >= config.maxFailures) {
                        timerJob?.cancel()
                        _state.value = GameState.Defeat(
                            level = current.level,
                            reason = DefeatReason.TOO_MANY_FAILURES,
                            failures = result.failures,
                            elapsedSeconds = elapsedSeconds(),
                        )
                        clearProgress = true
                        feedback = defeatFeedback()
                    } else {
                        _state.value = current.copy(
                            failures = result.failures,
                            elapsedSeconds = elapsedSeconds(),
                            wrongFlashId = arrowId,
                            wrongFlashToken = current.wrongFlashToken + 1,
                            correctFlashId = null,
                        )
                        persist = true
                        feedback = wrongFeedback()
                    }
                }

                ArrowLogic.ClickResult.AlreadyRemoved,
                ArrowLogic.ClickResult.Ignored,
                -> Unit
            }
        }

        when {
            clearProgress -> progressRepository.clear()
            persist -> (_state.value as? GameState.Playing)?.let(::persistProgress)
        }

        feedback?.let(_feedback::tryEmit)
    }

    /**
     * Dev option: taps the first open arrow, exactly as a player would (same
     * animation, sound, progress saving and victory handling).
     */
    fun devTapNextArrow() {
        val current = _state.value as? GameState.Playing ?: return
        val next = ArrowGeometry.availableArrows(current.level, current.removedIds)
            .firstOrNull()
            ?: return
        onArrowClick(next.id)
    }

    /**
     * Dev option: ends the level at once with a perfect result (no failures,
     * zero seconds), i.e. the maximum score shown in the level list. The score
     * is recorded and the next level unlocked like a real win.
     */
    fun devPerfectWin() {
        var won = false
        synchronized(clickLock) {
            val current = _state.value as? GameState.Playing ?: return@synchronized
            timerJob?.cancel()
            finishJob?.cancel()
            val breakdown = ScoreCalculator.calculate(
                arrowCount = current.level.arrowCount,
                timeElapsedSeconds = 0.0,
                failures = 0,
            )
            val record = ScoreRecord(
                levelId = config.levelId,
                points = breakdown.total,
                arrowsUsed = current.level.arrowCount,
                timeElapsed = 0.0,
                failures = 0,
                timestamp = System.currentTimeMillis(),
            )
            val isNewHighScore = scoreRepository.recordScore(record)
            _state.value = GameState.Victory(current.level, record, isNewHighScore)
            won = true
        }
        if (won) {
            progressRepository.clear()
            _feedback.tryEmit(victoryFeedback())
        }
    }

    /**
     * Writes the current playing state so it can be resumed later. Safe to call
     * repeatedly and from lifecycle callbacks.
     */
    fun saveProgress() {
        synchronized(clickLock) {
            val current = _state.value as? GameState.Playing ?: return
            persistProgress(current)
        }
    }

    private fun persistProgress(state: GameState.Playing) {
        if (config.isCustom) return
        // Never persist a board that is already fully cleared; victory is
        // recorded separately and nothing should be resumable at that point.
        if (state.removedIds.size >= state.level.arrowCount) return
        progressRepository.save(
            SavedGame(
                config = state.level.config,
                levelArrowCount = state.level.arrowCount,
                removedIds = state.removedIds,
                failures = state.failures,
                elapsedSeconds = elapsedSeconds(),
                savedAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    /**
     * Custom mode with seconds per arrow: the limit follows the real board,
     * which can hold more arrows than requested. The level carries the result,
     * so the game and the result screens use the same limit.
     */
    private fun withTimeLimit(level: GeneratedLevel): GeneratedLevel {
        val perArrow = level.config.secondsPerArrow ?: return level
        return level.copy(
            config = level.config.copy(timeLimitSeconds = level.arrowCount * perArrow),
        )
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(TIMER_TICK_MILLIS)
                val pendingDefeat = synchronized(clickLock) {
                    val current = _state.value as? GameState.Playing
                        ?: return@synchronized null
                    val elapsed = elapsedSeconds()
                    val limit = current.level.config.timeLimitSeconds
                    if (limit != null && elapsed >= limit) {
                        _state.value = GameState.Defeat(
                            level = current.level,
                            reason = DefeatReason.TIME_EXPIRED,
                            failures = current.failures,
                            elapsedSeconds = elapsed,
                        )
                        defeatFeedback()
                    } else {
                        _elapsed.value = elapsed
                        null
                    }
                }
                if (pendingDefeat != null) {
                    _feedback.emit(pendingDefeat)
                    return@launch
                }
            }
        }
    }

    private fun elapsedSeconds(): Double {
        if (startedNanos == 0L) return 0.0
        val elapsedNanos = (System.nanoTime() - startedNanos).coerceAtLeast(0L)
        return elapsedNanos / NANOS_PER_SECOND
    }

    private fun correctFeedback(): GameFeedback {
        val settings = settingsRepository.settings.value
        return GameFeedback.Correct(settings.hapticEnabled, settings.soundEnabled)
    }

    private fun wrongFeedback(): GameFeedback {
        val settings = settingsRepository.settings.value
        return GameFeedback.Wrong(settings.hapticEnabled, settings.soundEnabled)
    }

    private fun victoryFeedback(): GameFeedback {
        val settings = settingsRepository.settings.value
        return GameFeedback.Victory(settings.hapticEnabled, settings.soundEnabled)
    }

    private fun defeatFeedback(): GameFeedback {
        val settings = settingsRepository.settings.value
        return GameFeedback.Defeat(settings.hapticEnabled, settings.soundEnabled)
    }

    override fun onCleared() {
        timerJob?.cancel()
        finishJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val TIMER_TICK_MILLIS = 100L
        const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}