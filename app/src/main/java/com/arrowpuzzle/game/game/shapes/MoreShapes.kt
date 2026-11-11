package com.arrowpuzzle.game.game.shapes

import kotlin.math.abs
import kotlin.math.sin

/**
 * Regions of the second hundred shapes. They are functions in their own object
 * because the enum initialiser of [LevelShape] would otherwise exceed the JVM
 * limit of 64 KB per method. Add further shapes here, not inline in the enum.
 */
internal object MoreShapes {
    fun arch(): ShapeRegion = Regions.minus(Regions.union(Regions.rect(0.1, 0.4, 0.9, 0.95), Regions.above(Regions.ellipse(0.5, 0.4, 0.4, 0.35), 0.4)), Regions.union(Regions.rect(0.32, 0.45, 0.68, 0.96), Regions.above(Regions.ellipse(0.5, 0.45, 0.18, 0.2), 0.45)))

    fun stairs(): ShapeRegion = Regions.polygon(0.05, 0.95, 0.05, 0.75, 0.27, 0.75, 0.27, 0.55, 0.5, 0.55, 0.5, 0.35, 0.72, 0.35, 0.72, 0.12, 0.95, 0.12, 0.95, 0.95)

    fun zigzag(): ShapeRegion = Regions.polygon(0.03, 0.62, 0.27, 0.14, 0.5, 0.52, 0.73, 0.14, 0.97, 0.62, 0.97, 0.9, 0.73, 0.42, 0.5, 0.8, 0.27, 0.42, 0.03, 0.9)

    fun wave(): ShapeRegion = ShapeRegion { x, y -> x in 0.03..0.97 && abs(y - (0.5 + 0.22 * sin(x * 9.42))) <= 0.13 }

    fun infinity(): ShapeRegion = Regions.union(Regions.minus(Regions.circle(0.29, 0.5, 0.25), Regions.circle(0.29, 0.5, 0.1)), Regions.minus(Regions.circle(0.71, 0.5, 0.25), Regions.circle(0.71, 0.5, 0.1)))

    fun doubleArrow(): ShapeRegion = Regions.polygon(0.03, 0.5, 0.28, 0.2, 0.28, 0.38, 0.72, 0.38, 0.72, 0.2, 0.97, 0.5, 0.72, 0.8, 0.72, 0.62, 0.28, 0.62, 0.28, 0.8)

    fun pieSlice(): ShapeRegion = Regions.intersect(Regions.circle(0.1, 0.9, 0.86), Regions.polygon(0.1, 0.9, 0.3, 0.0, 1.0, 0.0, 1.0, 0.7))

    fun puzzlePiece(): ShapeRegion = Regions.minus(Regions.union(Regions.rect(0.16, 0.24, 0.8, 0.88), Regions.circle(0.48, 0.16, 0.12), Regions.circle(0.86, 0.56, 0.12)), Regions.circle(0.16, 0.56, 0.1))

    fun speechBubble(): ShapeRegion = Regions.union(Regions.rect(0.16, 0.12, 0.84, 0.66), Regions.rect(0.06, 0.22, 0.94, 0.56), Regions.circle(0.16, 0.22, 0.1), Regions.circle(0.84, 0.22, 0.1), Regions.circle(0.16, 0.56, 0.1), Regions.circle(0.84, 0.56, 0.1), Regions.polygon(0.24, 0.6, 0.5, 0.6, 0.2, 0.92))

    fun mapPin(): ShapeRegion = Regions.minus(Regions.union(Regions.circle(0.5, 0.36, 0.31), Regions.polygon(0.21, 0.46, 0.79, 0.46, 0.5, 0.97)), Regions.circle(0.5, 0.36, 0.11))

    fun magnifier(): ShapeRegion = Regions.union(Regions.minus(Regions.circle(0.4, 0.4, 0.34), Regions.circle(0.4, 0.4, 0.2)), Regions.bar(0.76, 0.76, 0.46, 0.15, 45.0))

