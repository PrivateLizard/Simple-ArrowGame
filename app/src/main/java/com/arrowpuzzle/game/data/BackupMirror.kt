package com.arrowpuzzle.game.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Optional backup of progress and settings.
 *
 * Android's backup only ever sees one file, [BACKUP_NAME] (see
 * res/xml/backup_rules.xml and data_extraction_rules.xml). While the player
 * has backup switched off that file is empty, so nothing leaves the device.
 * While it is on, every change of scores or settings copies the best scores,
 * the unlocked level and the settings into it (developer options and the
 * unfinished level stay out), so it is current whenever Android backs up.
 *
 * After a reinstall or on a new phone Android puts the file back before the
 * app first starts; it is then copied into the real stores. Create this
 * before the repositories so they load the restored data.
 */
class BackupMirror(context: Context) {
    private val appContext = context.applicationContext
    private val backup = prefs(BACKUP_NAME)
    private val scores = prefs(ScoreRepository.PREFERENCES_NAME)
    private val settings = prefs(SettingsRepository.PREFERENCES_NAME)

    init {
        restoreIfFresh()
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> sync() }
        // SharedPreferences holds listeners only weakly, and R8 drops fields
        // nobody reads, so a field here would not keep it alive. The static
        // list does, for the lifetime of the process.
        liveListeners += listener
        scores.registerOnSharedPreferenceChangeListener(listener)
        settings.registerOnSharedPreferenceChangeListener(listener)
    }

    /**
     * Copies a restored backup into the score and settings stores when both
     * are still empty (fresh install).
     */
    private fun restoreIfFresh() {
        if (scores.all.isNotEmpty() || settings.all.isNotEmpty()) return
        val saved = backup.all
        if (saved[KEY_FORMAT] == null) return
        scores.edit(commit = true) { copyIn(saved, SCORE_PREFIX) }
        settings.edit(commit = true) {
            copyIn(saved, SETTINGS_PREFIX)
            // Restored from a backup, so the player had it switched on.
            putBoolean(SettingsRepository.KEY_BACKUP, true)
        }
    }

    /**
     * Fills the backup file while the backup switch is on, empties it
     * otherwise. Written with apply(): Android finishes pending writes before
     * the app stops and before a backup runs.
     */
    private fun sync() {
        backup.edit {
            clear()
            if (!settings.getBoolean(SettingsRepository.KEY_BACKUP, false)) return@edit
            putInt(KEY_FORMAT, FORMAT_VERSION)
            copyOut(scores.all, SCORE_PREFIX) { true }
            copyOut(settings.all, SETTINGS_PREFIX) { !it.startsWith(DEV_KEY_PREFIX) }
        }
    }

    private fun prefs(name: String): SharedPreferences =
        appContext.getSharedPreferences(name, Context.MODE_PRIVATE)

    private fun SharedPreferences.Editor.copyOut(
        source: Map<String, *>,
        prefix: String,
        keep: (String) -> Boolean,
    ) {
        source.forEach { (key, value) ->
            if (keep(key)) put("$prefix$key", value)
        }
    }

    private fun SharedPreferences.Editor.copyIn(source: Map<String, *>, prefix: String) {
        source.forEach { (key, value) ->
            if (key.startsWith(prefix)) put(key.removePrefix(prefix), value)
        }
    }

    private fun SharedPreferences.Editor.put(key: String, value: Any?) {
        when (value) {
            is Int -> putInt(key, value)
            is Long -> putLong(key, value)
            is Boolean -> putBoolean(key, value)
            is Float -> putFloat(key, value)
            is String -> putString(key, value)
        }
    }

    companion object {
        /** Kept in sync with res/xml/backup_rules.xml and data_extraction_rules.xml. */
        const val BACKUP_NAME = "arrow_puzzle_backup"
        private const val KEY_FORMAT = "format"
        private const val FORMAT_VERSION = 1
        private const val SCORE_PREFIX = "score."
        private const val SETTINGS_PREFIX = "settings."
        private const val DEV_KEY_PREFIX = "dev_"
        private val liveListeners = mutableListOf<SharedPreferences.OnSharedPreferenceChangeListener>()
    }
}
