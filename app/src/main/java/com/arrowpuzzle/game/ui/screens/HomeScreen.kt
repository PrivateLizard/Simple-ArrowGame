package com.arrowpuzzle.game.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.arrowpuzzle.game.game.LevelCatalog
import com.arrowpuzzle.game.i18n.LocalStrings
import com.arrowpuzzle.game.ui.components.ArrowSwitch
import com.arrowpuzzle.game.ui.components.ArrowButton
import com.arrowpuzzle.game.ui.components.ScreenBackground
import com.arrowpuzzle.game.ui.components.SectionCard
import com.arrowpuzzle.game.ui.theme.AppPrimary
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
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            HomeStat(
                                label = s.t("total_points"),
                                value = s.number(highScore),
                                modifier = Modifier.weight(1f),
                            )
                            HomeStat(
                                label = s.t("progress"),
                                // A level number, written like on the level tiles.
                                value = highestUnlocked.toString(),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(verticalGap))

                CenteredContent {
                    if (resumeLevel != null) {
                        ArrowButton(
                            onClick = onResume,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp),
                            tint = AppPrimary,
                            emphasis = true,
                            lineWidth = 2.5.dp,
                        ) {
                            OneLineFitText(
                                text = s.t("continue"),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.5.sp,
                                ),
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    ArrowButton(
                        onClick = onPlay,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp),
                        tint = AppPrimary,
                        emphasis = true,
                        lineWidth = 2.5.dp,
                    ) {
                        Text(
                            text = s.t("play"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    ArrowButton(
                        onClick = onCustom,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        tint = AppText,
                    ) {
                        Text(
                            text = s.t("custom_mode"),
                            style = MaterialTheme.typography.titleMedium,
                            letterSpacing = 1.sp,
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    if (devModeEnabled) {
                        ArrowButton(
                            onClick = onTest,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            tint = AppText,
                        ) {
                            Text(
                                text = s.t("test_mode"),
                                style = MaterialTheme.typography.titleMedium,
                                letterSpacing = 1.sp,
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                    }

                    ArrowButton(
                        onClick = onSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        tint = AppText,
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
    // Two lines: "SIMPLE" on its own, "ARROW PUZZLE" right below. The second,
    // longer line sets the size so it runs edge to edge.
    val firstLine = s.t("title_simple").trim()
    val secondLine = s.t("title_arrow").trim()
    val baseSize = 40f
    val measured = measurer.measure(
        text = secondLine,
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
        val fontSize = (baseSize * scale).sp
        // One text with two lines: only then does the line height set the
        // distance between them. A bit below the font size, so they sit close
        // together like one word mark (capitals only, no descenders).
        val lineStyle = baseStyle.copy(
            fontSize = fontSize,
            lineHeight = fontSize * 0.84f,
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both,
            ),
        )
        val title = buildAnnotatedString {
            withStyle(SpanStyle(color = AppText)) { append(firstLine) }
            append('\n')
            withStyle(SpanStyle(color = AppPrimary)) { append(secondLine) }
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = lineStyle,
                maxLines = 2,
                softWrap = false,
            )
        }
    }
}

/** Label centred above a big number, for the summary card on Home. */
@Composable
private fun HomeStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = AppTextMuted,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge,
            color = AppPrimary,
            maxLines = 1,
        )
    }
}

/** One centred line; the font only shrinks when the text would not fit. */
@Composable
private fun OneLineFitText(text: String, style: TextStyle) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val available = constraints.maxWidth
        val needed = remember(text, style) {
            measurer.measure(text = text, style = style, maxLines = 1, softWrap = false).size.width
        }
        val scale = if (needed > available && available > 0) {
            available * 0.98f / needed
        } else {
            1f
        }
        Text(
            text = text,
            style = style.copy(
                fontSize = style.fontSize * scale,
                letterSpacing = style.letterSpacing * scale,
            ),
            maxLines = 1,
            softWrap = false,
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
            ArrowButton(
                onClick = { onToggleUnlockAll(!isAllUnlocked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                tint = AppPrimary,
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
                ArrowButton(
                    onClick = {
                        requestedLevel?.let { level ->
                            onUnlockLevelCount(
                                level.coerceIn(1, LevelCatalog.MAX_LEVEL),
                            )
                            focusManager.clearFocus()
                        }
                    },
                    enabled = inputValid,
                    modifier = Modifier.height(60.dp),
                    tint = AppPrimary,
                    emphasis = true,
                    contentPadding = PaddingValues(horizontal = 14.dp),
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
                ArrowSwitch(
                    checked = winButtons,
                    onCheckedChange = onWinButtonsChange,
                )
            }
        }
    }
}
