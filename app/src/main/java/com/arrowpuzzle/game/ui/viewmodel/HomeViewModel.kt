package com.arrowpuzzle.game.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.arrowpuzzle.game.data.ScoreRepository
import com.arrowpuzzle.game.game.LevelCatalog
import kotlinx.coroutines.flow.StateFlow

class HomeViewModel(private val scoreRepository: ScoreRepository) : ViewModel() {
    val globalHighScore: StateFlow<Int> = scoreRepository.globalHighScore
    val highestUnlocked: StateFlow<Int> = scoreRepository.highestUnlocked

    /** True when every campaign level is currently unlocked. */
    fun allLevelsUnlocked(): Boolean = scoreRepository.highestUnlocked.value >= LevelCatalog.MAX_LEVEL

    /** Developer helper: flip between "everything unlocked" and "locked". */
    fun setAllLevelsUnlocked(unlocked: Boolean) {
        scoreRepository.setAllLevelsUnlocked(unlocked, LevelCatalog.MAX_LEVEL)
    }
}
