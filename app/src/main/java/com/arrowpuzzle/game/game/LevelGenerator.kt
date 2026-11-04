package com.arrowpuzzle.game.game

import com.arrowpuzzle.game.data.model.AppSettings
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
        require(
            config.maxFailures in 0..99 || config.maxFailures == AppSettings.NO_FAILURE_LIMIT,
        ) { "Max failures must be between 0 and 99, or off" }

        val random = Random(seed)
        val layoutSize = layoutSizeFor(config)
        val openCount = freeArrowCount(config).coerceAtMost(config.arrowCount)
        val targetArrows = targetArrowCount(config)
        var lastError: String? = null

        repeat(maxAttempts) { attempt ->
            // Keep one attempt as a plain serpentine fallback so generation
            // never fails just because the heavy tangling left too few valid
            // cut points.
            val tangle = attempt < maxAttempts - 1
            val path = randomizedHamiltonianPath(layoutSize, random, tangle)
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

            // Flip a share of the non-open arrows so long parallel rows do not
            // all point the same way. If the flipped set breaks the rules, fall
            // back to the original arrows for this path.
            val flipped = flipSomeArrows(arrows, layoutSize, random)
            val flippedResult = validate(config, flipped, layoutSize, openCount)
            val flippedLevel = flippedResult.first
            if (flippedLevel != null) return flippedLevel
            val originalResult = validate(config, arrows, layoutSize, openCount)
            val originalLevel = originalResult.first
            if (originalLevel != null) return originalLevel
            lastError = originalResult.second ?: flippedResult.second
            return@repeat
        }

        throw IllegalStateException(lastError ?: "Unable to generate a full board")
    }

    private fun validate(
        config: LevelConfig,
        arrows: List<Arrow>,
        layoutSize: Int,
        openCount: Int,
    ): Pair<GeneratedLevel?, String?> {
        val level = GeneratedLevel(config, arrows, layoutSize)
        val validationError = PathValidator.validationError(level)
        if (validationError != null) return null to validationError
        val available = ArrowGeometry.availableArrows(level, emptySet()).size
        if (available != openCount) {
            return null to "Expected $openCount open arrows, got $available"
        }
        val covered = arrows
            .flatMap { ArrowGeometry.routePoints(it.route) }
            .toSet()
            .size
        val totalPoints = layoutSize * layoutSize
        if (covered != totalPoints) {
            return null to "Coverage is ${covered * 100 / totalPoints}%"
        }
        return level to null
    }

    /**
     * Reverses roughly 40% of the arrows that are not open. The direction is
     * taken from the new first segment, and the new head must stay inside the
     * board so the number of open arrows does not change.
     */
    private fun flipSomeArrows(
        arrows: List<Arrow>,
        layoutSize: Int,
        random: Random,
    ): List<Arrow> {
        val openIds = arrows
            .filter { ArrowGeometry.blockersFor(it, arrows, layoutSize).isEmpty() }
            .map { it.id }
            .toSet()
        return arrows.map { arrow ->
            if (arrow.id in openIds || random.nextFloat() > 0.4f) {
                return@map arrow
            }
            val reversed = arrow.route.reversed()
            val direction = ArrowDirection.between(
                reversed[reversed.lastIndex - 1],
                reversed.last(),
            ) ?: return@map arrow
            val frontX = reversed.last().x + direction.dx
            val frontY = reversed.last().y + direction.dy
            if (frontX !in 0 until layoutSize || frontY !in 0 until layoutSize) {
                return@map arrow
            }
            Arrow(arrow.id, reversed, direction)
        }
    }

    private fun layoutSizeFor(config: LevelConfig): Int =
        if (config.isCustom) {
            (10 + max(config.gridWidth, config.gridHeight) * 3).coerceIn(19, 38)
        } else {
            LevelCatalog.layoutSizeForLevel(config.levelId)
        }

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

    private fun randomizedHamiltonianPath(
        layoutSize: Int,
        random: Random,
        tangle: Boolean,
    ): IntArray {
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

        // Prefix moves only randomize the head of the path; a plain serpentine
        // tail would stay untouched. Periodically reversing the whole path
        // swaps head and tail, so the same move set reaches every region. This
        // is a valid Hamiltonian-path operation (just the reverse traversal)
        // and keeps all consecutive pairs adjacent.
        val targetMoves = if (tangle) (count * 3).coerceIn(1_500, 16_000) else 0
        val batchSize = 120
        val neighbors = IntArray(4)
        var moves = 0
        repeat(targetMoves) {
            val start = path[0]
            val next = path[1]
            val startX = start % layoutSize
            val startY = start / layoutSize
            var neighborCount = 0
            if (startX > 0) neighbors[neighborCount++] = start - 1
            if (startX < layoutSize - 1) neighbors[neighborCount++] = start + 1
            if (startY > 0) neighbors[neighborCount++] = start - layoutSize
            if (startY < layoutSize - 1) neighbors[neighborCount++] = start + layoutSize
            if (neighborCount == 0) return@repeat
            val target = neighbors[random.nextInt(neighborCount)]
            if (target == next) return@repeat
            val targetIndex = positions[target]
            // Never touch the current last edge: the final arrow has to keep
            // pointing off the board, otherwise it could close a cycle.
            if (targetIndex !in 2..count - 2) return@repeat
            reverseSegment(path, positions, 0, targetIndex - 1)
            moves += 1
            if (moves % batchSize == 0) reverseAll(path, positions)
        }
        if (!isLastArrowOpen(path, layoutSize)) {
            reverseAll(path, positions)
            if (!isLastArrowOpen(path, layoutSize)) return path
        }
        return path
    }

    private fun reverseAll(path: IntArray, positions: IntArray) {
        val count = path.size
        var left = 0
        var right = count - 1
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

    private fun reverseSegment(path: IntArray, positions: IntArray, start: Int, end: Int) {
        var left = start
        var right = end
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
        val turnCandidates = mutableListOf<Int>()
        val straightCandidates = mutableListOf<Int>()
        val relaxedCandidates = mutableListOf<Int>()
        for (i in 1 until count - 1) {
            val previous = point(path[i - 1], layoutSize)
            val head = point(path[i], layoutSize)
            val next = point(path[i + 1], layoutSize)
            val headDirection = ArrowDirection.between(previous, head) ?: continue
            val nextDirection = ArrowDirection.between(head, next) ?: continue
            when {
                // A head pointing off the board is an open arrow. Leave those
                // for the open-arrow selection below.
                isOutward(head, previous, headDirection, layoutSize) ->
                    openCandidates += i
                // Never point at an arrow that comes earlier in the path:
                // those edges could close a dependency cycle. With the
                // nearest-cell rule only the cell directly in front matters.
                // All remaining edges point forward, so the graph stays acyclic.
                hasBackwardCellInFront(head, headDirection, i, pathIndex, layoutSize) ->
                    relaxedCandidates += i
                // Cutting at a corner means the arrow does not point straight
                // into the next arrow. It creates cross-blocking (one arrow can
                // block several others) instead of a single chain.
                headDirection != nextDirection -> turnCandidates += i
                else -> straightCandidates += i
            }
        }

        val cuts = mutableListOf(count - 1)
        // The last path segment may or may not point off the board; account for
        // it instead of always assuming it is one of the open arrows.
        val neededOpenCuts = if (isLastArrowOpen(path, layoutSize)) {
            openCount - 1
        } else {
            openCount
        }
        var openAdded = 0
        for (candidate in openCandidates.shuffled(random)) {
            if (openAdded >= neededOpenCuts) break
            if (cuts.all { abs(it - candidate) >= 2 }) {
                cuts += candidate
                openAdded += 1
            }
        }
        if (openAdded < neededOpenCuts) return null

        // Prefer corner cuts, then fall back to straight cuts only if the path
        // does not offer enough corners.
        for (candidate in turnCandidates.shuffled(random)) {
            if (cuts.size >= targetArrows) break
            if (cuts.all { abs(it - candidate) >= 2 }) {
                cuts += candidate
            }
        }
        if (cuts.size < targetArrows) {
            for (candidate in straightCandidates.shuffled(random)) {
                if (cuts.size >= targetArrows) break
                if (cuts.all { abs(it - candidate) >= 2 }) {
                    cuts += candidate
                }
            }
        }
        // Last resort: cuts whose front cell belongs to an earlier arrow. They
        // may or may not form a cycle; the validator decides. This keeps the
        // heavily tangled boards usable instead of falling back to a serpentine.
        if (cuts.size < targetArrows) {
            for (candidate in relaxedCandidates.shuffled(random)) {
                if (cuts.size >= targetArrows) break
                if (cuts.all { abs(it - candidate) >= 2 }) {
                    cuts += candidate
                }
            }
        }
        if (cuts.size < targetArrows) return null
        return cuts.sorted().toIntArray()
    }

    private fun isLastArrowOpen(path: IntArray, layoutSize: Int): Boolean {
        val count = path.size
        if (count < 3) return false
        val previous = point(path[count - 2], layoutSize)
        val head = point(path[count - 1], layoutSize)
        val direction = ArrowDirection.between(previous, head) ?: return false
        return isOutward(head, previous, direction, layoutSize)
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

    private fun hasBackwardCellInFront(
        origin: GridPoint,
        direction: ArrowDirection,
        originIndex: Int,
        pathIndex: IntArray,
        layoutSize: Int,
    ): Boolean {
        val x = origin.x + direction.dx
        val y = origin.y + direction.dy
        if (x !in 0 until layoutSize || y !in 0 until layoutSize) return false
        return pathIndex[y * layoutSize + x] < originIndex
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
