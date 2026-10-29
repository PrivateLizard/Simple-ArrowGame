package com.arrowpuzzle.game

import android.app.Application
import com.arrowpuzzle.game.data.AppContainer

class ArrowPuzzleApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
