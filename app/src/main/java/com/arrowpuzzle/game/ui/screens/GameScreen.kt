package com.arrowpuzzle.game.ui.screens

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.arrowpuzzle.game.data.model.AppSettings
import com.arrowpuzzle.game.data.model.Arrow
import com.arrowpuzzle.game.data.model.GridPoint
import com.arrowpuzzle.game.data.model.LevelConfig
import com.arrowpuzzle.game.game.ArrowGeometry
import com.arrowpuzzle.game.game.arrowExitDurationMillis
import com.arrowpuzzle.game.game.GameFeedback
import com.arrowpuzzle.game.game.GameResult
import com.arrowpuzzle.game.game.GameState
import com.arrowpuzzle.game.game.GeneratedLevel
import com.arrowpuzzle.game.i18n.LocalStrings
import com.arrowpuzzle.game.ui.components.ScreenBackground
import com.arrowpuzzle.game.ui.components.StatPill
import com.arrowpuzzle.game.ui.theme.AppDanger
import com.arrowpuzzle.game.ui.theme.AppGridLine
import com.arrowpuzzle.game.ui.theme.AppPrimary
import com.arrowpuzzle.game.ui.theme.AppSecondary
import com.arrowpuzzle.game.ui.theme.AppSurface
import com.arrowpuzzle.game.ui.theme.AppSurfaceRaised
import com.arrowpuzzle.game.ui.theme.AppText
import com.arrowpuzzle.game.ui.theme.AppTextMuted
import com.arrowpuzzle.game.ui.theme.ArrowTheme
import com.arrowpuzzle.game.ui.viewmodel.GameViewModel
import com.arrowpuzzle.game.util.GridUtils
import com.arrowpuzzle.game.util.SoundEffects
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    soundEffects: SoundEffects,
    hintsEnabled: Boolean = true,
    devWinButtons: Boolean = false,
    onExit: () -> Unit,
    onVictory: (GameResult) -> Unit,
    onDefeat: (GameState.Defeat, LevelConfig) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val view = LocalView.current
    val s = LocalStrings.current

    // Leaving a level always keeps the progress; there is no confirmation
    // dialog and no lost board. The home screen then offers to resume it.
    val exitLevel: () -> Unit = {
        viewModel.saveProgress()
        onExit()
    }

    // Save when the app is backgrounded, and as a last safety net when this
    // screen is disposed (for example when the activity is closed).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.saveProgress()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.saveProgress()
        }
    }

    // Start decoding as early as possible. The sound object is app-scoped, so
    // this is a no-op after the first level; the aim is that samples are ready
    // before the first tap.
    LaunchedEffect(soundEffects) {
        soundEffects.preload()
    }

    LaunchedEffect(viewModel) {
        viewModel.feedback.collect { feedback ->
            if (feedback.hapticsEnabled) {
                performHaptic(view, feedback)
            }
            if (feedback.soundEnabled) {
                soundEffects.play(feedback)
            }
        }
    }

    LaunchedEffect(state) {
        when (val current = state) {
            is GameState.Victory -> {
                delay(500)
                onVictory(
                    GameResult(
                        config = current.level.config,
                        record = current.record,
                        isNewHighScore = current.isNewHighScore,
                    ),
                )
            }

            is GameState.Defeat -> {
                delay(400)
                onDefeat(current, current.level.config)
            }

            else -> Unit
        }
    }

    val canExit = state is GameState.Playing || state is GameState.Loading
    BackHandler(enabled = canExit) {
        exitLevel()
    }

    ScreenBackground {
        when (val current = state) {
            GameState.Loading -> LoadingContent()
            is GameState.Error -> ErrorContent(message = current.message, onExit = onExit)
            is GameState.Playing -> PlayingContent(
                state = current,
                onArrowTap = viewModel::onArrowClick,
                onExit = exitLevel,
                hintsEnabled = hintsEnabled,
                devWinButtons = devWinButtons,
                onDevNextArrow = viewModel::devTapNextArrow,
                onDevPerfectWin = viewModel::devPerfectWin,
            )

            is GameState.Victory -> ResultTransition(s.t("all_clear"), AppPrimary)
            is GameState.Defeat -> ResultTransition(s.t("too_many"), AppDanger)
        }
    }
}

