package com.arrowpuzzle.game.scoring

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreCalculatorTest {
    @Test
    fun perfectFastLevelGetsBothBonuses() {
        // 6 arrows: window 42 s, cleared in 25 s without failures.
        val score = ScoreCalculator.calculate(
            arrowCount = 6,
            timeElapsedSeconds = 25.0,
            failures = 0,
        )
        assertEquals(60, score.basePoints)
        assertEquals(6, score.timeBonus)
        assertEquals(12, score.comboBonus)
        assertEquals(78, score.total)
    }

    @Test
    fun noTimeBonusOutsideTheWindow() {
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
    fun timeWindowGrowsWithArrowCount() {
        // 200 arrows: window 1400 s.
        assertEquals(200, ScoreCalculator.calculate(200, 1399.9, 1).timeBonus)
        assertEquals(0, ScoreCalculator.calculate(200, 1400.0, 1).timeBonus)
    }

    @Test
    fun comboBonusIsOnlyAppliedWithoutFailures() {
        val withFailure = ScoreCalculator.calculate(8, 10.0, 1)
        val perfect = ScoreCalculator.calculate(8, 10.0, 0)
        assertEquals(0, withFailure.comboBonus)
        assertEquals(16, perfect.comboBonus)
        assertEquals(16, perfect.total - withFailure.total)
    }
}
