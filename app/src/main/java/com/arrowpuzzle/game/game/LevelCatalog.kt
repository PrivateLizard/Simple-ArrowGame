package com.arrowpuzzle.game.game

import com.arrowpuzzle.game.data.model.AppSettings
import com.arrowpuzzle.game.data.model.ComplexityLevel
import com.arrowpuzzle.game.data.model.LevelConfig
import kotlin.math.roundToInt

/**
 * Default campaign progression.
 *
 * The campaign has [MAX_LEVEL] levels on a [MAX_DIFFICULTY]-step difficulty
 * scale: level `N` sits at difficulty `N`. The first [LEGACY_MAX_LEVEL] levels
 * keep exactly the original curve; levels above that continue it smoothly.
 *
 * Arrow counts grow non-linearly, so the early levels stay approachable while
 * the late ones become dense. Complexity controls how many arrows are open at
 * the start. The very last stretch of the campaign keeps more arrows open on
 * purpose: a single open arrow out of ~150 is close to guessing, and the design
 * goal for the finale is a tangled board that is still solvable by reasoning.
 */
object LevelCatalog {
    const val MAX_LEVEL = 500

    /**
     * Unlock rule for the campaign.
     *
     * Levels unlock sequentially: [INITIALLY_UNLOCKED_LEVELS] levels are open
     * at the start, and clearing a level unlocks the next
     * [LEVELS_UNLOCKED_PER_CLEAR] levels. Only levels up to
     * [lastUnlockedLevel] are playable, so the player never has the whole
     * campaign open at once. Increase the constants to unlock in batches.
     */
    const val INITIALLY_UNLOCKED_LEVELS = 1
    const val LEVELS_UNLOCKED_PER_CLEAR = 1

    /** Highest playable level id for a given progress value. */
    fun lastUnlockedLevel(highestUnlocked: Int): Int =
        highestUnlocked.coerceIn(INITIALLY_UNLOCKED_LEVELS, MAX_LEVEL)

    /** Difficulty values used for the level-difficulty badge and the tuning. */
    const val MAX_DIFFICULTY = MAX_LEVEL

    /** The original 100-level campaign; its curve stays exactly as it was. */
    private const val LEGACY_MAX_LEVEL = 100

    /** Arrow count of the very first level. */
    private const val BASE_ARROWS = 34

    /** Arrow count at the end of the original 100-level curve. */
    private const val ARROWS_AT_LEGACY_MAX = 90

    /**
     * Arrow count at [MAX_LEVEL]. Kept modest on purpose: see
     * [arrowCountForLevel] for why more arrows means easier, not harder.
     */
    private const val FINAL_ARROWS = 160

    fun gridSizeForLevel(levelId: Int): Int {
        val safe = levelId.coerceIn(1, MAX_LEVEL)
        val growth = safe.coerceAtMost(LEGACY_MAX_LEVEL)
        // 3x3 at level 1 up to 8x8, reaching the maximum early enough that the
        // last levels grow in arrow count instead of board size.
        return (3 + (growth - 1) * 5 / 99).coerceIn(3, 8)
    }

    /**
     * Arrow target for a campaign level.
     *
     * More arrows do **not** make a board harder. Measured with a naive player
     * (always tap a random open arrow) the average number of available moves
     * *grows* with arrow count, because a fuller board keeps more independent
     * chains alive. Difficulty comes from the open-arrow count, not from here.
     *
     * Arrow count therefore only controls how big and busy a board looks. It
     * stays roughly flat and is not used as the difficulty knob.
     */
    fun arrowCountForLevel(levelId: Int): Int {
        val safe = levelId.coerceIn(1, MAX_LEVEL)
        if (safe == 1) return BASE_ARROWS
        // Gentle, almost linear growth. A steep ramp would silently *reduce*
        // difficulty, which is the opposite of what a level curve should do.
        // Levels 1..100 keep the original curve; the rest continues from there.
        return if (safe <= LEGACY_MAX_LEVEL) {
            val t = (safe - 1).toDouble() / (LEGACY_MAX_LEVEL - 1)
            (BASE_ARROWS + (ARROWS_AT_LEGACY_MAX - BASE_ARROWS) * t).roundToInt()
        } else {
            val t = (safe - LEGACY_MAX_LEVEL).toDouble() /
                (MAX_LEVEL - LEGACY_MAX_LEVEL)
            (ARROWS_AT_LEGACY_MAX + (FINAL_ARROWS - ARROWS_AT_LEGACY_MAX) * t)
                .roundToInt()
        }
    }

    /**
     * Arrow count the generator actually produces for a campaign level (it adds
     * a few arrows on top of the configured target, capped at the finale).
     * Used for the per-level maximum score shown in the level list.
     */
    fun expectedArrowCountForLevel(levelId: Int): Int {
        val safe = levelId.coerceIn(1, MAX_LEVEL)
        return (arrowCountForLevel(safe) + 6)
            .coerceAtMost(arrowCountForLevel(MAX_LEVEL))
    }

    /** Difficulty of a level on the 1..[MAX_DIFFICULTY] scale. */
    fun difficultyForLevel(levelId: Int): Int =
        levelId.coerceIn(1, MAX_LEVEL)

    /**
     * Complexity tier of a level. This is the real difficulty driver.
     *
     * With many open arrows the player almost always has a safe move, so the
     * board is easy. With two or three open arrows every mistake is a real
     * mistake. HARD deliberately does not go down to a single open arrow: on a
     * 150-arrow board that turns the game into guessing.
     */
    fun complexityForLevel(levelId: Int): ComplexityLevel {
        val safe = levelId.coerceIn(1, MAX_LEVEL)
        return when {
            safe <= 30 -> ComplexityLevel.EASY
            safe <= 70 -> ComplexityLevel.MEDIUM
            else -> ComplexityLevel.HARD
        }
    }

    fun openArrowCountForLevel(levelId: Int): Int = when (complexityForLevel(levelId)) {
        ComplexityLevel.EASY -> 4
        ComplexityLevel.MEDIUM -> 3
        ComplexityLevel.HARD -> 2
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
            maxFailures = if (maxFailures == AppSettings.NO_FAILURE_LIMIT) {
                AppSettings.NO_FAILURE_LIMIT
            } else {
                maxFailures.coerceIn(0, 99)
            },
            timeLimitSeconds = null,
            complexity = complexityForLevel(safeLevel),
        )
    }
}
