package com.arrowpuzzle.game.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arrowpuzzle.game.data.model.AppSettings
import com.arrowpuzzle.game.i18n.LocalStrings
import com.arrowpuzzle.game.ui.theme.AppGridLine
import com.arrowpuzzle.game.ui.theme.AppPrimary
import com.arrowpuzzle.game.ui.theme.AppSurfaceRaised
import com.arrowpuzzle.game.ui.theme.AppText
import com.arrowpuzzle.game.ui.theme.AppTextMuted

/**
 * "Max Failures" block with title, description, current value and the fixed
 * option chips ([AppSettings.FAILURE_OPTIONS], including OFF). Shared by the
 * Settings screen and the Custom Mode screen so both look identical.
 */
@Composable
fun MaxFailuresPicker(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    val s = LocalStrings.current
    val failuresOff = selected == AppSettings.NO_FAILURE_LIMIT
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
                text = if (failuresOff) s.t("off") else selected.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = AppPrimary,
                fontWeight = FontWeight.Black,
            )
        }
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
