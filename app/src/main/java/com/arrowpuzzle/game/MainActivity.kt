package com.arrowpuzzle.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.arrowpuzzle.game.navigation.AppNavHost
import com.arrowpuzzle.game.ui.theme.ArrowPuzzleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as ArrowPuzzleApplication).container
        setContent {
            val settings by container.settingsRepository.settings.collectAsState()
            ArrowPuzzleTheme(nightMode = settings.nightMode) {
                AppNavHost(container)
            }
        }
    }
}
