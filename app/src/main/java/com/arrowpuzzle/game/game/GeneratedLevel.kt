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
) {
    val arrowCount: Int get() = arrows.size

    /** Blocking relation, built once per level on first use. */
    val blocking: BlockingGraph by lazy { BlockingGraph(arrows, layoutSize) }

    fun arrowById(id: Int): Arrow? = arrows.firstOrNull { it.id == id }
}
