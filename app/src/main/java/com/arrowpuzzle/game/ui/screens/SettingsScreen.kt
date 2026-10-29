package com.arrowpuzzle.game.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowpuzzle.game.data.model.AppSettings
import com.arrowpuzzle.game.ui.components.AppTopBar
import com.arrowpuzzle.game.ui.components.ScreenBackground
import com.arrowpuzzle.game.ui.components.SectionCard
import com.arrowpuzzle.game.ui.theme.AppDanger
import com.arrowpuzzle.game.ui.theme.AppGridLine
import com.arrowpuzzle.game.ui.theme.AppPrimary
import com.arrowpuzzle.game.ui.theme.AppSurfaceRaised
import com.arrowpuzzle.game.ui.theme.AppText
import com.arrowpuzzle.game.ui.theme.AppTextMuted

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onMaxFailuresChange: (Int) -> Unit,
    onNightModeChange: (Boolean) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onHapticChange: (Boolean) -> Unit,
    onResetProgress: () -> Unit,
    onBack: () -> Unit,
) {
    var showResetDialog by remember { mutableStateOf(false) }

    ScreenBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = "Settings",
                subtitle = "Stored only on this device",
                onBack = onBack,
            )
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
            ) {
                val failuresOff = settings.maxFailures == AppSettings.NO_FAILURE_LIMIT
                SectionCard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Max Failures",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = AppText,
                                )
                                Text(
                                    text = if (failuresOff) {
                                        "Failures are off. Wrong taps never end a level."
                                    } else {
                                        "A level ends after this many wrong taps."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppTextMuted,
                                )
                            }
                            Text(
                                text = if (failuresOff) "AUS" else settings.maxFailures.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = AppPrimary,
                                fontWeight = FontWeight.Black,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        FailureOptions(
                            selected = settings.maxFailures,
                            onSelect = onMaxFailuresChange,
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                SectionCard {
                    Column {
                        ToggleRow(
                            title = "Night Mode",
                            description = "Use the dark color palette. Colors are theme-controlled.",
                            checked = settings.nightMode,
                            onCheckedChange = onNightModeChange,
                        )
                        Spacer(Modifier.height(16.dp))
                        ToggleRow(
                            title = "Sound Effects",
                            description = "Short tones for taps, wins and losses.",
                            checked = settings.soundEnabled,
                            onCheckedChange = onSoundChange,
                        )
                        Spacer(Modifier.height(16.dp))
                        ToggleRow(
                            title = "Haptic Feedback",
                            description = "Vibration feedback on supported devices.",
                            checked = settings.hapticEnabled,
                            onCheckedChange = onHapticChange,
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))

                OutlinedButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, AppDanger.copy(alpha = 0.8f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppDanger),
                ) {
                    Text(
                        text = "RESET PROGRESS",
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 1.sp,
                    )
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    text = "Arrow Puzzle works completely offline. The app has no internet permission, no ads and no analytics.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTextMuted.copy(alpha = 0.8f),
                )
            }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Progress") },
            text = {
                Text(
                    "This clears all high scores and resets level progression. Settings are kept. This cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetProgress()
                        showResetDialog = false
                    },
                ) {
                    Text("Reset", color = AppDanger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = AppTextMuted)
                }
            },
            containerColor = AppSurfaceRaised,
            titleContentColor = AppText,
            textContentColor = AppTextMuted,
            shape = RoundedCornerShape(20.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FailureOptions(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppSettings.FAILURE_OPTIONS.forEach { option ->
            val isSelected = selected == option
            val shape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(if (isSelected) AppPrimary else AppSurfaceRaised)
                    .border(1.dp, if (isSelected) AppPrimary else AppGridLine, shape)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = if (option == AppSettings.NO_FAILURE_LIMIT) {
                        "AUS"
                    } else {
                        option.toString()
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else AppText,
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = AppText,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = AppTextMuted,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AppSurfaceRaised,
                checkedTrackColor = AppPrimary,
                uncheckedThumbColor = AppTextMuted,
                uncheckedTrackColor = AppGridLine,
                uncheckedBorderColor = AppGridLine,
            ),
        )
    }
}
