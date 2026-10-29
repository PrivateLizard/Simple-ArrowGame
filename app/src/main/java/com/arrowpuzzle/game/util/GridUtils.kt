package com.arrowpuzzle.game.util

import com.arrowpuzzle.game.data.model.GridPoint
import java.util.Locale

object GridUtils {
    fun index(position: GridPoint, width: Int): Int = position.y * width + position.x

    fun position(index: Int, width: Int): GridPoint = GridPoint(index % width, index / width)

    fun isInside(position: GridPoint, width: Int, height: Int): Boolean =
        position.x in 0 until width && position.y in 0 until height

    fun formatTime(seconds: Double): String {
        val total = seconds.toInt().coerceAtLeast(0)
        return String.format(Locale.US, "%02d:%02d", total / 60, total % 60)
    }
}
