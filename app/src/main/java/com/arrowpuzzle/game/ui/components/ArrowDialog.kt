package com.arrowpuzzle.game.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.arrowpuzzle.game.i18n.LocalStrings
import com.arrowpuzzle.game.ui.theme.AppPrimary
import com.arrowpuzzle.game.ui.theme.AppSurface

/**
 * Dialog in the look of the app: an arrow-framed card with a green headline
 * and one arrow button that closes it.
 */
@Composable
fun ArrowDialog(
    title: String,
    buttonText: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val s = LocalStrings.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .arrowFrame(
                    color = AppPrimary,
                    seed = rememberFrameSeed(),
                    fill = AppSurface,
                    lineWidth = 2.5.dp,
                )
                .padding(arrowFrameInset(2.5.dp) + 20.dp),
        ) {
            Text(
                text = title.uppercase(s.locale),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                color = AppPrimary,
            )
            Spacer(Modifier.height(12.dp))
            content()
            Spacer(Modifier.height(20.dp))
            ArrowButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                tint = AppPrimary,
                emphasis = true,
            ) {
                Text(
                    text = buttonText.uppercase(s.locale),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                )
            }
        }
    }
}
