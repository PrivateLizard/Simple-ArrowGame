package com.arrowpuzzle.game.ui.viewmodel

import androidx.lifecycle.ViewModel
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

    /** The level that was left unfinished, if any. Drives the resume button. */
    val savedGame: StateFlow<SavedGame?> = progressRepository.savedGame

    /** True when every campaign level is currently unlocked. */
    fun allLevelsUnlocked(): Boolean = scoreRepository.highestUnlocked.value >= LevelCatalog.MAX_LEVEL

    /** Developer helper: flip between "everything unlocked" and "locked". */
    fun setAllLevelsUnlocked(unlocked: Boolean) {
        scoreRepository.setAllLevelsUnlocked(unlocked, LevelCatalog.MAX_LEVEL)
    }
}
