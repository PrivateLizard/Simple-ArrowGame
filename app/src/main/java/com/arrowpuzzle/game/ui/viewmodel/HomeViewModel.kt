package com.arrowpuzzle.game.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.arrowpuzzle.game.data.ProgressRepository
import com.arrowpuzzle.game.data.SavedGame
import com.arrowpuzzle.game.data.ScoreRepository
import com.arrowpuzzle.game.game.LevelCatalog
import kotlinx.coroutines.flow.StateFlow

class HomeViewModel(
    private val scoreRepository: ScoreRepository,
    progressRepository: ProgressRepository,
) : ViewModel() {
    /** Sum of every campaign level's best score. */
    val campaignScoreTotal: StateFlow<Int> = scoreRepository.campaignScoreTotal
    val highestUnlocked: StateFlow<Int> = scoreRepository.highestUnlocked

    /** Campaign levels cleared at least once (a best score above zero). */
    val completedLevels: StateFlow<Int> = scoreRepository.bestScores
        .map(::countCompleted)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = countCompleted(scoreRepository.bestScores.value),
        )

    private fun countCompleted(scores: Map<Int, Int>): Int =
        scores.count { (levelId, points) -> levelId in 1..LevelCatalog.MAX_LEVEL && points > 0 }

    /** The level that was left unfinished, if any. Drives the resume button. */
    val savedGame: StateFlow<SavedGame?> = progressRepository.savedGame

    /** True when every campaign level is currently unlocked. */
    fun allLevelsUnlocked(): Boolean = scoreRepository.highestUnlocked.value >= LevelCatalog.MAX_LEVEL

    /** Developer helper: flip between "everything unlocked" and "locked". */
    fun setAllLevelsUnlocked(unlocked: Boolean) {
        scoreRepository.setAllLevelsUnlocked(unlocked, LevelCatalog.MAX_LEVEL)
    }

    /** Developer helper: unlock exactly [count] levels. */
    fun setLevelsUnlocked(count: Int) {
        scoreRepository.setHighestUnlocked(count)
    }
}
