package space.linuxct.pulseloop.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Dark palette (iOS futuristic clone) ───────────────────────────────────────

object DarkPulseColors {
    val background           = Color(0xFF080A0F)
    val secondaryBackground  = Color(0xFF0E1118)
    val card                 = Color(0xFF151A23)
    val cardSoft             = Color(0xFF1B2230)
    val elevated             = Color(0xFF202838)
    val textPrimary          = Color(0xFFF5F7FA)
    val textSecondary        = Color(0xFFAAB3C2)
    val textMuted            = Color(0xFF6F7A8C)
    val accent               = Color(0xFF7C5CFF)
    val accentSoft           = Color(0x297C5CFF)
    val onAccentSoft         = textPrimary
    val success              = Color(0xFF35E0A1)
    val warning              = Color(0xFFFFB86B)
    val danger               = Color(0xFFFF4D6D)
    val info                 = Color(0xFF4DDCFF)
    val borderSubtle         = Color(0x14FFFFFF)
    val borderStrong         = Color(0x29FFFFFF)
    // Metric colours
    val steps                = Color(0xFF35E0A1)
    val heartRate            = Color(0xFFFF4D6D)
    val spo2                 = Color(0xFF4DDCFF)
    val sleep                = Color(0xFF8B7CFF)
    val calories             = Color(0xFFFF8A4C)
    val distance             = Color(0xFF4DA3FF)
    val readiness            = Color(0xFFD6FF65)
    val battery              = Color(0xFFA7F3D0)
    val stress               = Color(0xFFFF8A4C)
    val hrv                  = Color(0xFF9D7CFF)
    val temperature          = Color(0xFF2DD4D8)
    val bloodPressure        = Color(0xFF7C5CFF)
    val bloodSugar           = Color(0xFF35E0A1)
    val fatigue              = Color(0xFFFFB86B)
    // Sleep stage + tooltip
    val sleepDeep            = Color(0xFF3F2DD8)
    val sleepLight           = Color(0xFF7C5CFF)
    val sleepRem             = Color(0xFF2DD4D8)
    val sleepAwake           = Color(0xFFFFB86B)
    val sleepUnknown         = Color(0xFF6F7A8C)
    val sleepBar             = Color(0xFF8B7CFF)
    val tooltipBackground    = Color(0xFF1E2D45)
    val tooltipText          = Color.White
}

// ── Light palette ─────────────────────────────────────────────────────────────

object LightPulseColors {
    val background           = Color(0xFFF5F7FA)
    val secondaryBackground  = Color(0xFFEBEDF2)
    val card                 = Color(0xFFFFFFFF)
    val cardSoft             = Color(0xFFF0F2F7)
    val elevated             = Color(0xFFE4E7EE)
    val textPrimary          = Color(0xFF0D1117)
    val textSecondary        = Color(0xFF4A5568)
    val textMuted            = Color(0xFF8A97A8)
    val accent               = Color(0xFF2563EB)
    val accentSoft           = Color(0x292563EB)
    val onAccentSoft         = textPrimary
    val success              = Color(0xFF059669)
    val warning              = Color(0xFFD97706)
    val danger               = Color(0xFFDC2626)
    val info                 = Color(0xFF0284C7)
    val borderSubtle         = Color(0x1A000000)
    val borderStrong         = Color(0x33000000)
    val steps                = Color(0xFF059669)
    val heartRate            = Color(0xFFDC2626)
    val spo2                 = Color(0xFF0284C7)
    val sleep                = Color(0xFF6D28D9)
    val calories             = Color(0xFFEA580C)
    val distance             = Color(0xFF2563EB)
    val readiness            = Color(0xFF65A30D)
    val battery              = Color(0xFF10B981)
    val stress               = Color(0xFFEA580C)
    val hrv                  = Color(0xFF7C3AED)
    val temperature          = Color(0xFF0891B2)
    val bloodPressure        = Color(0xFF6D28D9)
    val bloodSugar           = Color(0xFF059669)
    val fatigue              = Color(0xFFD97706)
    // Sleep stage + tooltip
    val sleepDeep            = Color(0xFF3B2FC5)
    val sleepLight           = Color(0xFF6D28D9)
    val sleepRem             = Color(0xFF0891B2)
    val sleepAwake           = Color(0xFFD97706)
    val sleepUnknown         = Color(0xFF8A97A8)
    val sleepBar             = Color(0xFF6D28D9)
    val tooltipBackground    = Color(0xFF1E293B)
    val tooltipText          = Color.White
}

