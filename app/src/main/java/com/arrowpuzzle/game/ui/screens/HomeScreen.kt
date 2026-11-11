package com.arrowpuzzle.game.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import com.arrowpuzzle.game.ui.components.ArrowDialog
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
import com.arrowpuzzle.game.ui.theme.AppBoardLine
import com.arrowpuzzle.game.ui.components.drawPolylineArrow
import com.arrowpuzzle.game.ui.components.arrowHeadLength
import com.arrowpuzzle.game.ui.components.arrowHeadHalfWidth
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
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
    /** Campaign levels cleared at least once. */
    completedLevels: Int,
    /** Highest unlocked level; only the developer options use it. */
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
    /**
     * Dev option: the most points the whole campaign can give. When set, the
     * card shows it in place of the real total, to check how it looks.
     */
    maxPoints: Int? = null,
    devHomeMaxPoints: Boolean = false,
    onDevHomeMaxPointsChange: (Boolean) -> Unit = {},
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

                // Before the first cleared level there is nothing to show yet;
                // the buttons then move up right below the title.
                if (completedLevels > 0 || maxPoints != null) {
                    CenteredContent {
                        SectionCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                // Labels on one line even when a number
                                // shrinks to fit.
                                verticalAlignment = Alignment.Top,
                            ) {
                                HomeStat(
                                    label = s.t("total_points"),
                                    value = s.number(maxPoints ?: highScore),
                                    modifier = Modifier.weight(1f),
                                )
                                HomeStat(
                                    label = s.t("progress"),
                                    // Cleared levels, written like a level number.
                                    value = completedLevels.toString(),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(verticalGap))
                }

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
                            homeMaxPoints = devHomeMaxPoints,
                            onHomeMaxPointsChange = onDevHomeMaxPointsChange,
                        )
                    }
                }

                // Room to scroll the last button clear of the info bubble.
                Spacer(Modifier.height(56.dp))
            }
        }

        var showInfo by remember { mutableStateOf(false) }
        InfoBubble(
            onClick = { showInfo = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
        if (showInfo) {
            InfoDialog(onDismiss = { showInfo = false })
        }
    }
}

/** Small, quiet "?" in the corner; opens [InfoDialog]. */
@Composable
private fun InfoBubble(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .border(1.5.dp, AppTextMuted.copy(alpha = 0.4f), CircleShape)
            .clickable(onClickLabel = s.t("about_title"), onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppTextMuted.copy(alpha = 0.6f),
        )
    }
}

private const val REPOSITORY_URL = "https://github.com/PrivateLizard/Simple-ArrowGame"

