package com.arrowpuzzle.game.scoring

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

data class ScoreBreakdown(
    val basePoints: Int,
    val timeBonus: Int,
    val comboBonus: Int,
    val total: Int,
)

/**
 * Scoring rules:
 *   base  = arrows * 10
 *   time  = max(0, arrows * 7 - elapsedSeconds) * 5
 *   combo = base * 0.5 when there were no failures
 *
 * The time window grows with the board, so large levels can earn a time
 * bonus just like small ones.
 */
object ScoreCalculator {
    const val POINTS_PER_ARROW = 10
    const val TIME_WINDOW_SECONDS_PER_ARROW = 7
    const val TIME_BONUS_PER_SECOND = 5
    const val COMBO_BONUS_RATIO = 0.5

    fun calculate(
        arrowCount: Int,
        timeElapsedSeconds: Double,
        failures: Int,
    ): ScoreBreakdown {
        val base = arrowCount * POINTS_PER_ARROW
        val elapsedWhole = floor(max(0.0, timeElapsedSeconds)).toInt()
        val timeWindow = max(0, arrowCount) * TIME_WINDOW_SECONDS_PER_ARROW
        val timeBonus = max(0, timeWindow - elapsedWhole) * TIME_BONUS_PER_SECOND
        val combo = if (failures == 0) (base * COMBO_BONUS_RATIO).roundToInt() else 0
        return ScoreBreakdown(
            basePoints = base,
            timeBonus = timeBonus,
            comboBonus = combo,
            total = base + timeBonus + combo,
        )
    }
}
