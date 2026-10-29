package com.arrowpuzzle.game.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    isAllUnlocked: Boolean = false,
    onToggleUnlockAll: (Boolean) -> Unit = {},
    resumeLevel: Int? = null,
    resumeRemaining: Int? = null,
    onResume: () -> Unit = {},
) {
    var showDevOptions by rememberSaveable { mutableStateOf(false) }
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

            Spacer(Modifier.height(40.dp))

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = AppText)) { append("SIMPLE ") }
                    withStyle(SpanStyle(color = AppPrimary)) { append("ARROW PUZZLE") }
                },
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 27.sp,
                    letterSpacing = (-0.5).sp,
                ),
                maxLines = 1,
                softWrap = false,
            )

            Spacer(Modifier.height(30.dp))

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

            if (resumeLevel != null) {
                OutlinedButton(
                    onClick = onResume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        AppSecondary.copy(alpha = 0.8f),
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppSecondary),
                ) {
                    Text(
                        text = buildString {
                            append("RESUME · LEVEL ")
                            append(resumeLevel.toString().padStart(2, '0'))
                            if (resumeRemaining != null) {
                                append(" · ")
                                append(resumeRemaining)
                                append(" LEFT")
                            }
                        },
                        style = MaterialTheme.typography.titleMedium,
                        letterSpacing = 0.8.sp,
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

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

            Spacer(Modifier.height(10.dp))

            // Developer options stay hidden until the small DEV label is tapped,
            // so normal players never see level skipping.
            TextButton(
                onClick = { showDevOptions = !showDevOptions },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (showDevOptions) "DEV \u25B4" else "DEV \u25BE",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTextMuted.copy(alpha = 0.55f),
                    letterSpacing = 1.5.sp,
                )
            }

            AnimatedVisibility(visible = showDevOptions) {
                DevOptionsCard(
                    isAllUnlocked = isAllUnlocked,
                    highestUnlocked = highestUnlocked,
                    onToggleUnlockAll = onToggleUnlockAll,
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
private fun DevOptionsCard(
    isAllUnlocked: Boolean,
    highestUnlocked: Int,
    onToggleUnlockAll: (Boolean) -> Unit,
) {
    SectionCard {
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = "DEV OPTIONS",
                style = MaterialTheme.typography.labelMedium,
                color = AppSecondary,
                letterSpacing = 1.2.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Progress: $highestUnlocked / ${LevelCatalog.MAX_LEVEL}",
                style = MaterialTheme.typography.bodySmall,
                color = AppTextMuted,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { onToggleUnlockAll(!isAllUnlocked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppAccent.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppAccent),
            ) {
                Text(
                    text = if (isAllUnlocked) {
                        "LOCK ALL LEVELS"
                    } else {
                        "UNLOCK ALL ${LevelCatalog.MAX_LEVEL} LEVELS"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 0.8.sp,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (isAllUnlocked) {
                    "All levels are open. Locking resets progress to level 1; best scores are kept."
                } else {
                    "Temporary test switch. Scores and settings are never touched."
                },
                style = MaterialTheme.typography.bodySmall,
                color = AppTextMuted.copy(alpha = 0.8f),
            )
        }
    }
}

