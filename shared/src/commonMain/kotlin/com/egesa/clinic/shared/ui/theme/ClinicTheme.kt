package com.egesa.clinic.shared.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Primitive palette ──────────────────────────────────────────────────────────
val Navy950 = Color(0xFF061622)
val Navy900 = Color(0xFF0A2F4E)
val Navy800 = Color(0xFF0F4C75)
val Navy700 = Color(0xFF1A6598)
val Navy600 = Color(0xFF2075B0)
val Navy200 = Color(0xFF9DC4DF)
val Navy100 = Color(0xFFDDE8F5)
val Navy50  = Color(0xFFEEF4FB)

val Teal700 = Color(0xFF0F766E)
val Teal600 = Color(0xFF0D9488)
val Teal500 = Color(0xFF14B8A6)
val Teal100 = Color(0xFFCCFBF1)

// ── Semantic status ────────────────────────────────────────────────────────────
val StatusCritical = Color(0xFFEF4444)
val StatusWarning  = Color(0xFFF59E0B)
val StatusStable   = Color(0xFF10B981)
val StatusInfo     = Color(0xFF3B82F6)
val StatusMuted    = Color(0xFFCBD5E1)

// ── Neutrals ───────────────────────────────────────────────────────────────────
val White   = Color(0xFFFFFFFF)
val Slate50  = Color(0xFFF8FAFC)
val Slate100 = Color(0xFFF1F5F9)
val Slate200 = Color(0xFFE2E8F0)
val Slate300 = Color(0xFFCBD5E1)
val Slate400 = Color(0xFF94A3B8)
val Slate500 = Color(0xFF64748B)
val Slate600 = Color(0xFF475569)
val Slate700 = Color(0xFF334155)
val Slate800 = Color(0xFF1E293B)
val Slate900 = Color(0xFF0F172A)

// ── Sidebar accent ─────────────────────────────────────────────────────────────
val SidebarBg      = Navy900
val SidebarHover   = Color(0xFF0E3A5F)
val SidebarActive  = Color(0xFF1A3A5C)
val SidebarBorder  = Color(0xFF143352)

private val ColorScheme = lightColorScheme(
    primary            = Navy800,
    onPrimary          = White,
    primaryContainer   = Navy100,
    onPrimaryContainer = Navy900,
    secondary          = Teal600,
    onSecondary        = White,
    secondaryContainer = Teal100,
    onSecondaryContainer = Color(0xFF003330),
    background         = Slate50,
    onBackground       = Slate900,
    surface            = White,
    onSurface          = Slate900,
    surfaceVariant     = Slate100,
    onSurfaceVariant   = Slate500,
    outline            = Slate200,
    outlineVariant     = Slate100,
    error              = StatusCritical,
    onError            = White,
    errorContainer     = Color(0xFFFEE2E2),
    onErrorContainer   = Color(0xFF7F1D1D),
)

val ClinicTypography = Typography(
    displayLarge  = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold,     letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold,     letterSpacing = (-0.3).sp),
    displaySmall  = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    headlineLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium= TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleLarge    = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    titleMedium   = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    titleSmall    = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium,   letterSpacing = 0.1.sp),
    bodyLarge     = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal),
    bodyMedium    = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodySmall     = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    labelLarge    = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium,   letterSpacing = 0.1.sp),
    labelMedium   = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium,   letterSpacing = 0.4.sp),
    labelSmall    = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium,   letterSpacing = 0.5.sp),
)

val ClinicShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small      = RoundedCornerShape(6.dp),
    medium     = RoundedCornerShape(8.dp),
    large      = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

@Composable
fun ClinicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography  = ClinicTypography,
        shapes      = ClinicShapes,
        content     = content,
    )
}
