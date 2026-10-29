package com.arrowpuzzle.game.ui.screens

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowpuzzle.game.data.model.Arrow
import com.arrowpuzzle.game.data.model.GridPoint
import com.arrowpuzzle.game.data.model.LevelConfig
import com.arrowpuzzle.game.game.ARROW_EXIT_ANIMATION_MILLIS
import com.arrowpuzzle.game.game.ArrowGeometry
import com.arrowpuzzle.game.game.GameFeedback
import com.arrowpuzzle.game.game.GameResult
import com.arrowpuzzle.game.game.GameState
import com.arrowpuzzle.game.game.GeneratedLevel
import com.arrowpuzzle.game.ui.components.ScreenBackground
import com.arrowpuzzle.game.ui.components.StatPill
import com.arrowpuzzle.game.ui.theme.AppAccent
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

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onExit: () -> Unit,
    onVictory: (GameResult) -> Unit,
    onDefeat: (GameState.Defeat, LevelConfig) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val view = LocalView.current
    val soundEffects = remember { SoundEffects() }
    var showQuitDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose(soundEffects::release)
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
        showQuitDialog = true
    }

    ScreenBackground {
        when (val current = state) {
            GameState.Loading -> LoadingContent()
            is GameState.Error -> ErrorContent(message = current.message, onExit = onExit)
            is GameState.Playing -> PlayingContent(
                state = current,
                onArrowTap = viewModel::onArrowClick,
                onExit = { showQuitDialog = true },
            )

            is GameState.Victory -> ResultTransition("ALL CLEAR", AppPrimary)
            is GameState.Defeat -> ResultTransition("TOO MANY MISTAKES", AppDanger)
        }
    }

    if (showQuitDialog) {
        AlertDialog(
            onDismissRequest = { showQuitDialog = false },
            title = { Text("Quit level?") },
            text = { Text("Your current progress in this level will be lost.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showQuitDialog = false
                        onExit()
                    },
                ) {
                    Text("Quit", color = AppDanger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuitDialog = false }) {
                    Text("Keep playing", color = AppTextMuted)
                }
            },
            containerColor = AppSurfaceRaised,
            titleContentColor = AppText,
            textContentColor = AppTextMuted,
            shape = RoundedCornerShape(20.dp),
        )
    }
}

@Composable
private fun PlayingContent(
    state: GameState.Playing,
    onArrowTap: (Int) -> Unit,
    onExit: () -> Unit,
) {
    val level = state.level
    val config = level.config
    val removedCount = state.removedIds.size
    val progress = if (level.arrowCount == 0) 0f else {
        removedCount.toFloat() / level.arrowCount
    }
    val failuresTint = if (state.failures >= config.maxFailures - 1) AppDanger else AppAccent
    val available = remember(level, state.removedIds) {
        ArrowGeometry.availableArrows(level, state.removedIds)
    }

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
                label = "Cleared",
                value = "$removedCount/${level.arrowCount}",
                tint = AppPrimary,
                modifier = Modifier.weight(1f),
            )
            StatPill(
                label = "Failures",
                value = "${state.failures}/${config.maxFailures}",
                tint = failuresTint,
                modifier = Modifier.weight(1f),
            )
            StatPill(
                label = "Time",
                value = GridUtils.formatTime(state.elapsedSeconds),
                tint = AppSecondary,
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

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            val boardSize = min(maxWidth.value, maxHeight.value).dp
            GameBoard(
                level = level,
                state = state,
                onArrowTap = onArrowTap,
                modifier = Modifier.size(boardSize),
            )
        }

        Text(
            text = "Tap only arrows whose path ahead is clear. Clear every arrow.\nPinch to zoom · drag to pan.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = AppTextMuted,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun GameTopBar(
    state: GameState.Playing,
    availableCount: Int,
    onExit: () -> Unit,
) {
    val level = state.level
    val title = if (level.config.isCustom) {
        "CUSTOM PUZZLE"
    } else {
        "LEVEL ${level.config.levelId.toString().padStart(2, '0')}"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onExit) {
            Text(
                text = "EXIT",
                style = MaterialTheme.typography.labelMedium,
                color = AppTextMuted,
                letterSpacing = 1.sp,
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
                text = "${level.arrowCount} arrows  /  $availableCount open",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTextMuted,
            )
        }
        Text(
            text = "${state.removedIds.size * 100 / level.arrowCount.coerceAtLeast(1)}%",
            style = MaterialTheme.typography.titleMedium,
            color = AppPrimary,
            modifier = Modifier.padding(end = 12.dp),
        )
    }
}

private const val BOARD_INSET_FRACTION = 0.04f
private const val MIN_BOARD_SCALE = 1f
private const val MAX_BOARD_SCALE = 5f

