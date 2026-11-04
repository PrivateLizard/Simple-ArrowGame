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
 * An arrow blocks another arrow only when it occupies the cell directly in
 * front of the other arrow's tip. Only that nearest arrow is visible to the
 * player, so it is the one that blocks; arrows farther down the ray do not.
 * The tip still points along a horizontal / vertical ray.
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
    ): List<Arrow> {
        val frontX = arrow.endPosition.x + arrow.direction.dx
        val frontY = arrow.endPosition.y + arrow.direction.dy
        if (frontX !in 0 until boardSize || frontY !in 0 until boardSize) {
            return emptyList()
        }
        val front = GridPoint(frontX, frontY)
        val blocker = candidates.firstOrNull { other ->
            other.id != arrow.id && other.route.contains(front)
        }
        return if (blocker == null) emptyList() else listOf(blocker)
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
        val remaining = level.arrows.filter { it.id !in removedIds }
        return remaining.filter { arrow ->
            blockersFor(arrow, remaining, level.layoutSize).isEmpty()
        }
    }

    fun isClickable(
        level: GeneratedLevel,
        removedIds: Set<Int>,
        arrowId: Int,
    ): Boolean {
        if (arrowId in removedIds) return false
        val arrow = level.arrowById(arrowId) ?: return false
        val remaining = level.arrows.filter { it.id !in removedIds }
        return blockersFor(arrow, remaining, level.layoutSize).isEmpty()
    }

    fun hasCycle(arrows: List<Arrow>, boardSize: Int): Boolean {
        val colors = IntArray(arrows.size)
        val byId = arrows.associateBy { it.id }

        fun visit(arrow: Arrow): Boolean {
            val index = arrows.indexOfFirst { it.id == arrow.id }
            if (index < 0) return false
            if (colors[index] == 1) return true
            if (colors[index] == 2) return false
            colors[index] = 1
            val blockers = blockersFor(arrow, arrows, boardSize)
            for (blocker in blockers) {
                val blockerArrow = byId[blocker.id] ?: continue
                if (visit(blockerArrow)) return true
            }
            colors[index] = 2
            return false
        }

        for (arrow in arrows) {
            if (visit(arrow)) return true
        }
        return false
    }

    fun canClearAll(level: GeneratedLevel): Boolean {
        val removed = mutableSetOf<Int>()
        while (removed.size < level.arrowCount) {
            val available = availableArrows(level, removed)
            if (available.isEmpty()) return false
            removed += available.first().id
        }
        return true
    }

    fun isCardinal(direction: ArrowDirection): Boolean =
        direction.dx == 0 || direction.dy == 0
}
