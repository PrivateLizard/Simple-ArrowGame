package com.arrowpuzzle.game.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arrowpuzzle.game.data.ScoreRepository
import com.arrowpuzzle.game.data.SettingsRepository
import com.arrowpuzzle.game.data.model.LevelConfig
import com.arrowpuzzle.game.data.model.ScoreRecord
import com.arrowpuzzle.game.game.ARROW_EXIT_ANIMATION_MILLIS
import com.arrowpuzzle.game.game.ArrowLogic
import com.arrowpuzzle.game.game.DefeatReason
import com.arrowpuzzle.game.game.GameFeedback
import com.arrowpuzzle.game.game.GameState
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
) : ViewModel() {
    private val clickLock = Any()
    private val _state = MutableStateFlow<GameState>(GameState.Loading)
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _feedback = MutableSharedFlow<GameFeedback>(extraBufferCapacity = 12)
    val feedback: SharedFlow<GameFeedback> = _feedback.asSharedFlow()

    private var timerJob: Job? = null
    private var finishJob: Job? = null
    private var startedNanos = 0L

    init {
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                val seed = if (config.isCustom) {
                    System.nanoTime()
                } else {
                    1_000_003L * config.levelId
                }
                runCatching { levelGenerator.generate(config, seed) }
            }
            result.onSuccess { level ->
                synchronized(clickLock) {
                    startedNanos = System.nanoTime()
                    _state.value = GameState.Playing(
                        level = level,
                        removedIds = emptySet(),
                        failures = 0,
                        elapsedSeconds = 0.0,
                    )
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

        synchronized(clickLock) {
            val current = _state.value as? GameState.Playing ?: return@synchronized
            val limit = config.timeLimitSeconds
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
                        _state.value = current.copy(
                            removedIds = removedIds,
                            failures = current.failures,
                            elapsedSeconds = elapsed,
                            correctFlashId = arrowId,
                            correctFlashToken = current.correctFlashToken + 1,
                            wrongFlashId = null,
                        )
                        finishJob?.cancel()
                        finishJob = viewModelScope.launch {
                            delay(ARROW_EXIT_ANIMATION_MILLIS + 40L)
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
                        feedback = defeatFeedback()
                    } else {
                        _state.value = current.copy(
                            failures = result.failures,
                            wrongFlashId = arrowId,
                            wrongFlashToken = current.wrongFlashToken + 1,
                            correctFlashId = null,
                        )
                        feedback = wrongFeedback()
                    }
                }

                ArrowLogic.ClickResult.AlreadyRemoved,
                ArrowLogic.ClickResult.Ignored,
                -> Unit
            }
        }

        feedback?.let(_feedback::tryEmit)
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
                    val limit = config.timeLimitSeconds
                    if (limit != null && elapsed >= limit) {
                        _state.value = GameState.Defeat(
                            level = current.level,
                            reason = DefeatReason.TIME_EXPIRED,
                            failures = current.failures,
                            elapsedSeconds = elapsed,
                        )
                        defeatFeedback()
                    } else {
                        _state.value = current.copy(elapsedSeconds = elapsed)
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