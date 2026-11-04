package com.arrowpuzzle.game.ui.screens

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.arrowpuzzle.game.ui.components.MaxFailuresPicker
import com.arrowpuzzle.game.ui.components.ScreenBackground
import com.arrowpuzzle.game.ui.components.SectionCard
import com.arrowpuzzle.game.ui.theme.AppGridLine
import com.arrowpuzzle.game.ui.theme.AppPrimary
import com.arrowpuzzle.game.ui.theme.AppSurface
import com.arrowpuzzle.game.ui.theme.AppSurfaceRaised
import com.arrowpuzzle.game.ui.theme.AppText
import com.arrowpuzzle.game.ui.theme.AppTextMuted
import kotlin.math.roundToInt

@Composable
fun CustomModeScreen(
    onBack: () -> Unit,
    onStart: (LevelConfig) -> Unit,
) {
    var gridSize by rememberSaveable { mutableIntStateOf(5) }
    var arrowCount by rememberSaveable { mutableIntStateOf(12) }
    var maxFailures by rememberSaveable { mutableIntStateOf(AppSettings.DEFAULT_MAX_FAILURES) }
    var timeLimitEnabled by rememberSaveable { mutableStateOf(false) }
    var timeLimitSeconds by rememberSaveable { mutableIntStateOf(60) }
    var complexityName by rememberSaveable { mutableStateOf(ComplexityLevel.MEDIUM.name) }

    val complexity = ComplexityLevel.valueOf(complexityName)
    val maxArrows = 40
    val safeArrowCount = arrowCount.coerceIn(6, maxArrows)

    LaunchedEffect(maxArrows) {
        if (arrowCount > maxArrows) arrowCount = maxArrows
        if (arrowCount < 6) arrowCount = 6
    }

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
                        SliderHeader(
                            label = s.t("grid_size"),
                            value = "${gridSize}x${gridSize}",
                        )
                        Slider(
                            value = gridSize.toFloat(),
                            onValueChange = {
                                val next = it.roundToInt()
                                gridSize = next
                                val cap = 40
                                if (arrowCount > cap) arrowCount = cap
                            },
                            valueRange = 3f..8f,
                            steps = 4,
                            colors = customSliderColors(),
                        )
                        Text(
                            text = s.t("grid_hint"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppTextMuted,
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                SectionCard {
                    Column {
                        SliderHeader(
                            label = s.t("min_arrows"),
                            value = safeArrowCount.toString(),
                        )
                        Slider(
                            value = safeArrowCount.toFloat(),
                            onValueChange = { arrowCount = it.roundToInt() },
                            valueRange = 6f..maxArrows.toFloat(),
                            steps = (maxArrows - 7).coerceAtLeast(0),
                            colors = customSliderColors(),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "6",
                                style = MaterialTheme.typography.labelMedium,
                                color = AppTextMuted,
                            )
                            Text(
                                text = s.t("arrows_count_hint"),
                                style = MaterialTheme.typography.labelMedium,
                                color = AppTextMuted,
                            )
                        }
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = s.t("time_limit"),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = AppText,
                                )
                                Text(
                                    text = if (timeLimitEnabled) {
                                        s.f("seconds", timeLimitSeconds)
                                    } else {
                                        s.t("no_time_limit")
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppTextMuted,
                                )
                            }
                            Switch(
                                checked = timeLimitEnabled,
                                onCheckedChange = { timeLimitEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AppSurfaceRaised,
                                    checkedTrackColor = AppPrimary,
                                    uncheckedThumbColor = AppTextMuted,
                                    uncheckedTrackColor = AppGridLine,
                                ),
                            )
                        }
                        if (timeLimitEnabled) {
                            Spacer(Modifier.height(10.dp))
                            Slider(
                                value = timeLimitSeconds.toFloat(),
                                onValueChange = { timeLimitSeconds = it.roundToInt() },
                                valueRange = 30f..180f,
                                steps = 9,
                                colors = customSliderColors(),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "30s",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AppTextMuted,
                                )
                                Text(
                                    text = "180s",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AppTextMuted,
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
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = when (complexity) {
                                ComplexityLevel.EASY -> s.t("complexity_easy")
                                ComplexityLevel.MEDIUM -> s.t("complexity_medium")
                                ComplexityLevel.HARD -> s.t("complexity_hard")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppTextMuted,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ComplexityLevel.entries.forEach { option ->
                                val selected = option == complexity
                                FilterChip(
                                    selected = selected,
                                    onClick = { complexityName = option.name },
                                    label = {
                                        Text(
                                            text = when (option) {
                                                ComplexityLevel.EASY -> s.t("easy")
                                                ComplexityLevel.MEDIUM -> s.t("medium")
                                                ComplexityLevel.HARD -> s.t("hard")
                                            },
                                            fontSize = 12.sp,
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AppPrimary,
                                        selectedLabelColor = AppSurface,
                                        containerColor = AppSurfaceRaised,
                                        labelColor = AppTextMuted,
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = AppGridLine,
                                        selectedBorderColor = AppPrimary,
                                    ),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                Button(
                    onClick = {
                        onStart(
                            LevelConfig(
                                levelId = LevelConfig.CUSTOM_LEVEL_ID,
                                gridSize = gridSize to gridSize,
                                arrowCount = safeArrowCount,
                                maxFailures = maxFailures,
                                timeLimitSeconds = if (timeLimitEnabled) timeLimitSeconds else null,
                                complexity = complexity,
                            ),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppPrimary,
                        contentColor = AppSurface,
                    ),
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
private fun SliderHeader(label: String, value: String) {
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

@Composable
private fun customSliderColors() = SliderDefaults.colors(
    thumbColor = AppPrimary,
    activeTrackColor = AppPrimary,
    inactiveTrackColor = AppSurfaceRaised,
)
