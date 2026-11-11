package com.arrowpuzzle.game.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowpuzzle.game.data.model.AppSettings
import com.arrowpuzzle.game.data.model.ComplexityLevel
import com.arrowpuzzle.game.data.model.LevelConfig
import com.arrowpuzzle.game.i18n.LocalStrings
import com.arrowpuzzle.game.ui.components.AppTopBar
import com.arrowpuzzle.game.ui.components.ArrowDialog
import com.arrowpuzzle.game.ui.components.ArrowButton
import com.arrowpuzzle.game.ui.components.ArrowChip
import com.arrowpuzzle.game.ui.components.ArrowSlider
import com.arrowpuzzle.game.ui.components.MaxFailuresPicker
import com.arrowpuzzle.game.ui.components.ScreenBackground
import com.arrowpuzzle.game.ui.components.SectionCard
import com.arrowpuzzle.game.ui.theme.AppPrimary
import com.arrowpuzzle.game.ui.theme.AppText

private const val MIN_GRID = 3
private const val MAX_GRID = 8
private const val MIN_ARROWS = 6
private const val MAX_ARROWS = 40

/** Time limit choices: seconds per arrow of the board, 0 = no limit. */
private val SECONDS_PER_ARROW_OPTIONS = listOf(0, 3, 5, 10, 20)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomModeScreen(
    onBack: () -> Unit,
    onStart: (LevelConfig) -> Unit,
) {
    var gridSize by rememberSaveable { mutableIntStateOf(5) }
    var arrowCount by rememberSaveable { mutableIntStateOf(12) }
    var maxFailures by rememberSaveable { mutableIntStateOf(AppSettings.DEFAULT_MAX_FAILURES) }
    var secondsPerArrow by rememberSaveable { mutableIntStateOf(0) }
    var complexityName by rememberSaveable { mutableStateOf(ComplexityLevel.MEDIUM.name) }

    val complexity = ComplexityLevel.valueOf(complexityName)
    val safeArrowCount = arrowCount.coerceIn(MIN_ARROWS, MAX_ARROWS)

    val s = LocalStrings.current
    ScreenBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = s.t("custom_title"),
                subtitle = s.t("custom_subtitle"),
                onBack = onBack,
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            ) {
                SectionCard {
                    Column {
                        SettingHeader(
                            label = s.t("grid_size"),
                            value = "${gridSize}x$gridSize",
                        )
                        Spacer(Modifier.height(6.dp))
                        ArrowSlider(
                            value = gridSize,
                            range = MIN_GRID..MAX_GRID,
                            onValueChange = { gridSize = it },
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                SectionCard {
                    Column {
                        SettingHeader(
                            label = s.t("min_arrows"),
                            value = safeArrowCount.toString(),
                        )
                        Spacer(Modifier.height(6.dp))
                        ArrowSlider(
                            value = safeArrowCount,
                            range = MIN_ARROWS..MAX_ARROWS,
                            onValueChange = { arrowCount = it },
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                SectionCard {
                    MaxFailuresPicker(
                        selected = maxFailures,
                        onSelect = { maxFailures = it },
                    )
                }

                Spacer(Modifier.height(14.dp))

                SectionCard {
                    Column {
                        Text(
                            text = s.t("time_per_arrow"),
                            style = MaterialTheme.typography.titleMedium,
                            color = AppText,
                        )
                        Spacer(Modifier.height(12.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SECONDS_PER_ARROW_OPTIONS.forEach { option ->
                                ArrowChip(
                                    text = if (option == 0) {
                                        s.t("off")
                                    } else {
                                        s.f("seconds_short", option)
                                    },
                                    selected = option == secondsPerArrow,
                                    onClick = { secondsPerArrow = option },
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                SectionCard {
                    Column {
                        Text(
                            text = s.t("complexity"),
                            style = MaterialTheme.typography.titleMedium,
                            color = AppText,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ComplexityLevel.entries.forEach { option ->
                                ArrowChip(
                                    text = when (option) {
                                        ComplexityLevel.EASY -> s.t("easy")
                                        ComplexityLevel.MEDIUM -> s.t("medium")
                                        ComplexityLevel.HARD -> s.t("hard")
                                    },
                                    selected = option == complexity,
                                    onClick = { complexityName = option.name },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                ArrowButton(
                    onClick = {
                        onStart(
                            LevelConfig(
                                levelId = LevelConfig.CUSTOM_LEVEL_ID,
                                gridSize = gridSize to gridSize,
                                arrowCount = safeArrowCount,
                                maxFailures = maxFailures,
                                // Worked out from the real board once it exists.
                                timeLimitSeconds = null,
                                complexity = complexity,
                                secondsPerArrow = secondsPerArrow.takeIf { it > 0 },
                            ),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    tint = AppPrimary,
                    emphasis = true,
                    lineWidth = 2.5.dp,
                ) {
                    Text(
                        text = s.t("start_game"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                    )
                }

                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun SettingHeader(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = AppText,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = AppPrimary,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Shown the first time Custom Mode is opened: its boards are generated from
 * the chosen settings and are not hand-tuned like the campaign levels.
 */
@Composable
fun CustomModeWarningDialog(onConfirm: () -> Unit) {
    val s = LocalStrings.current
    // Closing it any way counts as read; it is only shown once.
    ArrowDialog(
        title = s.t("custom_warning_title"),
        buttonText = s.t("understood"),
        onDismiss = onConfirm,
    ) {
        Text(
            text = s.t("custom_warning_text"),
            style = MaterialTheme.typography.bodyMedium,
            color = AppText,
        )
    }
}
