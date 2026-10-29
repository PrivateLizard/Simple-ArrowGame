package com.arrowpuzzle.game.game

import com.arrowpuzzle.game.data.model.Arrow
import com.arrowpuzzle.game.data.model.ArrowDirection
import kotlin.math.abs
import kotlin.math.max

/**
 * Validates the no-crossing rule, the blocking graph and solvability.
 */
object PathValidator {
    fun validate(level: GeneratedLevel): Boolean = validationError(level) == null

    fun validationError(level: GeneratedLevel): String? {
        val arrows = level.arrows
        if (arrows.isEmpty()) return "empty board"
        if (arrows.map { it.id }.toSet().size != arrows.size) return "duplicate arrow ids"
        if (level.layoutSize < 3) return "layout too small"

        val occupied = mutableSetOf<com.arrowpuzzle.game.data.model.GridPoint>()
        for (arrow in arrows) {
            if (arrow.route.size < 2) return "arrow route too short"
            if (!ArrowGeometry.isCardinal(arrow.direction)) return "non-cardinal arrow"
            val expectedDirection = ArrowDirection.between(
                arrow.route[arrow.route.lastIndex - 1],
                arrow.endPosition,
            ) ?: return "invalid arrow final direction"
            if (arrow.direction != expectedDirection) return "arrow direction mismatch"

            val totalSteps = arrow.route.zipWithNext().sumOf { (from, to) ->
                max(abs(to.x - from.x), abs(to.y - from.y))
            }
            val points = ArrowGeometry.routePoints(arrow.route)
            if (points.size != totalSteps + 1) return "arrow self-intersection"
            for (point in points) {
                if (point.x !in 0 until level.layoutSize) return "point x outside board"
                if (point.y !in 0 until level.layoutSize) return "point y outside board"
            }
            for (point in points) {
                if (!occupied.add(point)) return "arrow strokes overlap"
            }
        }

        if (ArrowGeometry.hasCycle(arrows, level.layoutSize)) return "dependency cycle"
        if (ArrowGeometry.availableArrows(level, emptySet()).isEmpty()) return "no open arrow"
        if (!ArrowGeometry.canClearAll(level)) return "board cannot be cleared"
        return null
    }

    fun isPathComplete(level: GeneratedLevel, removedIds: Set<Int>): Boolean =
        removedIds.containsAll(level.arrows.map { it.id })
}