    fun gear(): ShapeRegion = Regions.minus(Regions.union(Regions.circle(0.5, 0.5, 0.35), Regions.bar(0.5, 0.5, 0.94, 0.16, 0.0), Regions.bar(0.5, 0.5, 0.94, 0.16, 45.0), Regions.bar(0.5, 0.5, 0.94, 0.16, 90.0), Regions.bar(0.5, 0.5, 0.94, 0.16, 135.0)), Regions.circle(0.5, 0.5, 0.14))

    fun flag(): ShapeRegion = Regions.union(Regions.rect(0.14, 0.04, 0.24, 0.97), Regions.polygon(0.24, 0.08, 0.92, 0.08, 0.72, 0.28, 0.92, 0.5, 0.24, 0.5))

    fun checkmark(): ShapeRegion = Regions.polygon(0.04, 0.54, 0.18, 0.4, 0.38, 0.6, 0.82, 0.14, 0.96, 0.28, 0.38, 0.88)

    fun hash(): ShapeRegion = Regions.union(Regions.rect(0.28, 0.05, 0.42, 0.95), Regions.rect(0.58, 0.05, 0.72, 0.95), Regions.rect(0.05, 0.28, 0.95, 0.42), Regions.rect(0.05, 0.58, 0.95, 0.72))

    fun smiley(): ShapeRegion = Regions.minus(Regions.circle(0.5, 0.5, 0.46), Regions.union(Regions.circle(0.35, 0.38, 0.07), Regions.circle(0.65, 0.38, 0.07), Regions.below(Regions.minus(Regions.circle(0.5, 0.52, 0.28), Regions.circle(0.5, 0.52, 0.17)), 0.54)))

    fun gem(): ShapeRegion = Regions.polygon(0.24, 0.1, 0.76, 0.1, 0.97, 0.38, 0.5, 0.95, 0.03, 0.38)

    fun spades(): ShapeRegion = Regions.union(Regions.circle(0.31, 0.56, 0.22), Regions.circle(0.69, 0.56, 0.22), Regions.polygon(0.5, 0.04, 0.93, 0.54, 0.07, 0.54), Regions.polygon(0.44, 0.66, 0.56, 0.66, 0.66, 0.95, 0.34, 0.95))

    fun clubs(): ShapeRegion = Regions.union(Regions.circle(0.5, 0.28, 0.22), Regions.circle(0.27, 0.6, 0.22), Regions.circle(0.73, 0.6, 0.22), Regions.polygon(0.45, 0.5, 0.55, 0.5, 0.66, 0.95, 0.34, 0.95))

    fun battery(): ShapeRegion = Regions.minus(Regions.union(Regions.rect(0.05, 0.28, 0.87, 0.72), Regions.rect(0.87, 0.4, 0.96, 0.6)), Regions.rect(0.6, 0.36, 0.8, 0.64))

    fun horseshoe(): ShapeRegion = Regions.minus(Regions.minus(Regions.circle(0.5, 0.46, 0.44), Regions.circle(0.5, 0.46, 0.22)), Regions.rect(0.36, 0.6, 0.64, 1.0))

    fun oak(): ShapeRegion = Regions.union(Regions.circle(0.5, 0.32, 0.28), Regions.circle(0.26, 0.42, 0.2), Regions.circle(0.74, 0.42, 0.2), Regions.circle(0.38, 0.18, 0.16), Regions.circle(0.62, 0.18, 0.16), Regions.rect(0.42, 0.55, 0.58, 0.96))

    fun cherry(): ShapeRegion = Regions.union(Regions.circle(0.3, 0.72, 0.22), Regions.circle(0.72, 0.66, 0.22), Regions.bar(0.45, 0.33, 0.62, 0.08, -61.0), Regions.bar(0.66, 0.28, 0.46, 0.08, 75.0), Regions.ellipse(0.76, 0.1, 0.16, 0.07))

    fun grapes(): ShapeRegion = Regions.union(Regions.circle(0.2, 0.3, 0.16), Regions.circle(0.5, 0.28, 0.16), Regions.circle(0.8, 0.3, 0.16), Regions.circle(0.35, 0.52, 0.16), Regions.circle(0.65, 0.52, 0.16), Regions.circle(0.5, 0.76, 0.16), Regions.rect(0.45, 0.02, 0.55, 0.2))

