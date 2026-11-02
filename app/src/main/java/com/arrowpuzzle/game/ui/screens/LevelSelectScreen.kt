package com.arrowpuzzle.game.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.arrowpuzzle.game.game.LevelCatalog
import com.arrowpuzzle.game.ui.components.AppTopBar
import com.arrowpuzzle.game.ui.components.ScreenBackground
import com.arrowpuzzle.game.ui.theme.AppAccent
import com.arrowpuzzle.game.ui.theme.AppGridLine
import com.arrowpuzzle.game.ui.theme.AppPrimary
import com.arrowpuzzle.game.ui.theme.AppSecondary
import com.arrowpuzzle.game.ui.theme.AppSurface
import com.arrowpuzzle.game.ui.theme.AppSurfaceRaised
import com.arrowpuzzle.game.ui.theme.AppText
import com.arrowpuzzle.game.ui.theme.AppTextMuted
import com.arrowpuzzle.game.ui.viewmodel.LevelItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun LevelSelectScreen(
    levels: List<LevelItem>,
    onBack: () -> Unit,
    onLevelSelected: (Int) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    // Last unlocked campaign level: the one the player should continue from.
    val currentIndex = remember(levels) {
        levels.indexOfLast { it.isUnlocked }.coerceAtLeast(0)
    }

    // Jump to the current level once, so it sits near the middle when possible.
    LaunchedEffect(Unit) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.isNotEmpty() }.first { it }
        if (currentIndex > 0) {
            gridState.centerItem(currentIndex)
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
            AppTopBar(
                title = "Levels",
                subtitle = "${levels.count { it.isUnlocked }} of ${LevelCatalog.MAX_LEVEL} unlocked",
                onBack = onBack,
            )
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
                    items(items = levels, key = { it.levelId }) { level ->
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
                        onClick = { scope.launch { gridState.centerItem(currentIndex) } },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppPrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(
                            text = "CURRENT LEVEL",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

/** Scrolls so that [index] sits in the vertical middle of the viewport. */
private suspend fun LazyGridState.centerItem(index: Int) {
    scrollToItem(index)
    val info = layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    val viewportStart = info.viewportStartOffset
    val viewportSize = info.viewportEndOffset - viewportStart
    val desiredTop = viewportStart + (viewportSize - item.size.height) / 2
    scrollBy((item.offset.y - desiredTop).toFloat())
}

@Composable
private fun LevelTile(level: LevelItem, onClick: () -> Unit) {
    val unlocked = level.isUnlocked
    val completed = level.bestScore > 0
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
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
            .padding(12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = level.levelId.toString(),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = when {
                    completed -> AppPrimary
                    unlocked -> AppText
                    else -> AppTextMuted.copy(alpha = 0.45f)
                },
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = when {
                    completed -> "✓  " + String.format(Locale.US, "%,d", level.bestScore)
                    unlocked -> "PLAY"
                    else -> "LOCKED"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    completed -> AppAccent
                    unlocked -> AppSecondary
                    else -> AppTextMuted.copy(alpha = 0.5f)
                },
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}
