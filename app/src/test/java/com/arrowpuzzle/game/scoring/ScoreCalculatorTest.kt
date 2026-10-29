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
        assertEquals(60, score.basePoints)
        assertEquals(175, score.timeBonus)
        assertEquals(30, score.comboBonus)
        assertEquals(265, score.total)
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
