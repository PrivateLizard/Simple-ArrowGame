package com.arrowpuzzle.game.scoring

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreCalculatorTest {
    @Test
    fun perfectLevelOneMatchesSpecificationExample() {
        val score = ScoreCalculator.calculate(
            arrowCount = 6,
            timeElapsedSeconds = 25.0,
            failures = 0,
        )
        // Window 6 * 7 = 42 s, 17 s left.
        assertEquals(60, score.basePoints)
        assertEquals(85, score.timeBonus)
        assertEquals(30, score.comboBonus)
        assertEquals(175, score.total)
    }

    @Test
    fun timeWindowGrowsWithArrowCount() {
        // 200 arrows: window 1400 s, 400 s used, 1000 s left.
        val score = ScoreCalculator.calculate(
            arrowCount = 200,
            timeElapsedSeconds = 400.9,
            failures = 1,
        )
        assertEquals(5000, score.timeBonus)
        assertEquals(0, ScoreCalculator.calculate(200, 1400.0, 0).timeBonus)
    }

    @Test
    fun timeBonusNeverGoesBelowZero() {
        val score = ScoreCalculator.calculate(
            arrowCount = 10,
            timeElapsedSeconds = 180.0,
            failures = 2,
        )
        assertEquals(100, score.basePoints)
        assertEquals(0, score.timeBonus)
        assertEquals(0, score.comboBonus)
        assertEquals(100, score.total)
    }

    @Test
    fun comboBonusIsOnlyAppliedWithoutFailures() {
        val withFailure = ScoreCalculator.calculate(8, 10.0, 1)
        val perfect = ScoreCalculator.calculate(8, 10.0, 0)
        assertEquals(0, withFailure.comboBonus)
        assertEquals(40, perfect.comboBonus)
        assertEquals(40, perfect.total - withFailure.total)
    }
}
