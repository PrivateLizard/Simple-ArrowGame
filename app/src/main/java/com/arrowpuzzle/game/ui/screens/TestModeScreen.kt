package com.arrowpuzzle.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.ConcurrentHashMap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowpuzzle.game.data.model.LevelConfig
import com.arrowpuzzle.game.game.shapes.LevelShape
import com.arrowpuzzle.game.game.shapes.ShapeMask
import com.arrowpuzzle.game.game.shapes.ShapeSize
import com.arrowpuzzle.game.i18n.LocalStrings
import com.arrowpuzzle.game.ui.components.AppTopBar
import com.arrowpuzzle.game.ui.components.ScreenBackground
import com.arrowpuzzle.game.ui.components.SectionCard
import com.arrowpuzzle.game.ui.theme.AppGridLine
import com.arrowpuzzle.game.ui.theme.AppPrimary
import com.arrowpuzzle.game.ui.theme.AppSurface
import com.arrowpuzzle.game.ui.theme.AppSurfaceRaised
import com.arrowpuzzle.game.ui.theme.AppText
import com.arrowpuzzle.game.ui.theme.AppTextMuted

/**
 * Test mode: play a board that is cut into a shape (heart, triangle, teddy).
 * Experimental and separate from the campaign; every start is a new random
 * board, like Custom Mode.
 */
@Composable
fun TestModeScreen(
    maxFailures: Int,
    onBack: () -> Unit,
    onStart: (LevelConfig) -> Unit,
) {
    val s = LocalStrings.current
    var shapeId by rememberSaveable { mutableStateOf(LevelShape.HEART.id) }
    var sizeName by rememberSaveable { mutableStateOf(ShapeSize.MEDIUM.name) }
    val shape = LevelShape.fromId(shapeId) ?: LevelShape.HEART
    val size = ShapeSize.valueOf(sizeName)

    ScreenBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = s.t("test_title"),
                subtitle = s.t("test_subtitle"),
                onBack = onBack,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            ) {
                SectionCard {
                    Column {
                        Text(
                            text = s.t("board_size"),
                            style = MaterialTheme.typography.titleMedium,
                            color = AppText,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ShapeSize.entries.forEach { option ->
                                val selected = option == size
                                FilterChip(
                                    selected = selected,
                                    onClick = { sizeName = option.name },
                                    label = {
                                        Text(
                                            text = s.t("size_${option.name.lowercase()}"),
                                            fontSize = 12.sp,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center,
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

                Spacer(Modifier.height(14.dp))

                SectionCard {
                    Column {
                        Text(
                            text = s.t("shape"),
                            style = MaterialTheme.typography.titleMedium,
                            color = AppText,
                        )
                        Spacer(Modifier.height(12.dp))
                        LevelShape.entries.chunked(SHAPE_COLUMNS).forEachIndexed { rowIndex, row ->
                            if (rowIndex > 0) Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                row.forEach { option ->
                                    ShapeTile(
                                        shape = option,
                                        selected = option == shape,
                                        label = s.t("shape_${option.id}"),
                                        onClick = { shapeId = option.id },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                repeat(SHAPE_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
            Button(
                onClick = { onStart(ShapeMask.levelConfig(shape, size, maxFailures)) },
                modifier = Modifier
                    .padding(horizontal = 18.dp, vertical = 12.dp)
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
        }
    }
}

private const val SHAPE_COLUMNS = 4

/**
 * Finished preview pictures. A mask has thousands of cells, far too many to draw
 * one by one for a hundred tiles on every frame, so each shape is painted once
 * (white, tinted when drawn) and then reused.
 */
private object ShapePreviews {
    private const val GRID = 48
    private val images = ConcurrentHashMap<Pair<LevelShape, Int>, ImageBitmap>()

    fun image(shape: LevelShape, sizePx: Int): ImageBitmap =
        images.getOrPut(shape to sizePx) {
            val mask = ShapeMask.mask(shape, GRID)
            val image = ImageBitmap(sizePx, sizePx)
            val canvas = androidx.compose.ui.graphics.Canvas(image)
            val paint = Paint().apply { color = Color.White }
            val cell = sizePx.toFloat() / GRID
            val gap = cell * 0.12f
            for (y in 0 until GRID) {
                for (x in 0 until GRID) {
                    if (!mask[y * GRID + x]) continue
                    canvas.drawRect(
                        left = x * cell + gap / 2,
                        top = y * cell + gap / 2,
                        right = (x + 1) * cell - gap / 2,
                        bottom = (y + 1) * cell - gap / 2,
                        paint = paint,
                    )
                }
            }
            image
        }
}

/** Selectable tile with a small preview of the shape mask. */
@Composable
private fun ShapeTile(
    shape: LevelShape,
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    var previewPx by remember { mutableIntStateOf(0) }
    val preview by produceState<ImageBitmap?>(null, shape, previewPx) {
        if (previewPx > 0) {
            value = withContext(Dispatchers.Default) { ShapePreviews.image(shape, previewPx) }
        }
    }
    val tileShape = RoundedCornerShape(16.dp)
    val fill = if (selected) AppPrimary else AppTextMuted.copy(alpha = 0.55f)
    Column(
        modifier = modifier
            .clip(tileShape)
            .background(if (selected) AppPrimary.copy(alpha = 0.10f) else AppSurface)
            .border(1.dp, if (selected) AppPrimary.copy(alpha = 0.7f) else AppGridLine, tileShape)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .onSizeChanged { previewPx = it.width }
                .drawBehind {
                    preview?.let { image ->
                        drawImage(image, colorFilter = ColorFilter.tint(fill))
                    }
                },
        )
        Spacer(Modifier.height(6.dp))
        // Long names (Schmetterling, Bonhomme de neige) break at syllables of
        // the app language instead of in the middle of a word.
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.sp,
                hyphens = Hyphens.Auto,
                lineBreak = LineBreak.Paragraph,
                localeList = LocaleList(Locale(strings.locale.toLanguageTag())),
            ),
            color = if (selected) AppPrimary else AppText,
            maxLines = 2,
            minLines = 2,
            textAlign = TextAlign.Center,
        )
    }
}
