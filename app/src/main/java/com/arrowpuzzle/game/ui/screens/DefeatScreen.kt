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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.arrowpuzzle.game.data.model.AppSettings
import com.arrowpuzzle.game.data.model.LevelConfig
import com.arrowpuzzle.game.game.DefeatReason
import com.arrowpuzzle.game.i18n.LocalStrings
import com.arrowpuzzle.game.ui.components.ResultBadge
import com.arrowpuzzle.game.ui.components.ResultMark
import com.arrowpuzzle.game.ui.components.ResultStat
import com.arrowpuzzle.game.ui.components.Reveal
import com.arrowpuzzle.game.ui.components.ScreenBackground
import com.arrowpuzzle.game.ui.theme.AppDanger
import com.arrowpuzzle.game.ui.theme.AppPrimary
import com.arrowpuzzle.game.ui.theme.AppSurface
import com.arrowpuzzle.game.ui.theme.AppText
import com.arrowpuzzle.game.ui.theme.AppTextMuted
import com.arrowpuzzle.game.util.GridUtils

/**
 * Level failed: same family as the level-complete screen, but calm. A cross
 * badge with one soft shake, the reason with one concrete tip, the two values
 * that ended the run (the one that hit its limit is tinted), and "try again"
 * as the obvious next step. No confetti, no arrow counts.
 */
@Composable
fun DefeatScreen(
    reason: DefeatReason,
    failures: Int,
    elapsedSeconds: Double,
    config: LevelConfig,
    onRetry: () -> Unit,
    onLevels: () -> Unit,
    onHome: () -> Unit,
) {
    val s = LocalStrings.current
    val byFailures = reason == DefeatReason.TOO_MANY_FAILURES
    val failureLimit = if (config.maxFailures == AppSettings.NO_FAILURE_LIMIT) {
        "∞"
    } else {
        config.maxFailures.toString()
    }
    val timeValue = config.timeLimitSeconds?.let { limit ->
        GridUtils.formatTime(elapsedSeconds.coerceAtMost(limit.toDouble())) +
            " / " + GridUtils.formatTime(limit.toDouble())
    } ?: GridUtils.formatTime(elapsedSeconds)

    ScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            ResultBadge(
                mark = ResultMark.CROSS,
                color = AppDanger,
                background = AppDanger.copy(alpha = 0.12f),
            )
            Spacer(Modifier.height(18.dp))

            Reveal(delayMillis = 250) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (config.isShaped) {
                            s.t("shape_${config.shapeId}").uppercase(s.locale)
                        } else if (config.isCustom) {
                            s.t("custom_puzzle")
                        } else {
                            s.f("level_title", config.levelId.toString())
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = AppTextMuted,
                        letterSpacing = 2.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = AppText)) { append(s.t("defeat_not")) }
                            append(" ")
                            withStyle(SpanStyle(color = AppDanger)) { append(s.t("defeat_cleared")) }
                        },
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp,
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            Reveal(delayMillis = 380) {
                ReasonCard(
                    title = s.t(if (byFailures) "reason_failures" else "reason_time"),
                    tip = s.t(if (byFailures) "defeat_hint" else "defeat_hint_time"),
                )
            }

            Spacer(Modifier.height(12.dp))

            Reveal(delayMillis = 500) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ResultStat(
                        label = s.t("failures"),
                        value = "$failures/$failureLimit",
                        highlight = if (byFailures) AppDanger else null,
                        modifier = Modifier.weight(1f),
                    )
                    ResultStat(
                        label = s.t("time"),
                        value = timeValue,
                        highlight = if (!byFailures) AppDanger else null,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(26.dp))

            Reveal(delayMillis = 620) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                            text = s.t(if (config.isCustom) "try_again" else "retry_level"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
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
    }
}

/** Reason headline with a thin accent bar and one practical tip. */
@Composable
private fun ReasonCard(title: String, tip: String) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppSurface)
            .border(1.dp, AppDanger.copy(alpha = 0.3f), shape)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(50))
                .background(AppDanger.copy(alpha = 0.8f)),
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = AppText,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = tip,
                style = MaterialTheme.typography.bodyMedium,
                color = AppTextMuted,
            )
        }
    }
}
