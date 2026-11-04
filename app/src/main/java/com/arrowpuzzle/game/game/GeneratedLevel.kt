package com.arrowpuzzle.game.game

import com.arrowpuzzle.game.data.model.Arrow
import com.arrowpuzzle.game.data.model.LevelConfig

/**
 * Fully generated, validated level.
 *
 * Every arrow in [arrows] must be removed. Arrows that have no other arrow on
 * their forward ray can be tapped first.
 */
data class GeneratedLevel(
    val config: LevelConfig,
    val arrows: List<Arrow>,
    val layoutSize: Int,
    /**
     * Cells that belong to the board (row-major, layoutSize squared), or null
     * for the full square. Cells outside the mask stay empty and never block.
     */
    val cellMask: BooleanArray? = null,
) {
    val arrowCount: Int get() = arrows.size

    fun isCellActive(x: Int, y: Int): Boolean =
        cellMask == null || cellMask[y * layoutSize + x]

    /** Blocking relation, built once per level on first use. */
    val blocking: BlockingGraph by lazy { BlockingGraph(arrows, layoutSize) }

    fun arrowById(id: Int): Arrow? = arrows.firstOrNull { it.id == id }
}
