package com.arrowpuzzle.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.arrowpuzzle.game.ui.theme.AppSurfaceRaised
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Switch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.arrowpuzzle.game.game.LevelCatalog
import com.arrowpuzzle.game.i18n.LocalStrings
import com.arrowpuzzle.game.ui.components.ScreenBackground
import com.arrowpuzzle.game.ui.components.SectionCard
import com.arrowpuzzle.game.ui.theme.AppGridLine
import com.arrowpuzzle.game.ui.theme.AppPrimary
import com.arrowpuzzle.game.ui.theme.AppSecondary
import com.arrowpuzzle.game.ui.theme.AppSurface
import com.arrowpuzzle.game.ui.theme.AppText
import com.arrowpuzzle.game.ui.theme.AppTextMuted

@Composable
fun HomeScreen(
    highScore: Int,
    highestUnlocked: Int,
    onPlay: () -> Unit,
    onCustom: () -> Unit,
    onSettings: () -> Unit,
    onTest: () -> Unit = {},
    isAllUnlocked: Boolean = false,
    onToggleUnlockAll: (Boolean) -> Unit = {},
    onUnlockLevelCount: (Int) -> Unit = {},
    devModeEnabled: Boolean = false,
    devWinButtons: Boolean = false,
    onDevWinButtonsChange: (Boolean) -> Unit = {},
    resumeLevel: Int? = null,
    resumeRemaining: Int? = null,
    onResume: () -> Unit = {},
) {
    val s = LocalStrings.current
    ScreenBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // 10 % of the screen height above and below the personal-best card.
            val verticalGap = maxHeight * 0.10f
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 28.dp),
            ) {
                Text(
                    text = s.t("home_status"),
                    modifier = Modifier.padding(horizontal = 24.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppTextMuted,
                )

                Spacer(Modifier.height(24.dp))

                TitleSpanningWidth(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                )

                Spacer(Modifier.height(verticalGap))

                CenteredContent {
                    SectionCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = s.t("total_points"),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AppTextMuted,
                                )
                                Text(
                                    text = s.number(highScore),
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = AppPrimary,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = s.t("progress"),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AppTextMuted,
                                )
                                Text(
                                    text = "$highestUnlocked / ${LevelCatalog.MAX_LEVEL}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = AppPrimary,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(verticalGap))

                CenteredContent {
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
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = AppSecondary,
                            ),
                        ) {
                            Text(
                                text = if (resumeRemaining != null) {
                                    s.f(
                                        "resume",
                                        resumeLevel.toString().padStart(2, '0'),
                                        resumeRemaining,
                                    )
                                } else {
                                    s.f(
                                        "resume_simple",
                                        resumeLevel.toString().padStart(2, '0'),
                                    )
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
                            text = s.t("play"),
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
                            text = s.t("custom_mode"),
                            style = MaterialTheme.typography.titleMedium,
                            letterSpacing = 1.sp,
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    if (devModeEnabled) {
                        OutlinedButton(
                            onClick = onTest,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp),
                            shape = RoundedCornerShape(18.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppGridLine),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppText),
                        ) {
                            Text(
                                text = s.t("test_mode"),
                                style = MaterialTheme.typography.titleMedium,
                                letterSpacing = 1.sp,
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                    }

                    OutlinedButton(
                        onClick = onSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppGridLine),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppText),
                    ) {
                        Text(
                            text = s.t("settings"),
                            style = MaterialTheme.typography.titleMedium,
                            letterSpacing = 1.sp,
                        )
                    }

                    if (devModeEnabled) {
                        Spacer(Modifier.height(14.dp))
                        DevOptionsCard(
                            isAllUnlocked = isAllUnlocked,
                            highestUnlocked = highestUnlocked,
                            onToggleUnlockAll = onToggleUnlockAll,
                            onUnlockLevelCount = onUnlockLevelCount,
                            winButtons = devWinButtons,
                            onWinButtonsChange = onDevWinButtonsChange,
                        )
                    }
                }

                Spacer(Modifier.height(26.dp))

                Text(
                    text = s.t("footer"),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTextMuted.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Measures the title once and scales the font so it runs edge to edge across
 * the available width. Compose has no auto-size text, so the size is derived
 * from the measured width at a fixed base size.
 */
@Composable
private fun TitleSpanningWidth(modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val measurer = rememberTextMeasurer()
    val baseStyle = MaterialTheme.typography.headlineLarge.copy(
        fontWeight = FontWeight.Black,
        letterSpacing = 0.sp,
    )
    val title = buildAnnotatedString {
        withStyle(SpanStyle(color = AppText)) { append(s.t("title_simple")) }
        withStyle(SpanStyle(color = AppPrimary)) { append(s.t("title_arrow")) }
    }
    val baseSize = 40f
    val measured = measurer.measure(
        text = title,
        style = baseStyle.copy(fontSize = baseSize.sp),
        maxLines = 1,
        softWrap = false,
    )
    BoxWithConstraints(modifier = modifier) {
        val targetWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val measuredWidth = measured.size.width.toFloat()
        val scale = if (measuredWidth > 0f && targetWidthPx > 0f) {
            targetWidthPx / measuredWidth
        } else {
            1f
        }
        Text(
            text = title,
            style = baseStyle.copy(fontSize = (baseSize * scale).sp),
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Centres its content and limits it to 75 % of the available width. */
@Composable
private fun CenteredContent(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(modifier = Modifier.fillMaxWidth(0.75f)) {
            content()
        }
    }
}

@Composable
private fun DevOptionsCard(
    isAllUnlocked: Boolean,
    highestUnlocked: Int,
    onToggleUnlockAll: (Boolean) -> Unit,
    onUnlockLevelCount: (Int) -> Unit,
    winButtons: Boolean,
    onWinButtonsChange: (Boolean) -> Unit,
) {
    val s = LocalStrings.current
    val focusManager = LocalFocusManager.current
    var levelText by remember(highestUnlocked) {
        mutableStateOf(
            highestUnlocked.coerceIn(1, LevelCatalog.MAX_LEVEL).toString(),
        )
    }
    val requestedLevel = levelText.toIntOrNull()
    val inputValid = requestedLevel != null &&
        requestedLevel in 1..LevelCatalog.MAX_LEVEL
    SectionCard {
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = s.t("dev_options"),
                style = MaterialTheme.typography.labelMedium,
                color = AppPrimary,
                letterSpacing = 1.2.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = s.f("dev_progress", highestUnlocked, LevelCatalog.MAX_LEVEL),
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
                border = androidx.compose.foundation.BorderStroke(1.dp, AppPrimary.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppPrimary),
            ) {
                Text(
                    text = if (isAllUnlocked) {
                        s.t("dev_lock_all")
                    } else {
                        s.f("dev_unlock_all", LevelCatalog.MAX_LEVEL)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 0.8.sp,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (isAllUnlocked) {
                    s.t("dev_all_open")
                } else {
                    s.t("dev_temp")
                },
                style = MaterialTheme.typography.bodySmall,
                color = AppTextMuted.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = s.f("dev_unlock_to", LevelCatalog.MAX_LEVEL),
                style = MaterialTheme.typography.bodySmall,
                color = AppTextMuted,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = levelText,
                    onValueChange = { input ->
                        levelText = input.filter { it.isDigit() }.take(4)
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = levelText.isNotEmpty() && !inputValid,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    label = { Text(s.t("dev_level")) },
                )
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = {
                        requestedLevel?.let { level ->
                            onUnlockLevelCount(
                                level.coerceIn(1, LevelCatalog.MAX_LEVEL),
                            )
                            focusManager.clearFocus()
                        }
                    },
                    enabled = inputValid,
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppPrimary,
                        contentColor = AppSurface,
                    ),
                ) {
                    Text(
                        text = s.t("dev_apply"),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = s.t("dev_win_buttons"),
                        style = MaterialTheme.typography.titleSmall,
                        color = AppText,
                    )
                    Text(
                        text = s.t("dev_win_buttons_desc"),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTextMuted.copy(alpha = 0.8f),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Switch(
                    checked = winButtons,
                    onCheckedChange = onWinButtonsChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AppSurfaceRaised,
                        checkedTrackColor = AppPrimary,
                        uncheckedThumbColor = AppTextMuted,
                        uncheckedTrackColor = AppGridLine,
                    ),
                )
            }
        }
    }
}
