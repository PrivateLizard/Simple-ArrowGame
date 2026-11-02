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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowpuzzle.game.BuildConfig
import com.arrowpuzzle.game.data.model.AppSettings
import com.arrowpuzzle.game.i18n.AppLanguage
import com.arrowpuzzle.game.i18n.LocalStrings
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
    onHintsChange: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit,
    onDevModeChange: (Boolean) -> Unit,
    onResetProgress: () -> Unit,
    onBack: () -> Unit,
) {
    var showResetDialog by remember { mutableStateOf(false) }
    var showDevDialog by remember { mutableStateOf(false) }
    val s = LocalStrings.current

    ScreenBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = s.t("settings_title"),
                subtitle = s.t("settings_subtitle"),
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
                                    text = s.t("max_failures"),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = AppText,
                                )
                                Text(
                                    text = if (failuresOff) {
                                        s.t("max_failures_off_desc")
                                    } else {
                                        s.t("max_failures_desc")
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppTextMuted,
                                )
                            }
                            Text(
                                text = if (failuresOff) s.t("off") else settings.maxFailures.toString(),
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
                            title = s.t("night_mode"),
                            checked = settings.nightMode,
                            onCheckedChange = onNightModeChange,
                        )
                        Spacer(Modifier.height(16.dp))
                        ToggleRow(
                            title = s.t("sound"),
                            checked = settings.soundEnabled,
                            onCheckedChange = onSoundChange,
                        )
                        Spacer(Modifier.height(16.dp))
                        ToggleRow(
                            title = s.t("haptics"),
                            checked = settings.hapticEnabled,
                            onCheckedChange = onHapticChange,
                        )
                        Spacer(Modifier.height(16.dp))
                        ToggleRow(
                            title = s.t("hints"),
                            checked = settings.hintsEnabled,
                            onCheckedChange = onHintsChange,
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                SectionCard {
                    Column {
                        Text(
                            text = s.t("language"),
                            style = MaterialTheme.typography.titleMedium,
                            color = AppText,
                        )
                        Text(
                            text = s.t("language_desc"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppTextMuted,
                        )
                        Spacer(Modifier.height(12.dp))
                        LanguageOptions(
                            selected = settings.language,
                            onSelect = onLanguageChange,
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
                        text = s.t("reset_progress"),
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 1.sp,
                    )
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    text = s.t("settings_footer"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTextMuted.copy(alpha = 0.8f),
                )

                Text(
                    text = s.f("build", BuildConfig.VERSION_CODE),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showDevDialog = true }
                        .padding(vertical = 14.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTextMuted.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center,
                )
            }
            }
        }
    }

    if (showDevDialog) {
        AlertDialog(
            onDismissRequest = { showDevDialog = false },
            title = { Text(s.t("dev_mode_title")) },
            text = {
                Text(
                    if (settings.devModeEnabled) {
                        s.t("dev_mode_disable")
                    } else {
                        s.t("dev_mode_enable")
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDevModeChange(!settings.devModeEnabled)
                        showDevDialog = false
                    },
                ) {
                    Text(
                        if (settings.devModeEnabled) s.t("disable") else s.t("enable"),
                        color = AppPrimary,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDevDialog = false }) {
                    Text(s.t("cancel"), color = AppTextMuted)
                }
            },
            containerColor = AppSurfaceRaised,
            titleContentColor = AppText,
            textContentColor = AppTextMuted,
            shape = RoundedCornerShape(20.dp),
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(s.t("reset_title")) },
            text = {
                Text(s.t("reset_text"))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetProgress()
                        showResetDialog = false
                    },
                ) {
                    Text(s.t("reset"), color = AppDanger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(s.t("cancel"), color = AppTextMuted)
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
    val s = LocalStrings.current
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
                        s.t("off")
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguageOptions(
    selected: String,
    onSelect: (String) -> Unit,
) {
    val s = LocalStrings.current
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppLanguage.entries.forEach { language ->
            val isSelected = selected == language.code
            val shape = RoundedCornerShape(12.dp)
            val label = if (language == AppLanguage.SYSTEM) {
                s.t("language_system")
            } else {
                language.displayName
            }
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(if (isSelected) AppPrimary else AppSurfaceRaised)
                    .border(1.dp, if (isSelected) AppPrimary else AppGridLine, shape)
                    .clickable { onSelect(language.code) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = label,
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
    description: String? = null,
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
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTextMuted,
                )
            }
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
