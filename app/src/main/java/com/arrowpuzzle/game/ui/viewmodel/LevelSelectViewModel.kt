package com.arrowpuzzle.game.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arrowpuzzle.game.data.ScoreRepository
import com.arrowpuzzle.game.data.SettingsRepository
import com.arrowpuzzle.game.data.model.LevelConfig
import com.arrowpuzzle.game.game.LevelCatalog
import com.arrowpuzzle.game.scoring.CampaignMaxScores
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
    /** Picture level (board cut into a shape); marked with a star in the list. */
    val isPictureLevel: Boolean = false,
)

private fun buildLevels(bestScores: Map<Int, Int>, highestUnlocked: Int): List<LevelItem> =
    (1..LevelCatalog.visibleLevelCount(highestUnlocked)).map { levelId ->
        LevelItem(
            levelId = levelId,
            openArrowCount = LevelCatalog.openArrowCountForLevel(levelId),
            bestScore = bestScores[levelId] ?: 0,
            maxScore = CampaignMaxScores.forLevel(levelId),
            isUnlocked = levelId <= highestUnlocked,
            isPictureLevel = levelId % LevelCatalog.PICTURE_LEVEL_INTERVAL == 0,
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
