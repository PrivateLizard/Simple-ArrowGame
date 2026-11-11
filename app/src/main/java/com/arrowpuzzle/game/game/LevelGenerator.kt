package com.arrowpuzzle.game.game

import com.arrowpuzzle.game.data.model.AppSettings
import com.arrowpuzzle.game.data.model.Arrow
import com.arrowpuzzle.game.data.model.ArrowDirection
import com.arrowpuzzle.game.data.model.ComplexityLevel
import com.arrowpuzzle.game.data.model.GridPoint
import com.arrowpuzzle.game.data.model.LevelConfig
import com.arrowpuzzle.game.game.shapes.LevelShape
import com.arrowpuzzle.game.game.shapes.ShapeMask
import com.arrowpuzzle.game.game.shapes.ShapeSize
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

/**
 * Builds a completely full board from an on-device Hamiltonian path.
 *
 * The path is a Hamiltonian cycle (2x2 block cycles merged along a random
 * spanning tree) broken at an outward-facing edge, or a serpentine fallback.
 * It is cut into arrows: outward-facing cuts create the open arrows, and the
 * remaining cuts are chosen from forward-facing candidates so the blocking
 * graph stays acyclic. Very short same-direction runs are merged afterwards.
 * Then a share of the arrows is reversed, and arrows above a soft length cap
 * are split wherever the real blocking graph stays acyclic (see
 * [capLongArrows]).
 */