// ── Semantic colour set surfaced to composables ───────────────────────────────

data class PulseColors(
    val background: Color,
    val secondaryBackground: Color,
    val card: Color,
    val cardSoft: Color,
    val elevated: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccentSoft: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,
    val borderSubtle: Color,
    val borderStrong: Color,
    val steps: Color,
    val heartRate: Color,
    val spo2: Color,
    val sleep: Color,
    val calories: Color,
    val distance: Color,
    val readiness: Color,
    val battery: Color,
    val stress: Color,
    val hrv: Color,
    val temperature: Color,
    val bloodPressure: Color,
    val bloodSugar: Color,
    val fatigue: Color,
    val sleepDeep: Color,
    val sleepLight: Color,
    val sleepRem: Color,
    val sleepAwake: Color,
    val sleepUnknown: Color,
    val sleepBar: Color,
    val tooltipBackground: Color,
    val tooltipText: Color
)

val darkPulseColors = PulseColors(
    background          = DarkPulseColors.background,
    secondaryBackground = DarkPulseColors.secondaryBackground,
    card                = DarkPulseColors.card,
    cardSoft            = DarkPulseColors.cardSoft,
    elevated            = DarkPulseColors.elevated,
    textPrimary         = DarkPulseColors.textPrimary,
    textSecondary       = DarkPulseColors.textSecondary,
    textMuted           = DarkPulseColors.textMuted,
    accent              = DarkPulseColors.accent,
    accentSoft          = DarkPulseColors.accentSoft,
    onAccentSoft        = DarkPulseColors.onAccentSoft,
    success             = DarkPulseColors.success,
    warning             = DarkPulseColors.warning,
    danger              = DarkPulseColors.danger,
    info                = DarkPulseColors.info,
    borderSubtle        = DarkPulseColors.borderSubtle,
    borderStrong        = DarkPulseColors.borderStrong,
    steps               = DarkPulseColors.steps,
    heartRate           = DarkPulseColors.heartRate,
    spo2                = DarkPulseColors.spo2,
    sleep               = DarkPulseColors.sleep,
    calories            = DarkPulseColors.calories,
    distance            = DarkPulseColors.distance,
    readiness           = DarkPulseColors.readiness,
    battery             = DarkPulseColors.battery,
    stress              = DarkPulseColors.stress,
    hrv                 = DarkPulseColors.hrv,
    temperature         = DarkPulseColors.temperature,
    bloodPressure       = DarkPulseColors.bloodPressure,
    bloodSugar          = DarkPulseColors.bloodSugar,
    fatigue             = DarkPulseColors.fatigue,
    sleepDeep           = DarkPulseColors.sleepDeep,
    sleepLight          = DarkPulseColors.sleepLight,
    sleepRem            = DarkPulseColors.sleepRem,
    sleepAwake          = DarkPulseColors.sleepAwake,
    sleepUnknown        = DarkPulseColors.sleepUnknown,
    sleepBar            = DarkPulseColors.sleepBar,
    tooltipBackground   = DarkPulseColors.tooltipBackground,
    tooltipText         = DarkPulseColors.tooltipText
)

val lightPulseColors = PulseColors(
    background          = LightPulseColors.background,
    secondaryBackground = LightPulseColors.secondaryBackground,
    card                = LightPulseColors.card,
    cardSoft            = LightPulseColors.cardSoft,
    elevated            = LightPulseColors.elevated,
    textPrimary         = LightPulseColors.textPrimary,
    textSecondary       = LightPulseColors.textSecondary,
    textMuted           = LightPulseColors.textMuted,
    accent              = LightPulseColors.accent,
    accentSoft          = LightPulseColors.accentSoft,
    onAccentSoft        = LightPulseColors.onAccentSoft,
    success             = LightPulseColors.success,
    warning             = LightPulseColors.warning,
    danger              = LightPulseColors.danger,
    info                = LightPulseColors.info,
    borderSubtle        = LightPulseColors.borderSubtle,
    borderStrong        = LightPulseColors.borderStrong,
    steps               = LightPulseColors.steps,
    heartRate           = LightPulseColors.heartRate,
    spo2                = LightPulseColors.spo2,
    sleep               = LightPulseColors.sleep,
    calories            = LightPulseColors.calories,
    distance            = LightPulseColors.distance,
    readiness           = LightPulseColors.readiness,
    battery             = LightPulseColors.battery,
    stress              = LightPulseColors.stress,
    hrv                 = LightPulseColors.hrv,
    temperature         = LightPulseColors.temperature,
    bloodPressure       = LightPulseColors.bloodPressure,
    bloodSugar          = LightPulseColors.bloodSugar,
    fatigue             = LightPulseColors.fatigue,
    sleepDeep           = LightPulseColors.sleepDeep,
    sleepLight          = LightPulseColors.sleepLight,
    sleepRem            = LightPulseColors.sleepRem,
    sleepAwake          = LightPulseColors.sleepAwake,
    sleepUnknown        = LightPulseColors.sleepUnknown,
    sleepBar            = LightPulseColors.sleepBar,
    tooltipBackground   = LightPulseColors.tooltipBackground,
    tooltipText         = LightPulseColors.tooltipText
)

