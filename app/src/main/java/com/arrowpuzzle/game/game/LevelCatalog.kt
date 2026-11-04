package com.arrowpuzzle.game.game

import com.arrowpuzzle.game.data.model.AppSettings
import com.arrowpuzzle.game.data.model.ComplexityLevel
import com.arrowpuzzle.game.data.model.LevelConfig
import com.arrowpuzzle.game.game.shapes.LevelShape
import com.arrowpuzzle.game.game.shapes.ShapeSize
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Default campaign progression.
 *
 * The campaign has [MAX_LEVEL] levels on a [MAX_DIFFICULTY]-step difficulty
 * scale: level `N` sits at difficulty `N`. The first [LEGACY_MAX_LEVEL] levels
 * keep exactly the original curve; levels above that continue it smoothly.
 *
 * Arrow counts grow in stages, so the early levels stay approachable while the
 * late ones become dense. Complexity controls how many arrows are open at the
 * start. Late HARD levels keep exactly two open arrows: a single open arrow on
 * a board with up to 240 arrows would turn the finale into guessing.
 *
 * Every [PICTURE_LEVEL_INTERVAL]th level is a picture level. With 200 shapes each
 * block of 1000 levels shows every shape exactly once, in small, medium and
 * large from block to block.
 */
object LevelCatalog {
    const val MAX_LEVEL = 3000

    /** Every n-th campaign level is a picture level (a board cut into a shape). */
    const val PICTURE_LEVEL_INTERVAL = 5

    /** Picture size steps up after this many levels: small, medium, then large. */
    private const val PICTURE_TIER_LEVELS = 1000

    /**
     * The difficulty curve was designed for this many levels. The campaign
     * stretches it: level [MAX_LEVEL] is as hard as design level
     * [CURVE_DESIGN_LEVELS].
     */
    private const val CURVE_DESIGN_LEVELS = 2000

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

    /**
     * Number of tiles the level list shows: all unlocked levels plus the locked
     * ones up to the next full hundred. This keeps the list short instead of
     * rendering thousands of locked entries at once.
     */
    fun visibleLevelCount(highestUnlocked: Int): Int {
        val safe = highestUnlocked.coerceIn(1, MAX_LEVEL)
        return (((safe / LEVEL_BLOCK) + 1) * LEVEL_BLOCK)
            .coerceAtMost(MAX_LEVEL)
    }

    private const val LEVEL_BLOCK = 100

    /** Difficulty values used for the level-difficulty badge and the tuning. */
    const val MAX_DIFFICULTY = MAX_LEVEL

    /** The legacy 100-level segment; its curve stays exactly as it was. */
    private const val LEGACY_MAX_LEVEL = 100

    /**
     * Level at which the first arrow-count ramp ends ([FINAL_ARROWS]). Above
     * this the late ramp grows to [LATE_FINAL_ARROWS] so the huge boards keep
     * shorter arrows.
     */
    private const val CURVE_MAX_LEVEL = 500

    /** Arrow count of the very first level. */
    private const val BASE_ARROWS = 34

    /** Arrow count at the end of the legacy 100-level segment. */
    private const val ARROWS_AT_LEGACY_MAX = 90

    /** Arrow count at [CURVE_MAX_LEVEL] (start of the late ramp). */
    private const val FINAL_ARROWS = 160

    /** Late-game density: more, shorter arrows for the huge boards. */
    private const val LATE_FINAL_ARROWS = 240

    /** Position of a campaign level on the design curve (1..[CURVE_DESIGN_LEVELS]). */
    private fun curveLevel(levelId: Int): Int {
        val safe = levelId.coerceIn(1, MAX_LEVEL)
        val t = (safe - 1).toDouble() / (MAX_LEVEL - 1)
        return 1 + (t * (CURVE_DESIGN_LEVELS - 1)).roundToInt()
    }

    /**
     * Fixed mixed order of the shapes, so neighbouring picture levels differ.
     * Adding a shape reorders it: raise [LevelGenerator.VERSION] when that happens.
     */
    private val pictureOrder: List<LevelShape> by lazy {
        LevelShape.entries.shuffled(Random(PICTURE_ORDER_SEED))
    }

    private const val PICTURE_ORDER_SEED = 20_261_104

    /** Shape of a picture level, or null for a normal square level. */
    fun pictureShapeForLevel(levelId: Int): LevelShape? {
        if (levelId < 1 || levelId > MAX_LEVEL || levelId % PICTURE_LEVEL_INTERVAL != 0) return null
        return pictureOrder[(levelId / PICTURE_LEVEL_INTERVAL - 1) % pictureOrder.size]
    }

