package com.arrowpuzzle.game

import android.app.Application
import com.arrowpuzzle.game.data.AppContainer

class ArrowPuzzleApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // Warm up the sound samples in the background so the very first tap of
        // the very first level already has a loaded cue. Cheap: four short
        // synthesised WAVs written to the cache dir once.
        container.soundEffects.preload()
    }
}
