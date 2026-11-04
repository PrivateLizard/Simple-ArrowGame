package com.arrowpuzzle.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.arrowpuzzle.game.i18n.AppStrings
import com.arrowpuzzle.game.i18n.LocalStrings
import com.arrowpuzzle.game.i18n.resolveLanguage
import com.arrowpuzzle.game.navigation.AppNavHost
import com.arrowpuzzle.game.ui.theme.ArrowPuzzleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Draw behind the status and navigation bars (mandatory from target SDK
        // 35). ScreenBackground keeps the content clear of them.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as ArrowPuzzleApplication).container
        setContent {
            val settings by container.settingsRepository.settings.collectAsState()
            val language = remember(settings.language) { resolveLanguage(settings.language) }
            val strings = remember(language) { AppStrings(language) }
            CompositionLocalProvider(LocalStrings provides strings) {
                ArrowPuzzleTheme(nightMode = settings.nightMode) {
                    AppNavHost(container)
                }
            }
        }
    }
}
