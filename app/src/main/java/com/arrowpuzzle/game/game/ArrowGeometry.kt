package com.arrowpuzzle.game.game

import com.arrowpuzzle.game.data.model.Arrow
import com.arrowpuzzle.game.data.model.ArrowDirection
import com.arrowpuzzle.game.data.model.GridPoint
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Geometry and dependency rules shared by the generator, validator, game logic
 * and tests.
 *
 * An arrow blocks another arrow when its visible stroke intersects the forward
 * ray that starts at the other arrow's tip. Only horizontal / vertical rays are
 * used by the generator.
 */
object ArrowGeometry {
    fun routePoints(route: List<GridPoint>): LinkedHashSet<GridPoint> {
        val points = LinkedHashSet<GridPoint>()
        route.zipWithNext().forEach { (from, to) ->
            val dx = (to.x - from.x).coerceIn(-1, 1)
            val dy = (to.y - from.y).coerceIn(-1, 1)
            val steps = max(abs(to.x - from.x), abs(to.y - from.y))
            for (step in 0..steps) {
                points += GridPoint(from.x + dx * step, from.y + dy * step)
            }
        }
        return points
    }

    fun blockersFor(
        arrow: Arrow,
        candidates: List<Arrow>,
        boardSize: Int,
    ): List<Arrow> = candidates.filter { other ->
        other.id != arrow.id && rayHits(arrow, other, boardSize)
    }

    fun rayHits(shooter: Arrow, target: Arrow, boardSize: Int): Boolean {
        val origin = shooter.endPosition
        val direction = shooter.direction
        if (direction.dx != 0 && direction.dy != 0) return false

        if (direction.dx != 0) {
            val endX = if (direction.dx > 0) boardSize - 1 else 0
            if (origin.x == endX) return false
            val minX = min(origin.x + direction.dx, endX)
            val maxX = max(origin.x + direction.dx, endX)
            for ((from, to) in target.route.zipWithNext()) {
                if (from.x == to.x) {
                    val minY = min(from.y, to.y)
                    val maxY = max(from.y, to.y)
                    if (from.x in minX..maxX && origin.y in minY..maxY) return true
                } else if (from.y == to.y && from.y == origin.y) {
                    val segmentMinX = min(from.x, to.x)
                    val segmentMaxX = max(from.x, to.x)
                    if (max(minX, segmentMinX) <= min(maxX, segmentMaxX)) return true
                }
            }
            return false
        }

        val endY = if (direction.dy > 0) boardSize - 1 else 0
        if (origin.y == endY) return false
        val minY = min(origin.y + direction.dy, endY)
        val maxY = max(origin.y + direction.dy, endY)
        for ((from, to) in target.route.zipWithNext()) {
            if (from.y == to.y) {
                val minX = min(from.x, to.x)
                val maxX = max(from.x, to.x)
                if (from.y in minY..maxY && origin.x in minX..maxX) return true
            } else if (from.x == to.x && from.x == origin.x) {
                val segmentMinY = min(from.y, to.y)
                val segmentMaxY = max(from.y, to.y)
                if (max(minY, segmentMinY) <= min(maxY, segmentMaxY)) return true
            }
        }
        return false
    }

    fun availableArrows(
        level: GeneratedLevel,
        removedIds: Set<Int>,
    ): List<Arrow> {
        val graph = level.blocking
        return level.arrows.filterIndexed { index, arrow ->
            arrow.id !in removedIds && graph.isFree(index, level.arrows, removedIds)
        }
    }

    fun isClickable(
        level: GeneratedLevel,
        removedIds: Set<Int>,
        arrowId: Int,
    ): Boolean {
        if (arrowId in removedIds) return false
        val index = level.arrows.indexOfFirst { it.id == arrowId }
        if (index < 0) return false
        return level.blocking.isFree(index, level.arrows, removedIds)
    }

    fun hasCycle(arrows: List<Arrow>, boardSize: Int): Boolean =
        !BlockingGraph(arrows, boardSize).isAcyclic()

    /**
     * Removing an arrow never blocks another one, so the board can be cleared
     * in some order exactly when the blocking graph has no cycle. The check is
     * therefore a proof, not a heuristic: any order of open taps finishes.
     */
    fun canClearAll(level: GeneratedLevel): Boolean = level.blocking.isAcyclic()

    fun isCardinal(direction: ArrowDirection): Boolean =
        direction.dx == 0 || direction.dy == 0
}