@Composable
private fun PlayingContent(
    state: GameState.Playing,
    onArrowTap: (Int) -> Unit,
    onExit: () -> Unit,
    hintsEnabled: Boolean,
    devWinButtons: Boolean,
    onDevNextArrow: () -> Unit,
    onDevPerfectWin: () -> Unit,
) {
    val level = state.level
    val config = level.config
    val removedCount = state.removedIds.size
    val progress = if (level.arrowCount == 0) 0f else {
        removedCount.toFloat() / level.arrowCount
    }
    val unlimitedFailures = config.maxFailures == AppSettings.NO_FAILURE_LIMIT
    val s = LocalStrings.current
    val available = remember(level, state.removedIds) {
        ArrowGeometry.availableArrows(level, state.removedIds)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // The board is the background: it fills the whole screen and fades out
        // underneath the top UI instead of being boxed into a separate panel.
        GameBoard(
            level = level,
            state = state,
            onArrowTap = onArrowTap,
            hintsEnabled = hintsEnabled,
            modifier = Modifier.fillMaxSize(),
        )

        Column(modifier = Modifier.fillMaxSize()) {
            GameTopBar(
                state = state,
                availableCount = available.size,
                onExit = onExit,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatPill(
                    label = s.t("cleared"),
                    value = "$removedCount/${level.arrowCount}",
                    tint = AppText,
                    modifier = Modifier.weight(1f),
                )
                StatPill(
                    label = s.t("failures"),
                    value = if (unlimitedFailures) {
                        "${state.failures}/∞"
                    } else {
                        "${state.failures}/${config.maxFailures}"
                    },
                    tint = AppText,
                    modifier = Modifier.weight(1f),
                )
                StatPill(
                    label = s.t("time"),
                    value = GridUtils.formatTime(state.elapsedSeconds),
                    tint = AppText,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(AppGridLine),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(AppSecondary, AppPrimary))),
                )
            }

            Spacer(Modifier.weight(1f))

            if (devWinButtons) {
                DevWinBar(onNextArrow = onDevNextArrow, onPerfectWin = onDevPerfectWin)
            }
        }
    }
}

