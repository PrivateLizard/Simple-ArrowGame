package com.arrowpuzzle.game.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arrowpuzzle.game.data.ScoreRepository
import com.arrowpuzzle.game.data.SettingsRepository
import com.arrowpuzzle.game.data.model.LevelConfig
import com.arrowpuzzle.game.game.LevelCatalog
import com.arrowpuzzle.game.scoring.ScoreCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class LevelItem(
    val levelId: Int,
    val openArrowCount: Int,
    val bestScore: Int,
    /** Theoretical best score for this level (no failures, instant clear). */
    val maxScore: Int,
    val isUnlocked: Boolean,
)

/** Maximum scores never change, so they are computed once per process. */
private val maxScores: IntArray by lazy {
    IntArray(LevelCatalog.MAX_LEVEL + 1) { levelId ->
        if (levelId == 0) {
            0
        } else {
            val arrows = LevelCatalog.expectedArrowCountForLevel(levelId)
            ScoreCalculator.calculate(arrows, 0.0, 0).total
        }
    }
}

private fun buildLevels(bestScores: Map<Int, Int>, highestUnlocked: Int): List<LevelItem> =
    (1..LevelCatalog.visibleLevelCount(highestUnlocked)).map { levelId ->
        LevelItem(
            levelId = levelId,
            openArrowCount = LevelCatalog.openArrowCountForLevel(levelId),
            bestScore = bestScores[levelId] ?: 0,
            maxScore = maxScores[levelId],
            isUnlocked = levelId <= highestUnlocked,
        )
    }

class LevelSelectViewModel(
    scoreRepository: ScoreRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val levels: StateFlow<List<LevelItem>> = combine(
        scoreRepository.bestScores,
        scoreRepository.highestUnlocked,
    ) { bestScores, highestUnlocked ->
        buildLevels(bestScores, highestUnlocked)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = buildLevels(
            scoreRepository.bestScores.value,
            scoreRepository.highestUnlocked.value,
        ),
    )

    fun configFor(levelId: Int): LevelConfig = LevelCatalog.defaultConfig(
        levelId = levelId,
        maxFailures = settingsRepository.settings.value.maxFailures,
    )
}
