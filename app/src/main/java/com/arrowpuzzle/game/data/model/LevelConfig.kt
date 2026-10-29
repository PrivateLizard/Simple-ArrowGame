package com.arrowpuzzle.game.data.model

enum class ComplexityLevel {
    /** Four arrows are open at the start. */
    EASY,

    /** Two arrows are open at the start. */
    MEDIUM,

    /** Three arrows are open at the start. */
    HARD,
}

/**
 * Complete description of a generated or custom level.
 *
 * A [levelId] of [CUSTOM_LEVEL_ID] marks a custom-mode run. [timeLimitSeconds]
 * being null means there is no time limit.
 */
data class LevelConfig(
    val levelId: Int,
    val gridSize: Pair<Int, Int>,
    val arrowCount: Int,
    val maxFailures: Int,
    val timeLimitSeconds: Int?,
    val complexity: ComplexityLevel,
    val obstacleCount: Int = 0,
) {
    val gridWidth: Int get() = gridSize.first
    val gridHeight: Int get() = gridSize.second
    val isCustom: Boolean get() = levelId == CUSTOM_LEVEL_ID

    companion object {
        const val CUSTOM_LEVEL_ID = 0
    }
}
