package com.arrowpuzzle.game.data

import android.content.Context
import com.arrowpuzzle.game.game.LevelGenerator
import com.arrowpuzzle.game.util.SoundEffects

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /** Created first: a restored backup must be in place before the stores load. */
    private val backupMirror = BackupMirror(appContext)

    val settingsRepository = SettingsRepository(appContext)
    val scoreRepository = ScoreRepository(appContext)
    val progressRepository = ProgressRepository(appContext)
    val levelGenerator = LevelGenerator()

    /**
     * App-scoped so the samples stay loaded across level transitions. Reloading
     * per level made the first tap of every level silent while SoundPool was
     * still decoding.
     */
    val soundEffects = SoundEffects(appContext)
}
