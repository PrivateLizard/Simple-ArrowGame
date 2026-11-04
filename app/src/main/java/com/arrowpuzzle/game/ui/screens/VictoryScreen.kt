package com.arrowpuzzle.game.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowpuzzle.game.game.GameResult
import com.arrowpuzzle.game.game.LevelCatalog
import com.arrowpuzzle.game.i18n.LocalStrings
import com.arrowpuzzle.game.scoring.ScoreBreakdown
import com.arrowpuzzle.game.scoring.ScoreCalculator
import com.arrowpuzzle.game.ui.components.ConfettiShower
import com.arrowpuzzle.game.ui.components.ScreenBackground
import com.arrowpuzzle.game.ui.theme.AppGridLine
import com.arrowpuzzle.game.ui.theme.AppPrimary
import com.arrowpuzzle.game.ui.theme.AppSecondary
import com.arrowpuzzle.game.ui.theme.AppSuccessDim
import com.arrowpuzzle.game.ui.theme.AppSurface
import com.arrowpuzzle.game.ui.theme.AppText
import com.arrowpuzzle.game.ui.theme.AppTextMuted
import com.arrowpuzzle.game.util.GridUtils
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Level complete: an animated check badge, the headline, a counting-up score,
 * the score breakdown and the run statistics, revealed one after another,
 * with a short and subtle confetti shower on top.
 */
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
    val perfect = record.failures == 0

    ScreenBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(8.dp))
                SuccessBadge(size = 92.dp)
                Spacer(Modifier.height(18.dp))

                Reveal(delayMillis = 250) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (config.isCustom) {
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
                                withStyle(SpanStyle(color = AppText)) { append(s.t("victory_all")) }
                                append(" ")
                                withStyle(SpanStyle(color = AppPrimary)) { append(s.t("victory_clear")) }
                            },
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.sp,
                        )
                        if (result.isNewHighScore) {
                            Spacer(Modifier.height(12.dp))
                            Pill(text = s.t("new_high"))
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                Reveal(delayMillis = 380) {
                    ScoreCard(
                        points = record.points,
                        breakdown = breakdown,
                        perfect = perfect,
                    )
                }

                Spacer(Modifier.height(12.dp))

                Reveal(delayMillis = 500) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        StatBox(
                            label = s.t("time"),
                            value = GridUtils.formatTime(record.timeElapsed),
                            modifier = Modifier.weight(1f),
                        )
                        StatBox(
                            label = s.t("arrows"),
                            value = record.arrowsUsed.toString(),
                            modifier = Modifier.weight(1f),
                        )
                        StatBox(
                            label = s.t("failures"),
                            value = record.failures.toString(),
                            highlight = perfect,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Reveal(delayMillis = 620) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (hasNextLevel) {
                            PrimaryAction(text = s.t("next_level"), onClick = onNextLevel)
                            Spacer(Modifier.height(10.dp))
                        } else if (!config.isCustom) {
                            Text(
                                text = s.t("all_cleared"),
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppPrimary,
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
                            border = BorderStroke(1.dp, AppGridLine),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppText),
                        ) {
                            Text(
                                text = s.t(if (config.isCustom) "play_again" else "replay_level"),
                                style = MaterialTheme.typography.labelLarge,
                                letterSpacing = 1.sp,
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

            // Subtle: muted theme colours, small strips, low alpha, ~3.5 s.
            ConfettiShower(
                colors = listOf(
                    AppPrimary,
                    AppSecondary,
                    AppText.copy(alpha = 0.55f),
                    AppPrimary.copy(alpha = 0.6f),
                ),
            )
        }
    }
}

/** Ring that draws itself, then a check mark. */
@Composable
private fun SuccessBadge(size: Dp) {
    val ring = remember { Animatable(0f) }
    val check = remember { Animatable(0f) }
    val pop = remember { Animatable(0.7f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        ring.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
        check.animateTo(1f, tween(300, easing = LinearOutSlowInEasing))
    }
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            }
            .clip(RoundedCornerShape(50))
            .background(AppSuccessDim.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        val badgeColor = AppPrimary
        Canvas(modifier = Modifier.size(size)) {
            val stroke = this.size.minDimension * 0.06f
            val inset = stroke * 1.5f
            drawArc(
                color = badgeColor,
                startAngle = -90f,
                sweepAngle = 360f * ring.value,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(
                    this.size.width - inset * 2,
                    this.size.height - inset * 2,
                ),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            // Check mark: short leg, then long leg, drawn by progress.
            val w = this.size.width
            val h = this.size.height
            val a = Offset(w * 0.31f, h * 0.52f)
            val b = Offset(w * 0.45f, h * 0.65f)
            val c = Offset(w * 0.70f, h * 0.38f)
            val p = check.value
            val first = (p / 0.4f).coerceIn(0f, 1f)
            val second = ((p - 0.4f) / 0.6f).coerceIn(0f, 1f)
            if (first > 0f) {
                drawLine(
                    color = badgeColor,
                    start = a,
                    end = a + (b - a) * first,
                    strokeWidth = stroke * 1.15f,
                    cap = StrokeCap.Round,
                )
            }
            if (second > 0f) {
                drawLine(
                    color = badgeColor,
                    start = b,
                    end = b + (c - b) * second,
                    strokeWidth = stroke * 1.15f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

/** Fades and slides its content in after [delayMillis]. */
@Composable
private fun Reveal(delayMillis: Int, content: @Composable () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            1f,
            tween(durationMillis = 420, delayMillis = delayMillis, easing = FastOutSlowInEasing),
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = progress.value
                translationY = (1f - progress.value) * 24.dp.toPx()
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun ScoreCard(points: Int, breakdown: ScoreBreakdown, perfect: Boolean) {
    val s = LocalStrings.current
    val counter = remember { Animatable(0f) }
    LaunchedEffect(points) {
        counter.animateTo(
            points.toFloat(),
            tween(durationMillis = 1100, delayMillis = 450, easing = FastOutSlowInEasing),
        )
    }
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(AppSuccessDim.copy(alpha = 0.9f), AppSurface)))
            .border(1.dp, AppPrimary.copy(alpha = 0.35f), shape)
            .padding(horizontal = 20.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = s.t("total_score"),
            style = MaterialTheme.typography.labelMedium,
            color = AppTextMuted,
            letterSpacing = 1.5.sp,
        )
        Text(
            text = String.format(Locale.US, "%,d", counter.value.roundToInt()),
            style = MaterialTheme.typography.displayMedium,
            color = AppPrimary,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = s.t("points"),
            style = MaterialTheme.typography.bodyMedium,
            color = AppTextMuted,
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AppGridLine.copy(alpha = 0.7f)),
        )
        Spacer(Modifier.height(12.dp))
        BreakdownRow(s.t("base_points"), breakdown.basePoints, AppText)
        BreakdownRow(s.t("time_bonus"), breakdown.timeBonus, AppSecondary)
        BreakdownRow(
            label = if (perfect) s.t("no_failure_bonus") else s.t("no_failure_bonus_missed"),
            value = breakdown.comboBonus,
            color = if (perfect) AppPrimary else AppTextMuted,
        )
    }
}

@Composable
private fun BreakdownRow(label: String, value: Int, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = AppTextMuted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(12.dp))
        Text(
            text = "+" + String.format(Locale.US, "%,d", value),
            color = color,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(AppSurface)
            .border(
                1.dp,
                if (highlight) AppPrimary.copy(alpha = 0.45f) else AppGridLine.copy(alpha = 0.7f),
                shape,
            )
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = AppTextMuted,
            maxLines = 1,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = if (highlight) AppPrimary else AppText,
        )
    }
}

@Composable
private fun Pill(text: String) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(AppPrimary.copy(alpha = 0.12f))
            .border(1.dp, AppPrimary.copy(alpha = 0.45f), shape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = AppPrimary,
            letterSpacing = 1.sp,
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
