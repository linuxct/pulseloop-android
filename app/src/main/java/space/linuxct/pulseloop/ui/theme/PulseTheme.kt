package space.linuxct.pulseloop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
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
    val temperature: Color
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
    temperature         = DarkPulseColors.temperature
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
    temperature         = LightPulseColors.temperature
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
    background         = DarkPulseColors.background,
    onBackground       = DarkPulseColors.textPrimary,
    surface            = DarkPulseColors.card,
    onSurface          = DarkPulseColors.textPrimary,
    surfaceVariant     = DarkPulseColors.elevated,
    onSurfaceVariant   = DarkPulseColors.textSecondary,
    outline            = DarkPulseColors.borderStrong,
    outlineVariant     = DarkPulseColors.borderSubtle,
    error              = DarkPulseColors.danger,
    onError            = Color.White
)

private val lightM3 = lightColorScheme(
    primary            = LightPulseColors.accent,
    onPrimary          = Color.White,
    primaryContainer   = LightPulseColors.accentSoft,
    onPrimaryContainer = LightPulseColors.textPrimary,
    secondary          = LightPulseColors.textSecondary,
    onSecondary        = LightPulseColors.background,
    background         = LightPulseColors.background,
    onBackground       = LightPulseColors.textPrimary,
    surface            = LightPulseColors.card,
    onSurface          = LightPulseColors.textPrimary,
    surfaceVariant     = LightPulseColors.elevated,
    onSurfaceVariant   = LightPulseColors.textSecondary,
    outline            = LightPulseColors.borderStrong,
    outlineVariant     = LightPulseColors.borderSubtle,
    error              = LightPulseColors.danger,
    onError            = Color.White
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