    fun pineapple(): ShapeRegion = Regions.union(Regions.ellipse(0.5, 0.64, 0.27, 0.33), Regions.polygon(0.5, 0.02, 0.62, 0.34, 0.38, 0.34), Regions.polygon(0.24, 0.1, 0.5, 0.34, 0.38, 0.36), Regions.polygon(0.76, 0.1, 0.5, 0.34, 0.62, 0.36))

    fun watermelon(): ShapeRegion = Regions.minus(Regions.below(Regions.circle(0.5, 0.28, 0.47), 0.28), Regions.union(Regions.circle(0.35, 0.43, 0.05), Regions.circle(0.65, 0.43, 0.05), Regions.circle(0.5, 0.56, 0.05)))

    fun pumpkin(): ShapeRegion = Regions.union(Regions.ellipse(0.3, 0.58, 0.22, 0.32), Regions.ellipse(0.5, 0.58, 0.22, 0.35), Regions.ellipse(0.7, 0.58, 0.22, 0.32), Regions.rect(0.45, 0.1, 0.57, 0.26))

    fun sunflower(): ShapeRegion = Regions.minus(Regions.union(Regions.star(0.5, 0.38, 0.38, 0.24, tips = 12), Regions.rect(0.46, 0.7, 0.54, 0.97), Regions.ellipse(0.66, 0.84, 0.13, 0.06)), Regions.circle(0.5, 0.38, 0.1))

    fun feather(): ShapeRegion = Regions.minus(Regions.union(Regions.intersect(Regions.circle(0.18, 0.72, 0.62), Regions.circle(0.72, 0.18, 0.62)), Regions.bar(0.84, 0.84, 0.36, 0.12, 45.0)), Regions.union(Regions.polygon(0.48, 0.42, 0.66, 0.16, 0.84, 0.34), Regions.polygon(0.56, 0.62, 0.2, 0.6, 0.36, 0.8)))

    fun mapleLeaf(): ShapeRegion = Regions.polygon(0.5, 0.03, 0.6, 0.22, 0.72, 0.16, 0.68, 0.38, 0.92, 0.34, 0.84, 0.48, 0.96, 0.56, 0.72, 0.72, 0.76, 0.82, 0.54, 0.78, 0.54, 0.97, 0.46, 0.97, 0.46, 0.78, 0.24, 0.82, 0.28, 0.72, 0.04, 0.56, 0.16, 0.48, 0.08, 0.34, 0.32, 0.38, 0.28, 0.16, 0.4, 0.22)

    fun island(): ShapeRegion = Regions.union(Regions.below(Regions.ellipse(0.5, 0.8, 0.46, 0.16), 0.72), Regions.polygon(0.46, 0.76, 0.56, 0.76, 0.56, 0.34, 0.5, 0.34), Regions.bar(0.36, 0.3, 0.34, 0.1, 20.0), Regions.bar(0.64, 0.3, 0.34, 0.1, -20.0), Regions.bar(0.36, 0.42, 0.3, 0.1, -30.0), Regions.bar(0.66, 0.42, 0.3, 0.1, 30.0))

    fun comet(): ShapeRegion = Regions.union(Regions.circle(0.72, 0.3, 0.22), Regions.polygon(0.6, 0.12, 0.9, 0.46, 0.1, 0.92))

    fun avocado(): ShapeRegion = Regions.minus(Regions.union(Regions.circle(0.5, 0.66, 0.3), Regions.ellipse(0.5, 0.38, 0.2, 0.3)), Regions.circle(0.5, 0.66, 0.13))

    fun broccoli(): ShapeRegion = Regions.union(Regions.circle(0.28, 0.38, 0.19), Regions.circle(0.5, 0.26, 0.21), Regions.circle(0.72, 0.38, 0.19), Regions.circle(0.5, 0.46, 0.2), Regions.polygon(0.36, 0.5, 0.64, 0.5, 0.6, 0.95, 0.4, 0.95))

    fun panda(): ShapeRegion = Regions.minus(Regions.union(Regions.circle(0.5, 0.56, 0.38), Regions.circle(0.2, 0.24, 0.14), Regions.circle(0.8, 0.24, 0.14)), Regions.union(Regions.ellipse(0.35, 0.52, 0.09, 0.11), Regions.ellipse(0.65, 0.52, 0.09, 0.11), Regions.circle(0.5, 0.7, 0.05)))

