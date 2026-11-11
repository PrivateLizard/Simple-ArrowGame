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

    /** Most points the whole campaign can give: every level perfect. */
    val total: Int by lazy { table.sum() }
}
