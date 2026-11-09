package com.arrowpuzzle.game.util

import com.arrowpuzzle.game.data.model.GridPoint
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

object GridUtils {
    fun index(position: GridPoint, width: Int): Int = position.y * width + position.x

    fun position(index: Int, width: Int): GridPoint = GridPoint(index % width, index / width)

    fun isInside(position: GridPoint, width: Int, height: Int): Boolean =
        position.x in 0 until width && position.y in 0 until height

    fun formatTime(seconds: Double): String {
        val total = seconds.toInt().coerceAtLeast(0)
        return String.format(Locale.US, "%02d:%02d", total / 60, total % 60)
    }

    /**
     * In-level clock: counts the bonus window down to 00:01, then keeps
     * counting below zero (-00:01, -00:02, ...) once the time bonus is lost.
     * The bonus is kept while the whole elapsed seconds stay below the window.
     */
    fun formatBonusClock(elapsedSeconds: Double, windowSeconds: Int): String {
        val elapsed = elapsedSeconds.coerceAtLeast(0.0)
        return if (elapsed < windowSeconds) {
            formatTime(ceil(windowSeconds - elapsed))
        } else {
            "-" + formatTime(floor(elapsed - windowSeconds) + 1)
        }
    }
}
