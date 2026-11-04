package com.arrowpuzzle.game.game

import com.arrowpuzzle.game.data.model.Arrow

/**
 * Precomputed blocking relation of a fixed set of arrows.
 *
 * Every lattice cell knows which arrow covers it, so each forward ray is a
 * simple walk over cells instead of a test against every segment of every
 * other arrow. The result is identical to [ArrowGeometry.blockersFor]: an arrow
 * is blocked by every other arrow whose stroke lies anywhere on its forward
 * ray (diagonal heads have no ray).
 *
 * Arrows are addressed by their index in the list passed in.
 */
class BlockingGraph(arrows: List<Arrow>, boardSize: Int) {
    /** `blockers[i]` = indices of the arrows on arrow i's ray, ascending. */
    val blockers: Array<IntArray>

    /** `blocks[j]` = indices of the arrows that arrow j blocks. */
    private val blocks: Array<IntArray>

    init {
        val size = arrows.size
        val owner = IntArray(boardSize * boardSize) { -1 }
        arrows.forEachIndexed { index, arrow ->
            for (point in ArrowGeometry.routePoints(arrow.route)) {
                if (point.x in 0 until boardSize && point.y in 0 until boardSize) {
                    owner[point.y * boardSize + point.x] = index
                }
            }
        }

        val seen = IntArray(size) { -1 }
        blockers = Array(size) { index ->
            val arrow = arrows[index]
            val dx = arrow.direction.dx
            val dy = arrow.direction.dy
            if (dx != 0 && dy != 0) return@Array IntArray(0)
            val hits = ArrayList<Int>()
            var x = arrow.endPosition.x + dx
            var y = arrow.endPosition.y + dy
            while (x in 0 until boardSize && y in 0 until boardSize) {
                val other = owner[y * boardSize + x]
                if (other >= 0 && other != index && seen[other] != index) {
                    seen[other] = index
                    hits += other
                }
                x += dx
                y += dy
            }
            hits.sort()
            hits.toIntArray()
        }

        val reverse = Array(size) { ArrayList<Int>() }
        blockers.forEachIndexed { blocked, list -> list.forEach { reverse[it] += blocked } }
        blocks = Array(size) { reverse[it].toIntArray() }
    }

    /** True when no arrow outside [removedIds] lies on arrow [index]'s ray. */
    fun isFree(index: Int, arrows: List<Arrow>, removedIds: Set<Int>): Boolean =
        blockers[index].all { arrows[it].id in removedIds }

    /**
     * Kahn's algorithm: repeatedly remove open arrows. All arrows get removed
     * exactly when there is no dependency cycle.
     */
    fun isAcyclic(): Boolean {
        val size = blockers.size
        val pending = IntArray(size) { blockers[it].size }
        val queue = ArrayDeque<Int>()
        for (i in 0 until size) if (pending[i] == 0) queue.addLast(i)
        var removed = 0
        while (queue.isNotEmpty()) {
            val next = queue.removeFirst()
            removed += 1
            for (blocked in blocks[next]) {
                pending[blocked] -= 1
                if (pending[blocked] == 0) queue.addLast(blocked)
            }
        }
        return removed == size
    }
}