@Composable
private fun GameBoard(
    level: GeneratedLevel,
    state: GameState.Playing,
    onArrowTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = ArrowTheme.colors
    val shape = RoundedCornerShape(22.dp)
    val wrongAlpha = remember { Animatable(0f) }
    val exitProgress = remember { Animatable(1f) }
    var boardScale by remember { mutableFloatStateOf(MIN_BOARD_SCALE) }
    var boardOffset by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val removedIds = state.removedIds
    val lastCorrectArrow = remember(state.correctFlashId, level) {
        state.correctFlashId?.let(level::arrowById)
    }

    LaunchedEffect(level) {
        boardScale = MIN_BOARD_SCALE
        boardOffset = Offset.Zero
    }
    LaunchedEffect(state.wrongFlashToken) {
        if (state.wrongFlashToken > 0) {
            wrongAlpha.snapTo(1f)
            wrongAlpha.animateTo(0f, animationSpec = tween(540))
        }
    }
    LaunchedEffect(state.correctFlashToken) {
        if (state.correctFlashToken > 0) {
            exitProgress.snapTo(0f)
            exitProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = ARROW_EXIT_ANIMATION_MILLIS.toInt(),
                    easing = FastOutLinearInEasing,
                ),
            )
        }
    }

    val wrongAlphaValue = wrongAlpha.value
    val exitValue = exitProgress.value

    Box(
        modifier = modifier
            .clip(shape)
            .background(palette.boardBackground)
            .border(1.dp, palette.gridLine.copy(alpha = 0.55f), shape)
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

            remaining.forEach { arrow ->
                drawPolylineArrow(
                    route = arrow.route.map(geometry::toOffset),
                    spacing = geometry.spacing,
                    lineWidth = lineWidth,
                    color = palette.boardLine,
                )
            }

            val wrongArrow = remaining.firstOrNull { it.id == state.wrongFlashId }
            if (wrongArrow != null && wrongAlphaValue > 0.01f) {
                drawPolylineArrow(
                    route = wrongArrow.route.map(geometry::toOffset),
                    spacing = geometry.spacing,
                    lineWidth = lineWidth * 1.8f,
                    color = palette.boardLineWrong.copy(alpha = wrongAlphaValue),
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
                    drawPolylineArrow(
                        route = exitRoute,
                        spacing = geometry.spacing,
                        lineWidth = lineWidth,
                        color = palette.boardLineSelected.copy(
                            alpha = (1f - exitValue).coerceIn(0f, 1f),
                        ),
                    )
                }
            }
        }
    }
}

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

private fun clampBoardOffset(offset: Offset, scale: Float, viewport: IntSize): Offset {
    if (viewport.width <= 0 || viewport.height <= 0) return Offset.Zero
    val width = viewport.width.toFloat()
    val height = viewport.height.toFloat()
    val minX = width - width * scale
    val minY = height - height * scale
    return Offset(
        x = offset.x.coerceIn(minX, 0f),
        y = offset.y.coerceIn(minY, 0f),
    )
}

private data class BoardGeometry(
    val layoutSize: Int,
    val inset: Float,
    val spacingX: Float,
    val spacingY: Float,
) {
    val spacing: Float get() = min(spacingX, spacingY)
    val hitRadius: Float get() = spacing * 0.38f

    fun toOffset(point: GridPoint): Offset = Offset(
        x = inset + point.x * spacingX,
        y = inset + point.y * spacingY,
    )
}

private fun boardGeometry(width: Float, height: Float, layoutSize: Int): BoardGeometry {
    val shortest = min(width, height)
    val inset = shortest * BOARD_INSET_FRACTION
    val innerWidth = (width - inset * 2f).coerceAtLeast(1f)
    val innerHeight = (height - inset * 2f).coerceAtLeast(1f)
    val divisions = (layoutSize - 1).coerceAtLeast(1)
    return BoardGeometry(
        layoutSize = layoutSize,
        inset = inset,
        spacingX = innerWidth / divisions,
        spacingY = innerHeight / divisions,
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
    val path = Path().apply {
        moveTo(route[0].x, route[0].y)
        for (index in 1..route.lastIndex) {
            lineTo(route[index].x, route[index].y)
        }
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

    val previous = route[route.lastIndex - 1]
    val tip = route.last()
    val dx = tip.x - previous.x
    val dy = tip.y - previous.y
    val length = hypot(dx, dy)
    if (length <= 0.001f) return
    val unitX = dx / length
    val unitY = dy / length
    val baseCenter = Offset(
        tip.x - unitX * spacing * 0.46f,
        tip.y - unitY * spacing * 0.46f,
    )
    val perpX = -unitY
    val perpY = unitX
    val halfWidth = spacing * 0.19f
    val head = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(baseCenter.x + perpX * halfWidth, baseCenter.y + perpY * halfWidth)
        lineTo(baseCenter.x - perpX * halfWidth, baseCenter.y - perpY * halfWidth)
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
            text = "Building the tangle...",
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
            text = "Level generation failed",
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
            Text("BACK")
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