    fun pictureSizeForLevel(levelId: Int): ShapeSize = when {
        levelId <= PICTURE_TIER_LEVELS -> ShapeSize.SMALL
        levelId <= PICTURE_TIER_LEVELS * 2 -> ShapeSize.MEDIUM
        else -> ShapeSize.LARGE
    }

    fun gridSizeForLevel(levelId: Int): Int {
        val safe = curveLevel(levelId)
        val growth = safe.coerceAtMost(LEGACY_MAX_LEVEL)
        // 3x3 at level 1 up to 8x8 at level 100. Levels above that widen the
        // point lattice (see [layoutSizeForLevel]) instead of this label.
        return (3 + (growth - 1) * 5 / 99).coerceIn(3, 8)
    }

    /**
     * Side length of the point lattice the arrows are routed on.
     *
     * The size is rounded up to an even number because the generator builds the
     * Hamiltonian cycle from 2x2 blocks. Design levels 1..100 stay at 20..34;
     * above that the field grows by one row/column every 50 design levels, so
     * the late campaign keeps opening up. The last level ends at 72x72.
     */
    fun layoutSizeForLevel(levelId: Int): Int {
        val safe = curveLevel(levelId)
        val legacy = 10 + gridSizeForLevel(levelId) * 3
        val raw = if (safe <= LEGACY_MAX_LEVEL) {
            legacy
        } else {
            legacy + (safe - LEGACY_MAX_LEVEL) / 50
        }
        return if (raw % 2 == 0) raw else raw + 1
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
        val safe = curveLevel(levelId)
        if (safe == 1) return BASE_ARROWS
        // Gentle, almost linear growth. A steep ramp would silently *reduce*
        // difficulty, which is the opposite of what a level curve should do.
        // Levels 1..100 keep the original curve; the rest continues from there.
        return if (safe <= LEGACY_MAX_LEVEL) {
            val t = (safe - 1).toDouble() / (LEGACY_MAX_LEVEL - 1)
            (BASE_ARROWS + (ARROWS_AT_LEGACY_MAX - BASE_ARROWS) * t).roundToInt()
        } else if (safe <= CURVE_MAX_LEVEL) {
            val t = (safe - LEGACY_MAX_LEVEL).toDouble() /
                (CURVE_MAX_LEVEL - LEGACY_MAX_LEVEL)
            (ARROWS_AT_LEGACY_MAX + (FINAL_ARROWS - ARROWS_AT_LEGACY_MAX) * t)
                .roundToInt()
        } else {
            // Levels 501..MAX_LEVEL get more, shorter arrows so the huge
            // boards do not end up with a few enormous self-coiled arrows.
            val t = (safe - CURVE_MAX_LEVEL).toDouble() /
                (CURVE_DESIGN_LEVELS - CURVE_MAX_LEVEL)
            (FINAL_ARROWS + (LATE_FINAL_ARROWS - FINAL_ARROWS) * t).roundToInt()
        }
    }

    /**
     * Arrow count the generator actually produces for a campaign level (it adds
     * a few arrows on top of the configured target, capped at the finale).
     * Used for the per-level maximum score shown in the level list.
     */
    fun expectedArrowCountForLevel(levelId: Int): Int {
        val safe = levelId.coerceIn(1, MAX_LEVEL)
        // A fixed count per size: the level list needs this for all levels at
        // once and must not build a shape mask for each of them.
        if (pictureShapeForLevel(safe) != null) return pictureSizeForLevel(safe).arrowCount
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
     * board is easy. With two (HARD) or three (MEDIUM) open arrows every mistake
     * is a real mistake. HARD deliberately does not go down to a single open
     * arrow: on a board with up to 240 arrows that would turn the game into
     * guessing.
     */
    fun complexityForLevel(levelId: Int): ComplexityLevel {
        val safe = curveLevel(levelId)
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
        val shape = pictureShapeForLevel(safeLevel)
        val pictureSize = pictureSizeForLevel(safeLevel)
        return LevelConfig(
            levelId = safeLevel,
            gridSize = size to size,
            arrowCount = if (shape != null) pictureSize.arrowCount else arrowCountForLevel(safeLevel),
            maxFailures = if (maxFailures == AppSettings.NO_FAILURE_LIMIT) {
                AppSettings.NO_FAILURE_LIMIT
            } else {
                maxFailures.coerceIn(0, 99)
            },
            timeLimitSeconds = null,
            complexity = complexityForLevel(safeLevel),
            shapeId = shape?.id,
            shapeCells = if (shape != null) pictureSize.targetCells else 0,
        )
    }
}
