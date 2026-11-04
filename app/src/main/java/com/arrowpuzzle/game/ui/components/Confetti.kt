package com.arrowpuzzle.game.ui.components

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * One short, subtle confetti shower: small paper strips fall from above the
 * top edge, sway a little, flip and fade out. Positions are computed from the
 * elapsed time alone, so there is no per-frame particle state.
 *
 * Skipped completely when the system "remove animations" setting is on.
 */
@Composable
fun ConfettiShower(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    particleCount: Int = 64,
    durationSeconds: Float = 3.6f,
    maxAlpha: Float = 0.7f,
) {
    val context = LocalContext.current
    val animationsOn = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) > 0f
    }
    if (!animationsOn || colors.isEmpty()) return

    var elapsed by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (elapsed < durationSeconds) {
            withFrameNanos { now -> elapsed = (now - start) / 1_000_000_000f }
        }
    }
    val finished by remember { derivedStateOf { elapsed >= durationSeconds } }
    if (finished) return

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val unit = density.density // pixels per dp
        val particles = remember(widthPx, heightPx) {
            val random = Random(7_331)
            List(particleCount) {
                ConfettiParticle(
                    startX = random.nextFloat() * widthPx,
                    startY = -heightPx * (0.03f + random.nextFloat() * 0.25f),
                    driftX = (random.nextFloat() - 0.5f) * widthPx * 0.12f,
                    fallSpeed = heightPx * (0.14f + random.nextFloat() * 0.16f),
                    swayAmplitude = unit * (6f + random.nextFloat() * 14f),
                    swaySpeed = 1.5f + random.nextFloat() * 2f,
                    phase = random.nextFloat() * 6.283f,
                    rotation = random.nextFloat() * 360f,
                    spin = (random.nextFloat() - 0.5f) * 360f,
                    flipSpeed = 3f + random.nextFloat() * 5f,
                    width = unit * (4f + random.nextFloat() * 3f),
                    height = unit * (7f + random.nextFloat() * 5f),
                    delay = random.nextFloat() * 0.9f,
                    color = colors[random.nextInt(colors.size)],
                )
            }
        }
        val gravity = heightPx * 0.22f
        Canvas(modifier = Modifier.fillMaxSize()) {
            val t = elapsed
            // Fade in over 0.25 s, fade out over the last second.
            val globalAlpha = when {
                t < 0.25f -> t / 0.25f
                t > durationSeconds - 1f -> ((durationSeconds - t) / 1f).coerceIn(0f, 1f)
                else -> 1f
            } * maxAlpha
            for (p in particles) {
                val local = t - p.delay
                if (local < 0f) continue
                val x = p.startX + p.driftX * local + sin(p.phase + local * p.swaySpeed) * p.swayAmplitude
                val y = p.startY + p.fallSpeed * local + 0.5f * gravity * local * local
                if (y > size.height + p.height || y < -size.height) continue
                // Paper flip: the strip narrows and widens as it turns.
                val flip = abs(cos(p.phase + local * p.flipSpeed)).coerceAtLeast(0.15f)
                rotate(degrees = p.rotation + p.spin * local, pivot = Offset(x, y)) {
                    scale(scaleX = flip, scaleY = 1f, pivot = Offset(x, y)) {
                        drawRect(
                            color = p.color.copy(alpha = p.color.alpha * globalAlpha),
                            topLeft = Offset(x - p.width / 2f, y - p.height / 2f),
                            size = Size(p.width, p.height),
                        )
                    }
                }
            }
        }
    }
}

private class ConfettiParticle(
    val startX: Float,
    val startY: Float,
    val driftX: Float,
    val fallSpeed: Float,
    val swayAmplitude: Float,
    val swaySpeed: Float,
    val phase: Float,
    val rotation: Float,
    val spin: Float,
    val flipSpeed: Float,
    val width: Float,
    val height: Float,
    val delay: Float,
    val color: Color,
)
