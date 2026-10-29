package com.arrowpuzzle.game.game

import com.arrowpuzzle.game.data.model.Arrow
import com.arrowpuzzle.game.data.model.ArrowDirection
import com.arrowpuzzle.game.data.model.ComplexityLevel
import com.arrowpuzzle.game.data.model.GridPoint
import com.arrowpuzzle.game.data.model.LevelConfig
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

/**
 * Builds a completely full board from a randomized Hamiltonian path.
 *
 * The path visits every lattice point exactly once. It is split into arrows.
 * Cuts at straight-through points leave the next arrow directly in front of the
 * arrowhead, so that arrow is blocked. Cuts at outward-facing boundary points
 * produce the K open arrows.
 */
class LevelGenerator(
    private val maxAttempts: Int = 40,
) {
    fun generate(config: LevelConfig, seed: Long = System.nanoTime()): GeneratedLevel {
        require(config.arrowCount in 1..200) { "Arrow count must be between 1 and 200" }
        require(config.maxFailures in 1..99) { "Max failures must be between 1 and 99" }

        val random = Random(seed)
        val layoutSize = layoutSizeFor(config)
        val openCount = freeArrowCount(config).coerceAtMost(config.arrowCount)
        val targetArrows = targetArrowCount(config)
        var lastError: String? = null

        repeat(maxAttempts) {
            val path = randomizedHamiltonianPath(layoutSize, random)
            val cuts = chooseCuts(
                path = path,
                layoutSize = layoutSize,
                openCount = openCount,
                targetArrows = targetArrows,
                random = random,
            )
            if (cuts == null) {
                lastError = "Could not place enough cut points"
                return@repeat
            }
            val arrows = buildArrows(path, cuts, layoutSize)
            if (arrows == null) {
                lastError = "Could not build arrows from path"
                return@repeat
            }
            val level = GeneratedLevel(config, arrows, layoutSize)
            val validationError = PathValidator.validationError(level)
            if (validationError != null) {
                lastError = validationError
                return@repeat
            }
            val available = ArrowGeometry.availableArrows(level, emptySet()).size
            if (available != openCount) {
                lastError = "Expected $openCount open arrows, got $available"
                return@repeat
            }
            val covered = arrows
                .flatMap { ArrowGeometry.routePoints(it.route) }
                .toSet()
                .size
            val totalPoints = layoutSize * layoutSize
            if (covered != totalPoints) {
                lastError = "Coverage is ${covered * 100 / totalPoints}%"
                return@repeat
            }
            return level
        }

        throw IllegalStateException(lastError ?: "Unable to generate a full board")
    }

    private fun layoutSizeFor(config: LevelConfig): Int =
        (10 + max(config.gridWidth, config.gridHeight) * 3).coerceIn(19, 38)

    /**
     * Number of arrows open at the start.
     *
     * Campaign levels defer to [LevelCatalog] so the curve lives in exactly one
     * place. Custom levels use the same tiers.
     */
    private fun freeArrowCount(config: LevelConfig): Int = when (config.complexity) {
        ComplexityLevel.EASY -> 4
        ComplexityLevel.MEDIUM -> 3
        ComplexityLevel.HARD -> 2
    }

    private fun targetArrowCount(config: LevelConfig): Int {
        if (config.isCustom) return config.arrowCount
        return (config.arrowCount + 6).coerceAtMost(LevelCatalog.arrowCountForLevel(LevelCatalog.MAX_LEVEL))
    }

    private fun randomizedHamiltonianPath(layoutSize: Int, random: Random): IntArray {
        val count = layoutSize * layoutSize
        val path = IntArray(count)
        var index = 0
        for (y in 0 until layoutSize) {
            if (y % 2 == 0) {
                for (x in 0 until layoutSize) {
                    path[index++] = y * layoutSize + x
                }
            } else {
                for (x in layoutSize - 1 downTo 0) {
                    path[index++] = y * layoutSize + x
                }
            }
        }

        val positions = IntArray(count)
        for (i in path.indices) positions[path[i]] = i

        val iterations = (count * 8).coerceAtLeast(2_000)
        repeat(iterations) {
            val start = path[0]
            val startX = start % layoutSize
            val startY = start / layoutSize
            val next = path[1]
            val candidates = ArrayList<Int>(4)
            if (startX > 0) candidates += start - 1
            if (startX < layoutSize - 1) candidates += start + 1
            if (startY > 0) candidates += start - layoutSize
            if (startY < layoutSize - 1) candidates += start + layoutSize
            val target = candidates.filter { it != next }.randomOrNull(random) ?: return@repeat
            val targetIndex = positions[target]
            if (targetIndex <= 1 || targetIndex == count - 1) return@repeat
            reversePrefix(path, positions, targetIndex)
        }
        return path
    }

    private fun reversePrefix(path: IntArray, positions: IntArray, endExclusive: Int) {
        var left = 0
        var right = endExclusive - 1
        while (left < right) {
            val leftValue = path[left]
            val rightValue = path[right]
            path[left] = rightValue
            path[right] = leftValue
            positions[rightValue] = left
            positions[leftValue] = right
            left += 1
            right -= 1
        }
    }

    private fun chooseCuts(
        path: IntArray,
        layoutSize: Int,
        openCount: Int,
        targetArrows: Int,
        random: Random,
    ): IntArray? {
        val count = path.size
        if (targetArrows < openCount || targetArrows > count - 1) return null

        val pathIndex = IntArray(count)
        for (i in path.indices) pathIndex[path[i]] = i

        val openCandidates = mutableListOf<Int>()
        val straightCandidates = mutableListOf<Int>()
        for (i in 1 until count - 1) {
            val previous = point(path[i - 1], layoutSize)
            val current = point(path[i], layoutSize)
            val next = point(path[i + 1], layoutSize)
            val direction = ArrowDirection.between(previous, current) ?: continue
            val nextDirection = ArrowDirection.between(current, next) ?: continue
            if (i <= count - 3 && isOutward(current, previous, direction, layoutSize)) {
                openCandidates += i
            }
            if (
                i <= count - 3 &&
                direction == nextDirection &&
                !hasBackwardCellOnRay(current, direction, i, pathIndex, layoutSize)
            ) {
                straightCandidates += i
            }
        }

        val cuts = mutableListOf(count - 1)
        for (candidate in openCandidates.shuffled(random)) {
            if (cuts.size - 1 >= openCount - 1) break
            if (cuts.all { abs(it - candidate) >= 2 }) {
                cuts += candidate
            }
        }
        if (cuts.size - 1 < openCount - 1) return null

        for (candidate in straightCandidates.shuffled(random)) {
            if (cuts.size >= targetArrows) break
            if (cuts.all { abs(it - candidate) >= 2 }) {
                cuts += candidate
            }
        }
        if (cuts.size < targetArrows) return null
        return cuts.sorted().toIntArray()
    }

    private fun buildArrows(path: IntArray, cuts: IntArray, layoutSize: Int): List<Arrow>? {
        val arrows = ArrayList<Arrow>(cuts.size)
        var start = 0
        for (cut in cuts) {
            if (cut < start || cut - start + 1 < 2) return null
            val route = ArrayList<GridPoint>(cut - start + 1)
            for (index in start..cut) {
                route += point(path[index], layoutSize)
            }
            val direction = ArrowDirection.between(
                route[route.lastIndex - 1],
                route.last(),
            ) ?: return null
            arrows += Arrow(arrows.size, route, direction)
            start = cut + 1
        }
        if (start != path.size) return null
        return arrows
    }

    private fun hasBackwardCellOnRay(
        origin: GridPoint,
        direction: ArrowDirection,
        originIndex: Int,
        pathIndex: IntArray,
        layoutSize: Int,
    ): Boolean {
        var x = origin.x
        var y = origin.y
        while (true) {
            x += direction.dx
            y += direction.dy
            if (x !in 0 until layoutSize || y !in 0 until layoutSize) return false
            if (pathIndex[y * layoutSize + x] < originIndex) return true
        }
    }

    private fun isOutward(
        current: GridPoint,
        previous: GridPoint,
        direction: ArrowDirection,
        layoutSize: Int,
    ): Boolean = when {
        current.y == 0 && previous.y == 1 -> direction == ArrowDirection.UP
        current.y == layoutSize - 1 && previous.y == layoutSize - 2 -> direction == ArrowDirection.DOWN
        current.x == 0 && previous.x == 1 -> direction == ArrowDirection.LEFT
        current.x == layoutSize - 1 && previous.x == layoutSize - 2 -> direction == ArrowDirection.RIGHT
        else -> false
    }

    private fun point(id: Int, layoutSize: Int): GridPoint =
        GridPoint(id % layoutSize, id / layoutSize)
}
