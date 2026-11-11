package com.arrowpuzzle.game.game.shapes

import com.arrowpuzzle.game.data.model.ComplexityLevel
import com.arrowpuzzle.game.data.model.LevelConfig
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.roundToInt

/**
 * A region in unit coordinates: x grows to the right, y grows downwards, the
 * whole drawing area is 0..1 in both directions. Shapes are built from these
 * pieces with [Regions].
 */
fun interface ShapeRegion {
    fun contains(x: Double, y: Double): Boolean
}

/** Building blocks for shapes. Combine them with [union] and [minus]. */
object Regions {
    fun circle(cx: Double, cy: Double, r: Double) = ShapeRegion { x, y ->
        val dx = x - cx
        val dy = y - cy
        dx * dx + dy * dy <= r * r
    }

    fun ellipse(cx: Double, cy: Double, rx: Double, ry: Double) = ShapeRegion { x, y ->
        val dx = (x - cx) / rx
        val dy = (y - cy) / ry
        dx * dx + dy * dy <= 1.0
    }

    /** Closed polygon; points as x1, y1, x2, y2, ... (even-odd rule). */
    fun polygon(vararg xy: Double): ShapeRegion {
        require(xy.size >= 6 && xy.size % 2 == 0) { "polygon needs at least three points" }
        val xs = DoubleArray(xy.size / 2) { xy[it * 2] }
        val ys = DoubleArray(xy.size / 2) { xy[it * 2 + 1] }
        return ShapeRegion { x, y ->
            var inside = false
            var j = xs.lastIndex
            for (i in xs.indices) {
                if ((ys[i] > y) != (ys[j] > y) &&
                    x < (xs[j] - xs[i]) * (y - ys[i]) / (ys[j] - ys[i]) + xs[i]
                ) {
                    inside = !inside
                }
                j = i
            }
            inside
        }
    }

    /**
     * Pixel-art shape: one string per row, '#' or 'X' = filled. The picture is
     * stretched over the unit square, so the grid size does not matter. The
     * easiest way to add a new shape by hand.
     */
    fun bitmap(vararg rows: String): ShapeRegion {
        require(rows.isNotEmpty()) { "bitmap needs rows" }
        val height = rows.size
        val width = rows.maxOf { it.length }
        return ShapeRegion { x, y ->
            val row = (y * height).toInt().coerceIn(0, height - 1)
            val column = (x * width).toInt().coerceIn(0, width - 1)
            rows[row].getOrNull(column).let { it == '#' || it == 'X' }
        }
    }

    fun rect(x0: Double, y0: Double, x1: Double, y1: Double) = ShapeRegion { x, y ->
        x in x0..x1 && y in y0..y1
    }

    /** Regular polygon with [sides] corners; the first corner points up. */
    fun regularPolygon(cx: Double, cy: Double, r: Double, sides: Int, rotationDeg: Double = 0.0): ShapeRegion {
        val points = DoubleArray(sides * 2)
        for (i in 0 until sides) {
            val angle = Math.toRadians(-90.0 + rotationDeg + 360.0 * i / sides)
            points[i * 2] = cx + r * cos(angle)
            points[i * 2 + 1] = cy + r * sin(angle)
        }
        return polygon(*points)
    }

    /** Star with [tips] points, outer radius [outer], inner radius [inner]. */
    fun star(cx: Double, cy: Double, outer: Double, inner: Double, tips: Int = 5): ShapeRegion {
        val points = DoubleArray(tips * 4)
        for (i in 0 until tips * 2) {
            val radius = if (i % 2 == 0) outer else inner
            val angle = Math.toRadians(-90.0 + 180.0 * i / tips)
            points[i * 2] = cx + radius * cos(angle)
            points[i * 2 + 1] = cy + radius * sin(angle)
        }
        return polygon(*points)
    }

    /** Straight bar of [length] x [width] centred on (cx, cy), rotated by [angleDeg]. */
    fun bar(cx: Double, cy: Double, length: Double, width: Double, angleDeg: Double): ShapeRegion {
        val angle = Math.toRadians(angleDeg)
        val c = cos(angle)
        val s = sin(angle)
        return ShapeRegion { x, y ->
            val dx = x - cx
            val dy = y - cy
            val along = dx * c + dy * s
            val across = -dx * s + dy * c
            kotlin.math.abs(along) <= length / 2 && kotlin.math.abs(across) <= width / 2
        }
    }

    /** Top half (y above [cutY]) of [region]. */
    fun above(region: ShapeRegion, cutY: Double) = ShapeRegion { x, y -> y <= cutY && region.contains(x, y) }

    /** Bottom half (y below [cutY]) of [region]. */
    fun below(region: ShapeRegion, cutY: Double) = ShapeRegion { x, y -> y >= cutY && region.contains(x, y) }

    fun intersect(vararg parts: ShapeRegion) = ShapeRegion { x, y -> parts.all { it.contains(x, y) } }

    fun union(vararg parts: ShapeRegion) = ShapeRegion { x, y -> parts.any { it.contains(x, y) } }

    fun minus(base: ShapeRegion, cut: ShapeRegion) = ShapeRegion { x, y ->
        base.contains(x, y) && !cut.contains(x, y)
    }

    /** Thick line from (x1, y1) to (x2, y2) with round ends; [r] is half the width. */
    fun capsule(x1: Double, y1: Double, x2: Double, y2: Double, r: Double): ShapeRegion {
        val dx = x2 - x1
        val dy = y2 - y1
        val lengthSquared = dx * dx + dy * dy
        return ShapeRegion { x, y ->
            val t = if (lengthSquared > 0.0) {
                (((x - x1) * dx + (y - y1) * dy) / lengthSquared).coerceIn(0.0, 1.0)
            } else {
                0.0
            }
            val px = x - (x1 + t * dx)
            val py = y - (y1 + t * dy)
            px * px + py * py <= r * r
        }
    }

