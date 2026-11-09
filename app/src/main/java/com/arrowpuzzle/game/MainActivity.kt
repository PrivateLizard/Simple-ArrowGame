package com.arrowpuzzle.game

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.Configuration
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
        applyOrientation(resources.configuration)
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyOrientation(newConfig)
    }

    /**
     * Phones stay in portrait; tablets (smallest width 600 dp and up) follow
     * the device rotation. Android 16 ignores the lock on large screens anyway.
     */
    @SuppressLint("SourceLockedOrientationActivity")
    private fun applyOrientation(config: Configuration) {
        val wanted = if (config.smallestScreenWidthDp >= TABLET_MIN_WIDTH_DP) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        if (requestedOrientation != wanted) requestedOrientation = wanted
    }

    private companion object {
        const val TABLET_MIN_WIDTH_DP = 600
    }
}
