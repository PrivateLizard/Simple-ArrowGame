package com.arrowpuzzle.game.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowpuzzle.game.i18n.LocalStrings
import com.arrowpuzzle.game.ui.components.ScreenBackground
import com.arrowpuzzle.game.ui.theme.AppGridLine
import com.arrowpuzzle.game.ui.theme.AppPrimary
import com.arrowpuzzle.game.ui.theme.AppSurface
import com.arrowpuzzle.game.ui.theme.AppSurfaceRaised
import com.arrowpuzzle.game.ui.theme.AppText
import com.arrowpuzzle.game.ui.theme.AppTextMuted
import com.arrowpuzzle.game.ui.viewmodel.LevelItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun LevelSelectScreen(
    levels: List<LevelItem>,
    onBack: () -> Unit,
    onLevelSelected: (Int) -> Unit,
) {
    val s = LocalStrings.current

    // Last unlocked campaign level: the one the player should continue from.
    val currentIndex = remember(levels) {
        levels.indexOfLast { it.isUnlocked }.coerceAtLeast(0)
    }

    // Start a few screens above the current level. Entering the screen then
    // shows a visible, smooth scroll of the last stretch instead of a long
    // animation across the whole list (or an instant teleport).
    val initialIndex = remember(currentIndex) { (currentIndex - 30).coerceAtLeast(0) }
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = initialIndex)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.isNotEmpty() }.first { it }
        if (currentIndex > 0) {
            gridState.animateCenterItem(currentIndex)
        }
    }

    val currentLevelVisible by remember(currentIndex) {
        derivedStateOf {
            val info = gridState.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.index == currentIndex }
            item != null &&
                item.offset.y >= info.viewportStartOffset &&
                item.offset.y + item.size.height <= info.viewportEndOffset
        }
    }

    ScreenBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.padding(start = 4.dp),
            ) {
                Text(
                    text = "‹",
                    fontSize = 32.sp,
                    color = AppText,
                )
            }
            Box(modifier = Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 96.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxSize()
                        .align(Alignment.TopCenter),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        items = levels,
                        key = { it.levelId },
                        contentType = { "level" },
                    ) { level ->
                        LevelTile(
                            level = level,
                            onClick = { onLevelSelected(level.levelId) },
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = !currentLevelVisible,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 22.dp),
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Button(
                        onClick = {
                            scope.launch { gridState.animateCenterItem(currentIndex) }
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppPrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(
                            text = s.t("current_level"),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Smoothly scrolls so that [index] sits in the vertical middle of the viewport.
 *
 * The distance is estimated from the visible rows and then animated with a
 * fixed duration, so a far jump does not turn into an instant teleport. A
 * short correction pass afterwards fixes any estimation error.
 */
private suspend fun LazyGridState.animateCenterItem(index: Int) {
    val info = layoutInfo
    if (info.visibleItemsInfo.isEmpty()) return

    val rows = info.visibleItemsInfo.groupBy { it.offset.y }
    val columns = rows.values.maxOf { it.size }.coerceAtLeast(1)
    val rowYs = rows.keys.sorted()
    val itemHeight = info.visibleItemsInfo.first().size.height
    val rowStep = if (rowYs.size >= 2) {
        (rowYs[1] - rowYs[0]).toFloat()
    } else {
        itemHeight * 1.2f
    }

    val firstIndex = info.visibleItemsInfo.minOf { it.index }
    val viewportHeight = info.viewportEndOffset - info.viewportStartOffset
    val desiredTop = (viewportHeight - itemHeight) / 2f
    val currentRow = firstIndex / columns
    val targetRow = index / columns
    val roughDelta = (targetRow - currentRow) * rowStep - desiredTop
    if (abs(roughDelta) > 1f) {
        // Roughly 2500 px/s, clamped so short moves stay snappy and very long
        // moves never take more than about a second.
        val duration = (abs(roughDelta) / 2.5f).toInt().coerceIn(250, 1100)
        animateScrollBy(
            roughDelta,
            tween(durationMillis = duration, easing = FastOutSlowInEasing),
        )
    }

    val newInfo = layoutInfo
    val item = newInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    val viewportStart = newInfo.viewportStartOffset
    val viewportSize = newInfo.viewportEndOffset - viewportStart
    val exactTop = viewportStart + (viewportSize - item.size.height) / 2
    animateScrollBy(
        (item.offset.y - exactTop).toFloat(),
        tween(durationMillis = 220, easing = FastOutSlowInEasing),
    )
}

@Composable
private fun LevelTile(level: LevelItem, onClick: () -> Unit) {
    val s = LocalStrings.current
    val unlocked = level.isUnlocked
    val completed = level.bestScore > 0
    val shape = RoundedCornerShape(22.dp)
    val numberStyle = MaterialTheme.typography.headlineLarge.copy(
        fontSize = 32.sp,
        lineHeight = 37.sp,
        fontWeight = FontWeight.Black,
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Slightly taller than wide so the big score and its maximum fit
            // below the (equally big) level number.
            .aspectRatio(0.86f)
            .clip(shape)
            .background(
                when {
                    completed -> Brush.verticalGradient(
                        listOf(AppSurfaceRaised, AppSurface),
                    )
                    unlocked -> Brush.verticalGradient(
                        listOf(
                            AppSurface.copy(alpha = 0.85f),
                            AppSurface.copy(alpha = 0.95f),
                        ),
                    )
                    else -> Brush.verticalGradient(
                        listOf(
                            AppSurface.copy(alpha = 0.4f),
                            AppSurface.copy(alpha = 0.6f),
                        ),
                    )
                },
            )
            .border(
                BorderStroke(
                    1.dp,
                    when {
                        completed -> AppPrimary.copy(alpha = 0.55f)
                        unlocked -> AppGridLine.copy(alpha = 0.7f)
                        else -> AppGridLine.copy(alpha = 0.25f)
                    },
                ),
                shape,
            )
            .clickable(enabled = unlocked, onClick = onClick)
            .padding(8.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = level.levelId.toString(),
                style = numberStyle,
                color = when {
                    completed -> AppText
                    unlocked -> AppText
                    else -> AppTextMuted.copy(alpha = 0.45f)
                },
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            // Locked tiles show only the dimmed number; no "locked" label.
            if (completed) {
                Text(
                    text = s.number(level.bestScore),
                    style = numberStyle,
                    color = AppPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                Text(
                    text = "/ " + s.number(level.maxScore),
                    fontSize = 9.sp,
                    color = AppTextMuted.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}