    /** [region] turned clockwise (on screen) by [angleDeg] around (cx, cy). */
    fun rotate(region: ShapeRegion, angleDeg: Double, cx: Double = 0.5, cy: Double = 0.5): ShapeRegion {
        val angle = Math.toRadians(angleDeg)
        val c = cos(angle)
        val s = sin(angle)
        return ShapeRegion { x, y ->
            val dx = x - cx
            val dy = y - cy
            region.contains(cx + dx * c + dy * s, cy - dx * s + dy * c)
        }
    }

    /** [region] mirrored left to right. */
    fun mirror(region: ShapeRegion) = ShapeRegion { x, y -> region.contains(1.0 - x, y) }
}

/**
 * Level shapes for the Test mode. A shape is only a region; the board size is
 * chosen afterwards so that the shape holds the requested number of cells
 * ("Verschnitt" is compensated by a larger square).
 *
 * To add a shape: add an entry with a stable [id] (it is part of navigation
 * routes) and a translation key `shape_<id>` in Strings.kt.
 */
enum class LevelShape(val id: String, val region: ShapeRegion) {
    HEART(
        "heart",
        // Classic implicit heart curve (x² + y² − 1)³ − x²·y³ ≤ 0, mapped into
        // the unit square with the tip at the bottom.
        ShapeRegion { x, y ->
            val u = (x - 0.5) * 2.55
            val v = 1.34 - y * 2.45
            (u * u + v * v - 1.0).pow(3) - u * u * v * v * v <= 0.0
        },
    ),
    TRIANGLE(
        "triangle",
        Regions.polygon(0.5, 0.04, 0.97, 0.94, 0.03, 0.94),
    ),
    TEDDY(
        "teddy",
        Regions.union(
            Regions.circle(0.50, 0.30, 0.19), // head
            Regions.circle(0.32, 0.13, 0.09), // left ear
            Regions.circle(0.68, 0.13, 0.09), // right ear
            Regions.ellipse(0.50, 0.66, 0.22, 0.25), // body
            Regions.circle(0.27, 0.57, 0.095), // left arm
            Regions.circle(0.73, 0.57, 0.095), // right arm
            Regions.circle(0.35, 0.88, 0.10), // left leg
            Regions.circle(0.65, 0.88, 0.10), // right leg
        ),
    ),
    CIRCLE("circle", Regions.circle(0.5, 0.5, 0.46)),
    STAR("star", Regions.star(0.5, 0.54, 0.50, 0.22)),
    DIAMOND("diamond", Regions.polygon(0.5, 0.03, 0.90, 0.5, 0.5, 0.97, 0.10, 0.5)),
    HEXAGON("hexagon", Regions.regularPolygon(0.5, 0.5, 0.47, 6, rotationDeg = 30.0)),
    PLUS(
        "plus",
        Regions.union(Regions.rect(0.35, 0.05, 0.65, 0.95), Regions.rect(0.05, 0.35, 0.95, 0.65)),
    ),
    RING("ring", Regions.minus(Regions.circle(0.5, 0.5, 0.46), Regions.circle(0.5, 0.5, 0.22))),
    HOURGLASS(
        "hourglass",
        Regions.union(
            Regions.polygon(0.12, 0.05, 0.88, 0.05, 0.60, 0.50, 0.88, 0.95, 0.12, 0.95, 0.40, 0.50),
            Regions.rect(0.08, 0.03, 0.92, 0.10), // top plate
            Regions.rect(0.08, 0.90, 0.92, 0.97), // bottom plate
        ),
    ),
    MOON("moon", Regions.minus(Regions.circle(0.5, 0.5, 0.45), Regions.circle(0.68, 0.38, 0.37))),
    CLOUD("cloud", DetailedShapes.cloud()),
    TREE(
        "tree",
        Regions.union(
            Regions.polygon(0.5, 0.03, 0.76, 0.34, 0.24, 0.34),
            Regions.polygon(0.5, 0.18, 0.86, 0.58, 0.14, 0.58),
            Regions.polygon(0.5, 0.36, 0.96, 0.82, 0.04, 0.82),
            Regions.rect(0.41, 0.80, 0.59, 0.97), // trunk
        ),
    ),
    APPLE(
        "apple",
        Regions.union(
            Regions.circle(0.37, 0.60, 0.30),
            Regions.circle(0.63, 0.60, 0.30),
            Regions.rect(0.46, 0.10, 0.54, 0.32), // stem
            Regions.ellipse(0.65, 0.17, 0.13, 0.07), // leaf
        ),
    ),
    MUSHROOM(
        "mushroom",
        Regions.union(
            Regions.minus(Regions.ellipse(0.5, 0.48, 0.47, 0.42), Regions.rect(0.0, 0.50, 1.0, 1.0)),
            Regions.rect(0.36, 0.46, 0.64, 0.95), // stem
        ),
    ),
    DROP(
        "drop",
        Regions.union(Regions.circle(0.5, 0.64, 0.31), Regions.polygon(0.5, 0.03, 0.79, 0.55, 0.21, 0.55)),
    ),
    FLOWER(
        "flower",
        Regions.union(
            Regions.circle(0.50, 0.38, 0.14), // centre
            Regions.circle(0.50, 0.17, 0.13),
            Regions.circle(0.70, 0.31, 0.13),
            Regions.circle(0.62, 0.55, 0.13),
            Regions.circle(0.38, 0.55, 0.13),
            Regions.circle(0.30, 0.31, 0.13),
            Regions.rect(0.46, 0.60, 0.54, 0.97), // stem
            Regions.ellipse(0.64, 0.80, 0.13, 0.06), // leaf
        ),
    ),
    LIGHTNING("lightning", DetailedShapes.lightning()),
    FISH(
        "fish",
        Regions.minus(
            Regions.union(
                Regions.ellipse(0.42, 0.50, 0.34, 0.25), // body
                Regions.polygon(0.66, 0.50, 0.97, 0.22, 0.97, 0.78), // tail
            ),
            Regions.circle(0.24, 0.44, 0.04), // eye
        ),
    ),
    CAT(
        "cat",
        Regions.union(
            Regions.circle(0.5, 0.58, 0.36), // head
            Regions.polygon(0.17, 0.46, 0.21, 0.05, 0.46, 0.27), // left ear
            Regions.polygon(0.83, 0.46, 0.79, 0.05, 0.54, 0.27), // right ear
        ),
    ),
    BUTTERFLY("butterfly", DetailedShapes.butterfly()),
    BUNNY(
        "bunny",
        Regions.union(
            Regions.circle(0.5, 0.67, 0.28), // head
            Regions.ellipse(0.37, 0.27, 0.09, 0.24), // left ear
            Regions.ellipse(0.63, 0.27, 0.09, 0.24), // right ear
        ),
    ),
    HOUSE(
        "house",
        Regions.minus(
            Regions.union(
                Regions.polygon(0.5, 0.05, 0.96, 0.47, 0.04, 0.47), // roof
                Regions.rect(0.14, 0.45, 0.86, 0.95), // walls
                Regions.rect(0.68, 0.10, 0.80, 0.36), // chimney
            ),
            Regions.rect(0.42, 0.68, 0.58, 0.96), // door
        ),
    ),
    BELL(
        "bell",
        Regions.union(
            Regions.circle(0.5, 0.09, 0.06), // knob
            Regions.ellipse(0.5, 0.40, 0.25, 0.27), // dome
            Regions.polygon(0.25, 0.40, 0.75, 0.40, 0.90, 0.80, 0.10, 0.80), // body
            Regions.rect(0.06, 0.77, 0.94, 0.86), // rim
            Regions.circle(0.5, 0.90, 0.07), // clapper
        ),
    ),
    GHOST(
        "ghost",
        Regions.minus(
            Regions.union(
                Regions.circle(0.5, 0.40, 0.34),
                Regions.rect(0.16, 0.40, 0.84, 0.84),
                Regions.circle(0.27, 0.84, 0.11),
                Regions.circle(0.50, 0.84, 0.11),
                Regions.circle(0.73, 0.84, 0.11),
            ),
            Regions.union(Regions.circle(0.38, 0.38, 0.06), Regions.circle(0.62, 0.38, 0.06)), // eyes
        ),
    ),
    ROCKET(
        "rocket",
        Regions.minus(
            Regions.union(
                Regions.ellipse(0.5, 0.45, 0.17, 0.41), // body
                Regions.polygon(0.36, 0.56, 0.36, 0.86, 0.14, 0.92), // left fin
                Regions.polygon(0.64, 0.56, 0.64, 0.86, 0.86, 0.92), // right fin
                Regions.polygon(0.41, 0.82, 0.59, 0.82, 0.5, 0.99), // flame
            ),
            Regions.circle(0.5, 0.36, 0.06), // window
        ),
    ),
    CROWN(
        "crown",
        Regions.polygon(0.05, 0.86, 0.05, 0.28, 0.28, 0.56, 0.5, 0.18, 0.72, 0.56, 0.95, 0.28, 0.95, 0.86),
    ),
    ARROW(
        "arrow",
        Regions.polygon(0.04, 0.37, 0.54, 0.37, 0.54, 0.14, 0.96, 0.5, 0.54, 0.86, 0.54, 0.63, 0.04, 0.63),
    ),
    SHIELD(
        "shield",
        Regions.union(
            Regions.rect(0.10, 0.08, 0.90, 0.50),
            Regions.ellipse(0.5, 0.50, 0.40, 0.46).let { lower ->
                Regions.minus(lower, Regions.rect(0.0, 0.0, 1.0, 0.50))
            },
        ),
    ),
    SNOWMAN(
        "snowman",
        Regions.union(
            Regions.circle(0.5, 0.76, 0.22),
            Regions.circle(0.5, 0.46, 0.15),
            Regions.circle(0.5, 0.24, 0.11),
            Regions.rect(0.40, 0.02, 0.60, 0.13), // hat
            Regions.rect(0.32, 0.12, 0.68, 0.16), // hat brim
        ),
    ),
    OVAL(
        "oval",
        Regions.ellipse(0.5, 0.5, 0.46, 0.32),
    ),
    PENTAGON(
        "pentagon",
        Regions.regularPolygon(0.5, 0.54, 0.48, 5),
    ),
    OCTAGON(
        "octagon",
        Regions.regularPolygon(0.5, 0.5, 0.48, 8, rotationDeg = 22.5),
    ),
    DEER("deer", DetailedShapes.deer()),
    PARALLELOGRAM(
        "parallelogram",
        Regions.polygon(0.28, 0.15, 0.97, 0.15, 0.72, 0.85, 0.03, 0.85),
    ),
    TRAPEZOID(
        "trapezoid",
        Regions.polygon(0.25, 0.18, 0.75, 0.18, 0.97, 0.84, 0.03, 0.84),
    ),
    SEMICIRCLE(
        "semicircle",
        Regions.above(Regions.circle(0.5, 0.74, 0.47), 0.74),
    ),
    CHEVRON(
        "chevron",
        Regions.polygon(0.08, 0.08, 0.48, 0.5, 0.08, 0.92, 0.36, 0.92, 0.76, 0.5, 0.36, 0.08),
    ),
    STAR6(
        "star6",
        Regions.star(0.5, 0.5, 0.48, 0.28, tips = 6),
    ),
    SPARKLE(
        "sparkle",
        Regions.star(0.5, 0.5, 0.48, 0.17, tips = 4),
    ),
    XCROSS(
        "xcross",
        Regions.union(
            Regions.bar(0.5, 0.5, 1.2, 0.26, 45.0),
            Regions.bar(0.5, 0.5, 1.2, 0.26, -45.0),
        ),
    ),
    FRAME(
        "frame",
        Regions.minus(Regions.rect(0.05, 0.05, 0.95, 0.95), Regions.rect(0.3, 0.3, 0.7, 0.7)),
    ),
    SUN(
        "sun",
        Regions.union(
            Regions.circle(0.5, 0.5, 0.27),
            Regions.star(0.5, 0.5, 0.48, 0.30, tips = 12),
        ),
    ),
    LEAF(
        "leaf",
        Regions.union(
            Regions.intersect(Regions.circle(0.32, 0.68, 0.56), Regions.circle(0.68, 0.32, 0.56)),
            Regions.bar(0.84, 0.84, 0.3, 0.12, 45.0),
        ),
    ),
    CACTUS(
        "cactus",
        Regions.union(
            Regions.rect(0.4, 0.14, 0.6, 0.96),
            Regions.circle(0.5, 0.14, 0.1),
            Regions.rect(0.2, 0.5, 0.42, 0.6),
            Regions.rect(0.14, 0.28, 0.26, 0.6),
            Regions.circle(0.2, 0.28, 0.06),
            Regions.rect(0.58, 0.4, 0.8, 0.5),
            Regions.rect(0.74, 0.18, 0.86, 0.5),
            Regions.circle(0.8, 0.18, 0.06),
        ),
    ),
    TULIP(
        "tulip",
        Regions.union(
            Regions.ellipse(0.5, 0.38, 0.24, 0.22),
            Regions.polygon(0.26, 0.38, 0.26, 0.14, 0.38, 0.28, 0.5, 0.1, 0.62, 0.28, 0.74, 0.14, 0.74, 0.38),
            Regions.rect(0.46, 0.56, 0.54, 0.97),
            Regions.ellipse(0.64, 0.76, 0.13, 0.06),
        ),
    ),
    CLOVER(
        "clover",
        Regions.union(
            Regions.circle(0.5, 0.31, 0.18),
            Regions.circle(0.31, 0.50, 0.18),
            Regions.circle(0.69, 0.50, 0.18),
            Regions.circle(0.5, 0.69, 0.18),
            Regions.circle(0.5, 0.50, 0.12),
            Regions.bar(0.62, 0.86, 0.26, 0.09, 60.0), // stem
        ),
    ),
    ACORN(
        "acorn",
        Regions.union(
            Regions.above(Regions.ellipse(0.5, 0.4, 0.4, 0.26), 0.4),
            Regions.rect(0.1, 0.36, 0.9, 0.44),
            Regions.ellipse(0.5, 0.6, 0.3, 0.34),
            Regions.rect(0.45, 0.06, 0.55, 0.18),
        ),
    ),
    PEAR(
        "pear",
        Regions.union(
            Regions.circle(0.5, 0.68, 0.28),
            Regions.circle(0.5, 0.36, 0.17),
            Regions.polygon(0.34, 0.4, 0.66, 0.4, 0.78, 0.64, 0.22, 0.64),
            Regions.rect(0.47, 0.08, 0.54, 0.22),
        ),
    ),
    BANANA("banana", DetailedShapes.banana()),
    STRAWBERRY(
        "strawberry",
        Regions.union(
            Regions.circle(0.34, 0.42, 0.2),
            Regions.circle(0.66, 0.42, 0.2),
            Regions.polygon(0.15, 0.46, 0.85, 0.46, 0.5, 0.96),
            Regions.polygon(0.28, 0.24, 0.5, 0.12, 0.72, 0.24, 0.5, 0.32),
            Regions.rect(0.46, 0.04, 0.54, 0.16),
        ),
    ),
    LEMON("lemon", DetailedShapes.lemon()),
    CARROT("carrot", DetailedShapes.carrot()),
    MOUNTAIN(
        "mountain",
        Regions.polygon(0.02, 0.92, 0.34, 0.3, 0.46, 0.46, 0.62, 0.1, 0.98, 0.92),
    ),
    SNOWFLAKE("snowflake", DetailedShapes.snowflake()),
    RAINBOW(
        "rainbow",
        Regions.above(Regions.minus(Regions.circle(0.5, 0.82, 0.47), Regions.circle(0.5, 0.82, 0.22)), 0.82),
    ),
    VOLCANO("volcano", DetailedShapes.volcano()),
    EGG(
        "egg",
        Regions.union(
            Regions.ellipse(0.5, 0.6, 0.32, 0.34),
            Regions.ellipse(0.5, 0.42, 0.26, 0.36),
        ),
    ),
    DOG("dog", DetailedShapes.dog()),
    OWL(
        "owl",
        Regions.minus(Regions.union(
            Regions.ellipse(0.5, 0.58, 0.32, 0.38),
            Regions.polygon(0.2, 0.34, 0.24, 0.04, 0.42, 0.24),
            Regions.polygon(0.8, 0.34, 0.76, 0.04, 0.58, 0.24),
        ), Regions.union(
            Regions.circle(0.38, 0.42, 0.08),
            Regions.circle(0.62, 0.42, 0.08),
        )),
    ),
    PENGUIN("penguin", DetailedShapes.penguin()),
    TURTLE(
        "turtle",
        Regions.union(
            Regions.above(Regions.ellipse(0.5, 0.56, 0.36, 0.32), 0.56),
            Regions.rect(0.14, 0.52, 0.86, 0.62),
            Regions.circle(0.88, 0.52, 0.09),
            Regions.rect(0.22, 0.58, 0.32, 0.76),
            Regions.rect(0.66, 0.58, 0.76, 0.76),
            Regions.polygon(0.14, 0.54, 0.03, 0.6, 0.14, 0.62),
        ),
    ),
    SNAIL(
        "snail",
        Regions.union(
            Regions.minus(Regions.circle(0.42, 0.44, 0.28), Regions.circle(0.42, 0.44, 0.08)),
            Regions.rect(0.08, 0.68, 0.92, 0.8),
            Regions.ellipse(0.86, 0.62, 0.08, 0.16),
        ),
    ),
    WHALE("whale", DetailedShapes.whale()),
    DUCK(
        "duck",
        Regions.union(
            Regions.ellipse(0.54, 0.68, 0.36, 0.22),
            Regions.circle(0.32, 0.34, 0.16),
            Regions.polygon(0.17, 0.32, 0.03, 0.4, 0.17, 0.44),
            Regions.rect(0.25, 0.4, 0.41, 0.6),
            Regions.polygon(0.84, 0.58, 0.98, 0.42, 0.92, 0.68),
        ),
    ),
    BIRD("bird", DetailedShapes.bird()),
    ELEPHANT(
        "elephant",
        Regions.union(
            Regions.ellipse(0.56, 0.55, 0.34, 0.26),
            Regions.circle(0.26, 0.42, 0.2),
            Regions.polygon(0.09, 0.44, 0.19, 0.44, 0.17, 0.9, 0.06, 0.9),
            Regions.rect(0.32, 0.7, 0.44, 0.95),
            Regions.rect(0.66, 0.7, 0.78, 0.95),
        ),
    ),
    MOUSE(
        "mouse",
        Regions.union(
            Regions.ellipse(0.44, 0.62, 0.32, 0.22),
            Regions.circle(0.74, 0.52, 0.15),
            Regions.circle(0.68, 0.34, 0.1),
            Regions.polygon(0.86, 0.46, 0.98, 0.56, 0.86, 0.62),
            Regions.rect(0.03, 0.66, 0.14, 0.73),
        ),
    ),
    PIG("pig", DetailedShapes.pig()),
    FOX("fox", DetailedShapes.fox()),
    FROG(
        "frog",
        Regions.minus(Regions.union(
            Regions.ellipse(0.5, 0.62, 0.4, 0.28),
            Regions.circle(0.28, 0.32, 0.15),
            Regions.circle(0.72, 0.32, 0.15),
        ), Regions.union(
            Regions.circle(0.28, 0.32, 0.06),
            Regions.circle(0.72, 0.32, 0.06),
        )),
    ),
    OCTOPUS(
        "octopus",
        Regions.union(
            Regions.ellipse(0.5, 0.36, 0.34, 0.30), // head
            Regions.ellipse(0.24, 0.70, 0.08, 0.25),
            Regions.ellipse(0.41, 0.74, 0.08, 0.24),
            Regions.ellipse(0.59, 0.74, 0.08, 0.24),
            Regions.ellipse(0.76, 0.70, 0.08, 0.25),
        ),
    ),
    CRAB(
        "crab",
        Regions.union(
            Regions.ellipse(0.5, 0.6, 0.3, 0.2),
            Regions.minus(Regions.circle(0.14, 0.28, 0.13), Regions.polygon(0.14, 0.28, 0.02, 0.08, 0.26, 0.08)),
            Regions.minus(Regions.circle(0.86, 0.28, 0.13), Regions.polygon(0.86, 0.28, 0.98, 0.08, 0.74, 0.08)),
            Regions.polygon(0.15, 0.36, 0.26, 0.36, 0.32, 0.58, 0.22, 0.6),
            Regions.polygon(0.85, 0.36, 0.74, 0.36, 0.68, 0.58, 0.78, 0.6),
            Regions.rect(0.24, 0.7, 0.31, 0.9),
            Regions.rect(0.38, 0.74, 0.45, 0.92),
            Regions.rect(0.55, 0.74, 0.62, 0.92),
            Regions.rect(0.69, 0.7, 0.76, 0.9),
        ),
    ),
    DINOSAUR(
        "dinosaur",
        Regions.union(
            Regions.ellipse(0.56, 0.62, 0.28, 0.18),
            Regions.polygon(0.4, 0.58, 0.52, 0.52, 0.3, 0.12, 0.2, 0.18),
            Regions.ellipse(0.2, 0.13, 0.12, 0.07),
            Regions.polygon(0.8, 0.56, 0.98, 0.84, 0.76, 0.7),
            Regions.rect(0.4, 0.7, 0.5, 0.94),
            Regions.rect(0.62, 0.7, 0.72, 0.94),
        ),
    ),
    CAR(
        "car",
        Regions.minus(Regions.union(
            Regions.rect(0.04, 0.45, 0.96, 0.72),
            Regions.polygon(0.22, 0.46, 0.32, 0.22, 0.66, 0.22, 0.8, 0.46),
            Regions.circle(0.26, 0.74, 0.12),
            Regions.circle(0.74, 0.74, 0.12),
        ), Regions.union(
            Regions.polygon(0.33, 0.42, 0.37, 0.28, 0.48, 0.28, 0.48, 0.42),
            Regions.polygon(0.54, 0.42, 0.54, 0.28, 0.63, 0.28, 0.7, 0.42),
        )),
    ),
    SAILBOAT(
        "sailboat",
        Regions.union(
            Regions.polygon(0.06, 0.7, 0.94, 0.7, 0.76, 0.9, 0.24, 0.9),
            Regions.rect(0.46, 0.08, 0.54, 0.72),
            Regions.polygon(0.54, 0.1, 0.54, 0.64, 0.9, 0.64),
            Regions.polygon(0.46, 0.22, 0.46, 0.64, 0.16, 0.64),
        ),
    ),
    PLANE(
        "plane",
        Regions.union(
            Regions.ellipse(0.5, 0.5, 0.09, 0.46),
            Regions.polygon(0.5, 0.28, 0.98, 0.5, 0.98, 0.62, 0.5, 0.54, 0.02, 0.62, 0.02, 0.5),
            Regions.polygon(0.3, 0.9, 0.5, 0.8, 0.7, 0.9, 0.7, 0.96, 0.3, 0.96),
        ),
    ),
    UFO(
        "ufo",
        Regions.union(
            Regions.above(Regions.ellipse(0.5, 0.46, 0.22, 0.22), 0.48),
            Regions.ellipse(0.5, 0.56, 0.47, 0.13),
            Regions.rect(0.3, 0.64, 0.36, 0.8),
            Regions.rect(0.64, 0.64, 0.7, 0.8),
        ),
    ),
    BALLOON(
        "balloon",
        Regions.union(
            Regions.ellipse(0.5, 0.38, 0.3, 0.34),
            Regions.polygon(0.44, 0.7, 0.56, 0.7, 0.5, 0.78),
            Regions.rect(0.46, 0.76, 0.54, 0.98),
        ),
    ),
    KITE(
        "kite",
        Regions.union(
            Regions.polygon(0.5, 0.03, 0.82, 0.36, 0.5, 0.8, 0.18, 0.36),
            Regions.polygon(0.46, 0.78, 0.54, 0.78, 0.58, 0.97, 0.42, 0.97),
        ),
    ),
    GIFT(
        "gift",
        Regions.union(
            Regions.rect(0.1, 0.38, 0.9, 0.94),
            Regions.rect(0.05, 0.26, 0.95, 0.4),
            Regions.ellipse(0.36, 0.18, 0.15, 0.09),
            Regions.ellipse(0.64, 0.18, 0.15, 0.09),
        ),
    ),
    LIGHTBULB(
        "lightbulb",
        Regions.union(
            Regions.circle(0.5, 0.38, 0.3),
            Regions.polygon(0.32, 0.58, 0.68, 0.58, 0.62, 0.72, 0.38, 0.72),
            Regions.rect(0.37, 0.72, 0.63, 0.9),
            Regions.rect(0.43, 0.88, 0.57, 0.96),
        ),
    ),
    KEY(
        "key",
        Regions.union(
            Regions.minus(Regions.circle(0.27, 0.5, 0.22), Regions.circle(0.27, 0.5, 0.09)),
            Regions.rect(0.45, 0.43, 0.97, 0.57),
            Regions.rect(0.73, 0.56, 0.82, 0.72),
            Regions.rect(0.86, 0.56, 0.95, 0.72),
        ),
    ),
    PADLOCK(
        "padlock",
        Regions.minus(Regions.union(
            Regions.rect(0.14, 0.44, 0.86, 0.94),
            Regions.above(Regions.minus(Regions.circle(0.5, 0.46, 0.3), Regions.circle(0.5, 0.46, 0.17)), 0.46),
        ), Regions.union(
            Regions.circle(0.5, 0.63, 0.07),
            Regions.rect(0.46, 0.63, 0.54, 0.8),
        )),
    ),
    MUG(
        "mug",
        Regions.union(
            Regions.rect(0.12, 0.22, 0.7, 0.92),
            Regions.minus(Regions.circle(0.72, 0.56, 0.22), Regions.circle(0.72, 0.56, 0.11)),
        ),
    ),
    TEAPOT(
        "teapot",
        Regions.union(
            Regions.ellipse(0.48, 0.64, 0.3, 0.26),
            Regions.ellipse(0.48, 0.38, 0.17, 0.07),
            Regions.circle(0.48, 0.29, 0.06),
            Regions.polygon(0.74, 0.62, 0.98, 0.34, 0.98, 0.44, 0.78, 0.74),
            Regions.minus(Regions.circle(0.17, 0.62, 0.15), Regions.circle(0.17, 0.62, 0.07)),
        ),
    ),
    BOTTLE(
        "bottle",
        Regions.union(
            Regions.rect(0.3, 0.42, 0.7, 0.95),
            Regions.ellipse(0.5, 0.43, 0.2, 0.13),
            Regions.rect(0.41, 0.1, 0.59, 0.38),
            Regions.rect(0.38, 0.03, 0.62, 0.12),
        ),
    ),
    ICE_CREAM(
        "ice_cream",
        Regions.union(
            Regions.polygon(0.24, 0.5, 0.76, 0.5, 0.5, 0.98),
            Regions.circle(0.5, 0.32, 0.24),
            Regions.circle(0.32, 0.46, 0.14),
            Regions.circle(0.68, 0.46, 0.14),
        ),
    ),
    CUPCAKE(
        "cupcake",
        Regions.union(
            Regions.polygon(0.18, 0.54, 0.82, 0.54, 0.72, 0.95, 0.28, 0.95),
            Regions.circle(0.29, 0.48, 0.15),
            Regions.circle(0.5, 0.4, 0.21),
            Regions.circle(0.71, 0.48, 0.15),
            Regions.circle(0.5, 0.15, 0.08),
        ),
    ),
    CAKE(
        "cake",
        Regions.union(
            Regions.rect(0.06, 0.58, 0.94, 0.94),
            Regions.rect(0.2, 0.32, 0.8, 0.6),
            Regions.rect(0.45, 0.12, 0.55, 0.34),
            Regions.ellipse(0.5, 0.08, 0.06, 0.07),
        ),
    ),
    BURGER("burger", DetailedShapes.burger()),
    CANDY(
        "candy",
        Regions.union(
            Regions.ellipse(0.5, 0.5, 0.25, 0.19),
            Regions.polygon(0.3, 0.5, 0.03, 0.28, 0.03, 0.72),
            Regions.polygon(0.7, 0.5, 0.97, 0.28, 0.97, 0.72),
        ),
    ),
    LOLLIPOP(
        "lollipop",
        Regions.union(
            Regions.circle(0.5, 0.34, 0.29),
            Regions.rect(0.45, 0.6, 0.55, 0.98),
        ),
    ),
    TSHIRT(
        "tshirt",
        Regions.polygon(0.3, 0.08, 0.4, 0.15, 0.6, 0.15, 0.7, 0.08, 0.97, 0.28, 0.84, 0.44, 0.76, 0.38, 0.76, 0.94, 0.24, 0.94, 0.24, 0.38, 0.16, 0.44, 0.03, 0.28),
    ),
    TOP_HAT(
        "top_hat",
        Regions.union(
            Regions.rect(0.27, 0.08, 0.73, 0.76),
            Regions.ellipse(0.5, 0.8, 0.47, 0.1),
        ),
    ),
    UMBRELLA(
        "umbrella",
        Regions.union(
            Regions.above(Regions.ellipse(0.5, 0.52, 0.47, 0.44), 0.52),
            Regions.rect(0.46, 0.5, 0.55, 0.84),
            Regions.below(Regions.minus(Regions.circle(0.38, 0.84, 0.16), Regions.circle(0.38, 0.84, 0.07)), 0.84),
        ),
    ),
    ANCHOR(
        "anchor",
        Regions.union(
            Regions.minus(Regions.circle(0.5, 0.13, 0.1), Regions.circle(0.5, 0.13, 0.04)),
            Regions.rect(0.45, 0.2, 0.55, 0.88),
            Regions.rect(0.26, 0.26, 0.74, 0.35),
            Regions.below(Regions.minus(Regions.circle(0.5, 0.56, 0.4), Regions.circle(0.5, 0.56, 0.29)), 0.62),
            Regions.polygon(0.05, 0.62, 0.2, 0.55, 0.17, 0.74),
            Regions.polygon(0.95, 0.62, 0.8, 0.55, 0.83, 0.74),
        ),
    ),
    GUITAR(
        "guitar",
        Regions.minus(Regions.union(
            Regions.circle(0.38, 0.7, 0.26),
            Regions.circle(0.55, 0.47, 0.19),
            Regions.bar(0.76, 0.26, 0.5, 0.11, -45.0),
            Regions.rect(0.86, 0.03, 0.98, 0.16),
        ), Regions.circle(0.44, 0.63, 0.07)),
    ),
    MUSIC_NOTE(
        "music_note",
        Regions.union(
            Regions.ellipse(0.36, 0.79, 0.21, 0.15),
            Regions.rect(0.47, 0.1, 0.58, 0.78),
            Regions.polygon(0.58, 0.1, 0.88, 0.32, 0.88, 0.48, 0.58, 0.28),
        ),
    ),
    TROPHY(
        "trophy",
        Regions.union(
            Regions.rect(0.2, 0.08, 0.8, 0.3),
            Regions.below(Regions.ellipse(0.5, 0.3, 0.3, 0.28), 0.3),
            Regions.minus(Regions.circle(0.17, 0.25, 0.13), Regions.circle(0.17, 0.25, 0.06)),
            Regions.minus(Regions.circle(0.83, 0.25, 0.13), Regions.circle(0.83, 0.25, 0.06)),
            Regions.rect(0.44, 0.56, 0.56, 0.78),
            Regions.rect(0.25, 0.76, 0.75, 0.93),
        ),
    ),
    SQUIRREL("squirrel", DetailedShapes.squirrel()),
    ARCH("arch", MoreShapes.arch()),
    STAIRS("stairs", MoreShapes.stairs()),
    ZIGZAG("zigzag", MoreShapes.zigzag()),
    WAVE("wave", MoreShapes.wave()),
    INFINITY("infinity", MoreShapes.infinity()),
    DOUBLE_ARROW("double_arrow", MoreShapes.doubleArrow()),
    PIE_SLICE("pie_slice", MoreShapes.pieSlice()),
    PUZZLE_PIECE("puzzle_piece", MoreShapes.puzzlePiece()),
    SPEECH_BUBBLE("speech_bubble", MoreShapes.speechBubble()),
    MAP_PIN("map_pin", MoreShapes.mapPin()),
    MAGNIFIER("magnifier", MoreShapes.magnifier()),
    GEAR("gear", MoreShapes.gear()),
    FLAG("flag", MoreShapes.flag()),
    CHECKMARK("checkmark", MoreShapes.checkmark()),
    HASH("hash", MoreShapes.hash()),
    SMILEY("smiley", MoreShapes.smiley()),
    GEM("gem", MoreShapes.gem()),
    SPADES("spades", MoreShapes.spades()),
    CLUBS("clubs", MoreShapes.clubs()),
    BATTERY("battery", MoreShapes.battery()),
    HORSESHOE("horseshoe", MoreShapes.horseshoe()),
    PALM("palm", DetailedShapes.palm()),
    OAK("oak", MoreShapes.oak()),
    CHERRY("cherry", MoreShapes.cherry()),
    GRAPES("grapes", MoreShapes.grapes()),
    PINEAPPLE("pineapple", MoreShapes.pineapple()),
    WATERMELON("watermelon", MoreShapes.watermelon()),
    PUMPKIN("pumpkin", MoreShapes.pumpkin()),
    SUNFLOWER("sunflower", MoreShapes.sunflower()),
    FEATHER("feather", MoreShapes.feather()),
    SEASHELL("seashell", DetailedShapes.seashell()),
    MAPLE_LEAF("maple_leaf", MoreShapes.mapleLeaf()),
    ISLAND("island", MoreShapes.island()),
    TORNADO("tornado", DetailedShapes.tornado()),
    COMET("comet", MoreShapes.comet()),
    PLANET("planet", DetailedShapes.planet()),
    AVOCADO("avocado", MoreShapes.avocado()),
    BROCCOLI("broccoli", MoreShapes.broccoli()),
    CHILI("chili", DetailedShapes.chili()),
    LION("lion", DetailedShapes.lion()),
    PANDA("panda", MoreShapes.panda()),
    KOALA("koala", MoreShapes.koala()),
    MONKEY("monkey", DetailedShapes.monkey()),
    COW("cow", MoreShapes.cow()),
    HORSE("horse", MoreShapes.horse()),
    GIRAFFE("giraffe", MoreShapes.giraffe()),
    SHEEP("sheep", DetailedShapes.sheep()),
    CHICKEN("chicken", MoreShapes.chicken()),
    SHARK("shark", MoreShapes.shark()),
    DOLPHIN("dolphin", DetailedShapes.dolphin()),
    SEAHORSE("seahorse", MoreShapes.seahorse()),
    JELLYFISH("jellyfish", MoreShapes.jellyfish()),
    BEE("bee", MoreShapes.bee()),
    LADYBUG("ladybug", MoreShapes.ladybug()),
    SPIDER("spider", DetailedShapes.spider()),
    SNAKE("snake", MoreShapes.snake()),
    SWAN("swan", MoreShapes.swan()),
    BAT("bat", DetailedShapes.bat()),
    HEDGEHOG("hedgehog", DetailedShapes.hedgehog()),
    CAMEL("camel", MoreShapes.camel()),
    PAW("paw", DetailedShapes.paw()),
    TRUCK("truck", MoreShapes.truck()),
    BUS("bus", MoreShapes.bus()),
    TRAIN("train", MoreShapes.train()),
    HELICOPTER("helicopter", MoreShapes.helicopter()),
    SUBMARINE("submarine", MoreShapes.submarine()),
    HOT_AIR_BALLOON("hot_air_balloon", DetailedShapes.hotAirBalloon()),
    CASTLE("castle", MoreShapes.castle()),
    TENT("tent", MoreShapes.tent()),
    LIGHTHOUSE("lighthouse", MoreShapes.lighthouse()),
    WINDMILL("windmill", MoreShapes.windmill()),
    IGLOO("igloo", DetailedShapes.igloo()),
    HAMMER("hammer", DetailedShapes.hammer()),
    WRENCH("wrench", MoreShapes.wrench()),
    SCISSORS("scissors", MoreShapes.scissors()),
    SWORD("sword", MoreShapes.sword()),
    PENCIL("pencil", MoreShapes.pencil()),
    BOOK("book", MoreShapes.book()),
    ENVELOPE("envelope", MoreShapes.envelope()),
    CLOCK("clock", MoreShapes.clock()),
    CAMERA("camera", MoreShapes.camera()),
    PHONE("phone", MoreShapes.phone()),
    TV("tv", MoreShapes.tv()),
    HEADPHONES("headphones", MoreShapes.headphones()),
    GAMEPAD("gamepad", MoreShapes.gamepad()),
    DICE("dice", MoreShapes.dice()),
    CHESS_PAWN("chess_pawn", MoreShapes.chessPawn()),
    BOOT("boot", MoreShapes.boot()),
    GLASSES("glasses", MoreShapes.glasses()),
    BOW_TIE("bow_tie", MoreShapes.bowTie()),
    DRESS("dress", MoreShapes.dress()),
    PIZZA("pizza", MoreShapes.pizza()),
    DONUT("donut", MoreShapes.donut()),
    POPSICLE("popsicle", MoreShapes.popsicle()),
    CANDLE("candle", MoreShapes.candle()),
    ROBOT("robot", MoreShapes.robot()),
    TREASURE_CHEST("treasure_chest", MoreShapes.treasureChest()),
    BOMB("bomb", MoreShapes.bomb()),
    MAGNET("magnet", DetailedShapes.magnet()),
    ;

