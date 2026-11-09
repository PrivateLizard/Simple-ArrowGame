package com.arrowpuzzle.game.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arrowpuzzle.game.ui.theme.AppPrimary
import com.arrowpuzzle.game.ui.theme.AppTextMuted
import kotlin.math.roundToInt

/** Same proportions as the arrows on the board (see GameScreen). */
private const val HEAD_LENGTH_PER_LINE = 0.40f / 0.09f
private const val HEAD_HALF_WIDTH_PER_LINE = 0.25f / 0.09f

/** A straight game arrow from [tail] to [tip]: round-capped shaft, solid head. */
internal fun DrawScope.drawStraightArrow(tail: Offset, tip: Offset, line: Float, color: Color) {
    val delta = tip - tail
    val length = delta.getDistance()
    if (length <= 0.001f) return
    val direction = delta / length
    val perpendicular = Offset(-direction.y, direction.x)
    val headLength = line * HEAD_LENGTH_PER_LINE
    val headHalfWidth = line * HEAD_HALF_WIDTH_PER_LINE
    val headBase = tip - direction * headLength
    val shaftEnd = tip - direction * (headLength * 0.55f)
    if ((shaftEnd - tail).getDistance() > 0f && length > headLength * 0.55f) {
        drawLine(color, tail, shaftEnd, strokeWidth = line, cap = StrokeCap.Round)
    }
    val head = Path().apply {
        moveTo(tip.x, tip.y)
        val a = headBase + perpendicular * headHalfWidth
        val b = headBase - perpendicular * headHalfWidth
        lineTo(a.x, a.y)
        lineTo(b.x, b.y)
        close()
    }
    drawPath(head, color)
}

/**
 * On/off switch drawn as a game arrow pointing right. Off: a short, colourless
 * arrow on the left. On: the arrow grows to the right and takes the theme's
 * accent colour.
 */
@Composable
fun ArrowSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reach by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "arrowSwitchReach",
    )
    val color by animateColorAsState(
        targetValue = if (checked) AppPrimary else AppTextMuted.copy(alpha = 0.55f),
        animationSpec = tween(durationMillis = 220),
        label = "arrowSwitchColor",
    )
    val lineWidth = 3.dp
    Canvas(
        modifier = modifier
            .size(width = 64.dp, height = 40.dp)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
    ) {
        val line = lineWidth.toPx()
        val headHalfWidth = line * HEAD_HALF_WIDTH_PER_LINE
        val y = size.height / 2f
        val tail = Offset(line, y)
        val shortTip = size.width * 0.46f
        val longTip = size.width - headHalfWidth * 0.3f
        val tip = Offset(shortTip + (longTip - shortTip) * reach, y)
        drawStraightArrow(tail, tip, line, color)
    }
}

/**
 * Level progress as a game arrow: a row of faint dots like the empty board,
 * and a coloured arrow that grows to the right as arrows are cleared.
 */
@Composable
fun ArrowProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    lineWidth: Dp = 3.dp,
) {
    val shown by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "arrowProgress",
    )
    val color = AppPrimary
    val dotColor = AppTextMuted.copy(alpha = 0.35f)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(22.dp),
    ) {
        val line = lineWidth.toPx()
        val y = size.height / 2f
        val start = line / 2f
        val end = size.width - line * HEAD_HALF_WIDTH_PER_LINE * 0.3f
        // Dots about every 10 dp, evenly spread over the full width.
        val gaps = (size.width / 10.dp.toPx()).roundToInt().coerceAtLeast(1)
        val dots = (0..gaps).map { Offset(start + (end - start) * it / gaps, y) }
        drawPoints(
            points = dots,
            pointMode = PointMode.Points,
            color = dotColor,
            strokeWidth = line,
            cap = StrokeCap.Round,
        )
        // Even at 0 % the head is there, so the bar reads as an arrow.
        val shortest = line * HEAD_LENGTH_PER_LINE * 1.2f
        val tipX = start + shortest + (end - start - shortest) * shown
        drawStraightArrow(Offset(start, y), Offset(tipX, y), line, color)
    }
}

/**
 * Integer slider drawn like the board: one dot per value, and a coloured game
 * arrow whose head sits on the chosen value. Drag or tap to change it.
 */
@Composable
fun ArrowSlider(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    lineWidth: Dp = 3.dp,
) {
    val steps = (range.last - range.first).coerceAtLeast(1)
    val current by rememberUpdatedState(value)
    val onChange by rememberUpdatedState(onValueChange)
    val color = AppPrimary
    val dotColor = AppTextMuted.copy(alpha = 0.45f)

    // Shared by drawing and touch: where the first and last value sit.
    fun trackStart(line: Float) = line / 2f + line * HEAD_LENGTH_PER_LINE + line * 2f
    fun trackEnd(width: Float, line: Float) = width - line * HEAD_HALF_WIDTH_PER_LINE

    fun valueAt(x: Float, width: Float, line: Float): Int {
        val start = trackStart(line)
        val end = trackEnd(width, line)
        val fraction = ((x - start) / (end - start)).coerceIn(0f, 1f)
        return range.first + (fraction * steps).roundToInt()
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = current.toFloat(),
                    range = range.first.toFloat()..range.last.toFloat(),
                    steps = (steps - 1).coerceAtLeast(0),
                )
                setProgress { target ->
                    val next = target.roundToInt().coerceIn(range.first, range.last)
                    if (next != current) onChange(next)
                    true
                }
                if (range.first == range.last) disabled()
            }
            .pointerInput(range) {
                val line = lineWidth.toPx()
                detectTapGestures { offset ->
                    val next = valueAt(offset.x, size.width.toFloat(), line)
                    if (next != current) onChange(next)
                }
            }
            .pointerInput(range) {
                val line = lineWidth.toPx()
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    val next = valueAt(change.position.x, size.width.toFloat(), line)
                    if (next != current) onChange(next)
                }
            },
    ) {
        val line = lineWidth.toPx()
        val start = trackStart(line)
        val end = trackEnd(size.width, line)
        val y = size.height / 2f
        val dots = (0..steps).map { index ->
            Offset(start + (end - start) * index / steps, y)
        }
        drawPoints(
            points = dots,
            pointMode = PointMode.Points,
            color = dotColor,
            strokeWidth = line * 1.1f,
            cap = StrokeCap.Round,
        )
        val index = (value - range.first).coerceIn(0, steps)
        drawStraightArrow(
            tail = Offset(line / 2f, y),
            tip = dots[index],
            line = line,
            color = color,
        )
    }
}
