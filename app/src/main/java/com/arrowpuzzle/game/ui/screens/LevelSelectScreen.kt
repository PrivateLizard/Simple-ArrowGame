package com.arrowpuzzle.game.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import java.util.Locale

@Composable
fun LevelSelectScreen(
    levels: List<LevelItem>,
    onBack: () -> Unit,
    onLevelSelected: (Int) -> Unit,
) {
    ScreenBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = "Levels",
                subtitle = "${levels.count { it.isUnlocked }} unlocked · ${LevelCatalog.MAX_LEVEL} total",
                onBack = onBack,
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 148.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items = levels, key = { it.levelId }) { level ->
                    LevelCard(
                        level = level,
                        onClick = { onLevelSelected(level.levelId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelCard(level: LevelItem, onClick: () -> Unit) {
    val unlocked = level.isUnlocked
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(154.dp)
            .clip(shape)
            .background(
                if (unlocked) {
                    Brush.verticalGradient(listOf(AppSurfaceRaised, AppSurface))
                } else {
                    Brush.verticalGradient(listOf(AppSurface.copy(alpha = 0.6f), AppSurface.copy(alpha = 0.9f)))
                },
            )
            .border(
                BorderStroke(
                    1.dp,
                    if (unlocked) AppGridLine.copy(alpha = 0.85f) else AppGridLine.copy(alpha = 0.35f),
                ),
                shape,
            )
            .clickable(enabled = unlocked, onClick = onClick)
            .padding(16.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        text = "LEVEL",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppTextMuted,
                    )
                    Text(
                        text = level.levelId.toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = if (unlocked) AppText else AppTextMuted.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Black,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (unlocked) AppPrimary else AppGridLine),
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "${level.openArrowCount} open",
                style = MaterialTheme.typography.bodyMedium,
                color = if (unlocked) AppTextMuted else AppTextMuted.copy(alpha = 0.45f),
            )
            Spacer(Modifier.height(7.dp))
            when {
                !unlocked -> Text(
                    text = "LOCKED",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppTextMuted.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
                level.bestScore > 0 -> Text(
                    text = "BEST  ${String.format(Locale.US, "%,d", level.bestScore)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppAccent,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
                else -> Text(
                    text = "READY",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppSecondary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}
