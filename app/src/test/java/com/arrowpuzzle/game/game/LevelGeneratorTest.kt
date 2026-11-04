package com.arrowpuzzle.game.game

import com.arrowpuzzle.game.data.model.ArrowDirection
import com.arrowpuzzle.game.data.model.ComplexityLevel
import com.arrowpuzzle.game.data.model.LevelConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelGeneratorTest {
    private val generator = LevelGenerator(maxAttempts = 60)

    /** Representative levels across the 2000-level curve (keeps tests fast). */
    private val sampleLevels = listOf(
        1, 2, 3, 5, 10, 20, 30, 40, 50, 60, 70, 80, 90, 99, 100,
        101, 120, 150, 200, 250, 300, 350, 400, 450, 499, 500,
        600, 800, 1000, 1500, 2000,
    )

    @Test
    fun campaignLevelsAreValidAndSolvable() {
        for (levelId in sampleLevels) {
            val config = LevelCatalog.defaultConfig(levelId, maxFailures = 5)
            val level = generator.generate(config, seed = 9_191L * levelId)
            assertTrue("Level $levelId must validate", PathValidator.validate(level))
            assertTrue(level.arrowCount >= config.arrowCount)
            assertTrue("Level $levelId must have at least one open arrow", ArrowGeometry.availableArrows(level, emptySet()).isNotEmpty())
            assertTrue("Level $levelId must be clearable", ArrowGeometry.canClearAll(level))
            assertFalse("Level $levelId must be acyclic", ArrowGeometry.hasCycle(level.arrows, level.layoutSize))
            val covered = level.arrows
                .flatMap { ArrowGeometry.routePoints(it.route) }
                .toSet()
                .size
            assertEquals(
                "Level $levelId must have no free lattice points",
                level.layoutSize * level.layoutSize,
                covered,
            )
        }
    }

    @Test
    fun difficultyControlsInitiallyOpenArrows() {
        val easy = generator.generate(
            LevelConfig(0, 5 to 5, 24, 5, null, ComplexityLevel.EASY),
            seed = 11L,
        )
        val medium = generator.generate(
            LevelConfig(0, 5 to 5, 24, 5, null, ComplexityLevel.MEDIUM),
            seed = 12L,
        )
        val hard = generator.generate(
            LevelConfig(0, 5 to 5, 24, 5, null, ComplexityLevel.HARD),
            seed = 13L,
        )

        assertEquals(4, ArrowGeometry.availableArrows(easy, emptySet()).size)
        assertEquals(3, ArrowGeometry.availableArrows(medium, emptySet()).size)
        assertEquals(2, ArrowGeometry.availableArrows(hard, emptySet()).size)
    }

    @Test
    fun everyCampaignLevelUsesTheConfiguredOpenCount() {
        for (levelId in sampleLevels) {
            val config = LevelCatalog.defaultConfig(levelId, maxFailures = 5)
            val level = generator.generate(config, seed = 1_337L + levelId)
            val expected = when (config.complexity) {
                ComplexityLevel.EASY -> 4
                ComplexityLevel.MEDIUM -> 3
                ComplexityLevel.HARD -> 2
            }
            assertEquals(
                "Level $levelId open count",
                expected.coerceAtMost(config.arrowCount),
                ArrowGeometry.availableArrows(level, emptySet()).size,
            )
        }
    }

    @Test
    fun blockedArrowsAreNotTappableAndOpenArrowsAre() {
        val config = LevelConfig(0, 5 to 5, 20, 5, null, ComplexityLevel.EASY)
        val level = generator.generate(config, seed = 77L)
        val open = ArrowGeometry.availableArrows(level, emptySet())
        assertTrue(open.isNotEmpty())

        val openClick = ArrowLogic.evaluateClick(level, emptySet(), open.first().id, 0)
        assertTrue(openClick is ArrowLogic.ClickResult.Correct)

        val blocked = level.arrows.first { arrow ->
            ArrowGeometry.blockersFor(
                arrow,
                level.arrows,
                level.layoutSize,
            ).isNotEmpty()
        }
        val blockedClick = ArrowLogic.evaluateClick(level, emptySet(), blocked.id, 0)
        assertTrue(blockedClick is ArrowLogic.ClickResult.Wrong)
    }

    @Test
    fun clearingArrowsRevealsNewOpenArrows() {
        val config = LevelConfig(0, 5 to 5, 20, 5, null, ComplexityLevel.MEDIUM)
        val level = generator.generate(config, seed = 88L)
        var removed = emptySet<Int>()
        var steps = 0
        while (removed.size < level.arrowCount) {
            val available = ArrowGeometry.availableArrows(level, removed)
            assertTrue("Board must never lock", available.isNotEmpty())
            removed = removed + available.first().id
            steps += 1
            if (steps > level.arrowCount + 2) break
        }
        assertEquals(level.arrowCount, removed.size)
    }

    @Test
    fun levelListStopsAtTheNextHundred() {
        assertEquals(100, LevelCatalog.visibleLevelCount(1))
        assertEquals(100, LevelCatalog.visibleLevelCount(99))
        assertEquals(200, LevelCatalog.visibleLevelCount(100))
        assertEquals(200, LevelCatalog.visibleLevelCount(150))
        assertEquals(300, LevelCatalog.visibleLevelCount(200))
        assertEquals(2000, LevelCatalog.visibleLevelCount(2000))
    }

    @Test
    fun fieldGrowsEveryFiftyLevelsAfterOneHundred() {
        assertEquals(34, LevelCatalog.layoutSizeForLevel(100))
        assertEquals(34, LevelCatalog.layoutSizeForLevel(149))
        assertEquals(35, LevelCatalog.layoutSizeForLevel(150))
        assertEquals(36, LevelCatalog.layoutSizeForLevel(200))
        assertEquals(38, LevelCatalog.layoutSizeForLevel(300))
        assertEquals(50, LevelCatalog.layoutSizeForLevel(900))
        assertEquals(72, LevelCatalog.layoutSizeForLevel(2000))
        for (level in 2..LevelCatalog.MAX_LEVEL) {
            assertTrue(
                "Layout must never shrink at level $level",
                LevelCatalog.layoutSizeForLevel(level) >=
                    LevelCatalog.layoutSizeForLevel(level - 1),
            )
        }
    }

    @Test
    fun campaignHasTwoThousandLevelsOnADifficultyScale() {
        assertEquals(2000, LevelCatalog.MAX_LEVEL)
        for (levelId in 1..LevelCatalog.MAX_LEVEL) {
            assertEquals(
                "Difficulty must map 1:1 to the level id",
                levelId,
                LevelCatalog.difficultyForLevel(levelId),
            )
        }
    }

    @Test
    fun campaignArrowCountsGrowMonotonicallyAndStayInRange() {
        var previous = 0
        for (levelId in 1..LevelCatalog.MAX_LEVEL) {
            val arrows = LevelCatalog.arrowCountForLevel(levelId)
            assertTrue(
                "Level $levelId arrow count must grow (was $previous, now $arrows)",
                arrows >= previous,
            )
            previous = arrows
        }
        assertEquals(34, LevelCatalog.arrowCountForLevel(1))
        assertEquals(90, LevelCatalog.arrowCountForLevel(100))
        assertEquals(160, LevelCatalog.arrowCountForLevel(500))
        assertEquals(160, LevelCatalog.arrowCountForLevel(LevelCatalog.MAX_LEVEL))
    }

    @Test
    fun levelTwentySitsAtTwentyOnTheDifficultyScale() {
        // The campaign maps difficulty 1:1 to the level id, so the twentieth
        // level sits twenty percent in, not at the end of the scale.
        assertEquals(20, LevelCatalog.difficultyForLevel(20))
        assertEquals(ComplexityLevel.EASY, LevelCatalog.complexityForLevel(20))
        assertEquals(4, LevelCatalog.openArrowCountForLevel(20))
        assertTrue(
            "Level 20 must be easier than the finale (more open arrows)",
            LevelCatalog.openArrowCountForLevel(20) > LevelCatalog.openArrowCountForLevel(LevelCatalog.MAX_LEVEL),
        )
    }

    @Test
    fun difficultyComesFromFewerOpenArrowsNotFromMoreArrows() {
        // Measured behaviour: a fuller board gives a naive player MORE valid
        // moves, so piling on arrows makes a level easier. The campaign must
        // therefore get harder only by reducing the open-arrow count, and the
        // arrow count must stay gentle.
        var previousOpen = Int.MAX_VALUE
        for (levelId in 1..LevelCatalog.MAX_LEVEL) {
            val open = LevelCatalog.openArrowCountForLevel(levelId)
            assertTrue(
                "Open arrows must not grow with the level (level $levelId: $open)",
                open <= previousOpen,
            )
            previousOpen = open
        }
        assertEquals(4, LevelCatalog.openArrowCountForLevel(1))
        assertEquals(2, LevelCatalog.openArrowCountForLevel(LevelCatalog.MAX_LEVEL))

        // Arrow count must not grow so steeply that it cancels out the drop.
        val early = LevelCatalog.arrowCountForLevel(1)
        val late = LevelCatalog.arrowCountForLevel(LevelCatalog.MAX_LEVEL)
        assertTrue(
            "Arrow count must stay gentle (level 1: $early, level 500: $late)",
            late <= early * 5,
        )
    }

    @Test
    fun hardLevelsNeverDropToOneOpenArrow() {
        for (levelId in 1..LevelCatalog.MAX_LEVEL) {
            assertTrue(
                "Level $levelId needs more than one open arrow",
                LevelCatalog.openArrowCountForLevel(levelId) >= 2,
            )
        }
        assertEquals(2, LevelCatalog.openArrowCountForLevel(LevelCatalog.MAX_LEVEL))
    }

    @Test
    fun sameSeedProducesSameBoard() {
        val config = LevelCatalog.defaultConfig(3, maxFailures = 5)
        val first = generator.generate(config, seed = 4242L)
        val second = generator.generate(config, seed = 4242L)
        assertEquals(first.arrows, second.arrows)
        assertEquals(first.layoutSize, second.layoutSize)
    }

    @Test
    fun boardsAreCompletelyFilledAndContainLongBentArrows() {
        for (levelId in listOf(1, 20, 40, 60, 100, 250, 500, 2000)) {
            val config = LevelCatalog.defaultConfig(levelId, maxFailures = 5)
            val level = generator.generate(config, seed = 5_000L + levelId)
            val usedPoints = level.arrows
                .flatMap { ArrowGeometry.routePoints(it.route) }
                .toSet()
            val coverage = usedPoints.size.toDouble() / (level.layoutSize * level.layoutSize)
            assertEquals(
                "Level $levelId must be completely filled",
                1.0,
                coverage,
                0.0001,
            )
            assertTrue(
                "Level $levelId needs arrows with at least two bends",
                level.arrows.any { it.route.size >= 4 },
            )
            val lengths = level.arrows.map { it.lengthCells }
            assertTrue("Level $levelId needs varied arrow lengths", lengths.distinct().size >= 6)
            assertTrue("Level $levelId needs a significantly long arrow", lengths.max() >= 20)
            assertTrue(
                "Level $levelId needs heavily bent arrows",
                level.arrows.maxOf(::bends) >= 6,
            )
        }
    }

    @Test
    fun laterLevelsAreDenserAndStaySolvable() {
        // With a 100-level campaign the late levels are harder because they pack
        // more arrows into the board, not because a single arrow is longer than
        // in level 1. What must hold: late boards carry far more arrows, still
        // cover the lattice completely and still have more than one open arrow.
        val early = generator.generate(
            LevelCatalog.defaultConfig(1, maxFailures = 5),
            seed = 31L,
        )
        val late = generator.generate(
            LevelCatalog.defaultConfig(LevelCatalog.MAX_LEVEL, maxFailures = 5),
            seed = 32L,
        )
        assertTrue(
            "Late board needs many more arrows (${late.arrowCount} vs ${early.arrowCount})",
            late.arrowCount > early.arrowCount * 2,
        )
        assertTrue(
            "Late level needs a very long arrow",
            late.arrows.maxOf { it.lengthCells } >= 20,
        )
        assertTrue(
            "Late levels need heavily bent arrows",
            late.arrows.maxOf(::bends) >= 8,
        )
        assertTrue(
            "Late level must still offer more than one open arrow",
            ArrowGeometry.availableArrows(late, emptySet()).size >= 2,
        )
        assertTrue("Late level must be clearable", ArrowGeometry.canClearAll(late))
    }

    @Test
    fun lateArrowsHaveManyBendsAndWideLengthRange() {
        val level = generator.generate(
            LevelCatalog.defaultConfig(LevelCatalog.MAX_LEVEL, maxFailures = 5),
            seed = 5_020L,
        )
        val lengths = level.arrows.map { it.lengthCells }
        assertTrue("Expected short arrows for variation", lengths.count { it <= 4 } > 0)
        assertTrue("Expected very long arrows", lengths.max() >= 30)
        assertTrue("Expected a wide length range", lengths.distinct().size >= 12)
        assertTrue("Expected many deeply bent arrows", level.arrows.count { bends(it) >= 8 } >= 3)
        assertTrue("Expected at least 10 bends in a long arrow", level.arrows.maxOf(::bends) >= 10)
    }
}

private fun bends(arrow: com.arrowpuzzle.game.data.model.Arrow): Int {
    val directions = arrow.route.zipWithNext().mapNotNull { (from, to) ->
        ArrowDirection.between(from, to)
    }
    return directions.zipWithNext().count { (first, second) -> first != second }
}
