package com.arrowpuzzle.game.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import com.arrowpuzzle.game.ui.theme.AppPrimary
import com.arrowpuzzle.game.ui.theme.AppText
import com.arrowpuzzle.game.ui.theme.AppTextMuted
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/**
 * Proportions of the arrows on the board (GameScreen draws them from the grid
 * spacing): the line is 0.09 of a spacing, the head 0.40 long and 0.50 wide.
 * Here the line width is given, so the head follows from it.
 */
private const val HEAD_LENGTH_PER_LINE = 0.40f / 0.09f
private const val HEAD_HALF_WIDTH_PER_LINE = 0.25f / 0.09f

/** How far the frame line sits inside the element: room for the arrowhead. */
fun arrowFrameInset(lineWidth: Dp): Dp = lineWidth * HEAD_HALF_WIDTH_PER_LINE

/** A random frame layout that stays the same while the element is shown. */
@Composable
fun rememberFrameSeed(key: Any? = null): Int {
    val source = LocalFrameSeedSource.current
    return remember(key) { source.nextInt() }
}

/**
 * Where frame seeds come from. Normally truly random; the first-start screen
 * provides a fixed sequence so its light and dark halves get the same frames.
 */
val LocalFrameSeedSource = staticCompositionLocalOf<Random> { Random.Default }

/**
 * Draws the element's outline as one closed game arrow: a rectangle with 90°
 * corners whose head points at, and just touches, its own tail. Where the
 * head sits and which way the arrow runs come from [seed].
 */
fun Modifier.arrowFrame(
    color: Color,
    seed: Int,
    fill: Color = Color.Transparent,
    lineWidth: Dp = 2.dp,
): Modifier = drawWithCache {
    val line = lineWidth.toPx()
    val frame = buildArrowFrame(size, line, seed)
    val stroke = Stroke(width = line, cap = StrokeCap.Round, join = StrokeJoin.Round)
    onDrawBehind {
        if (frame == null) return@onDrawBehind
        if (fill.alpha > 0f) {
            drawRoundRect(
                color = fill,
                topLeft = frame.fillTopLeft,
                size = frame.fillSize,
                cornerRadius = CornerRadius(line / 2f),
            )
        }
        drawPath(path = frame.shaft, color = color, style = stroke)
        drawPath(path = frame.head, color = color)
    }
}

/**
 * A button framed by an arrow. [tint] colours the frame and the content;
 * [emphasis] adds a soft fill for the main action of a screen.
 */
@Composable
fun ArrowButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color,
    emphasis: Boolean = false,
    background: Color = Color.Transparent,
    enabled: Boolean = true,
    seed: Int = rememberFrameSeed(),
    lineWidth: Dp = 2.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val inset = arrowFrameInset(lineWidth)
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .arrowFrame(
                color = tint,
                seed = seed,
                fill = if (emphasis) {
                    tint.copy(alpha = 0.16f).compositeOver(background)
                } else {
                    background
                },
                lineWidth = lineWidth,
            )
            .padding(inset)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides tint) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

/**
 * A small selectable option (language, max failures). The selected one gets
 * a coloured arrow and a soft fill, the others a muted arrow.
 */
@Composable
fun ArrowChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lineWidth = 1.5.dp
    val color = if (selected) AppPrimary else AppTextMuted.copy(alpha = 0.7f)
    Box(
        modifier = modifier
            .arrowFrame(
                color = color,
                seed = rememberFrameSeed(),
                fill = if (selected) AppPrimary.copy(alpha = 0.18f) else Color.Transparent,
                lineWidth = lineWidth,
            )
            .padding(arrowFrameInset(lineWidth))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
            color = if (selected) AppPrimary else AppText,
        )
    }
}

internal class ArrowFrameShape(
    val shaft: Path,
    val head: Path,
    val fillTopLeft: Offset,
    val fillSize: Size,
)

/**
 * Builds the frame for [size]. The line runs on a rectangle [inset] from the
 * edge (room for the head). It starts at its tail on one side, goes once
 * around, and its tip stops where the round tail cap begins, so head and tail
 * touch. Returns null when the element is too small for an arrow.
 */
internal fun buildArrowFrame(size: Size, line: Float, seed: Int): ArrowFrameShape? {
    val headLength = line * HEAD_LENGTH_PER_LINE
    val headHalfWidth = line * HEAD_HALF_WIDTH_PER_LINE
    val inset = headHalfWidth
    val left = inset
    val top = inset
    val right = size.width - inset
    val bottom = size.height - inset
    if (right - left < line * 4f || bottom - top < line * 4f) return null

    val random = Random(seed)
    var corners = listOf(
        Offset(left, top),
        Offset(right, top),
        Offset(right, bottom),
        Offset(left, bottom),
    )
    if (random.nextBoolean()) corners = corners.reversed()

    // Head and tail must sit on one straight side, clear of both corners:
    // behind the tail point come the gap, the head and some room before the
    // corner; ahead of it the line needs room too. A head right at a corner
    // looks glued on, so the wide margin is used whenever the side allows it.
    // On a side that is barely long enough the head still ends up next to a
    // corner, so roomy sides (the long ones of a chip) come first.
    fun sidesFor(margin: Float, room: Float) = (0 until 4).filter { side ->
        val length = (corners[(side + 1) % 4] - corners[side]).getDistance()
        length >= (line / 2f + headLength + margin * 2f) * room
    }
    var margin = line * 6f
    var sides = sidesFor(margin, room = 1.7f)
    if (sides.isEmpty()) sides = sidesFor(margin, room = 1f)
    if (sides.isEmpty()) {
        margin = line * 2.5f
        sides = sidesFor(margin, room = 1f)
    }
    if (sides.isEmpty()) return null
    val behind = line / 2f + headLength + margin
    val ahead = margin
    // Longer sides are more likely, so heads spread evenly along the outline.
    val lengths = sides.map { (corners[(it + 1) % 4] - corners[it]).getDistance() }
    var pick = random.nextFloat() * lengths.sum()
    var side = sides.last()
    for (index in sides.indices) {
        if (pick < lengths[index]) {
            side = sides[index]
            break
        }
        pick -= lengths[index]
    }

    val from = corners[side]
    val to = corners[(side + 1) % 4]
    val sideLength = (to - from).getDistance()
    val direction = (to - from) / sideLength
    val perpendicular = Offset(-direction.y, direction.x)
    val tailDistance = behind + random.nextFloat() * (sideLength - behind - ahead)
    val tail = from + direction * tailDistance

    // The tail's round cap reaches line/2 behind the tail point; the tip ends
    // exactly there.
    val tip = tail - direction * (line / 2f)
    val headBase = tip - direction * headLength
    val shaftEnd = tip - direction * (headLength * 0.55f)

    val shaft = Path().apply {
        moveTo(tail.x, tail.y)
        for (step in 1..4) {
            val corner = corners[(side + step) % 4]
            lineTo(corner.x, corner.y)
        }
        lineTo(shaftEnd.x, shaftEnd.y)
    }
    val head = Path().apply {
        moveTo(tip.x, tip.y)
        val a = headBase + perpendicular * headHalfWidth
        val b = headBase - perpendicular * headHalfWidth
        lineTo(a.x, a.y)
        lineTo(b.x, b.y)
        close()
    }
    return ArrowFrameShape(
        shaft = shaft,
        head = head,
        fillTopLeft = Offset(left, top),
        fillSize = Size(right - left, bottom - top),
    )
}
