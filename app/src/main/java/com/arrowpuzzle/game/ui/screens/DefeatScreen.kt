package com.arrowpuzzle.game.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowpuzzle.game.data.model.LevelConfig
import com.arrowpuzzle.game.game.DefeatReason
import com.arrowpuzzle.game.ui.components.ScreenBackground
import com.arrowpuzzle.game.ui.components.SectionCard
import com.arrowpuzzle.game.ui.theme.AppAccent
import com.arrowpuzzle.game.ui.theme.AppDanger
import com.arrowpuzzle.game.ui.theme.AppGridLine
import com.arrowpuzzle.game.ui.theme.AppPrimary
import com.arrowpuzzle.game.ui.theme.AppSecondary
import com.arrowpuzzle.game.ui.theme.AppSurface
import com.arrowpuzzle.game.ui.theme.AppText
import com.arrowpuzzle.game.ui.theme.AppTextMuted
import com.arrowpuzzle.game.util.GridUtils
import kotlin.math.max

@Composable
fun DefeatScreen(
    reason: DefeatReason,
    failures: Int,
    elapsedSeconds: Double,
    arrowCount: Int,
    config: LevelConfig,
    onRetry: () -> Unit,
    onLevels: () -> Unit,
    onHome: () -> Unit,
) {
    val reasonText = when (reason) {
        DefeatReason.TOO_MANY_FAILURES -> "Too many wrong taps."
        DefeatReason.TIME_EXPIRED -> "The time limit was reached."
    }

    ScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 30.dp),
        ) {
            Text(
                text = "NOT",
                style = MaterialTheme.typography.displayMedium,
                color = AppText,
            )
            Text(
                text = "CLEARED",
                style = MaterialTheme.typography.displayMedium,
                color = AppDanger,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (config.isCustom) {
                    "Custom puzzle · ${config.arrowCount} arrows"
                } else {
                    "Level ${config.levelId} · ${config.arrowCount} arrows"
                },
                style = MaterialTheme.typography.titleMedium,
                color = AppTextMuted,
            )

            Spacer(Modifier.height(18.dp))

            SectionCard {
                Column {
                    Text(
                        text = reasonText,
                        style = MaterialTheme.typography.titleMedium,
                        color = AppDanger,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Tap only arrows with a clear path ahead. A wrong tap costs one failure.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTextMuted,
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        DefeatStat(
                            label = "FAILURES",
                            value = "$failures/${max(1, config.maxFailures)}",
                            tint = AppDanger,
                            modifier = Modifier.weight(1f),
                        )
                        DefeatStat(
                            label = "TIME",
                            value = GridUtils.formatTime(elapsedSeconds),
                            tint = AppSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        DefeatStat(
                            label = "ARROWS",
                            value = arrowCount.toString(),
                            tint = AppAccent,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = if (reason == DefeatReason.TOO_MANY_FAILURES) {
                    "Clear the first open arrows to unlock the next ones."
                } else {
                    "Speed matters, but accuracy matters more."
                },
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = AppTextMuted,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onRetry,
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
                    text = if (config.isCustom) "TRY AGAIN" else "RETRY LEVEL",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                )
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = onLevels,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, AppGridLine),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppText),
            ) {
                Text(
                    text = "LEVELS",
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 1.sp,
                )
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = onHome,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "HOME",
                    color = AppTextMuted,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun DefeatStat(
    label: String,
    value: String,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
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
