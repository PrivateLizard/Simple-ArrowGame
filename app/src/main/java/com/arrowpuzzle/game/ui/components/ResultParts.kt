package com.arrowpuzzle.game.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowpuzzle.game.ui.theme.AppGridLine
import com.arrowpuzzle.game.ui.theme.AppSurface
import com.arrowpuzzle.game.ui.theme.AppText
import com.arrowpuzzle.game.ui.theme.AppTextMuted
import kotlin.math.PI
import kotlin.math.sin

/**
 * Shared building blocks of the level-complete and level-failed screens, so
 * both results look like one family.
 */

enum class ResultMark { CHECK, CROSS }

/**
 * Round badge whose ring draws itself, followed by a check mark or a cross.
 * The cross version gives one short, soft shake at the end.
 */
@Composable
fun ResultBadge(
    mark: ResultMark,
    color: Color,
    background: Color,
    size: Dp = 92.dp,
) {
    val ring = remember { Animatable(0f) }
    val symbol = remember { Animatable(0f) }
    val pop = remember { Animatable(0.7f) }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        ring.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
        symbol.animateTo(1f, tween(300, easing = LinearOutSlowInEasing))
        if (mark == ResultMark.CROSS) {
            shake.animateTo(1f, tween(420))
        }
    }
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
                // Damped sine: two small swings, then still.
                val t = shake.value
                translationX = if (t in 0.001f..0.999f) {
                    sin(t * 4f * PI.toFloat()) * (1f - t) * 7.dp.toPx()
                } else {
                    0f
                }
            }
            .clip(RoundedCornerShape(50))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = this.size.minDimension * 0.06f
            val inset = stroke * 1.5f
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * ring.value,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(this.size.width - inset * 2, this.size.height - inset * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            val w = this.size.width
            val h = this.size.height
            val p = symbol.value
            val first = (p / 0.45f).coerceIn(0f, 1f)
            val second = ((p - 0.45f) / 0.55f).coerceIn(0f, 1f)
            when (mark) {
                ResultMark.CHECK -> {
                    val a = Offset(w * 0.31f, h * 0.52f)
                    val b = Offset(w * 0.45f, h * 0.65f)
                    val c = Offset(w * 0.70f, h * 0.38f)
                    leg(color, a, b, first, stroke)
                    leg(color, b, c, second, stroke)
                }
                ResultMark.CROSS -> {
                    leg(color, Offset(w * 0.36f, h * 0.36f), Offset(w * 0.64f, h * 0.64f), first, stroke)
                    leg(color, Offset(w * 0.64f, h * 0.36f), Offset(w * 0.36f, h * 0.64f), second, stroke)
                }
            }
        }
    }
}

private fun DrawScope.leg(color: Color, from: Offset, to: Offset, progress: Float, stroke: Float) {
    if (progress <= 0f) return
    drawLine(
        color = color,
        start = from,
        end = from + (to - from) * progress,
        strokeWidth = stroke * 1.15f,
        cap = StrokeCap.Round,
    )
}

/** Fades and slides its content in after [delayMillis]. */
@Composable
fun Reveal(delayMillis: Int, content: @Composable () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            1f,
            tween(durationMillis = 420, delayMillis = delayMillis, easing = FastOutSlowInEasing),
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = progress.value
                translationY = (1f - progress.value) * 24.dp.toPx()
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Small stat tile; [highlight] tints value and border. */
@Composable
fun ResultStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Color? = null,
) {
    val line = 1.5.dp
    Column(
        modifier = modifier
            .arrowFrame(
                color = highlight ?: AppTextMuted.copy(alpha = 0.7f),
                seed = rememberFrameSeed(),
                fill = AppSurface,
                lineWidth = line,
            )
            .padding(arrowFrameInset(line))
            .padding(vertical = 9.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = AppTextMuted,
            maxLines = 1,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = highlight ?: AppText,
        )
    }
}

/** Small framed label, for example "NEW HIGH SCORE". */
@Composable
fun ResultPill(text: String, color: Color) {
    val line = 1.5.dp
    Box(
        modifier = Modifier
            .arrowFrame(
                color = color,
                seed = rememberFrameSeed(),
                fill = color.copy(alpha = 0.12f),
                lineWidth = line,
            )
            .padding(arrowFrameInset(line))
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            letterSpacing = 1.sp,
        )
    }
}
