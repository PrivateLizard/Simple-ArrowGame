package com.arrowpuzzle.game.scoring

import com.arrowpuzzle.game.game.LevelCatalog

/**
 * Theoretical best score of every campaign level (no failures, instant clear).
 * The values never change, so they are computed once per process.
 */
object CampaignMaxScores {
    private val table: IntArray by lazy {
        IntArray(LevelCatalog.MAX_LEVEL + 1) { levelId ->
            if (levelId == 0) {
                0
            } else {
                val arrows = LevelCatalog.expectedArrowCountForLevel(levelId)
                ScoreCalculator.calculate(arrows, 0.0, 0).total
            }
        }
    }

    fun forLevel(levelId: Int): Int =
        if (levelId in 1..LevelCatalog.MAX_LEVEL) table[levelId] else 0

    /** Sum of the maxima of all campaign levels cleared at least once. */
    fun sumOfCleared(bestScores: Map<Int, Int>): Int =
        bestScores.entries.sumOf { (levelId, points) -> if (points > 0) forLevel(levelId) else 0 }
}
