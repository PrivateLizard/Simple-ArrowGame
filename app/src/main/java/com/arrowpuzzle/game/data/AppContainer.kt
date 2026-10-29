package com.arrowpuzzle.game.data

import android.content.Context
import com.arrowpuzzle.game.game.LevelGenerator

class AppContainer(context: Context) {
    val settingsRepository = SettingsRepository(context)
    val scoreRepository = ScoreRepository(context)
    val levelGenerator = LevelGenerator()
}