    fun koala(): ShapeRegion = Regions.minus(Regions.union(Regions.ellipse(0.5, 0.58, 0.3, 0.34), Regions.circle(0.16, 0.36, 0.16), Regions.circle(0.84, 0.36, 0.16)), Regions.union(Regions.circle(0.38, 0.5, 0.05), Regions.circle(0.62, 0.5, 0.05), Regions.ellipse(0.5, 0.64, 0.07, 0.11)))

    fun cow(): ShapeRegion = Regions.minus(Regions.union(Regions.ellipse(0.5, 0.5, 0.26, 0.3), Regions.ellipse(0.5, 0.76, 0.2, 0.16), Regions.ellipse(0.13, 0.42, 0.13, 0.07), Regions.ellipse(0.87, 0.42, 0.13, 0.07), Regions.polygon(0.28, 0.28, 0.2, 0.06, 0.4, 0.22), Regions.polygon(0.72, 0.28, 0.8, 0.06, 0.6, 0.22)), Regions.union(Regions.circle(0.38, 0.44, 0.05), Regions.circle(0.62, 0.44, 0.05), Regions.circle(0.43, 0.78, 0.035), Regions.circle(0.57, 0.78, 0.035)))

    fun horse(): ShapeRegion = Regions.union(Regions.ellipse(0.52, 0.52, 0.3, 0.18), Regions.polygon(0.26, 0.46, 0.16, 0.12, 0.3, 0.1, 0.44, 0.46), Regions.polygon(0.2, 0.14, 0.04, 0.32, 0.1, 0.38, 0.26, 0.28), Regions.rect(0.28, 0.6, 0.38, 0.96), Regions.rect(0.66, 0.6, 0.76, 0.96), Regions.polygon(0.8, 0.42, 0.97, 0.66, 0.86, 0.68))

    fun giraffe(): ShapeRegion = Regions.union(Regions.ellipse(0.58, 0.66, 0.26, 0.14), Regions.polygon(0.36, 0.62, 0.5, 0.58, 0.34, 0.12, 0.24, 0.12), Regions.ellipse(0.24, 0.1, 0.15, 0.07), Regions.rect(0.38, 0.72, 0.46, 0.97), Regions.rect(0.7, 0.72, 0.78, 0.97))

    fun chicken(): ShapeRegion = Regions.union(Regions.ellipse(0.54, 0.62, 0.34, 0.26), Regions.circle(0.3, 0.36, 0.15), Regions.polygon(0.18, 0.34, 0.04, 0.38, 0.18, 0.44), Regions.polygon(0.22, 0.24, 0.26, 0.1, 0.32, 0.2, 0.36, 0.1, 0.4, 0.24), Regions.polygon(0.82, 0.6, 0.98, 0.3, 0.96, 0.6), Regions.rect(0.46, 0.82, 0.54, 0.97), Regions.rect(0.6, 0.82, 0.68, 0.97))

    fun shark(): ShapeRegion = Regions.union(Regions.ellipse(0.46, 0.56, 0.42, 0.17), Regions.polygon(0.4, 0.42, 0.54, 0.12, 0.6, 0.42), Regions.polygon(0.8, 0.5, 0.98, 0.3, 0.92, 0.56, 0.98, 0.8, 0.8, 0.62), Regions.polygon(0.4, 0.66, 0.5, 0.66, 0.34, 0.86))

    fun seahorse(): ShapeRegion = Regions.union(Regions.circle(0.5, 0.2, 0.15), Regions.polygon(0.34, 0.18, 0.06, 0.22, 0.06, 0.3, 0.38, 0.3), Regions.ellipse(0.54, 0.5, 0.18, 0.24), Regions.below(Regions.minus(Regions.circle(0.46, 0.76, 0.2), Regions.circle(0.46, 0.76, 0.09)), 0.7), Regions.polygon(0.62, 0.1, 0.78, 0.16, 0.66, 0.24))