    companion object {
        fun fromId(id: String?): LevelShape? = entries.firstOrNull { it.id == id }
    }
}

/** Size presets of the Test mode: target number of cells inside the shape. */
enum class ShapeSize(val targetCells: Int) {
    SMALL(567),
    MEDIUM(1485),
    LARGE(2970),
    ;

    /** Arrow count of a campaign picture level of this size. */
    val arrowCount: Int get() = ShapeMask.arrowCountFor(targetCells)
}

/**
 * Turns a [LevelShape] into a cell mask for a square layout.
 *
 * The generator builds its Hamiltonian cycle from 2x2 blocks, so the mask is
 * decided per block (a block is in when its centre is in the shape). Only the
 * largest 4-connected group of blocks is kept, because the cycle can only run
 * through connected blocks. All cells of a kept block are active.
 */
object ShapeMask {
    private const val MIN_LAYOUT = 12
    private const val MAX_LAYOUT = 112

    /** Arrows per active cell, about the density of the campaign boards. */
    private const val ARROWS_PER_CELL = 0.08

    private val cache = ConcurrentHashMap<Pair<LevelShape, Int>, BooleanArray>()

    fun mask(shape: LevelShape, layoutSize: Int): BooleanArray {
        require(layoutSize % 2 == 0) { "layout size must be even" }
        return cache.getOrPut(shape to layoutSize) { buildMask(shape, layoutSize) }
    }