class LevelGenerator(
    private val maxAttempts: Int = 40,
) {
    companion object {
        /**
         * Bump whenever the same seed produces a different board. Saved games
         * from another version are discarded instead of resumed on a board
         * whose arrow ids no longer match.
         *
         * 1 = up to build 38, 2 = soft arrow length cap, 3 = 3000 levels with
         * picture levels, 4 = picture levels 35% larger,
         * 5 = reworked pencil, feather and envelope,
         * 6 = 34 shapes redrawn, square and rounded square replaced.
         */
        const val VERSION = 6

        private const val SHORT_ARROW = 8
        private const val MAX_MERGED_ARROW = 80
        private const val MIN_MERGE_RUN = 3

        /** Soft length cap (path cells) for a single arrow. */
        private const val MAX_ARROW_LENGTH = 80
        /** Two neighbours are only merged to compensate a split if this short. */
        private const val MAX_MERGE_FOR_CAP = 24
        private const val MAX_CAP_ROUNDS = 200
        private const val MAX_MERGE_TRIES = 8
        private val BOOLEANS = booleanArrayOf(false, true)
    }

    fun generate(config: LevelConfig, seed: Long = System.nanoTime()): GeneratedLevel {
        require(config.arrowCount in 1..400) { "Arrow count must be between 1 and 400" }
        require(
            config.maxFailures in 0..99 || config.maxFailures == AppSettings.NO_FAILURE_LIMIT,
        ) { "Max failures must be between 0 and 99, or off" }

        val random = Random(seed)
        val shape = LevelShape.fromId(config.shapeId)
        val layoutSize = layoutSizeFor(config, shape)
        // Test mode: only the cells inside the shape are part of the board.
        val mask = shape?.let { ShapeMask.mask(it, layoutSize) }
        val openCount = freeArrowCount(config).coerceAtMost(config.arrowCount)
        val targetArrows = targetArrowCount(config)
        var lastError: String? = null

        repeat(maxAttempts) { attempt ->
            // Last attempt: a plain serpentine, which always offers enough
            // forward-facing cut points under the full-ray blocking rule. A
            // shaped board has no serpentine, so it keeps trying random paths.
            val path = if (mask != null || attempt < maxAttempts - 1) {
                randomHamiltonianPath(layoutSize, random, mask)
            } else {
                serpentinePath(layoutSize)
            }
            val cuts = chooseCuts(
                path = path,
                layoutSize = layoutSize,
                openCount = openCount,
                targetArrows = targetArrows,
                random = random,
                mask = mask,
            )
            if (cuts == null) {
                lastError = "Could not place enough cut points"
                return@repeat
            }

            // Flip a share of the non-open arrows so long parallel rows do not
            // all point the same way. If the flipped set breaks the rules, fall
            // back to unflipped arrows for this path. The length cap runs after
            // the flip choice, because flips change which splits are safe.
            val flips = chooseFlips(path, cuts, layoutSize, random, mask)
            for (flags in listOf(flips, BooleanArray(cuts.size))) {
                val capped = capLongArrows(path, layoutSize, cuts, flags)
                val arrows = buildArrows(path, capped.cuts, capped.flipped, layoutSize)
                if (arrows == null) {
                    lastError = "Could not build arrows from path"
                    continue
                }
                val result = validate(config, arrows, layoutSize, openCount, mask)
                result.first?.let { return it }
                lastError = result.second
            }
        }

        throw IllegalStateException(lastError ?: "Unable to generate a full board")
    }

    private fun validate(
        config: LevelConfig,
        arrows: List<Arrow>,
        layoutSize: Int,
        openCount: Int,
        mask: BooleanArray?,
    ): Pair<GeneratedLevel?, String?> {
        val level = GeneratedLevel(config, arrows, layoutSize, mask)
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
        val totalPoints = mask?.count { it } ?: (layoutSize * layoutSize)
        if (covered != totalPoints) {
            return null to "Coverage is ${covered * 100 / totalPoints}%"
        }
        return level to null
    }

    /**
     * Chooses roughly 40% of the arrows that are not open to be reversed, so
     * the head sits at the first path cell of the arrow. The new head must
     * point into the board so the number of open arrows does not change.
     */
    private fun chooseFlips(
        path: IntArray,
        cuts: IntArray,
        layoutSize: Int,
        random: Random,
        mask: BooleanArray?,
    ): BooleanArray {
        val unflipped = BooleanArray(cuts.size)
        val open = openArrows(path, layoutSize, cuts, unflipped)
        return BooleanArray(cuts.size) { a ->
            if (open[a] || random.nextFloat() > 0.4f) return@BooleanArray false
            val start = if (a == 0) 0 else cuts[a - 1] + 1
            val head = path[start]
            val previous = path[start + 1]
            val frontX = 2 * (head % layoutSize) - previous % layoutSize
            val frontY = 2 * (head / layoutSize) - previous / layoutSize
            frontX in 0 until layoutSize && frontY in 0 until layoutSize &&
                (mask == null || mask[frontY * layoutSize + frontX])
        }
    }

    /** Arrows whose forward ray is empty (open), by the same rule as [BlockingGraph]. */
    private fun openArrows(
        path: IntArray,
        layoutSize: Int,
        cuts: IntArray,
        flipped: BooleanArray,
    ): BooleanArray {
        val owner = ownerOf(path, cuts, layoutSize)
        return BooleanArray(cuts.size) { a ->
            var free = true
            forEachRayCell(path, layoutSize, cuts, flipped, a) { cell ->
                if (owner[cell] >= 0 && owner[cell] != a) {
                    free = false
                    false
                } else {
                    true
                }
            }
            free
        }
    }

    private fun layoutSizeFor(config: LevelConfig, shape: LevelShape?): Int =
        if (shape != null) {
            ShapeMask.layoutFor(
                shape,
                config.shapeCells.takeIf { it > 0 } ?: ShapeSize.MEDIUM.targetCells,
            )
        } else if (config.isCustom) {
            val raw = 10 + max(config.gridWidth, config.gridHeight) * 3
            val even = if (raw % 2 == 0) raw else raw + 1
            even.coerceIn(20, 38)
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
        if (config.isCustom || config.isShaped) return config.arrowCount
        return (config.arrowCount + 6).coerceAtMost(LevelCatalog.arrowCountForLevel(LevelCatalog.MAX_LEVEL))
    }

    /**
     * Builds a Hamiltonian path as a random Hamiltonian cycle minus one edge.
     *
     * The grid is tiled with 2x2 blocks, each carrying its own 4-cycle. A random
     * spanning tree over the block grid merges the cycles with two cross edges
     * per tree edge. The result is one cycle through every cell. Traversing it
     * from a random start and stopping one step before the end yields a
     * Hamiltonian path with no long parallel serpentine rows.
     *
     * Layout sizes are even, so the tiling is exact.
     */
    private fun randomHamiltonianPath(
        layoutSize: Int,
        random: Random,
        mask: BooleanArray? = null,
    ): IntArray {
        val count = layoutSize * layoutSize
        val blocks = layoutSize / 2
        // With a mask only the blocks inside the shape take part. The mask is
        // block-aligned and connected (see ShapeMask), so the spanning tree
        // still reaches every active block.
        fun blockActive(bx: Int, by: Int): Boolean =
            mask == null || mask[(by * 2) * layoutSize + bx * 2]
        val edges = HashSet<Long>(count)

        fun cell(x: Int, y: Int): Int = y * layoutSize + x
        fun key(first: Int, second: Int): Long =
            if (first < second) {
                first.toLong() * count + second
            } else {
                second.toLong() * count + first
            }
        fun addEdge(first: Int, second: Int) {
            edges.add(key(first, second))
        }
        fun removeEdge(first: Int, second: Int) {
            edges.remove(key(first, second))
        }

        for (by in 0 until blocks) {
            for (bx in 0 until blocks) {
                if (!blockActive(bx, by)) continue
                val x = bx * 2
                val y = by * 2
                val a = cell(x, y)
                val b = cell(x + 1, y)
                val c = cell(x, y + 1)
                val d = cell(x + 1, y + 1)
                addEdge(a, b)
                addEdge(b, d)
                addEdge(d, c)
                addEdge(c, a)
            }
        }

        val parent = IntArray(blocks * blocks) { it }
        fun find(node: Int): Int {
            var root = node
            while (parent[root] != root) {
                parent[root] = parent[parent[root]]
                root = parent[root]
            }
            return root
        }
        fun union(first: Int, second: Int): Boolean {
            val a = find(first)
            val b = find(second)
            if (a == b) return false
            parent[a] = b
            return true
        }

        val blockEdges = ArrayList<BlockEdge>(2 * blocks * (blocks - 1))
        for (by in 0 until blocks) {
            for (bx in 0 until blocks) {
                val id = by * blocks + bx
                if (!blockActive(bx, by)) continue
                if (bx + 1 < blocks && blockActive(bx + 1, by)) {
                    blockEdges += BlockEdge(id, id + 1, true, bx, by)
                }
                if (by + 1 < blocks && blockActive(bx, by + 1)) {
                    blockEdges += BlockEdge(id, id + blocks, false, bx, by)
                }
            }
        }
        blockEdges.shuffle(random)

        for (edge in blockEdges) {
            if (!union(edge.first, edge.second)) continue
            val x = edge.bx * 2
            val y = edge.by * 2
            if (edge.horizontal) {
                val rightTop = cell(x + 1, y)
                val rightBottom = cell(x + 1, y + 1)
                val leftTop = cell(x + 2, y)
                val leftBottom = cell(x + 2, y + 1)
                removeEdge(rightTop, rightBottom)
                removeEdge(leftTop, leftBottom)
                addEdge(rightTop, leftTop)
                addEdge(rightBottom, leftBottom)
            } else {
                val bottomLeft = cell(x, y + 1)
                val bottomRight = cell(x + 1, y + 1)
                val topLeft = cell(x, y + 2)
                val topRight = cell(x + 1, y + 2)
                removeEdge(bottomLeft, bottomRight)
                removeEdge(topLeft, topRight)
                addEdge(bottomLeft, topLeft)
                addEdge(bottomRight, topRight)
            }
        }

        val adjacency = Array(count) { IntArray(2) { -1 } }
        val degree = IntArray(count)
        for (edge in edges) {
            val first = (edge / count).toInt()
            val second = (edge % count).toInt()
            adjacency[first][degree[first]++] = second
            adjacency[second][degree[second]++] = first
        }

        // Choose a start so the path's final edge points off the board. That
        // keeps the last arrow open and avoids a backward edge / cycle there.
        var forcedPrevious = -1
        var start = -1
        for (cell in 0 until count) {
            if (degree[cell] < 2) continue
            for (neighbor in adjacency[cell]) {
                val head = point(neighbor, layoutSize)
                val previousPoint = point(cell, layoutSize)
                val direction = ArrowDirection.between(previousPoint, head) ?: continue
                if (!isOutward(head, previousPoint, direction, layoutSize, mask)) continue
                val other = adjacency[neighbor].firstOrNull { it != cell } ?: continue
                start = other
                forcedPrevious = neighbor
                break
            }
            if (start >= 0) break
        }
        val pathLength = if (mask == null) count else mask.count { it }
        if (start < 0) {
            start = if (mask == null) {
                random.nextInt(count)
            } else {
                (0 until count).filter { mask[it] }.random(random)
            }
        }

        val path = IntArray(pathLength)
        var previous = forcedPrevious
        var current = start
        for (i in 0 until pathLength) {
            path[i] = current
            val neighbors = adjacency[current]
            val next = if (neighbors[0] != previous) neighbors[0] else neighbors[1]
            previous = current
            current = next
        }
        return path
    }

    private fun serpentinePath(layoutSize: Int): IntArray {
        val count = layoutSize * layoutSize
        val path = IntArray(count)
        var index = 0
        for (y in 0 until layoutSize) {
            if (y % 2 == 0) {
                for (x in 0 until layoutSize) path[index++] = y * layoutSize + x
            } else {
                for (x in layoutSize - 1 downTo 0) path[index++] = y * layoutSize + x
            }
        }
        return path
    }


    private data class BlockEdge(
        val first: Int,
        val second: Int,
        val horizontal: Boolean,
        val bx: Int,
        val by: Int,
    )

    private fun chooseCuts(
        path: IntArray,
        layoutSize: Int,
        openCount: Int,
        targetArrows: Int,
        random: Random,
        mask: BooleanArray? = null,
    ): IntArray? {
        val count = path.size
        if (targetArrows < openCount || targetArrows > count - 1) return null

        // Cells outside a mask have no path index; they never count as an
        // earlier part of the path.
        val pathIndex = IntArray(layoutSize * layoutSize) { Int.MAX_VALUE }
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
                isOutward(head, previous, headDirection, layoutSize, mask) ->
                    openCandidates += i
                // Never point at an arrow that comes earlier in the path:
                // those edges could close a dependency cycle. All remaining
                // edges point forward, so the blocking graph stays acyclic.
                hasBackwardCellOnRay(head, headDirection, i, pathIndex, layoutSize) ->
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
        val neededOpenCuts = if (isLastArrowOpen(path, layoutSize, mask)) {
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

        // Place the remaining cuts at roughly even intervals, choosing the
        // closest forward (straight/turn) candidate. Even spacing prevents the
        // worst long self-coiled arrows without unsafe after-the-fact splits,
        // and forward candidates keep the blocking graph acyclic.
        val needed = targetArrows - cuts.size
        if (needed > 0) {
            val pool = ArrayList<Int>(straightCandidates.size + turnCandidates.size)
            pool += straightCandidates
            pool += turnCandidates
            pool.removeAll { candidate -> cuts.any { abs(it - candidate) < 2 } }
            pool.sort()
            for (k in 0 until needed) {
                if (pool.isEmpty()) break
                val desired = ((k + 1).toLong() * count / (needed + 1)).toInt()
                var bestIndex = 0
                var bestDistance = Int.MAX_VALUE
                for (i in pool.indices) {
                    val distance = abs(pool[i] - desired)
                    if (distance < bestDistance) {
                        bestDistance = distance
                        bestIndex = i
                    }
                }
                val best = pool[bestIndex]
                cuts += best
                pool.removeAll { abs(it - best) < 2 }
            }
        }
        if (cuts.size < targetArrows) {
            for (candidate in relaxedCandidates.shuffled(random)) {
                if (cuts.size >= targetArrows) break
                if (cuts.all { abs(it - candidate) >= 2 }) {
                    cuts += candidate
                }
            }
        }
        if (cuts.size < targetArrows) return null
        return mergeShortRuns(
            path = path,
            layoutSize = layoutSize,
            cuts = cuts.sorted().toIntArray(),
            targetArrows = targetArrows,
            straightCandidates = straightCandidates,
            turnCandidates = turnCandidates,
            mask = mask,
        )
    }

    /** A cut set plus, per arrow, whether its head sits at its first path cell. */
    private class CutSet(val cuts: IntArray, val flipped: BooleanArray)

    /**
     * Splits arrows longer than [MAX_ARROW_LENGTH] cells.
     *
     * Inside a very long arrow there is usually no cut whose ray only hits
     * later path cells, so a split may close a dependency cycle. Each split is
     * therefore checked against the real blocking graph ([evaluateCuts]). To
     * keep the arrow count (and with it the score maximum) unchanged, every
     * accepted split is paired with a merge of two short neighbouring arrows
     * elsewhere. A pair is only kept when the board stays acyclic and the
     * number of open arrows does not change. Arrows without a safe pair stay
     * as they are, so the cap is soft.
     */
    private fun capLongArrows(
        path: IntArray,
        layoutSize: Int,
        cuts: IntArray,
        flipped: BooleanArray,
    ): CutSet {
        val original = CutSet(cuts, flipped)
        val openCount = evaluateCuts(path, layoutSize, cuts, flipped) ?: return original
        var current = original
        // End cuts of arrows that had no safe split. Other splits can make
        // them splittable later, so they get another chance after progress.
        val unsplittable = HashSet<Int>()
        var progressSinceReset = false

        repeat(MAX_CAP_ROUNDS) {
            var longest = -1
            var longestLength = MAX_ARROW_LENGTH
            val c = current.cuts
            for (i in c.indices) {
                val start = if (i == 0) 0 else c[i - 1] + 1
                val length = c[i] - start + 1
                if (length > longestLength && c[i] !in unsplittable) {
                    longest = i
                    longestLength = length
                }
            }
            if (longest < 0) {
                if (!progressSinceReset || unsplittable.isEmpty()) return current
                unsplittable.clear()
                progressSinceReset = false
                return@repeat
            }
            val accepted = trySplitAndMerge(path, layoutSize, current, longest, openCount)
            if (accepted == null) {
                unsplittable += c[longest]
            } else {
                current = accepted
                progressSinceReset = true
            }
        }
        return current
    }

    private fun trySplitAndMerge(
        path: IntArray,
        layoutSize: Int,
        set: CutSet,
        index: Int,
        openCount: Int,
    ): CutSet? {
        val cuts = set.cuts
        val start = if (index == 0) 0 else cuts[index - 1] + 1
        val end = cuts[index]
        val headAtStart = set.flipped[index]
        // Long arrows coil around themselves, so after a split the two halves
        // often block each other. Safe points are rare and clustered; try every
        // position, most balanced first. [evaluateCuts] is cheap enough for it.
        val middle = (start + end) / 2
        val from = start + 1
        val to = end - 2
        if (from > to) return null
        val splits = (from..to).sortedBy { abs(it - middle) }
        for (split in splits) {
            // The half that keeps the original head keeps its orientation; the
            // new half may point either way.
            for (newHalfFlipped in BOOLEANS) {
                val firstFlipped = if (headAtStart) true else newHalfFlipped
                val secondFlipped = if (headAtStart) newHalfFlipped else false
                val withSplit = splitAt(set, index, split, firstFlipped, secondFlipped)
                if (evaluateCuts(path, layoutSize, withSplit.cuts, withSplit.flipped) != openCount) {
                    continue
                }
                mergeSomewhere(path, layoutSize, withSplit, index, openCount)?.let { return it }
            }
        }
        return null
    }

    /** Merges the shortest neighbouring pair that keeps the board valid. */
    private fun mergeSomewhere(
        path: IntArray,
        layoutSize: Int,
        set: CutSet,
        splitIndex: Int,
        openCount: Int,
    ): CutSet? {
        val cuts = set.cuts
        // Removing cut k joins arrows k and k + 1. Never undo the split itself.
        val merges = (0 until cuts.lastIndex)
            .filter { it != splitIndex }
            .map { k ->
                val pairStart = if (k == 0) 0 else cuts[k - 1] + 1
                k to (cuts[k + 1] - pairStart + 1)
            }
            .filter { (_, length) -> length <= MAX_MERGE_FOR_CAP }
            .sortedBy { (_, length) -> length }
            .take(MAX_MERGE_TRIES)
        for ((k, _) in merges) {
            for (mergedFlipped in BOOLEANS) {
                val candidate = mergeAt(set, k, mergedFlipped)
                if (evaluateCuts(path, layoutSize, candidate.cuts, candidate.flipped) == openCount) {
                    return candidate
                }
            }
        }
        return null
    }

    private fun splitAt(
        set: CutSet,
        index: Int,
        split: Int,
        firstFlipped: Boolean,
        secondFlipped: Boolean,
    ): CutSet {
        val size = set.cuts.size + 1
        val cuts = IntArray(size)
        val flipped = BooleanArray(size)
        for (i in 0 until size) {
            when {
                i < index -> {
                    cuts[i] = set.cuts[i]
                    flipped[i] = set.flipped[i]
                }
                i == index -> {
                    cuts[i] = split
                    flipped[i] = firstFlipped
                }
                i == index + 1 -> {
                    cuts[i] = set.cuts[index]
                    flipped[i] = secondFlipped
                }
                else -> {
                    cuts[i] = set.cuts[i - 1]
                    flipped[i] = set.flipped[i - 1]
                }
            }
        }
        return CutSet(cuts, flipped)
    }

    private fun mergeAt(set: CutSet, k: Int, mergedFlipped: Boolean): CutSet {
        val size = set.cuts.size - 1
        val cuts = IntArray(size)
        val flipped = BooleanArray(size)
        for (i in 0 until size) {
            when {
                i < k -> {
                    cuts[i] = set.cuts[i]
                    flipped[i] = set.flipped[i]
                }
                i == k -> {
                    cuts[i] = set.cuts[k + 1]
                    flipped[i] = mergedFlipped
                }
                else -> {
                    cuts[i] = set.cuts[i + 1]
                    flipped[i] = set.flipped[i + 1]
                }
            }
        }
        return CutSet(cuts, flipped)
    }

    /** Arrow index per cell. Assumes a valid cut set. */
    /** Arrow index per cell, -1 for cells outside the board mask. */
    private fun ownerOf(path: IntArray, cuts: IntArray, layoutSize: Int): IntArray {
        val owner = IntArray(layoutSize * layoutSize) { -1 }
        var start = 0
        for (a in cuts.indices) {
            for (i in start..cuts[a]) owner[path[i]] = a
            start = cuts[a] + 1
        }
        return owner
    }

    /**
     * Walks the cells on the forward ray of arrow [a], nearest first, until
     * [visit] returns false or the ray leaves the board.
     */
    private inline fun forEachRayCell(
        path: IntArray,
        layoutSize: Int,
        cuts: IntArray,
        flipped: BooleanArray,
        a: Int,
        visit: (Int) -> Boolean,
    ) {
        val start = if (a == 0) 0 else cuts[a - 1] + 1
        val head = if (flipped[a]) path[start] else path[cuts[a]]
        val previous = if (flipped[a]) path[start + 1] else path[cuts[a] - 1]
        val dx = head % layoutSize - previous % layoutSize
        val dy = head / layoutSize - previous / layoutSize
        var x = head % layoutSize + dx
        var y = head / layoutSize + dy
        while (x in 0 until layoutSize && y in 0 until layoutSize) {
            if (!visit(y * layoutSize + x)) return
            x += dx
            y += dy
        }
    }

    /**
     * Open-arrow count of an acyclic cut set, or null if it is invalid or has a
     * dependency cycle.
     *
     * Same rule as [BlockingGraph], but computed directly on the path with
     * primitive arrays: no [Arrow] objects are built, so thousands of
     * candidate cut sets can be checked per level.
     */
    private fun evaluateCuts(
        path: IntArray,
        layoutSize: Int,
        cuts: IntArray,
        flipped: BooleanArray,
    ): Int? {
        val arrowCount = cuts.size
        var start = 0
        for (a in 0 until arrowCount) {
            if (cuts[a] - start + 1 < 2) return null
            start = cuts[a] + 1
        }
        if (start != path.size) return null
        val owner = ownerOf(path, cuts, layoutSize)

        // Edge list "blocked -> blocker", stored as linked lists per blocker.
        val pending = IntArray(arrowCount)
        val firstEdge = IntArray(arrowCount) { -1 }
        val edgeTarget = IntArray(arrowCount * layoutSize)
        val edgeNext = IntArray(arrowCount * layoutSize)
        val seen = IntArray(arrowCount) { -1 }
        var edges = 0
        for (a in 0 until arrowCount) {
            forEachRayCell(path, layoutSize, cuts, flipped, a) { cell ->
                val blocker = owner[cell]
                if (blocker >= 0 && blocker != a && seen[blocker] != a) {
                    seen[blocker] = a
                    pending[a] += 1
                    edgeTarget[edges] = a
                    edgeNext[edges] = firstEdge[blocker]
                    firstEdge[blocker] = edges
                    edges += 1
                }
                true
            }
        }

        val queue = IntArray(arrowCount)
        var tail = 0
        for (a in 0 until arrowCount) if (pending[a] == 0) queue[tail++] = a
        val open = tail
        var headIndex = 0
        while (headIndex < tail) {
            var edge = firstEdge[queue[headIndex++]]
            while (edge >= 0) {
                val blocked = edgeTarget[edge]
                pending[blocked] -= 1
                if (pending[blocked] == 0) queue[tail++] = blocked
                edge = edgeNext[edge]
            }
        }
        return if (tail == arrowCount) open else null
    }

    /**
     * Merges runs of very short, same-direction arrows into one longer arrow.
     * The arrow count is restored by splitting the longest remaining arrows at
     * forward (straight/turn) candidates, so the score max and the open count
     * stay unchanged. If the count cannot be restored, the original cuts are
     * returned.
     */
    private fun mergeShortRuns(
        path: IntArray,
        layoutSize: Int,
        cuts: IntArray,
        targetArrows: Int,
        straightCandidates: List<Int>,
        turnCandidates: List<Int>,
        mask: BooleanArray? = null,
    ): IntArray {
        if (cuts.size != targetArrows) return cuts
        val original = cuts.copyOf()
        val cutList = cuts.toMutableList()
        val remove = HashSet<Int>()

        var index = 0
        while (index < cutList.size) {
            val start = if (index == 0) 0 else cutList[index - 1] + 1
            val end = cutList[index]
            if (end - start + 1 > SHORT_ARROW) {
                index += 1
                continue
            }
            val direction = arrowDirection(path, end, layoutSize)
            if (direction == null) {
                index += 1
                continue
            }
            var runEnd = index
            var mergedLength = end - start + 1
            while (runEnd + 1 < cutList.size) {
                val nextStart = cutList[runEnd] + 1
                val nextEnd = cutList[runEnd + 1]
                val nextLength = nextEnd - nextStart + 1
                if (nextLength > SHORT_ARROW) break
                if (arrowDirection(path, nextEnd, layoutSize) != direction) break
                if (mergedLength + nextLength > MAX_MERGED_ARROW) break
                mergedLength += nextLength
                runEnd += 1
            }
            val runSize = runEnd - index + 1
            if (runSize >= MIN_MERGE_RUN) {
                var containsOpen = false
                for (i in index until runEnd) {
                    if (isOpenCut(path, cutList[i], layoutSize, mask)) {
                        containsOpen = true
                        break
                    }
                }
                if (!containsOpen) {
                    for (i in index until runEnd) remove.add(cutList[i])
                    index = runEnd + 1
                    continue
                }
            }
            index += 1
        }

        if (remove.isEmpty()) return original
        cutList.removeAll { it in remove }

        var splitGuard = 0
        while (cutList.size < targetArrows && splitGuard < 10_000) {
            splitGuard += 1
            var bestIndex = -1
            var bestLength = 0
            var bestSplit = -1
            for (i in cutList.indices) {
                val start = if (i == 0) 0 else cutList[i - 1] + 1
                val end = cutList[i]
                val length = end - start + 1
                if (length <= bestLength) continue
                val split = forwardSplit(start, end, cutList, straightCandidates, turnCandidates)
                if (split != null) {
                    bestLength = length
                    bestIndex = i
                    bestSplit = split
                }
            }
            if (bestIndex < 0) break
            cutList.add(bestSplit)
            cutList.sort()
        }
        return if (cutList.size == targetArrows) cutList.toIntArray() else original
    }

    private fun forwardSplit(
        start: Int,
        end: Int,
        cuts: List<Int>,
        straightCandidates: List<Int>,
        turnCandidates: List<Int>,
    ): Int? {
        val from = start + 1
        val to = end - 2
        if (from > to) return null
        val desired = (start + end) / 2
        var best = -1
        var bestDistance = Int.MAX_VALUE
        for (candidate in straightCandidates) {
            if (candidate !in from..to) continue
            if (cuts.any { abs(it - candidate) < 2 }) continue
            val distance = abs(candidate - desired)
            if (distance < bestDistance) {
                bestDistance = distance
                best = candidate
            }
        }
        for (candidate in turnCandidates) {
            if (candidate !in from..to) continue
            if (cuts.any { abs(it - candidate) < 2 }) continue
            val distance = abs(candidate - desired)
            if (distance < bestDistance) {
                bestDistance = distance
                best = candidate
            }
        }
        return if (best < 0) null else best
    }

    private fun arrowDirection(path: IntArray, cut: Int, layoutSize: Int): ArrowDirection? {
        if (cut <= 0) return null
        return ArrowDirection.between(
            point(path[cut - 1], layoutSize),
            point(path[cut], layoutSize),
        )
    }

    private fun isLastArrowOpen(path: IntArray, layoutSize: Int, mask: BooleanArray?): Boolean {
        val count = path.size
        if (count < 3) return false
        val previous = point(path[count - 2], layoutSize)
        val head = point(path[count - 1], layoutSize)
        val direction = ArrowDirection.between(previous, head) ?: return false
        return isOutward(head, previous, direction, layoutSize, mask)
    }

    private fun buildArrows(
        path: IntArray,
        cuts: IntArray,
        flipped: BooleanArray,
        layoutSize: Int,
    ): List<Arrow>? {
        val arrows = ArrayList<Arrow>(cuts.size)
        var start = 0
        for ((a, cut) in cuts.withIndex()) {
            if (cut < start || cut - start + 1 < 2) return null
            val route = ArrayList<GridPoint>(cut - start + 1)
            val indices = if (flipped[a]) cut downTo start else start..cut
            for (index in indices) {
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

    private fun isOpenCut(path: IntArray, cut: Int, layoutSize: Int, mask: BooleanArray?): Boolean {
        if (cut <= 0) return false
        val previous = point(path[cut - 1], layoutSize)
        val head = point(path[cut], layoutSize)
        val direction = ArrowDirection.between(previous, head) ?: return false
        return isOutward(head, previous, direction, layoutSize, mask)
    }

    /**
     * True when a head at [current] has an empty forward ray from the start,
     * i.e. the arrow would be open. On the full square that means the head is
     * on the border and points off the board. With a [mask] it means the ray
     * crosses no cell of the shape before it leaves the board.
     */
    private fun isOutward(
        current: GridPoint,
        previous: GridPoint,
        direction: ArrowDirection,
        layoutSize: Int,
        mask: BooleanArray?,
    ): Boolean {
        if (mask == null) return isOutwardOnSquare(current, previous, direction, layoutSize)
        if (direction.dx != 0 && direction.dy != 0) return false
        var x = current.x + direction.dx
        var y = current.y + direction.dy
        while (x in 0 until layoutSize && y in 0 until layoutSize) {
            if (mask[y * layoutSize + x]) return false
            x += direction.dx
            y += direction.dy
        }
        return true
    }

    private fun isOutwardOnSquare(
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