    fun jellyfish(): ShapeRegion = Regions.union(Regions.above(Regions.ellipse(0.5, 0.46, 0.42, 0.4), 0.46), Regions.rect(0.14, 0.44, 0.24, 0.8), Regions.rect(0.32, 0.44, 0.42, 0.95), Regions.rect(0.58, 0.44, 0.68, 0.95), Regions.rect(0.76, 0.44, 0.86, 0.8))

    fun bee(): ShapeRegion = Regions.minus(Regions.union(Regions.ellipse(0.52, 0.62, 0.36, 0.22), Regions.circle(0.14, 0.6, 0.12), Regions.ellipse(0.42, 0.24, 0.12, 0.2), Regions.ellipse(0.66, 0.24, 0.12, 0.2), Regions.polygon(0.84, 0.56, 0.98, 0.62, 0.84, 0.68)), Regions.union(Regions.rect(0.42, 0.38, 0.5, 0.74), Regions.rect(0.6, 0.38, 0.68, 0.74)))

    fun ladybug(): ShapeRegion = Regions.minus(Regions.union(Regions.circle(0.5, 0.56, 0.38), Regions.circle(0.5, 0.16, 0.14)), Regions.union(Regions.circle(0.34, 0.46, 0.06), Regions.circle(0.66, 0.46, 0.06), Regions.circle(0.34, 0.7, 0.06), Regions.circle(0.66, 0.7, 0.06), Regions.circle(0.5, 0.58, 0.06)))

    fun snake(): ShapeRegion = Regions.union(Regions.minus(Regions.minus(Regions.circle(0.5, 0.3, 0.27), Regions.circle(0.5, 0.3, 0.12)), Regions.rect(0.5, 0.3, 1.0, 0.6)), Regions.minus(Regions.minus(Regions.circle(0.5, 0.7, 0.27), Regions.circle(0.5, 0.7, 0.12)), Regions.rect(0.0, 0.4, 0.5, 0.7)), Regions.ellipse(0.78, 0.24, 0.1, 0.12))

    fun swan(): ShapeRegion = Regions.union(Regions.ellipse(0.56, 0.72, 0.36, 0.2), Regions.polygon(0.92, 0.66, 0.99, 0.46, 0.88, 0.76), Regions.circle(0.3, 0.14, 0.1), Regions.polygon(0.22, 0.1, 0.06, 0.2, 0.22, 0.2), Regions.bar(0.38, 0.42, 0.58, 0.12, 74.0))

    fun camel(): ShapeRegion = Regions.union(Regions.ellipse(0.56, 0.56, 0.34, 0.18), Regions.circle(0.44, 0.38, 0.14), Regions.circle(0.7, 0.38, 0.14), Regions.polygon(0.24, 0.46, 0.4, 0.58, 0.22, 0.3, 0.1, 0.22), Regions.ellipse(0.12, 0.24, 0.12, 0.09), Regions.rect(0.3, 0.62, 0.4, 0.96), Regions.rect(0.72, 0.62, 0.82, 0.96))

    fun truck(): ShapeRegion = Regions.minus(Regions.union(Regions.rect(0.03, 0.26, 0.62, 0.72), Regions.polygon(0.62, 0.4, 0.84, 0.4, 0.97, 0.56, 0.97, 0.72, 0.62, 0.72), Regions.circle(0.24, 0.76, 0.12), Regions.circle(0.76, 0.76, 0.12)), Regions.polygon(0.68, 0.46, 0.82, 0.46, 0.9, 0.56, 0.68, 0.56))

    fun bus(): ShapeRegion = Regions.minus(Regions.union(Regions.rect(0.03, 0.24, 0.97, 0.74), Regions.circle(0.24, 0.78, 0.11), Regions.circle(0.76, 0.78, 0.11)), Regions.union(Regions.rect(0.1, 0.34, 0.28, 0.5), Regions.rect(0.38, 0.34, 0.56, 0.5), Regions.rect(0.66, 0.34, 0.84, 0.5)))