    fun activeCells(shape: LevelShape, layoutSize: Int): Int = mask(shape, layoutSize).count { it }

    /**
     * Smallest even layout whose shape holds at least [targetCells] cells, so
     * a shape with much empty space around it gets a bigger square.
     */
    fun layoutFor(shape: LevelShape, targetCells: Int): Int {
        var size = MIN_LAYOUT
        while (size < MAX_LAYOUT && activeCells(shape, size) < targetCells) size += 2
        return size
    }

    /** Test-mode level config for a shape and size (random board, like Custom Mode). */
    fun levelConfig(shape: LevelShape, size: ShapeSize, maxFailures: Int): LevelConfig {
        val layout = layoutFor(shape, size.targetCells)
        return LevelConfig(
            levelId = LevelConfig.CUSTOM_LEVEL_ID,
            gridSize = layout / 2 to layout / 2,
            arrowCount = arrowCountFor(activeCells(shape, layout)),
            maxFailures = maxFailures,
            timeLimitSeconds = null,
            complexity = ComplexityLevel.MEDIUM,
            shapeId = shape.id,
            shapeCells = size.targetCells,
        )
    }

    fun arrowCountFor(activeCells: Int): Int =
        (activeCells * ARROWS_PER_CELL).roundToInt().coerceIn(12, 400)

