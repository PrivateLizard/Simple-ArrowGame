package com.arrowpuzzle.game.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowpuzzle.game.R
import com.arrowpuzzle.game.game.LevelCatalog
import com.arrowpuzzle.game.ui.components.ScreenBackground
import com.arrowpuzzle.game.ui.components.SectionCard
import com.arrowpuzzle.game.ui.theme.AppAccent
import com.arrowpuzzle.game.ui.theme.AppGridLine
import com.arrowpuzzle.game.ui.theme.AppPrimary
import com.arrowpuzzle.game.ui.theme.AppSecondary
import com.arrowpuzzle.game.ui.theme.AppSurface
import com.arrowpuzzle.game.ui.theme.AppText
import com.arrowpuzzle.game.ui.theme.AppTextMuted
import java.util.Locale

@Composable
fun HomeScreen(
    highScore: Int,
    highestUnlocked: Int,
    onPlay: () -> Unit,
    onCustom: () -> Unit,
    onSettings: () -> Unit,
) {
    ScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "OFFLINE  ·  NO ADS  ·  NO TRACKING",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppTextMuted,
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(AppPrimary),
                )
            }

            Spacer(Modifier.height(34.dp))

            Text(
                text = "ARROW",
                style = MaterialTheme.typography.displayLarge,
                color = AppText,
                letterSpacing = (-1).sp,
            )
            Text(
                text = "PUZZLE",
                style = MaterialTheme.typography.displayLarge,
                color = AppPrimary,
                letterSpacing = (-1).sp,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Clear every arrow. Only open paths can be tapped.",
                style = MaterialTheme.typography.bodyLarge,
                color = AppTextMuted,
            )

            Spacer(Modifier.height(26.dp))

            ArrowTrail()

            Spacer(Modifier.height(26.dp))

            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "PERSONAL BEST",
                            style = MaterialTheme.typography.labelMedium,
                            color = AppTextMuted,
                        )
                        Text(
                            text = String.format(Locale.US, "%,d", highScore),
                            style = MaterialTheme.typography.headlineLarge,
                            color = AppAccent,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "PROGRESS",
                            style = MaterialTheme.typography.labelMedium,
                            color = AppTextMuted,
                        )
                        Text(
                            text = "$highestUnlocked / ${LevelCatalog.MAX_LEVEL}",
                            style = MaterialTheme.typography.titleLarge,
                            color = AppSecondary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(26.dp))

            Button(
                onClick = onPlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppPrimary,
                    contentColor = AppSurface,
                ),
            ) {
                Text(
                    text = "PLAY",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onCustom,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppGridLine),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppText),
            ) {
                Text(
                    text = "CUSTOM MODE",
                    style = MaterialTheme.typography.titleMedium,
                    letterSpacing = 1.sp,
                )
            }

            Spacer(Modifier.height(6.dp))

            TextButton(
                onClick = onSettings,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "SETTINGS",
                    style = MaterialTheme.typography.labelLarge,
                    color = AppTextMuted,
                    letterSpacing = 1.sp,
                )
            }

            Spacer(Modifier.height(26.dp))

            Text(
                text = "Every level is generated and validated on-device.\nNo internet permission is requested.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = AppTextMuted.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ArrowTrail() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AppSurface.copy(alpha = 0.55f))
            .border(1.dp, AppGridLine.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArrowIcon(R.drawable.ic_arrow_right, 0f, AppSecondary)
        ArrowIcon(R.drawable.ic_arrow_right, 45f, AppPrimary)
        ArrowIcon(R.drawable.ic_arrow_right, 90f, AppTextMuted)
        ArrowIcon(R.drawable.ic_arrow_right, 135f, AppPrimary)
        ArrowIcon(R.drawable.ic_arrow_right, 180f, AppAccent)
    }
}

@Composable
private fun ArrowIcon(drawable: Int, rotation: Float, tint: Color) {
    Image(
        painter = painterResource(drawable),
        contentDescription = null,
        modifier = Modifier
            .size(34.dp)
            .graphicsLayer(rotationZ = rotation),
        colorFilter = ColorFilter.tint(tint),
    )
}