    fun train(): ShapeRegion = Regions.minus(Regions.union(Regions.rect(0.16, 0.38, 0.78, 0.74), Regions.rect(0.52, 0.22, 0.88, 0.74), Regions.rect(0.22, 0.2, 0.34, 0.4), Regions.polygon(0.16, 0.1, 0.4, 0.1, 0.34, 0.22, 0.22, 0.22), Regions.circle(0.3, 0.78, 0.12), Regions.circle(0.66, 0.78, 0.12), Regions.polygon(0.16, 0.74, 0.04, 0.82, 0.16, 0.6)), Regions.rect(0.62, 0.32, 0.78, 0.48))

    fun helicopter(): ShapeRegion = Regions.minus(Regions.union(Regions.ellipse(0.42, 0.56, 0.3, 0.2), Regions.rect(0.6, 0.48, 0.92, 0.58), Regions.rect(0.88, 0.36, 0.97, 0.66), Regions.rect(0.38, 0.2, 0.46, 0.4), Regions.rect(0.06, 0.14, 0.78, 0.22), Regions.rect(0.2, 0.8, 0.66, 0.87), Regions.rect(0.3, 0.72, 0.36, 0.84), Regions.rect(0.5, 0.72, 0.56, 0.84)), Regions.ellipse(0.28, 0.52, 0.1, 0.1))

    fun submarine(): ShapeRegion = Regions.minus(Regions.union(Regions.ellipse(0.5, 0.6, 0.46, 0.2), Regions.rect(0.38, 0.26, 0.62, 0.44), Regions.rect(0.54, 0.12, 0.6, 0.28)), Regions.union(Regions.circle(0.3, 0.6, 0.06), Regions.circle(0.5, 0.6, 0.06), Regions.circle(0.7, 0.6, 0.06)))

    fun castle(): ShapeRegion = Regions.minus(Regions.union(Regions.rect(0.04, 0.14, 0.28, 0.96), Regions.rect(0.72, 0.14, 0.96, 0.96), Regions.rect(0.2, 0.38, 0.8, 0.96), Regions.rect(0.38, 0.2, 0.62, 0.4)), Regions.union(Regions.rect(0.12, 0.12, 0.2, 0.2), Regions.rect(0.8, 0.12, 0.88, 0.2), Regions.rect(0.46, 0.18, 0.54, 0.26), Regions.rect(0.42, 0.72, 0.58, 0.97)))

    fun tent(): ShapeRegion = Regions.minus(Regions.polygon(0.5, 0.06, 0.97, 0.92, 0.03, 0.92), Regions.polygon(0.5, 0.46, 0.66, 0.93, 0.34, 0.93))

    fun lighthouse(): ShapeRegion = Regions.minus(Regions.union(Regions.polygon(0.38, 0.36, 0.62, 0.36, 0.72, 0.92, 0.28, 0.92), Regions.rect(0.22, 0.88, 0.78, 0.97), Regions.rect(0.34, 0.28, 0.66, 0.36), Regions.rect(0.4, 0.12, 0.6, 0.28), Regions.polygon(0.36, 0.12, 0.64, 0.12, 0.5, 0.02)), Regions.rect(0.46, 0.16, 0.54, 0.26))

    fun windmill(): ShapeRegion = Regions.union(Regions.polygon(0.38, 0.42, 0.62, 0.42, 0.72, 0.97, 0.28, 0.97), Regions.circle(0.5, 0.38, 0.08), Regions.bar(0.5, 0.38, 0.84, 0.14, 45.0), Regions.bar(0.5, 0.38, 0.84, 0.14, -45.0))

    fun wrench(): ShapeRegion = Regions.minus(Regions.union(Regions.circle(0.26, 0.26, 0.22), Regions.circle(0.74, 0.74, 0.22), Regions.bar(0.5, 0.5, 0.8, 0.14, 45.0)), Regions.union(Regions.polygon(0.26, 0.26, 0.0, 0.16, 0.16, 0.0), Regions.polygon(0.74, 0.74, 1.0, 0.84, 0.84, 1.0)))

    fun scissors(): ShapeRegion = Regions.union(Regions.minus(Regions.circle(0.26, 0.78, 0.17), Regions.circle(0.26, 0.78, 0.07)), Regions.minus(Regions.circle(0.74, 0.78, 0.17), Regions.circle(0.74, 0.78, 0.07)), Regions.bar(0.53, 0.35, 0.73, 0.12, -58.5), Regions.bar(0.47, 0.35, 0.73, 0.12, 58.5))

