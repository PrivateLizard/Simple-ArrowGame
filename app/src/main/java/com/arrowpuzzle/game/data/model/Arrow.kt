package com.arrowpuzzle.game.data.model

/**
 * Integer grid coordinate. (0, 0) is the top-left cell.
 */
data class GridPoint(val x: Int, val y: Int)

/**
 * The eight possible arrow directions. Grid y grows downwards.
 */
enum class ArrowDirection(val dx: Int, val dy: Int) {
    UP(0, -1),
    UP_RIGHT(1, -1),
    RIGHT(1, 0),
    DOWN_RIGHT(1, 1),
    DOWN(0, 1),
    DOWN_LEFT(-1, 1),
    LEFT(-1, 0),
    UP_LEFT(-1, -1),
    ;

    /** Clockwise angle in degrees, used by the renderer. */
    val angleDegrees: Float
        get() = when (this) {
            RIGHT -> 0f
            DOWN_RIGHT -> 45f
            DOWN -> 90f
            DOWN_LEFT -> 135f
            LEFT -> 180f
            UP_LEFT -> 225f
            UP -> 270f
            UP_RIGHT -> 315f
        }

    companion object {
        /**
         * Resolves the direction from [from] to [to]. Only the sign of the
         * coordinate delta matters, so long straight connections are supported.
         */
        fun between(from: GridPoint, to: GridPoint): ArrowDirection? {
            val dx = (to.x - from.x).coerceIn(-1, 1)
            val dy = (to.y - from.y).coerceIn(-1, 1)
            if (dx == 0 && dy == 0) return null
            return entries.firstOrNull { it.dx == dx && it.dy == dy }
        }
    }
}

/**
 * One removable line-art arrow.
 *
 * The arrow has a polyline [route] on a fine lattice, ending with an arrowhead
 * in [direction]. Its forward ray is cast from the last route point; another
 * arrow on that ray blocks it.
 */
data class Arrow(
    val id: Int,
    val route: List<GridPoint>,
    val direction: ArrowDirection,
) {
    init {
        require(route.size >= 2) { "Arrow route needs at least two points" }
        require(route.zipWithNext().all { (a, b) -> a.x == b.x || a.y == b.y }) {
            "Arrow segments must be horizontal or vertical"
        }
    }

    val position: GridPoint get() = route.first()
    val endPosition: GridPoint get() = route.last()

    val lengthCells: Int
        get() = route.zipWithNext().sumOf { (a, b) ->
            kotlin.math.abs(b.x - a.x) + kotlin.math.abs(b.y - a.y)
        }
}
