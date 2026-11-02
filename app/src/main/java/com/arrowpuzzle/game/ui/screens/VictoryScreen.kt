package com.arrowpuzzle.game.ui.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowpuzzle.game.game.GameResult
import com.arrowpuzzle.game.game.LevelCatalog
import com.arrowpuzzle.game.i18n.LocalStrings
import com.arrowpuzzle.game.scoring.ScoreBreakdown
import com.arrowpuzzle.game.scoring.ScoreCalculator
import com.arrowpuzzle.game.ui.components.ScreenBackground
import com.arrowpuzzle.game.ui.components.SectionCard
import com.arrowpuzzle.game.ui.theme.AppAccent
import com.arrowpuzzle.game.ui.theme.AppDanger
import com.arrowpuzzle.game.ui.theme.AppGridLine
import com.arrowpuzzle.game.ui.theme.AppPrimary
import com.arrowpuzzle.game.ui.theme.AppSecondary
import com.arrowpuzzle.game.ui.theme.AppSuccessDim
import com.arrowpuzzle.game.ui.theme.AppSurface
import com.arrowpuzzle.game.ui.theme.AppText
import com.arrowpuzzle.game.ui.theme.AppTextMuted
import com.arrowpuzzle.game.util.GridUtils
import java.util.Locale

@Composable
fun VictoryScreen(
    result: GameResult,
    onNextLevel: () -> Unit,
    onRetry: () -> Unit,
    onLevels: () -> Unit,
    onHome: () -> Unit,
) {
    val s = LocalStrings.current
    val record = result.record
    val config = result.config
    val breakdown = ScoreCalculator.calculate(
        arrowCount = record.arrowsUsed,
        timeElapsedSeconds = record.timeElapsed,
        failures = record.failures,
    )
    val hasNextLevel = !config.isCustom && config.levelId < LevelCatalog.MAX_LEVEL

    ScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 28.dp),
        ) {
            Text(
                text = s.t("victory_all"),
                style = MaterialTheme.typography.displayMedium,
                color = AppText,
            )
            Text(
                text = s.t("victory_clear"),
                style = MaterialTheme.typography.displayMedium,
                color = AppPrimary,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (config.isCustom) {
                    s.f("custom_info", config.arrowCount)
                } else {
                    s.f("level_info", config.levelId, config.arrowCount)
                },
                style = MaterialTheme.typography.titleMedium,
                color = AppTextMuted,
            )

            if (result.isNewHighScore) {
                Spacer(Modifier.height(18.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AppAccent.copy(alpha = 0.14f))
                        .border(1.dp, AppAccent.copy(alpha = 0.45f), RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Text(
                        text = s.t("new_high"),
                        style = MaterialTheme.typography.labelMedium,
                        color = AppAccent,
                        letterSpacing = 1.sp,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(AppSuccessDim, AppSurface),
                        ),
                    )
                    .border(1.dp, AppPrimary.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = s.t("total_score"),
                        style = MaterialTheme.typography.labelMedium,
                        color = AppTextMuted,
                        letterSpacing = 1.5.sp,
                    )
                    Text(
                        text = String.format(Locale.US, "%,d", record.points),
                        style = MaterialTheme.typography.displayLarge,
                        color = AppPrimary,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = s.t("points"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTextMuted,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            SectionCard {
                ScoreRows(breakdown, record.failures)
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                InfoBox(
                    label = s.t("time"),
                    value = GridUtils.formatTime(record.timeElapsed),
                    tint = AppSecondary,
                    modifier = Modifier.weight(1f),
                )
                InfoBox(
                    label = s.t("arrows"),
                    value = record.arrowsUsed.toString(),
                    tint = AppText,
                    modifier = Modifier.weight(1f),
                )
                InfoBox(
                    label = s.t("failures"),
                    value = record.failures.toString(),
                    tint = if (record.failures == 0) AppPrimary else AppAccent,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(24.dp))

            if (hasNextLevel) {
                PrimaryAction(text = s.t("next_level"), onClick = onNextLevel)
                Spacer(Modifier.height(10.dp))
            } else if (!config.isCustom) {
                Text(
                    text = s.t("all_cleared"),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppAccent,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
            }

            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppGridLine),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppText),
            ) {
                Text(
                    text = s.t(if (config.isCustom) "play_again" else "replay_level"),
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 1.sp,
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TextButton(onClick = onLevels) {
                    Text(s.t("levels"), color = AppTextMuted, style = MaterialTheme.typography.labelLarge)
                }
                TextButton(onClick = onHome) {
                    Text(s.t("home"), color = AppTextMuted, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun ScoreRows(breakdown: ScoreBreakdown, failures: Int) {
    val s = LocalStrings.current
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(s.t("base_points"), color = AppTextMuted, style = MaterialTheme.typography.bodyMedium)
            Text(
                "+${breakdown.basePoints}",
                color = AppText,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(s.t("time_bonus"), color = AppTextMuted, style = MaterialTheme.typography.bodyMedium)
            Text(
                "+${breakdown.timeBonus}",
                color = AppSecondary,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (failures == 0) s.t("no_failure_bonus") else s.t("no_failure_bonus_missed"),
                color = AppTextMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "+${breakdown.comboBonus}",
                color = if (failures == 0) AppPrimary else AppTextMuted,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AppGridLine),
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(s.t("total"), color = AppText, style = MaterialTheme.typography.titleMedium)
            Text(
                breakdown.total.toString(),
                color = AppPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun InfoBox(
    label: String,
    value: String,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(AppSurface)
            .border(1.dp, AppGridLine.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = AppTextMuted,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = tint,
        )
    }
}

@Composable
private fun PrimaryAction(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppPrimary,
            contentColor = AppSurface,
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp,
        )
    }
}
