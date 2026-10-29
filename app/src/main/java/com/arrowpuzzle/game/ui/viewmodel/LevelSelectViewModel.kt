package com.arrowpuzzle.game.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arrowpuzzle.game.data.ScoreRepository
import com.arrowpuzzle.game.data.SettingsRepository
import com.arrowpuzzle.game.data.model.LevelConfig
import com.arrowpuzzle.game.game.LevelCatalog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class LevelItem(
    val levelId: Int,
    val openArrowCount: Int,
    val bestScore: Int,
    val isUnlocked: Boolean,
)

class LevelSelectViewModel(
    scoreRepository: ScoreRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val levels: StateFlow<List<LevelItem>> = combine(
        scoreRepository.bestScores,
        scoreRepository.highestUnlocked,
    ) { bestScores, highestUnlocked ->
        (1..LevelCatalog.MAX_LEVEL).map { levelId ->
            LevelItem(
                levelId = levelId,
                openArrowCount = LevelCatalog.openArrowCountForLevel(levelId),
                bestScore = bestScores[levelId] ?: 0,
                isUnlocked = levelId <= highestUnlocked,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = buildInitialLevels(scoreRepository),
    )

    fun configFor(levelId: Int): LevelConfig = LevelCatalog.defaultConfig(
        levelId = levelId,
        maxFailures = settingsRepository.settings.value.maxFailures,
    )

    private companion object {
        fun buildInitialLevels(scoreRepository: ScoreRepository): List<LevelItem> {
            val best = scoreRepository.bestScores.value
            val unlocked = scoreRepository.highestUnlocked.value
            return (1..LevelCatalog.MAX_LEVEL).map { levelId ->
                LevelItem(
                    levelId = levelId,
                    openArrowCount = LevelCatalog.openArrowCountForLevel(levelId),
                    bestScore = best[levelId] ?: 0,
                    isUnlocked = levelId <= unlocked,
                )
            }
        }
    }
}