/** What the app is: on-device and offline notes, the project note and the source link. */
@Composable
private fun InfoDialog(onDismiss: () -> Unit) {
    val s = LocalStrings.current
    val uriHandler = LocalUriHandler.current
    ArrowDialog(
        title = s.t("about_title"),
        buttonText = s.t("close"),
        onDismiss = onDismiss,
    ) {
        // First line of the old Home footer plus the offline note that used to
        // sit under "Reset progress" in Settings (it repeats the second line).
        Text(
            text = s.t("footer").substringBefore('\n') + s.wordGap + s.t("settings_footer"),
            style = MaterialTheme.typography.bodyMedium,
            color = AppText,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = s.t("about_text"),
            style = MaterialTheme.typography.bodyMedium,
            color = AppText,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = s.t("about_source"),
            style = MaterialTheme.typography.labelMedium,
            color = AppTextMuted,
        )
        Text(
            text = REPOSITORY_URL.removePrefix("https://"),
            modifier = Modifier
                .clickable { runCatching { uriHandler.openUri(REPOSITORY_URL) } }
                .padding(vertical = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = AppPrimary,
            textDecoration = TextDecoration.Underline,
        )
    }
}

/** Title arrow, in units of the font size (Roboto capitals are 0.711 high). */
private const val CAP_HEIGHT = 0.711f
private const val TITLE_LINE = 0.06f
private const val TITLE_GAP = 0.2f

/**
 * Two-line title wrapped by one game arrow: it starts above "SIMPLE" on the
 * left, runs right over it, turns down behind it, runs left between the two
 * lines, turns down in front of "ARROW", underlines "ARROW PUZZLE" and ends
 * with its head behind "PUZZLE". 90° bends like the arrows on the board.
 *
 * Compose has no auto-size text, so the font size comes from the second line
 * measured at a fixed size, scaled to fill the width left beside the arrow.
 */
@Composable
private fun TitleSpanningWidth(modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val baseStyle = MaterialTheme.typography.headlineLarge.copy(
        fontWeight = FontWeight.Black,
        letterSpacing = 0.sp,
    )
    val firstLine = s.t("title_simple").trim()
    val secondLine = s.t("title_arrow").trim()
    val baseSize = 40.sp
    val measured = measurer.measure(
        text = secondLine,
        style = baseStyle.copy(fontSize = baseSize),
        maxLines = 1,
        softWrap = false,
    )
    val arrowColor = AppBoardLine
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val basePx = with(density) { baseSize.toPx() }
        val textPerFont = measured.size.width / basePx
        // Room for the arrow, in units of the font size: left of the text the
        // vertical line, right of it the gap and the head.
        val headLength = arrowHeadLength(TITLE_LINE)
        val leftRoom = TITLE_GAP + TITLE_LINE
        val rightRoom = TITLE_GAP + headLength + TITLE_LINE
        val fontPx = (widthPx / (textPerFont + leftRoom + rightRoom)).coerceAtLeast(1f)
        val fontSize = with(density) { fontPx.toSp() }
        val style = baseStyle.copy(
            fontSize = fontSize,
            // Enough space between the lines for the arrow to run through.
            // Caps (0.711) plus the gap above and below the arrow line.
            lineHeight = fontSize * (CAP_HEIGHT + 2 * TITLE_GAP + TITLE_LINE + 0.01f),
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
        val padStart = with(density) { (fontPx * leftRoom).toDp() }
        val padTop = with(density) { (fontPx * (TITLE_GAP + TITLE_LINE)).toDp() }
        val padBottom = with(density) {
            (fontPx * (TITLE_GAP + arrowHeadHalfWidth(TITLE_LINE))).toDp()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val text = layout ?: return@drawBehind
                    if (text.lineCount < 2) return@drawBehind
                    val x0 = padStart.toPx()
                    val y0 = padTop.toPx()
                    val line = fontPx * TITLE_LINE
                    val gap = fontPx * TITLE_GAP
                    val cap = fontPx * CAP_HEIGHT
                    val base0 = y0 + text.firstBaseline
                    val base1 = y0 + text.lastBaseline
                    val left = x0 + text.getLineLeft(0)
                    val right0 = x0 + text.getLineRight(0)
                    val left1 = x0 + text.getLineLeft(1)
                    val right1 = x0 + text.getLineRight(1)
                    val over = base0 - cap - gap
                    val between = (base0 + base1 - cap) / 2f
                    val under = base1 + gap
                    drawPolylineArrow(
                        points = listOf(
                            Offset(left, over),
                            Offset(right0 + gap, over),
                            Offset(right0 + gap, between),
                            Offset(left1 - gap, between),
                            Offset(left1 - gap, under),
                            Offset(right1 + gap + arrowHeadLength(line), under),
                        ),
                        line = line,
                        color = arrowColor,
                    )
                },
        ) {
            Text(
                text = title,
                style = style,
                maxLines = 2,
                softWrap = false,
                onTextLayout = { layout = it },
                modifier = Modifier.padding(start = padStart, top = padTop, bottom = padBottom),
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
        // Shrinks for large totals (up to seven digits plus separators)
        // instead of being cut off.
        OneLineFitText(
            text = value,
            style = MaterialTheme.typography.headlineLarge,
            color = AppPrimary,
        )
    }
}

/** One centred line; the font only shrinks when the text would not fit. */
@Composable
private fun OneLineFitText(
    text: String,
    style: TextStyle,
    color: Color = Color.Unspecified,
) {
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
                // Not every style sets a letter spacing; scaling an unset
                // one throws.
                letterSpacing = if (style.letterSpacing.isSpecified) {
                    style.letterSpacing * scale
                } else {
                    style.letterSpacing
                },
            ),
            color = color,
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
    homeMaxPoints: Boolean,
    onHomeMaxPointsChange: (Boolean) -> Unit,
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
            DevSwitchRow(
                title = s.t("dev_win_buttons"),
                checked = winButtons,
                onCheckedChange = onWinButtonsChange,
            )
            Spacer(Modifier.height(14.dp))
            DevSwitchRow(
                title = s.t("dev_home_max"),
                checked = homeMaxPoints,
                onCheckedChange = onHomeMaxPointsChange,
            )
        }
    }
}

@Composable
private fun DevSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            color = AppText,
        )
        Spacer(Modifier.width(10.dp))
        ArrowSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
