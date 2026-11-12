package com.arrowpuzzle.game.ui.screens

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowpuzzle.game.i18n.LocalStrings
import com.arrowpuzzle.game.ui.components.ArrowButton
import com.arrowpuzzle.game.ui.components.LocalFrameSeedSource
import com.arrowpuzzle.game.ui.theme.AppBackground
import com.arrowpuzzle.game.ui.theme.AppPrimary
import com.arrowpuzzle.game.ui.theme.ArrowPaletteScope
import kotlin.random.Random

/**
 * Seed for the arrow frames of the menu preview. Home uses the same one right
 * after this screen, so the frames do not jump when it takes over.
 */
const val FIRST_START_FRAME_SEED = 47

private const val BLUR_DP = 14f
private const val SWIPE_MILLIS = 650

/** Below Android 12 the blur runs on a bitmap this many times smaller. */
private const val BLUR_DOWNSCALE = 4f

/**
 * First start: the screen is split down the middle. The left half shows the
 * main menu in the light colours, the right half in the dark ones, both
 * blurred behind one sharp button each. After a tap the other theme is pulled
 * out to its side and fades, the chosen one comes into focus, then
 * [onChoose] is called.
 */
@Composable
fun FirstStartScreen(onChoose: (nightMode: Boolean) -> Unit) {
    var chosenNight by remember { mutableStateOf<Boolean?>(null) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(chosenNight) {
        val night = chosenNight ?: return@LaunchedEffect
        progress.animateTo(1f, tween(SWIPE_MILLIS, easing = FastOutSlowInEasing))
        onChoose(night)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val half = widthPx / 2f
        val p = progress.value
        val lightChosen = chosenNight == false
        val darkChosen = chosenNight == true

        // Where each side is cut, and how far the losing side has moved out.
        val lightClipRight = if (lightChosen) half + p * half else half
        val darkClipLeft = if (darkChosen) half - p * half else half
        val lightShift = if (darkChosen) -p * half else 0f
        val darkShift = if (lightChosen) p * half else 0f

        MenuPreview(
            nightMode = false,
            clipLeft = 0f,
            clipRight = lightClipRight,
            shiftX = lightShift,
            alpha = if (darkChosen) 1f - p else 1f,
            focus = if (lightChosen) p else 0f,
        )
        MenuPreview(
            nightMode = true,
            clipLeft = darkClipLeft,
            clipRight = widthPx,
            shiftX = darkShift,
            alpha = if (lightChosen) 1f - p else 1f,
            focus = if (darkChosen) p else 0f,
        )

        // The preview is only a picture: no taps reach its buttons.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    }
                },
        )

        ChoiceButton(
            nightMode = false,
            enabled = chosenNight == null,
            onClick = { chosenNight = false },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .graphicsLayer {
                    translationX = lightShift
                    alpha = 1f - p
                },
        )
        ChoiceButton(
            nightMode = true,
            enabled = chosenNight == null,
            onClick = { chosenNight = true },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .graphicsLayer {
                    translationX = darkShift
                    alpha = 1f - p
                },
        )
    }
}

/**
 * The main menu in one theme, laid out at full screen width and cut to
 * [clipLeft]..[clipRight]. [focus] runs from 0 (blurred and greyed) to 1
 * (sharp, exactly like Home).
 */
@Composable
private fun MenuPreview(
    nightMode: Boolean,
    clipLeft: Float,
    clipRight: Float,
    shiftX: Float,
    alpha: Float,
    focus: Float,
) {
    val blurCache = remember { BlurCache() }
    ArrowPaletteScope(nightMode = nightMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = shiftX
                    this.alpha = alpha
                }
                .drawWithContent {
                    clipRect(left = clipLeft, right = clipRight) {
                        this@drawWithContent.drawContent()
                    }
                },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .softBlur(BLUR_DP.dp, amount = 1f - focus, cache = blurCache),
            ) {
                // Same seed sequence in both halves: the frames match across
                // the split, and Home starts with the same ones.
                val seeds = remember { Random(FIRST_START_FRAME_SEED) }
                CompositionLocalProvider(LocalFrameSeedSource provides seeds) {
                    HomeScreen(
                        highScore = 0,
                        completedLevels = 0,
                        highestUnlocked = 1,
                        onPlay = {},
                        onCustom = {},
                        onSettings = {},
                    )
                }
            }
            // Greys the menu out so the buttons in front stand out.
            val scrim = if (nightMode) Color(0x59000000) else Color(0x40808080)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrim.copy(alpha = scrim.alpha * (1f - focus))),
            )
        }
    }
}

