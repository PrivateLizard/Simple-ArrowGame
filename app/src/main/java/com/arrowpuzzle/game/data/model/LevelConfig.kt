package com.arrowpuzzle.game.data.model

enum class ComplexityLevel {
    /** Four arrows are open at the start. */
    EASY,

    /** Three arrows are open at the start. */
    MEDIUM,

    /** Two arrows are open at the start. */
    HARD,
}

/**
 * Complete description of a generated or custom level.
 *
 * A [levelId] of [CUSTOM_LEVEL_ID] marks a custom-mode run. [timeLimitSeconds]
 * being null means there is no time limit. With [secondsPerArrow] set, the
 * limit is that many seconds per arrow of the generated board (the board can
 * hold more arrows than [arrowCount]), worked out once the board exists.
 */
data class LevelConfig(
    val levelId: Int,
    val gridSize: Pair<Int, Int>,
    val arrowCount: Int,
    val maxFailures: Int,
    val timeLimitSeconds: Int?,
    val complexity: ComplexityLevel,
    val obstacleCount: Int = 0,
    /** Test mode: id of a [com.arrowpuzzle.game.game.shapes.LevelShape], null = full square. */
    val shapeId: String? = null,
    /** Test mode: number of cells the shape should hold (board size follows from it). */
    val shapeCells: Int = 0,
    /** Custom mode: seconds per arrow, null = no per-arrow limit. */
    val secondsPerArrow: Int? = null,
) {
    val gridWidth: Int get() = gridSize.first
    val gridHeight: Int get() = gridSize.second
    val isCustom: Boolean get() = levelId == CUSTOM_LEVEL_ID
    val isShaped: Boolean get() = shapeId != null

    companion object {
        const val CUSTOM_LEVEL_ID = 0
    }
}