    fun sword(): ShapeRegion = Regions.union(Regions.polygon(0.5, 0.02, 0.58, 0.14, 0.58, 0.7, 0.42, 0.7, 0.42, 0.14), Regions.rect(0.26, 0.68, 0.74, 0.76), Regions.rect(0.44, 0.76, 0.56, 0.92), Regions.circle(0.5, 0.93, 0.06))

    fun pencil(): ShapeRegion = Regions.union(Regions.rect(0.32, 0.04, 0.68, 0.17), Regions.rect(0.39, 0.17, 0.61, 0.23), Regions.rect(0.32, 0.23, 0.68, 0.72), Regions.polygon(0.32, 0.72, 0.68, 0.72, 0.5, 0.98))

    fun book(): ShapeRegion = Regions.minus(Regions.rect(0.14, 0.06, 0.88, 0.94), Regions.union(Regions.rect(0.36, 0.2, 0.74, 0.36), Regions.rect(0.22, 0.12, 0.27, 0.88)))

    fun envelope(): ShapeRegion = Regions.minus(Regions.rect(0.04, 0.18, 0.96, 0.82), Regions.union(Regions.bar(0.31, 0.39, 0.5, 0.07, 38.0), Regions.bar(0.69, 0.39, 0.5, 0.07, -38.0)))

    fun clock(): ShapeRegion = Regions.minus(Regions.circle(0.5, 0.5, 0.46), Regions.union(Regions.rect(0.46, 0.2, 0.54, 0.54), Regions.rect(0.46, 0.46, 0.74, 0.54)))

    fun camera(): ShapeRegion = Regions.minus(Regions.union(Regions.rect(0.04, 0.3, 0.96, 0.86), Regions.polygon(0.32, 0.3, 0.4, 0.16, 0.6, 0.16, 0.68, 0.3), Regions.rect(0.12, 0.22, 0.24, 0.3)), Regions.circle(0.5, 0.58, 0.14))

    fun phone(): ShapeRegion = Regions.minus(Regions.rect(0.22, 0.03, 0.78, 0.97), Regions.rect(0.3, 0.12, 0.7, 0.8))

    fun tv(): ShapeRegion = Regions.minus(Regions.union(Regions.rect(0.05, 0.22, 0.95, 0.82), Regions.bar(0.36, 0.12, 0.26, 0.08, 65.0), Regions.bar(0.64, 0.12, 0.26, 0.08, -65.0), Regions.rect(0.3, 0.82, 0.4, 0.92), Regions.rect(0.6, 0.82, 0.7, 0.92)), Regions.rect(0.14, 0.32, 0.72, 0.72))

    fun headphones(): ShapeRegion = Regions.union(Regions.above(Regions.minus(Regions.circle(0.5, 0.52, 0.44), Regions.circle(0.5, 0.52, 0.33)), 0.52), Regions.rect(0.06, 0.5, 0.26, 0.84), Regions.rect(0.74, 0.5, 0.94, 0.84))

    fun gamepad(): ShapeRegion = Regions.minus(Regions.union(Regions.rect(0.2, 0.3, 0.8, 0.66), Regions.circle(0.22, 0.52, 0.2), Regions.circle(0.78, 0.52, 0.2)), Regions.union(Regions.rect(0.2, 0.46, 0.36, 0.54), Regions.rect(0.24, 0.42, 0.32, 0.58), Regions.circle(0.72, 0.44, 0.05), Regions.circle(0.8, 0.54, 0.05)))

    fun dice(): ShapeRegion = Regions.minus(Regions.rect(0.06, 0.06, 0.94, 0.94), Regions.union(Regions.circle(0.28, 0.28, 0.08), Regions.circle(0.72, 0.28, 0.08), Regions.circle(0.5, 0.5, 0.08), Regions.circle(0.28, 0.72, 0.08), Regions.circle(0.72, 0.72, 0.08)))

