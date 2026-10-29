package com.arrowpuzzle.game.game

import com.arrowpuzzle.game.data.model.AppSettings
import com.arrowpuzzle.game.data.model.ComplexityLevel
import com.arrowpuzzle.game.data.model.LevelConfig
import kotlin.math.roundToInt

/**
 * Default campaign progression.
 *
 * The campaign has [MAX_LEVEL] levels on a 100-step difficulty scale: level
 * `N` sits at difficulty `N`, so level 20 plays like "80 out of 100" and the
 * campaign ends at level 100.
 *
 * Arrow counts grow non-linearly, so the early levels stay approachable while
 * the late ones become dense. Complexity controls how many arrows are open at
 * the start. The very last stretch of the campaign keeps more arrows open on
 * purpose: a single open arrow out of ~150 is close to guessing, and the design
 * goal for the finale is a tangled board that is still solvable by reasoning.
 */
object LevelCatalog {
    const val MAX_LEVEL = 100

    /** Difficulty values used for the level-difficulty badge and the tuning. */
    const val MAX_DIFFICULTY = 100

    /** Arrow count of the very first level. */
    private const val BASE_ARROWS = 34

    /** Arrow count at [MAX_LEVEL]. */
    private const val FINAL_ARROWS = 150

    fun gridSizeForLevel(levelId: Int): Int {
        val safe = levelId.coerceIn(1, MAX_LEVEL)
        // 3x3 at level 1 up to 8x8, reaching the maximum early enough that the
        // last levels grow in arrow count instead of board size.
        return (3 + (safe - 1) * 5 / 99).coerceIn(3, 8)
    }

    /**
     * Arrow target for a campaign level.
     *
     * Grows from [BASE_ARROWS] to [FINAL_ARROWS] with a slight ease-in curve,
     * so the step between neighbouring levels stays small and readable.
     */
    fun arrowCountForLevel(levelId: Int): Int {
        val safe = levelId.coerceIn(1, MAX_LEVEL)
        if (safe == 1) return BASE_ARROWS
        val t = (safe - 1).toDouble() / (MAX_LEVEL - 1)
        val eased = t * t * (3.0 - 2.0 * t) * 0.35 + t * 0.65
        return (BASE_ARROWS + (FINAL_ARROWS - BASE_ARROWS) * eased).roundToInt()
    }

    /** Difficulty of a level on the 1..[MAX_DIFFICULTY] scale. */
    fun difficultyForLevel(levelId: Int): Int =
        levelId.coerceIn(1, MAX_LEVEL)

    /**
     * Complexity tier of a level.
     *
     * EASY (4 open) for the introduction, MEDIUM (2 open) for the middle, and
     * HARD (3 open) for the finale. HARD intentionally does not use a single
     * open arrow any more: see the class comment.
     */
    fun complexityForLevel(levelId: Int): ComplexityLevel {
        val safe = levelId.coerceIn(1, MAX_LEVEL)
        return when {
            safe <= 20 -> ComplexityLevel.EASY
            safe <= 60 -> ComplexityLevel.MEDIUM
            else -> ComplexityLevel.HARD
        }
    }

    fun openArrowCountForLevel(levelId: Int): Int = when (complexityForLevel(levelId)) {
        ComplexityLevel.EASY -> 4
        ComplexityLevel.MEDIUM -> 2
        ComplexityLevel.HARD -> 3
    }

    fun defaultConfig(
        levelId: Int,
        maxFailures: Int = AppSettings.DEFAULT_MAX_FAILURES,
    ): LevelConfig {
        val safeLevel = levelId.coerceIn(1, MAX_LEVEL)
        val size = gridSizeForLevel(safeLevel)
        return LevelConfig(
            levelId = safeLevel,
            gridSize = size to size,
            arrowCount = arrowCountForLevel(safeLevel),
            maxFailures = maxFailures.coerceIn(1, 99),
            timeLimitSeconds = null,
            complexity = complexityForLevel(safeLevel),
        )
    }
}
