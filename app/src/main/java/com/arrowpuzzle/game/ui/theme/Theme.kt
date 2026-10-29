package com.arrowpuzzle.game.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * All visual colors live in these palettes. Changing a palette updates the
 * complete app without touching individual screens.
 */
@Immutable
data class ArrowPalette(
    val background: Color,
    val backgroundDeep: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val gridLine: Color,
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val accent: Color,
    val danger: Color,
    val text: Color,
    val textMuted: Color,
    val successDim: Color,
    val boardBackground: Color,
    val boardLine: Color,
    val boardLineSelected: Color,
    val boardLineWrong: Color,
    val boardStart: Color,
    val boardGoal: Color,
)

val DarkArrowPalette = ArrowPalette(
    background = Color(0xFF0B1020),
    backgroundDeep = Color(0xFF070B16),
    surface = Color(0xFF161E33),
    surfaceRaised = Color(0xFF1E2944),
    gridLine = Color(0xFF2A3656),
    primary = Color(0xFF39D98A),
    onPrimary = Color(0xFF062619),
    secondary = Color(0xFF4CC9F0),
    accent = Color(0xFFF4A261),
    danger = Color(0xFFEF476F),
    text = Color(0xFFF4F7FF),
    textMuted = Color(0xFF8C9BB8),
    successDim = Color(0xFF1C4B3A),
    boardBackground = Color(0xFF0D1527),
    boardLine = Color(0xFFF1F4FA),
    boardLineSelected = Color(0xFF39D98A),
    boardLineWrong = Color(0xFFEF476F),
    boardStart = Color(0xFF4CC9F0),
    boardGoal = Color(0xFFF4A261),
)

val LightArrowPalette = ArrowPalette(
    background = Color(0xFFF5F2EA),
    backgroundDeep = Color(0xFFE8E2D6),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFF1ECE1),
    gridLine = Color(0xFFD5CEBF),
    primary = Color(0xFF1D3557),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF2A6F97),
    accent = Color(0xFFB5651D),
    danger = Color(0xFFB3261E),
    text = Color(0xFF141414),
    textMuted = Color(0xFF6A675F),
    successDim = Color(0xFFDCEBE2),
    boardBackground = Color(0xFFFBFAF6),
    boardLine = Color(0xFF111111),
    boardLineSelected = Color(0xFF2A7F62),
    boardLineWrong = Color(0xFFB3261E),
    boardStart = Color(0xFF2A7F62),
    boardGoal = Color(0xFFB5651D),
)

val LocalArrowPalette = staticCompositionLocalOf { DarkArrowPalette }

object ArrowTheme {
    val colors: ArrowPalette
        @Composable get() = LocalArrowPalette.current
}

val AppBackground: Color
    @Composable get() = ArrowTheme.colors.background
val AppBackgroundDeep: Color
    @Composable get() = ArrowTheme.colors.backgroundDeep
val AppSurface: Color
    @Composable get() = ArrowTheme.colors.surface
val AppSurfaceRaised: Color
    @Composable get() = ArrowTheme.colors.surfaceRaised
val AppGridLine: Color
    @Composable get() = ArrowTheme.colors.gridLine
val AppPrimary: Color
    @Composable get() = ArrowTheme.colors.primary
val AppSecondary: Color
    @Composable get() = ArrowTheme.colors.secondary
val AppAccent: Color
    @Composable get() = ArrowTheme.colors.accent
val AppDanger: Color
    @Composable get() = ArrowTheme.colors.danger
val AppText: Color
    @Composable get() = ArrowTheme.colors.text
val AppTextMuted: Color
    @Composable get() = ArrowTheme.colors.textMuted
val AppSuccessDim: Color
    @Composable get() = ArrowTheme.colors.successDim
val AppBoardBackground: Color
    @Composable get() = ArrowTheme.colors.boardBackground
val AppBoardLine: Color
    @Composable get() = ArrowTheme.colors.boardLine
val AppBoardLineSelected: Color
    @Composable get() = ArrowTheme.colors.boardLineSelected
val AppBoardLineWrong: Color
    @Composable get() = ArrowTheme.colors.boardLineWrong
val AppBoardStart: Color
    @Composable get() = ArrowTheme.colors.boardStart
val AppBoardGoal: Color
    @Composable get() = ArrowTheme.colors.boardGoal

private val ArrowTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 58.sp,
        lineHeight = 56.sp,
        letterSpacing = (-2).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 42.sp,
        lineHeight = 44.sp,
        letterSpacing = (-1).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        letterSpacing = 0.4.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
    ),
)

@Composable
fun ArrowPuzzleTheme(
    nightMode: Boolean = true,
    content: @Composable () -> Unit,
) {
    val palette = if (nightMode) DarkArrowPalette else LightArrowPalette
    val colorScheme = if (nightMode) {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.successDim,
            onPrimaryContainer = palette.primary,
            secondary = palette.secondary,
            onSecondary = palette.backgroundDeep,
            secondaryContainer = palette.surfaceRaised,
            onSecondaryContainer = palette.secondary,
            tertiary = palette.accent,
            onTertiary = palette.backgroundDeep,
            background = palette.background,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.surfaceRaised,
            onSurfaceVariant = palette.textMuted,
            outline = palette.gridLine,
            error = palette.danger,
            onError = Color.White,
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.successDim,
            onPrimaryContainer = palette.text,
            secondary = palette.secondary,
            onSecondary = Color.White,
            secondaryContainer = palette.surfaceRaised,
            onSecondaryContainer = palette.secondary,
            tertiary = palette.accent,
            onTertiary = Color.White,
            background = palette.background,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.surfaceRaised,
            onSurfaceVariant = palette.textMuted,
            outline = palette.gridLine,
            error = palette.danger,
            onError = Color.White,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = palette.background.toArgb()
            window.navigationBarColor = palette.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !nightMode
                isAppearanceLightNavigationBars = !nightMode
            }
        }
    }

    CompositionLocalProvider(LocalArrowPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ArrowTypography,
            content = content,
        )
    }
}
