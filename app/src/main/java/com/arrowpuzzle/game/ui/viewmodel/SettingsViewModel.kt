package com.arrowpuzzle.game.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.arrowpuzzle.game.data.ScoreRepository
import com.arrowpuzzle.game.data.SettingsRepository
import com.arrowpuzzle.game.data.model.AppSettings
import kotlinx.coroutines.flow.StateFlow


class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val scoreRepository: ScoreRepository,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = settingsRepository.settings

    fun setMaxFailures(value: Int) = settingsRepository.setMaxFailures(value)

    fun setNightMode(enabled: Boolean) = settingsRepository.setNightMode(enabled)

    fun setSoundEnabled(enabled: Boolean) = settingsRepository.setSoundEnabled(enabled)

    fun setHapticEnabled(enabled: Boolean) = settingsRepository.setHapticEnabled(enabled)

    fun resetProgress() = scoreRepository.clearProgress()
}