/** Dev option: two test buttons at the bottom of the level. */
@Composable
private fun DevWinBar(onNextArrow: () -> Unit, onPerfectWin: () -> Unit) {
    val s = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(
            onClick = onNextArrow,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, AppPrimary.copy(alpha = 0.7f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = AppSurface.copy(alpha = 0.92f),
                contentColor = AppPrimary,
            ),
        ) {
            Text(s.t("dev_next_arrow"), style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
        Button(
            onClick = onPerfectWin,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppPrimary,
                contentColor = AppSurface,
            ),
        ) {
            Text(s.t("dev_perfect_win"), style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
private fun GameTopBar(
    state: GameState.Playing,
    availableCount: Int,
    onExit: () -> Unit,
) {
    val s = LocalStrings.current
    val level = state.level
    val title = if (level.config.isCustom) {
        s.t("custom_puzzle")
    } else {
        s.f("level_title", level.config.levelId.toString().padStart(2, '0'))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onExit) {
            Text(
                text = "<",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = AppText,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = AppText,
                letterSpacing = 1.sp,
            )
            Text(
                text = s.f("arrows_open", level.arrowCount, availableCount),
                style = MaterialTheme.typography.bodyMedium,
                color = AppTextMuted,
            )
        }
    }
}

private const val BOARD_INSET_FRACTION = 0.02f

/** Zoom out far enough that the whole field sits with free space around it. */
private const val MIN_BOARD_SCALE = 0.45f

/**
 * Start slightly zoomed out so the field does not sit flush against the screen
 * edges. The user can still pinch in both directions from here.
 */
private const val START_BOARD_SCALE = 0.85f

private const val MAX_BOARD_SCALE = 5f

/**
 * Fixed size of the free strip that can be left at the screen edge, as a
 * fraction of the viewport. It stays the same at every zoom level; if the user
 * gets lost in a zoomed-in area they can simply zoom out again.
 */
private const val PAN_MARGIN_FRACTION = 0.21f

/**
 * Hint timing: the first hint comes after this long without a successful move,
 * subsequent ones after the repeat delay until the player makes a successful
 * move (which restarts the effect and therefore the first delay).
 */
private const val HINT_FIRST_DELAY_MILLIS = 20_000L
private const val HINT_REPEAT_DELAY_MILLIS = 10_000L
private const val HINT_SHAKE_MILLIS = 650
private const val HINT_SHAKE_FREQUENCY = 40f

@Composable
private fun GameBoard(
    level: GeneratedLevel,
    state: GameState.Playing,
    onArrowTap: (Int) -> Unit,
    hintsEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = ArrowTheme.colors
    // Drives both the red tint and the shake of a wrongly tapped arrow.
    val wrongAnim = remember { Animatable(0f) }
    val exitProgress = remember { Animatable(1f) }
    val hintAnim = remember { Animatable(1f) }
    var hintArrowId by remember { mutableStateOf<Int?>(null) }
    var boardScale by remember { mutableFloatStateOf(START_BOARD_SCALE) }
    var boardOffset by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val removedIds = state.removedIds
    val lastCorrectArrow = remember(state.correctFlashId, level) {
        state.correctFlashId?.let(level::arrowById)
    }
    // Lattice points that are no longer covered by an arrow. They are the only
    // places where the dot grid is shown, so the grid is revealed step by step
    // as arrows are cleared.
    val revealedGridPoints = remember(level, removedIds) {
        val covered = level.arrows
            .filter { it.id !in removedIds }
            .flatMap { ArrowGeometry.routePoints(it.route) }
            .toSet()
        buildList {
            for (gx in 0 until level.layoutSize) {
                for (gy in 0 until level.layoutSize) {
                    val point = GridPoint(gx, gy)
                    if (point !in covered) add(point)
                }
            }
        }
    }

    // Temporary hint loop: first hint after 20 s without a successful move,
    // then one every 10 s. A successful removal changes removedIds, restarts
    // this effect and resets the delay to 20 s. Purely visual; no score effect.
    LaunchedEffect(level, removedIds, hintsEnabled) {
        // Stop any running wiggle when hints are switched off or the board changes.
        hintArrowId = null
        hintAnim.snapTo(1f)
        if (!hintsEnabled) return@LaunchedEffect
        var firstHint = true
        while (true) {
            delay(if (firstHint) HINT_FIRST_DELAY_MILLIS else HINT_REPEAT_DELAY_MILLIS)
            firstHint = false
            val available = ArrowGeometry.availableArrows(level, removedIds)
            if (available.isNotEmpty()) {
                hintArrowId = available.random().id
                hintAnim.snapTo(0f)
                hintAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(HINT_SHAKE_MILLIS, easing = LinearEasing),
                )
                hintArrowId = null
            }
        }
    }

    LaunchedEffect(level, viewportSize) {
        if (viewportSize.width > 0 && viewportSize.height > 0) {
            boardScale = START_BOARD_SCALE
            boardOffset = centeredBoardOffset(START_BOARD_SCALE, viewportSize)
        }
    }
    LaunchedEffect(state.wrongFlashToken) {
        if (state.wrongFlashToken > 0) {
            // A single 0..1 progress keeps the red tint and the shake in sync.
            // The tint holds for the first half, then fades out, so a mistake
            // stays readable noticeably longer than before.
            wrongAnim.snapTo(0f)
            wrongAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(900, easing = LinearEasing),
            )
        }
    }
    LaunchedEffect(state.correctFlashToken) {
        if (state.correctFlashToken > 0) {
            exitProgress.snapTo(0f)
            exitProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    // Length-based duration and linear easing: the head moves
                    // at a constant speed, so long arrows no longer race past
                    // short ones.
                    durationMillis = arrowExitDurationMillis(lastCorrectArrow?.lengthCells ?: 1).toInt(),
                    easing = LinearEasing,
                ),
            )
        }
    }

    val wrongProgress = wrongAnim.value
    val hintProgress = hintAnim.value
    val wrongAlphaValue = when {
        wrongProgress <= 0f -> 0f
        wrongProgress < 0.55f -> 1f
        wrongProgress < 0.85f -> 1f - (wrongProgress - 0.55f) / 0.30f
        else -> 0f
    }
    val wrongVisible = wrongAlphaValue > 0.01f
    val exitValue = exitProgress.value

    Box(
        modifier = modifier
            // Isolated layer so the DstIn fade below only masks the board and
            // not whatever is drawn behind the screen.
            .graphicsLayer()
            .drawWithContent {
                drawContent()
                // Fade towards the actual screen background colour, so the
                // transition is dark in dark mode and light in light mode.
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to palette.backgroundDeep,
                        0.26f to palette.backgroundDeep.copy(alpha = 0f),
                        0.965f to palette.backgroundDeep.copy(alpha = 0f),
                        1f to palette.backgroundDeep,
                    ),
                )
            }
            .onSizeChanged {
                viewportSize = it
                boardOffset = clampBoardOffset(boardOffset, boardScale, it)
            }
            .pointerInput(level, removedIds) {
                detectTapGestures { tap ->
                    val scale = boardScale
                    val offset = boardOffset
                    val boardTap = Offset(
                        x = (tap.x - offset.x) / scale,
                        y = (tap.y - offset.y) / scale,
                    )
                    val geometry = boardGeometry(size.width.toFloat(), size.height.toFloat(), level.layoutSize)
                    var closestId: Int? = null
                    var closestDistance = Float.MAX_VALUE
                    level.arrows.forEach { arrow ->
                        if (arrow.id in removedIds) return@forEach
                        val distance = distanceToPolyline(
                            point = boardTap,
                            route = arrow.route.map { geometry.toOffset(it) },
                        )
                        if (distance < closestDistance) {
                            closestDistance = distance
                            closestId = arrow.id
                        }
                    }
                    if (closestId != null && closestDistance <= geometry.hitRadius) {
                        onArrowTap(closestId!!)
                    }
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    if (viewportSize.width == 0 || viewportSize.height == 0) {
                        return@detectTransformGestures
                    }
                    val oldScale = boardScale
                    val newScale = (oldScale * zoom).coerceIn(MIN_BOARD_SCALE, MAX_BOARD_SCALE)
                    val focalShift = centroid - (centroid - boardOffset) * (newScale / oldScale)
                    boardScale = newScale
                    boardOffset = clampBoardOffset(focalShift + pan, newScale, viewportSize)
                }
            },
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = boardScale
                    scaleY = boardScale
                    translationX = boardOffset.x
                    translationY = boardOffset.y
                    transformOrigin = TransformOrigin(0f, 0f)
                },
        ) {
            val geometry = boardGeometry(size.width, size.height, level.layoutSize)
            val lineWidth = geometry.spacing * 0.09f
            val remaining = level.arrows.filter { it.id !in removedIds }

            // Only the points of already cleared arrows are dotted, so the
            // grid appears where the lines are gone.
            val dotRadius = (geometry.spacing * 0.09f).coerceAtLeast(1f)
            val dotColor = palette.textMuted.copy(alpha = 0.38f)
            revealedGridPoints.forEach { point ->
                drawCircle(
                    color = dotColor,
                    radius = dotRadius,
                    center = geometry.toOffset(point),
                )
            }

            remaining.forEach { arrow ->
                val isWrong = wrongVisible && arrow.id == state.wrongFlashId
                // Twice the frequency of before and only for the first third
                // of the red flash, so the shake reads as a quick jolt while
                // the red tint keeps its existing timing.
                val shakeWindow = 0.35f
                val shakeProgress = (wrongProgress / shakeWindow).coerceIn(0f, 1f)
                val wrongShake = if (isWrong && wrongProgress < shakeWindow) {
                    sin(wrongProgress * 52f) * geometry.spacing * 0.22f * (1f - shakeProgress)
                } else {
                    0f
                }
                val hintShake = if (hintArrowId == arrow.id && hintProgress < 1f) {
                    sin(hintProgress * HINT_SHAKE_FREQUENCY) *
                        geometry.spacing * 0.18f * (1f - hintProgress)
                } else {
                    0f
                }
                val shakeX = wrongShake + hintShake
                val route = if (shakeX != 0f) {
                    arrow.route.map { geometry.toOffset(it) + Offset(shakeX, 0f) }
                } else {
                    arrow.route.map(geometry::toOffset)
                }
                drawPolylineArrow(
                    route = route,
                    spacing = geometry.spacing,
                    lineWidth = if (isWrong) lineWidth * 1.4f else lineWidth,
                    color = if (isWrong) {
                        lerp(palette.boardLine, palette.boardLineWrong, wrongAlphaValue)
                    } else {
                        palette.boardLine
                    },
                )
            }

            if (lastCorrectArrow != null && exitValue < 0.999f) {
                val exitRoute = exitRouteFor(
                    arrow = lastCorrectArrow,
                    route = lastCorrectArrow.route.map(geometry::toOffset),
                    progress = exitValue,
                    width = size.width,
                    height = size.height,
                    spacing = geometry.spacing,
                )
                if (exitRoute != null && exitRoute.size >= 2) {
                    // No highlight colour: the movement itself is the feedback.
                    // Only a gentle fade at the very end so the arrow does not
                    // pop out of existence.
                    drawPolylineArrow(
                        route = exitRoute,
                        spacing = geometry.spacing,
                        lineWidth = lineWidth,
                        color = palette.boardLine.copy(
                            alpha = exitFadeAlpha(exitValue),
                        ),
                    )
                }
            }
        }
    }
}

