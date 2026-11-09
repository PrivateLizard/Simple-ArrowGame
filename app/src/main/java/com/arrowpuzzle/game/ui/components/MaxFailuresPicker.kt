package com.arrowpuzzle.game.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arrowpuzzle.game.data.model.AppSettings
import com.arrowpuzzle.game.i18n.LocalStrings
import com.arrowpuzzle.game.ui.theme.AppText

/**
 * "Max Failures" block: title and the fixed option chips ([AppSettings.FAILURE_OPTIONS], including OFF). Shared by the
 * Settings screen and the Custom Mode screen so both look identical.
 */
@Composable
fun MaxFailuresPicker(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    val s = LocalStrings.current
    Column {
        // The selected chip shows the value; no extra description needed.
        Text(
            text = s.t("max_failures"),
            style = MaterialTheme.typography.titleMedium,
            color = AppText,
        )
        Spacer(Modifier.height(12.dp))
        FailureOptions(selected = selected, onSelect = onSelect)
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
            ArrowChip(
                text = if (option == AppSettings.NO_FAILURE_LIMIT) {
                    s.t("off")
                } else {
                    option.toString()
                },
                selected = selected == option,
                onClick = { onSelect(option) },
            )
        }
    }
}