@Composable
private fun ChoiceButton(
    nightMode: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    ArrowPaletteScope(nightMode = nightMode) {
        Box(
            modifier = modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f),
            contentAlignment = Alignment.Center,
        ) {
            ArrowButton(
                onClick = { if (enabled) onClick() },
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(68.dp),
                tint = AppPrimary,
                emphasis = true,
                background = AppBackground,
                lineWidth = 2.5.dp,
            ) {
                Text(
                    text = s.t(if (nightMode) "theme_dark" else "theme_light"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

/** The blurred picture of one preview, kept so it is computed only once. */
private class BlurCache {
    var bitmap: ImageBitmap? = null
    var size: Size = Size.Zero
}

/**
 * Blur of [radius], faded in by [amount] (0 = sharp, 1 = fully blurred).
 * [Modifier.blur] needs Android 12 (RenderEffect) and is silently ignored
 * below. There the content is drawn into a bitmap [BLUR_DOWNSCALE] times
 * smaller, blurred with three box passes (close to a Gaussian blur), cached in
 * [cache] and scaled back up. While [amount] falls, the sharp content shows
 * through the cached blur, so nothing is recomputed during the animation.
 */
private fun Modifier.softBlur(radius: Dp, amount: Float, cache: BlurCache): Modifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        blur(radius * amount)
    } else {
        drawWithContent {
            if (amount <= 0f || size.width < 1f || size.height < 1f) {
                drawContent()
                return@drawWithContent
            }
            // Recomputed whenever the preview is drawn fully blurred, so a
            // change of the content (fonts, layout) is picked up while idle.
            var blurred = cache.bitmap
            if (blurred == null || amount >= 1f || cache.size != size) {
                // Inside draw(...) `size` is the small bitmap's, so keep the full one.
                val full = size
                val width = (full.width / BLUR_DOWNSCALE).toInt().coerceAtLeast(1)
                val height = (full.height / BLUR_DOWNSCALE).toInt().coerceAtLeast(1)
                val small = ImageBitmap(width, height)
                draw(this, layoutDirection, Canvas(small), Size(width.toFloat(), height.toFloat())) {
                    scale(width / full.width, height / full.height, pivot = Offset.Zero) {
                        this@drawWithContent.drawContent()
                    }
                }
                // Same strength as RenderEffect: Skia's sigma for a blur radius.
                val sigma = 0.57735f * radius.toPx() + 0.5f
                val boxRadius = (sigma / BLUR_DOWNSCALE).toInt().coerceAtLeast(1)
                val android = small.asAndroidBitmap()
                val pixels = IntArray(width * height)
                android.getPixels(pixels, 0, width, 0, 0, width, height)
                boxBlur(pixels, width, height, boxRadius)
                android.setPixels(pixels, 0, width, 0, 0, width, height)
                blurred = small
                cache.bitmap = small
                cache.size = full
            }
            if (amount < 1f) drawContent()
            drawImage(
                image = blurred,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                alpha = amount.coerceIn(0f, 1f),
                filterQuality = FilterQuality.Low,
            )
        }
    }

/**
 * Blurs ARGB [pixels] in place with three horizontal and vertical box passes
 * of [radius], which together come close to a Gaussian blur. The colour
 * channels are premultiplied first so transparent pixels leave no dark seams.
 */
private fun boxBlur(pixels: IntArray, width: Int, height: Int, radius: Int) {
    val n = width * height
    val channels = Array(4) { IntArray(n) }
    for (i in 0 until n) {
        val c = pixels[i]
        val a = c ushr 24
        channels[0][i] = a
        channels[1][i] = ((c shr 16) and 0xFF) * a / 255
        channels[2][i] = ((c shr 8) and 0xFF) * a / 255
        channels[3][i] = (c and 0xFF) * a / 255
    }
    val tmp = IntArray(n)
    for (channel in channels) {
        repeat(3) {
            boxPass(channel, tmp, width, height, radius, horizontal = true)
            boxPass(tmp, channel, width, height, radius, horizontal = false)
        }
    }
    for (i in 0 until n) {
        val a = channels[0][i]
        if (a == 0) {
            pixels[i] = 0
            continue
        }
        val r = (channels[1][i] * 255 / a).coerceAtMost(255)
        val g = (channels[2][i] * 255 / a).coerceAtMost(255)
        val b = (channels[3][i] * 255 / a).coerceAtMost(255)
        pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}

/** One sliding-window box pass along rows or columns; edges are clamped. */
private fun boxPass(src: IntArray, dst: IntArray, width: Int, height: Int, radius: Int, horizontal: Boolean) {
    val lines = if (horizontal) height else width
    val len = if (horizontal) width else height
    val step = if (horizontal) 1 else width
    val div = 2 * radius + 1
    for (line in 0 until lines) {
        val start = if (horizontal) line * width else line
        var sum = 0
        for (k in -radius..radius) sum += src[start + k.coerceIn(0, len - 1) * step]
        for (i in 0 until len) {
            dst[start + i * step] = (sum + div / 2) / div
            val out = (i - radius).coerceIn(0, len - 1)
            val inn = (i + radius + 1).coerceIn(0, len - 1)
            sum += src[start + inn * step] - src[start + out * step]
        }
    }
}