val LocalPulseColors = staticCompositionLocalOf { darkPulseColors }

// ── Material3 colour schemes ──────────────────────────────────────────────────

private val darkM3 = darkColorScheme(
    primary            = DarkPulseColors.accent,
    onPrimary          = Color.White,
    primaryContainer   = DarkPulseColors.accentSoft,
    onPrimaryContainer = DarkPulseColors.textPrimary,
    secondary          = DarkPulseColors.textSecondary,
    onSecondary        = DarkPulseColors.background,
    // Without these the raw-M3 roles fall back to the M3 baseline (purple-grey) palette, which
    // clashes with the V1 navy. Map them to PulseColors so any plain Material3 component (e.g. the
    // OTLP/Data-export screen's chips and fields) is on-brand in LEGACY. V2 uses the dynamic scheme,
    // so it is unaffected by these.
    secondaryContainer      = DarkPulseColors.elevated,
    onSecondaryContainer    = DarkPulseColors.textPrimary,
    tertiary                = DarkPulseColors.info,
    onTertiary              = Color.White,
    tertiaryContainer       = DarkPulseColors.elevated,
    onTertiaryContainer     = DarkPulseColors.textPrimary,
    background         = DarkPulseColors.background,
    onBackground       = DarkPulseColors.textPrimary,
    surface            = DarkPulseColors.card,
    onSurface          = DarkPulseColors.textPrimary,
    surfaceVariant     = DarkPulseColors.elevated,
    onSurfaceVariant   = DarkPulseColors.textSecondary,
    surfaceContainerLowest  = DarkPulseColors.background,
    surfaceContainerLow     = DarkPulseColors.secondaryBackground,
    surfaceContainer        = DarkPulseColors.card,
    surfaceContainerHigh    = DarkPulseColors.elevated,
    surfaceContainerHighest = DarkPulseColors.elevated,
    surfaceBright           = DarkPulseColors.elevated,
    surfaceDim              = DarkPulseColors.background,
    surfaceTint             = DarkPulseColors.accent,
    inverseSurface          = DarkPulseColors.textPrimary,
    inverseOnSurface        = DarkPulseColors.background,
    inversePrimary          = DarkPulseColors.accent,
    outline            = DarkPulseColors.borderStrong,
    outlineVariant     = DarkPulseColors.borderSubtle,
    error              = DarkPulseColors.danger,
    onError            = Color.White,
    errorContainer     = DarkPulseColors.elevated,
    onErrorContainer   = DarkPulseColors.danger,
    scrim              = Color.Black
)

private val lightM3 = lightColorScheme(
    primary            = LightPulseColors.accent,
    onPrimary          = Color.White,
    primaryContainer   = LightPulseColors.accentSoft,
    onPrimaryContainer = LightPulseColors.textPrimary,
    secondary          = LightPulseColors.textSecondary,
    onSecondary        = LightPulseColors.background,
    secondaryContainer      = LightPulseColors.elevated,
    onSecondaryContainer    = LightPulseColors.textPrimary,
    tertiary                = LightPulseColors.info,
    onTertiary              = Color.White,
    tertiaryContainer       = LightPulseColors.elevated,
    onTertiaryContainer     = LightPulseColors.textPrimary,
    background         = LightPulseColors.background,
    onBackground       = LightPulseColors.textPrimary,
    surface            = LightPulseColors.card,
    onSurface          = LightPulseColors.textPrimary,
    surfaceVariant     = LightPulseColors.elevated,
    onSurfaceVariant   = LightPulseColors.textSecondary,
    surfaceContainerLowest  = LightPulseColors.background,
    surfaceContainerLow     = LightPulseColors.secondaryBackground,
    surfaceContainer        = LightPulseColors.card,
    surfaceContainerHigh    = LightPulseColors.elevated,
    surfaceContainerHighest = LightPulseColors.elevated,
    surfaceBright           = LightPulseColors.card,
    surfaceDim              = LightPulseColors.secondaryBackground,
    surfaceTint             = LightPulseColors.accent,
    inverseSurface          = LightPulseColors.textPrimary,
    inverseOnSurface        = LightPulseColors.background,
    inversePrimary          = LightPulseColors.accent,
    outline            = LightPulseColors.borderStrong,
    outlineVariant     = LightPulseColors.borderSubtle,
    error              = LightPulseColors.danger,
    onError            = Color.White,
    errorContainer     = LightPulseColors.elevated,
    onErrorContainer   = LightPulseColors.danger,
    scrim              = Color.Black
)

