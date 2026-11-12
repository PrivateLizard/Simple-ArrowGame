package com.arrowpuzzle.game.data

import android.content.Context
import androidx.core.content.edit
import com.arrowpuzzle.game.data.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val lock = Any()
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun setMaxFailures(value: Int) = update {
        it.copy(maxFailures = normalizeMaxFailures(value))
    }

    fun setNightMode(enabled: Boolean) = update {
        it.copy(nightMode = enabled)
    }

    fun setSoundEnabled(enabled: Boolean) = update {
        it.copy(soundEnabled = enabled)
    }

    fun setHapticEnabled(enabled: Boolean) = update {
        it.copy(hapticEnabled = enabled)
    }

    fun setHintsEnabled(enabled: Boolean) = update {
        it.copy(hintsEnabled = enabled)
    }

    fun setLanguage(code: String) = update {
        it.copy(language = code)
    }

    fun setDevModeEnabled(enabled: Boolean) = update {
        it.copy(devModeEnabled = enabled)
    }

    fun setDevWinButtons(enabled: Boolean) = update {
        it.copy(devWinButtons = enabled)
    }

    fun setDevHomeMaxPoints(enabled: Boolean) = update {
        it.copy(devHomeMaxPoints = enabled)
    }

    /** First start: store the chosen theme and never ask again. */
    fun completeFirstRun(nightMode: Boolean) = update {
        it.copy(nightMode = nightMode, firstRunDone = true)
    }

    fun setBackupEnabled(enabled: Boolean) = update {
        it.copy(backupEnabled = enabled)
    }

    fun markCustomWarningSeen() = update {
        it.copy(customWarningSeen = true)
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        synchronized(lock) {
            val updated = transform(_settings.value)
            preferences.edit {
                putInt(KEY_MAX_FAILURES, updated.maxFailures)
                putBoolean(KEY_NIGHT_MODE, updated.nightMode)
                putBoolean(KEY_SOUND, updated.soundEnabled)
                putBoolean(KEY_HAPTICS, updated.hapticEnabled)
                putBoolean(KEY_HINTS, updated.hintsEnabled)
                putString(KEY_LANGUAGE, updated.language)
                putBoolean(KEY_DEV_MODE, updated.devModeEnabled)
                putBoolean(KEY_DEV_WIN_BUTTONS, updated.devWinButtons)
                putBoolean(KEY_DEV_HOME_MAX, updated.devHomeMaxPoints)
                putBoolean(KEY_FIRST_RUN_DONE, updated.firstRunDone)
                putBoolean(KEY_CUSTOM_WARNING_SEEN, updated.customWarningSeen)
                putBoolean(KEY_BACKUP, updated.backupEnabled)
            }
            _settings.value = updated
        }
    }

    private fun load(): AppSettings = AppSettings(
        maxFailures = normalizeMaxFailures(
            preferences.getInt(KEY_MAX_FAILURES, AppSettings.DEFAULT_MAX_FAILURES),
        ),
        nightMode = preferences.getBoolean(KEY_NIGHT_MODE, true),
        soundEnabled = preferences.getBoolean(KEY_SOUND, true),
        hapticEnabled = preferences.getBoolean(KEY_HAPTICS, true),
        hintsEnabled = preferences.getBoolean(KEY_HINTS, true),
        language = preferences.getString(KEY_LANGUAGE, "") ?: "",
        devModeEnabled = preferences.getBoolean(KEY_DEV_MODE, false),
        devWinButtons = preferences.getBoolean(KEY_DEV_WIN_BUTTONS, false),
        devHomeMaxPoints = preferences.getBoolean(KEY_DEV_HOME_MAX, false),
        // Settings written by an older build mean the app was used before:
        // no first-start screen for those players.
        firstRunDone = preferences.getBoolean(KEY_FIRST_RUN_DONE, preferences.contains(KEY_NIGHT_MODE)),
        customWarningSeen = preferences.getBoolean(KEY_CUSTOM_WARNING_SEEN, false),
        backupEnabled = preferences.getBoolean(KEY_BACKUP, false),
    )

    private fun normalizeMaxFailures(value: Int): Int =
        if (value == AppSettings.NO_FAILURE_LIMIT) {
            AppSettings.NO_FAILURE_LIMIT
        } else {
            value.coerceIn(0, 99)
        }

    companion object {
        const val PREFERENCES_NAME = "arrow_puzzle_settings"
        private const val KEY_MAX_FAILURES = "max_failures"
        private const val KEY_NIGHT_MODE = "night_mode"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_HAPTICS = "haptics_enabled"
        private const val KEY_HINTS = "hints_enabled"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_DEV_MODE = "dev_mode_enabled"
        private const val KEY_DEV_WIN_BUTTONS = "dev_win_buttons"
        private const val KEY_DEV_HOME_MAX = "dev_home_max_points"
        private const val KEY_FIRST_RUN_DONE = "first_run_done"
        private const val KEY_CUSTOM_WARNING_SEEN = "custom_warning_seen"
        const val KEY_BACKUP = "backup_enabled"
    }
}