    fun chessPawn(): ShapeRegion = Regions.union(Regions.circle(0.5, 0.22, 0.16), Regions.polygon(0.38, 0.34, 0.62, 0.34, 0.7, 0.76, 0.3, 0.76), Regions.rect(0.3, 0.32, 0.7, 0.38), Regions.rect(0.18, 0.76, 0.82, 0.94))

    fun boot(): ShapeRegion = Regions.polygon(0.2, 0.04, 0.6, 0.04, 0.6, 0.56, 0.94, 0.7, 0.94, 0.94, 0.2, 0.94)

    fun glasses(): ShapeRegion = Regions.union(Regions.minus(Regions.circle(0.26, 0.52, 0.22), Regions.circle(0.26, 0.52, 0.12)), Regions.minus(Regions.circle(0.74, 0.52, 0.22), Regions.circle(0.74, 0.52, 0.12)), Regions.rect(0.44, 0.46, 0.56, 0.54))

    fun bowTie(): ShapeRegion = Regions.union(Regions.polygon(0.04, 0.2, 0.44, 0.42, 0.44, 0.58, 0.04, 0.8), Regions.polygon(0.96, 0.2, 0.56, 0.42, 0.56, 0.58, 0.96, 0.8), Regions.rect(0.4, 0.38, 0.6, 0.62))

    fun dress(): ShapeRegion = Regions.polygon(0.34, 0.04, 0.44, 0.04, 0.5, 0.12, 0.56, 0.04, 0.66, 0.04, 0.66, 0.3, 0.6, 0.42, 0.92, 0.94, 0.08, 0.94, 0.4, 0.42, 0.34, 0.3)

    fun pizza(): ShapeRegion = Regions.minus(Regions.polygon(0.04, 0.12, 0.96, 0.12, 0.5, 0.97), Regions.union(Regions.circle(0.4, 0.32, 0.07), Regions.circle(0.62, 0.4, 0.06), Regions.circle(0.48, 0.58, 0.06)))

    fun donut(): ShapeRegion = Regions.minus(Regions.star(0.5, 0.5, 0.48, 0.42, tips = 16), Regions.circle(0.5, 0.5, 0.16))

    fun popsicle(): ShapeRegion = Regions.union(Regions.rect(0.24, 0.14, 0.76, 0.68), Regions.above(Regions.circle(0.5, 0.26, 0.26), 0.26), Regions.rect(0.44, 0.66, 0.56, 0.97))

    fun candle(): ShapeRegion = Regions.union(Regions.rect(0.32, 0.38, 0.68, 0.92), Regions.rect(0.2, 0.88, 0.8, 0.97), Regions.ellipse(0.5, 0.22, 0.1, 0.16), Regions.rect(0.46, 0.3, 0.54, 0.4))

    fun robot(): ShapeRegion = Regions.minus(Regions.union(Regions.rect(0.24, 0.16, 0.76, 0.46), Regions.rect(0.46, 0.04, 0.54, 0.18), Regions.circle(0.5, 0.05, 0.05), Regions.rect(0.42, 0.44, 0.58, 0.52), Regions.rect(0.2, 0.5, 0.8, 0.82), Regions.rect(0.06, 0.52, 0.24, 0.66), Regions.rect(0.76, 0.52, 0.94, 0.66), Regions.rect(0.3, 0.8, 0.44, 0.97), Regions.rect(0.56, 0.8, 0.7, 0.97)), Regions.union(Regions.circle(0.38, 0.3, 0.06), Regions.circle(0.62, 0.3, 0.06)))

    fun treasureChest(): ShapeRegion = Regions.minus(Regions.union(Regions.above(Regions.ellipse(0.5, 0.44, 0.44, 0.3), 0.44), Regions.rect(0.06, 0.44, 0.94, 0.9)), Regions.union(Regions.rect(0.06, 0.42, 0.42, 0.47), Regions.rect(0.58, 0.42, 0.94, 0.47), Regions.circle(0.5, 0.6, 0.06)))

    fun bomb(): ShapeRegion = Regions.union(Regions.circle(0.45, 0.6, 0.35), Regions.bar(0.7, 0.28, 0.22, 0.14, -45.0), Regions.star(0.84, 0.14, 0.14, 0.06, tips = 6))

}
