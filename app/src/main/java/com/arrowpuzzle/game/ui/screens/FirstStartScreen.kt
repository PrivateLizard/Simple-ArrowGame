package com.arrowpuzzle.game.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
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
                    .blur((BLUR_DP * (1f - focus)).dp),
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
            // Greys the menu out; below Android 12 there is no blur, so this
            // alone keeps the buttons in front readable.
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
