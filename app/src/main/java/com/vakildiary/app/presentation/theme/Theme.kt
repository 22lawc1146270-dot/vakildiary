package com.vakildiary.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Custom color palette for VakilDiary
data class VakilDiaryColors(
    val bgPrimary: Color,
    val bgSecondary: Color,
    val bgElevated: Color,
    val bgSurfaceSoft: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accentPrimary: Color,
    val accentSoft: Color,
    val accentSubtle: Color,
    val onAccent: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color,
    val isDark: Boolean
)

val LocalVakilDiaryColors = staticCompositionLocalOf {
    VakilDiaryColors(
        bgPrimary = AppColors.SlateBg,
        bgSecondary = AppColors.SlateSurface,
        bgElevated = AppColors.SlateElevated,
        bgSurfaceSoft = AppColors.SlateSoft,
        textPrimary = AppColors.SlateText,
        textSecondary = AppColors.SlateTextSecondary,
        textTertiary = Color(0xFF7C859A),
        accentPrimary = AccentPack.Indigo.primary,
        accentSoft = AccentPack.Indigo.soft,
        accentSubtle = AccentPack.Indigo.subtle,
        onAccent = AccentPack.Indigo.onAccent,
        success = AppColors.Success,
        warning = AppColors.Warning,
        error = AppColors.Error,
        info = AppColors.Info,
        isDark = true
    )
}

object VakilTheme {
    val colors: VakilDiaryColors
        @Composable
        @ReadOnlyComposable
        get() = LocalVakilDiaryColors.current
    
    val typography = VakilDiaryTypography
    val spacing = Spacing
}

@Composable
fun VakilDiaryTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentPack: AccentPack = AccentPack.Indigo,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    
    val colors = when (themeMode) {
        ThemeMode.SYSTEM -> if (systemInDark) darkSlateColors(accentPack) else lightNordicColors(accentPack)
        ThemeMode.LIGHT_IVORY -> lightIvoryColors(accentPack)
        ThemeMode.LIGHT_NORDIC -> lightNordicColors(accentPack)
        ThemeMode.DARK_SLATE -> darkSlateColors(accentPack)
        ThemeMode.DARK_ONYX -> darkOnyxColors(accentPack)
    }

    val materialColorScheme = if (colors.isDark) {
        darkColorScheme(
            primary = colors.accentPrimary,
            onPrimary = colors.onAccent,
            background = colors.bgPrimary,
            onBackground = colors.textPrimary,
            surface = colors.bgSecondary,
            onSurface = colors.textPrimary,
            error = colors.error,
            onError = colors.onAccent,
            secondary = colors.accentSoft,
            surfaceVariant = colors.bgElevated,
            onSurfaceVariant = colors.textSecondary
        )
    } else {
        lightColorScheme(
            primary = colors.accentPrimary,
            onPrimary = colors.onAccent,
            background = colors.bgPrimary,
            onBackground = colors.textPrimary,
            surface = colors.bgSecondary,
            onSurface = colors.textPrimary,
            error = colors.error,
            onError = Color.White,
            secondary = colors.accentSoft,
            surfaceVariant = colors.bgElevated,
            onSurfaceVariant = colors.textSecondary
        )
    }

    CompositionLocalProvider(LocalVakilDiaryColors provides colors) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = VakilDiaryTypography,
            content = content
        )
    }
}

private fun darkSlateColors(accent: AccentPack) = VakilDiaryColors(
    bgPrimary = AppColors.SlateBg,
    bgSecondary = AppColors.SlateSurface,
    bgElevated = AppColors.SlateElevated,
    bgSurfaceSoft = AppColors.SlateSoft,
    textPrimary = AppColors.SlateText,
    textSecondary = AppColors.SlateTextSecondary,
    textTertiary = Color(0xFF7C859A),
    accentPrimary = accent.primary,
    accentSoft = accent.soft,
    accentSubtle = accent.subtle,
    onAccent = accent.onAccent,
    success = AppColors.Success,
    warning = AppColors.Warning,
    error = AppColors.Error,
    info = AppColors.Info,
    isDark = true
)

private fun darkOnyxColors(accent: AccentPack) = VakilDiaryColors(
    bgPrimary = AppColors.OnyxBg,
    bgSecondary = AppColors.OnyxSurface,
    bgElevated = AppColors.OnyxElevated,
    bgSurfaceSoft = AppColors.OnyxSoft,
    textPrimary = AppColors.OnyxText,
    textSecondary = AppColors.OnyxTextSecondary,
    textTertiary = Color(0xFF666666),
    accentPrimary = accent.primary,
    accentSoft = accent.soft,
    accentSubtle = accent.subtle,
    onAccent = accent.onAccent,
    success = AppColors.Success,
    warning = AppColors.Warning,
    error = AppColors.Error,
    info = AppColors.Info,
    isDark = true
)

private fun lightIvoryColors(accent: AccentPack) = VakilDiaryColors(
    bgPrimary = AppColors.IvoryBg,
    bgSecondary = AppColors.IvorySurface,
    bgElevated = AppColors.IvoryElevated,
    bgSurfaceSoft = AppColors.IvorySoft,
    textPrimary = AppColors.IvoryText,
    textSecondary = AppColors.IvoryTextSecondary,
    textTertiary = Color(0xFF8E8E93),
    accentPrimary = accent.primary,
    accentSoft = accent.soft,
    accentSubtle = accent.subtle,
    onAccent = accent.onAccent,
    success = AppColors.Success,
    warning = AppColors.Warning,
    error = AppColors.Error,
    info = AppColors.Info,
    isDark = false
)

private fun lightNordicColors(accent: AccentPack) = VakilDiaryColors(
    bgPrimary = AppColors.NordicBg,
    bgSecondary = AppColors.NordicSurface,
    bgElevated = AppColors.NordicElevated,
    bgSurfaceSoft = AppColors.NordicSoft,
    textPrimary = AppColors.NordicText,
    textSecondary = AppColors.NordicTextSecondary,
    textTertiary = Color(0xFF94A3B8),
    accentPrimary = accent.primary,
    accentSoft = accent.soft,
    accentSubtle = accent.subtle,
    onAccent = accent.onAccent,
    success = AppColors.Success,
    warning = AppColors.Warning,
    error = AppColors.Error,
    info = AppColors.Info,
    isDark = false
)