/**
 * The exiting arrow keeps full opacity for most of the flight and only fades in
 * the last stretch, so the movement reads as the feedback.
 */
private fun exitFadeAlpha(progress: Float): Float =
    (1f - ((progress - 0.82f) / 0.18f).coerceIn(0f, 1f)).coerceIn(0f, 1f)

private fun exitRouteFor(
    arrow: Arrow,
    route: List<Offset>,
    progress: Float,
    width: Float,
    height: Float,
    spacing: Float,
): List<Offset>? {
    if (progress <= 0f) return route
    if (progress >= 1f || route.size < 2) return null

    val cumulative = FloatArray(route.size)
    for (index in 1 until route.size) {
        cumulative[index] = cumulative[index - 1] + hypot(
            route[index].x - route[index - 1].x,
            route[index].y - route[index - 1].y,
        )
    }
    val totalLength = cumulative.last()
    if (totalLength <= 0.001f) return null

    val tip = route.last()
    val margin = spacing * 1.4f
    val direction = arrow.direction
    val exitDistance = when {
        direction.dx > 0 -> width - tip.x + margin
        direction.dx < 0 -> tip.x + margin
        direction.dy > 0 -> height - tip.y + margin
        else -> tip.y + margin
    }.coerceAtLeast(margin)
    val slide = progress * (totalLength + exitDistance)
    val unit = Offset(direction.dx.toFloat(), direction.dy.toFloat())
    val head = tip + unit * slide

    if (slide >= totalLength) {
        val tail = tip + unit * (slide - totalLength)
        return listOf(tail, head)
    }

    val result = ArrayList<Offset>(route.size + 1)
    result += pointAtDistance(route, cumulative, slide)
    for (index in route.indices) {
        if (cumulative[index] > slide) result += route[index]
    }
    if (result.last() != head) result += head
    return result
}