// ── Typography ────────────────────────────────────────────────────────────────

private val pulseTypography = Typography(
    headlineLarge  = TextStyle(fontWeight = FontWeight.Bold,   fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold,   fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall  = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge     = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium    = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall     = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge      = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium     = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall      = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge     = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium    = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall     = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp)
)

// ── Root theme composable ─────────────────────────────────────────────────────

@Composable
fun PulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val pulseColors = if (darkTheme) darkPulseColors else lightPulseColors
    val m3Colors    = if (darkTheme) darkM3 else lightM3

    CompositionLocalProvider(LocalPulseColors provides pulseColors) {
        MaterialTheme(
            colorScheme = m3Colors,
            typography  = pulseTypography,
            content     = content
        )
    }
}

// ── Material You color mapping ────────────────────────────────────────────────

private fun materialYouPulseColors(scheme: ColorScheme, darkTheme: Boolean): PulseColors {
    val base = if (darkTheme) darkPulseColors else lightPulseColors
    return base.copy(
        background          = scheme.background,
        secondaryBackground = scheme.surfaceContainerLowest,
        card                = scheme.surfaceContainerLow,
        cardSoft            = scheme.surfaceContainerLowest,
        elevated            = scheme.surfaceContainer,
        textPrimary         = scheme.onSurface,
        textSecondary       = scheme.onSurfaceVariant,
        textMuted           = scheme.outline,
        accent              = scheme.primary,
        accentSoft          = scheme.primaryContainer,
        onAccentSoft        = scheme.onPrimaryContainer,
        success             = scheme.tertiary,
        danger              = scheme.error,
        borderSubtle        = Color.Transparent,
        borderStrong        = scheme.outlineVariant,
        heartRate           = scheme.error,
        spo2                = scheme.tertiary,
        steps               = scheme.primary,
        calories            = scheme.secondary,
        sleep               = scheme.secondary,
        warning             = scheme.tertiary,
        info                = scheme.secondary,
        distance            = scheme.primary,
        readiness           = scheme.tertiary,
        battery             = scheme.tertiary,
        stress              = scheme.error,
        hrv                 = scheme.secondary,
        temperature         = scheme.tertiary,
        bloodPressure       = scheme.secondary,
        bloodSugar          = scheme.tertiary,
        fatigue             = scheme.error,
        sleepDeep           = scheme.primary,
        sleepLight          = scheme.secondary,
        sleepRem            = scheme.tertiary,
        sleepAwake          = scheme.error,
        sleepUnknown        = scheme.outline,
        sleepBar            = scheme.primary,
        tooltipBackground   = scheme.inverseSurface,
        tooltipText         = scheme.inverseOnSurface
    )
}

// ── AppTheme — root composable used by MainActivity ──────────────────────────

@Composable
fun AppTheme(
    useMaterialYou: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    if (!useMaterialYou) {
        CompositionLocalProvider(LocalUiMode provides UiMode.LEGACY) {
            PulseTheme(darkTheme = darkTheme, content = content)
        }
        return
    }

    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme  -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !darkTheme -> dynamicLightColorScheme(context)
        darkTheme  -> darkColorScheme(primary = Color(0xFF5B8DEF))
        else       -> lightColorScheme(primary = Color(0xFF5B8DEF))
    }
    val pulseColors = materialYouPulseColors(scheme, darkTheme)

    CompositionLocalProvider(
        LocalPulseColors provides pulseColors,
        LocalUiMode provides UiMode.MATERIAL_YOU
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography  = pulseTypography,
            content     = content
        )
    }
}
