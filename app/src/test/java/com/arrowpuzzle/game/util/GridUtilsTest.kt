package com.arrowpuzzle.game.util

import com.arrowpuzzle.game.scoring.ScoreCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GridUtilsTest {
    @Test
    fun bonusClockCountsDownThenBelowZero() {
        assertEquals("00:42", GridUtils.formatBonusClock(0.0, 42))
        assertEquals("00:42", GridUtils.formatBonusClock(0.4, 42))
        assertEquals("00:01", GridUtils.formatBonusClock(41.9, 42))
        assertEquals("-00:01", GridUtils.formatBonusClock(42.0, 42))
        assertEquals("-00:02", GridUtils.formatBonusClock(43.5, 42))
        assertEquals("-01:18", GridUtils.formatBonusClock(119.0, 42))
    }

    @Test
    fun clockGoesNegativeExactlyWhenTheTimeBonusIsLost() {
        val arrows = 6
        val window = ScoreCalculator.timeWindowSeconds(arrows)
        for (tenths in 0..600) {
            val elapsed = tenths / 10.0
            val negative = GridUtils.formatBonusClock(elapsed, window).startsWith("-")
            val bonus = ScoreCalculator.calculate(arrows, elapsed, 1).timeBonus > 0
            assertTrue("at $elapsed s", negative != bonus)
        }
    }
}