    private fun buildMask(shape: LevelShape, layoutSize: Int): BooleanArray {
        val blocks = layoutSize / 2
        val active = BooleanArray(blocks * blocks) { index ->
            val bx = index % blocks
            val by = index / blocks
            shape.region.contains((bx + 0.5) / blocks, (by + 0.5) / blocks)
        }
        val kept = largestComponent(active, blocks)
        val mask = BooleanArray(layoutSize * layoutSize)
        for (by in 0 until blocks) {
            for (bx in 0 until blocks) {
                if (!kept[by * blocks + bx]) continue
                for (dy in 0..1) {
                    for (dx in 0..1) {
                        mask[(by * 2 + dy) * layoutSize + bx * 2 + dx] = true
                    }
                }
            }
        }
        return mask
    }

    private fun largestComponent(active: BooleanArray, blocks: Int): BooleanArray {
        val component = IntArray(active.size) { -1 }
        var best = -1
        var bestSize = 0
        var next = 0
        val stack = ArrayDeque<Int>()
        for (startBlock in active.indices) {
            if (!active[startBlock] || component[startBlock] >= 0) continue
            val id = next++
            var size = 0
            stack.addLast(startBlock)
            component[startBlock] = id
            while (stack.isNotEmpty()) {
                val block = stack.removeLast()
                size += 1
                val bx = block % blocks
                val by = block / blocks
                for ((nx, ny) in listOf(bx - 1 to by, bx + 1 to by, bx to by - 1, bx to by + 1)) {
                    if (nx !in 0 until blocks || ny !in 0 until blocks) continue
                    val neighbour = ny * blocks + nx
                    if (active[neighbour] && component[neighbour] < 0) {
                        component[neighbour] = id
                        stack.addLast(neighbour)
                    }
                }
            }
            if (size > bestSize) {
                bestSize = size
                best = id
            }
        }
        return BooleanArray(active.size) { component[it] == best && best >= 0 }
    }
}
