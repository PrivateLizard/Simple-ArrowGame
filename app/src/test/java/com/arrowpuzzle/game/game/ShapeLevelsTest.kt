package com.arrowpuzzle.game.game

import com.arrowpuzzle.game.data.model.ComplexityLevel
import com.arrowpuzzle.game.data.model.LevelConfig
import com.arrowpuzzle.game.game.shapes.LevelShape
import com.arrowpuzzle.game.game.shapes.ShapeMask
import com.arrowpuzzle.game.game.shapes.ShapeSize
import com.arrowpuzzle.game.i18n.referenceStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.zip.CRC32

class ShapeLevelsTest {
    private val generator = LevelGenerator()

    @Test
    fun everyShapeAndSizeIsSolvableAndStaysInsideTheShape() {
        for (shape in LevelShape.entries) {
            for (size in ShapeSize.entries) {
                val config = ShapeMask.levelConfig(shape, size, maxFailures = 3)
                val layout = ShapeMask.layoutFor(shape, size.targetCells)
                val cells = ShapeMask.activeCells(shape, layout)
                for (seed in 1L..4L) {
                    val name = "${shape.id}/${size.name}/seed $seed"
                    val level = generator.generate(config, seed = seed * 7_919)
                    assertNull("$name validates", PathValidator.validationError(level))
                    assertTrue("$name is acyclic", level.blocking.isAcyclic())
                    assertEquals("$name open arrows", 3, ArrowGeometry.availableArrows(level, emptySet()).size)
                    assertEquals("$name arrow count", config.arrowCount, level.arrowCount)
                    val covered = level.arrows.flatMap { ArrowGeometry.routePoints(it.route) }.toSet()
                    assertEquals("$name fills the shape", cells, covered.size)
                    assertTrue("$name stays inside", covered.all { level.isCellActive(it.x, it.y) })
                }
            }
        }
    }

    @Test
    fun shapeIdsAreUniqueAndEveryShapeHasAName() {
        val ids = LevelShape.entries.map { it.id }
        assertEquals("unique ids", ids.size, ids.toSet().size)
        for (id in ids) {
            assertTrue("name for $id", referenceStrings.containsKey("shape_$id"))
        }
    }

    @Test
    fun shapeNeedsMoreRoomThanItsCells() {
        for (shape in LevelShape.entries) {
            val layout = ShapeMask.layoutFor(shape, ShapeSize.MEDIUM.targetCells)
            val cells = ShapeMask.activeCells(shape, layout)
            assertTrue("${shape.id} holds the target", cells >= ShapeSize.MEDIUM.targetCells)
            assertTrue("${shape.id} leaves empty space", cells < layout * layout)
            assertEquals("${shape.id} layout is even", 0, layout % 2)
        }
    }

    @Test
    fun everyFifthLevelIsAPictureAndEachBlockShowsEveryShapeOnce() {
        for (block in 0 until 3) {
            val levels = (block * 1000 + 1..(block + 1) * 1000)
            val shapes = levels.mapNotNull(LevelCatalog::pictureShapeForLevel)
            assertEquals("block $block picture levels", 200, shapes.size)
            assertEquals("block $block uses every shape", LevelShape.entries.toSet(), shapes.toSet())
        }
        assertEquals(ShapeSize.SMALL, LevelCatalog.pictureSizeForLevel(1000))
        assertEquals(ShapeSize.MEDIUM, LevelCatalog.pictureSizeForLevel(1005))
        assertEquals(ShapeSize.MEDIUM, LevelCatalog.pictureSizeForLevel(2000))
        assertEquals(ShapeSize.LARGE, LevelCatalog.pictureSizeForLevel(2005))
        assertNull(LevelCatalog.pictureShapeForLevel(4))
    }

    @Test
    fun allPictureLevelsAreValidWithTheExpectedCounts() {
        for (levelId in LevelCatalog.PICTURE_LEVEL_INTERVAL..LevelCatalog.MAX_LEVEL step LevelCatalog.PICTURE_LEVEL_INTERVAL) {
            val level = generator.generate(LevelCatalog.defaultConfig(levelId, 3), 1_000_003L * levelId)
            assertNull("level $levelId", PathValidator.validationError(level))
            assertEquals("level $levelId arrows", LevelCatalog.expectedArrowCountForLevel(levelId), level.arrowCount)
            assertEquals(
                "level $levelId open",
                LevelCatalog.openArrowCountForLevel(levelId),
                ArrowGeometry.availableArrows(level, emptySet()).size,
            )
        }
    }

    /**
     * Guards generated boards against accidental changes. If a change is meant
     * to alter boards, raise [LevelGenerator.VERSION] and update these values.
     */
    @Test
    fun knownBoardsStayTheSame() {
        val expected = mapOf(
            1 to "210c31e4",
            5 to "ae60e304",
            151 to "521b8fd3",
            751 to "61ab6931",
            2005 to "33143fec",
            2999 to "232d3355",
            3000 to "2cfe1bf5",
        )
        for ((levelId, fingerprint) in expected) {
            val level = generator.generate(LevelCatalog.defaultConfig(levelId, 3), 1_000_003L * levelId)
            assertEquals("campaign level $levelId", fingerprint, fingerprint(level))
        }
        val custom = generator.generate(LevelConfig(0, 5 to 5, 24, 5, null, ComplexityLevel.MEDIUM), 11L)
        assertEquals("custom board", "c7802120", fingerprint(custom))
    }

    private fun fingerprint(level: GeneratedLevel): String {
        val text = StringBuilder().append(level.layoutSize).append('|')
        for (arrow in level.arrows) {
            text.append(arrow.id).append(':').append(arrow.direction.ordinal).append(':')
            for (point in arrow.route) text.append(point.x).append(',').append(point.y).append(';')
            text.append('/')
        }
        val crc = CRC32()
        crc.update(text.toString().toByteArray())
        return java.lang.Long.toHexString(crc.value)
    }
}