private fun pointAtDistance(
    route: List<Offset>,
    cumulative: FloatArray,
    distance: Float,
): Offset {
    if (distance <= 0f) return route.first()
    for (index in 1 until route.size) {
        if (distance <= cumulative[index]) {
            val segment = cumulative[index] - cumulative[index - 1]
            if (segment <= 0.001f) return route[index]
            val fraction = (distance - cumulative[index - 1]) / segment
            return route[index - 1] + (route[index] - route[index - 1]) * fraction
        }
    }
    return route.last()
}

/**
 * Keeps the board fully inside the viewport.
 *
 * When the board is larger than the viewport (scale > 1) the offset is negative
 * and lets the user pan around the enlarged board. When it is smaller
 * (scale < 1) the offset is positive and lets the user move the board within
 * the free space instead of nailing it to the centre.
 */
private fun clampBoardOffset(offset: Offset, scale: Float, viewport: IntSize): Offset {
    if (viewport.width <= 0 || viewport.height <= 0) return Offset.Zero
    val width = viewport.width.toFloat()
    val height = viewport.height.toFloat()
    val rangeX = width - width * scale
    val rangeY = height - height * scale
    // Constant strip of background around the board, independent of the zoom.
    val marginX = width * PAN_MARGIN_FRACTION
    val marginY = height * PAN_MARGIN_FRACTION

    // If the content is small enough, keep the full margin on both sides.
    // Otherwise allow the margin on one side while the other side is cropped;
    // both branches meet continuously, so zooming never snaps the board.
    val minX: Float
    val maxX: Float
    if (rangeX < 2f * marginX) {
        minX = rangeX - marginX
        maxX = marginX
    } else {
        minX = marginX
        maxX = rangeX - marginX
    }
    val minY: Float
    val maxY: Float
    if (rangeY < 2f * marginY) {
        minY = rangeY - marginY
        maxY = marginY
    } else {
        minY = marginY
        maxY = rangeY - marginY
    }
    return Offset(
        x = offset.x.coerceIn(minX, maxX),
        y = offset.y.coerceIn(minY, maxY),
    )
}

