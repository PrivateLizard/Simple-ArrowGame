package com.arrowpuzzle.game.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.arrowpuzzle.game.data.ScoreRepository
import kotlinx.coroutines.flow.StateFlow

class HomeViewModel(scoreRepository: ScoreRepository) : ViewModel() {
    val globalHighScore: StateFlow<Int> = scoreRepository.globalHighScore
    val highestUnlocked: StateFlow<Int> = scoreRepository.highestUnlocked
}
