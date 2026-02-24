package com.vakildiary.app.presentation.theme

import androidx.compose.ui.graphics.Color

// Base Neutral Tokens
object AppColors {
    // 1. Slate (Pro Dark - Default)
    val SlateBg = Color(0xFF0F1115)
    val SlateSurface = Color(0xFF151922)
    val SlateElevated = Color(0xFF1B2130)
    val SlateSoft = Color(0xFF20283A)
    val SlateText = Color(0xFFE6EAF2)
    val SlateTextSecondary = Color(0xFFAAB2C5)

    // 2. Onyx (Pure Dark)
    val OnyxBg = Color(0xFF000000)
    val OnyxSurface = Color(0xFF0A0A0A)
    val OnyxElevated = Color(0xFF121212)
    val OnyxSoft = Color(0xFF1A1A1A)
    val OnyxText = Color(0xFFFFFFFF)
    val OnyxTextSecondary = Color(0xFF999999)

    // 3. Ivory (Warm Light)
    val IvoryBg = Color(0xFFFBFBF9)
    val IvorySurface = Color(0xFFFFFFFF)
    val IvoryElevated = Color(0xFFF2F2EE)
    val IvorySoft = Color(0xFFEAEAE3)
    val IvoryText = Color(0xFF1A1C1E)
    val IvoryTextSecondary = Color(0xFF5D5E61)

    // 4. Nordic (Cool Light)
    val NordicBg = Color(0xFFF4F7FA)
    val NordicSurface = Color(0xFFFFFFFF)
    val NordicElevated = Color(0xFFE8EEF5)
    val NordicSoft = Color(0xFFDAE2ED)
    val NordicText = Color(0xFF1E293B)
    val NordicTextSecondary = Color(0xFF64748B)

    // Utility Colors (Shared)
    val Success = Color(0xFF22C55E)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFEF4444)
    val Info = Color(0xFF3B82F6)
    val White = Color(0xFFFFFFFF)
}

// Accent Packs
sealed class AccentPack(
    val primary: Color,
    val soft: Color,
    val subtle: Color,
    val onAccent: Color
) {
    object Indigo : AccentPack(
        primary = Color(0xFF5B6CFF),
        soft = Color(0x335B6CFF),
        subtle = Color(0xFF1F2748),
        onAccent = Color(0xFFFFFFFF)
    )

    object Burgundy : AccentPack(
        primary = Color(0xFFB91C1C),
        soft = Color(0x33B91C1C),
        subtle = Color(0xFF2A1414),
        onAccent = Color(0xFFFFFFFF)
    )

    object Emerald : AccentPack(
        primary = Color(0xFF10B981),
        soft = Color(0x3310B981),
        subtle = Color(0xFF061B14),
        onAccent = Color(0xFFFFFFFF)
    )

    object Gold : AccentPack(
        primary = Color(0xFFD4AF37),
        soft = Color(0x33D4AF37),
        subtle = Color(0xFF1F1B0B),
        onAccent = Color(0xFFFFFFFF)
    )
}