/** Places a board that is smaller than the viewport in its centre. */
private fun centeredBoardOffset(scale: Float, viewport: IntSize): Offset {
    if (viewport.width <= 0 || viewport.height <= 0) return Offset.Zero
    val width = viewport.width.toFloat()
    val height = viewport.height.toFloat()
    return Offset(
        x = (width - width * scale) / 2f,
        y = (height - height * scale) / 2f,
    )
}

private data class BoardGeometry(
    val layoutSize: Int,
    val originX: Float,
    val originY: Float,
    val spacing: Float,
) {
    val hitRadius: Float get() = spacing * 0.38f

    fun toOffset(point: GridPoint): Offset = Offset(
        x = originX + point.x * spacing,
        y = originY + point.y * spacing,
    )
}

/**
 * Square cells on purpose: the shorter screen side defines the cell size, so
 * the field is never stretched vertically. The resulting square is centred in
 * the viewport; zoom and pan move it from there.
 */
private fun boardGeometry(width: Float, height: Float, layoutSize: Int): BoardGeometry {
    val shortest = min(width, height)
    val inset = shortest * BOARD_INSET_FRACTION
    val inner = (shortest - inset * 2f).coerceAtLeast(1f)
    val divisions = (layoutSize - 1).coerceAtLeast(1)
    val spacing = inner / divisions
    val boardExtent = spacing * divisions
    return BoardGeometry(
        layoutSize = layoutSize,
        originX = (width - boardExtent) / 2f,
        originY = (height - boardExtent) / 2f,
        spacing = spacing,
    )
}

private fun distanceToPolyline(point: Offset, route: List<Offset>): Float {
    if (route.size < 2) return Float.MAX_VALUE
    var minimum = Float.MAX_VALUE
    for (index in 0 until route.lastIndex) {
        minimum = min(minimum, distanceToSegment(point, route[index], route[index + 1]))
    }
    return minimum
}

private fun distanceToSegment(point: Offset, start: Offset, end: Offset): Float {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val lengthSquared = dx * dx + dy * dy
    if (lengthSquared <= 0.0001f) return hypot(point.x - start.x, point.y - start.y)
    val t = (((point.x - start.x) * dx + (point.y - start.y) * dy) / lengthSquared)
        .coerceIn(0f, 1f)
    val projection = Offset(start.x + t * dx, start.y + t * dy)
    return hypot(point.x - projection.x, point.y - projection.y)
}

private fun DrawScope.drawPolylineArrow(
    route: List<Offset>,
    spacing: Float,
    lineWidth: Float,
    color: Color,
) {
    if (route.size < 2) return
    val previous = route[route.lastIndex - 1]
    val tip = route.last()
    val dx = tip.x - previous.x
    val dy = tip.y - previous.y
    val length = hypot(dx, dy)
    if (length <= 0.001f) return
    val unitX = dx / length
    val unitY = dy / length
    val perpX = -unitY
    val perpY = unitX

    // The arrowhead is a solid triangle that is clearly wider than the shaft
    // and slightly longer than it is wide, so it reads as an arrowhead and not
    // as a plain line end. The shaft stops short of the tip so the stroke never
    // pokes through the head.
    val headLength = spacing * 0.40f
    val headHalfWidth = spacing * 0.25f
    val headBase = Offset(
        tip.x - unitX * headLength,
        tip.y - unitY * headLength,
    )

    // Shaft: shorten the final segment so it ends inside the head base instead
    // of running to the tip.
    val shaftEnd = Offset(
        tip.x - unitX * headLength * 0.55f,
        tip.y - unitY * headLength * 0.55f,
    )
    val path = Path().apply {
        moveTo(route[0].x, route[0].y)
        for (index in 1 until route.lastIndex) {
            lineTo(route[index].x, route[index].y)
        }
        lineTo(shaftEnd.x, shaftEnd.y)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = lineWidth,
            cap = StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round,
        ),
    )

    val head = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(headBase.x + perpX * headHalfWidth, headBase.y + perpY * headHalfWidth)
        lineTo(headBase.x - perpX * headHalfWidth, headBase.y - perpY * headHalfWidth)
        close()
    }
    drawPath(path = head, color = color)
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            color = AppPrimary,
            strokeWidth = 3.dp,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = LocalStrings.current.t("building"),
            style = MaterialTheme.typography.titleMedium,
            color = AppTextMuted,
        )
    }
}

@Composable
private fun ErrorContent(message: String, onExit: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = LocalStrings.current.t("gen_failed"),
            style = MaterialTheme.typography.headlineMedium,
            color = AppDanger,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = AppTextMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onExit,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppPrimary,
                contentColor = AppSurface,
            ),
        ) {
            Text(LocalStrings.current.t("back"))
        }
    }
}

@Composable
private fun ResultTransition(label: String, color: Color) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.displayMedium,
            color = color,
            textAlign = TextAlign.Center,
        )
    }
}

private fun performHaptic(view: View, feedback: GameFeedback) {
    val type = when (feedback) {
        is GameFeedback.Correct -> HapticFeedbackConstants.VIRTUAL_KEY
        is GameFeedback.Wrong -> HapticFeedbackConstants.LONG_PRESS
        is GameFeedback.Victory -> HapticFeedbackConstants.VIRTUAL_KEY
        is GameFeedback.Defeat -> HapticFeedbackConstants.LONG_PRESS
    }
    view.performHapticFeedback(type)
}