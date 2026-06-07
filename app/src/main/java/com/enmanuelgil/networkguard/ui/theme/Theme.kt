package com.enmanuelgil.networkguard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Paleta NetworkGuard: temática ciberseguridad / red ───────────────────────
// Completamente diferente de BatteryGuard (verde) y StorageCleaner (azul/teal)

val NetPurple      = Color(0xFF9D4EDD)   // Púrpura principal — identidad
val NetPurpleLight = Color(0xFFC77DFF)   // Púrpura claro — secundario / iconos
val NetCyan        = Color(0xFF00D4FF)   // Cyan eléctrico — descarga (RX)
val NetGreen       = Color(0xFF00FF87)   // Verde néon — upload seguro (TX)
val NetOrange      = Color(0xFFFF6B35)   // Naranja — alerta / alto consumo
val NetRed         = Color(0xFFFF3366)   // Rojo néon — crítico / peligro

// Fondo ultra oscuro — estética hacker/matrix
val NetBackground  = Color(0xFF07080F)   // Casi negro azulado
val NetSurface     = Color(0xFF0E1020)   // Fondo de cards principal
val NetCard        = Color(0xFF151829)   // Cards secundarias
val NetCardAlt     = Color(0xFF1A1E30)   // Cards expandibles
val NetDivider     = Color(0xFF2A2D45)   // Líneas divisoras

val NetTextPrimary   = Color(0xFFE8E8FF)  // Blanco ligeramente azulado
val NetTextSecondary = Color(0xFF8888BB)  // Gris/lavanda — texto secundario
val NetTextAccent    = Color(0xFFBBAAFF)  // Lavanda — énfasis

private val DarkColors = darkColorScheme(
    primary      = NetPurple,
    secondary    = NetCyan,
    tertiary     = NetGreen,
    background   = NetBackground,
    surface      = NetSurface,
    onBackground = NetTextPrimary,
    onSurface    = NetTextPrimary,
    onPrimary    = Color.White,
    error        = NetRed
)

@Composable
fun NetworkGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}

/** Color dinámico según velocidad relativa */
fun speedColor(speedBps: Long): Color = when {
    speedBps <= 0L              -> NetTextSecondary
    speedBps < 50 * 1024L      -> NetGreen           // < 50 KB/s — normal
    speedBps < 500 * 1024L     -> NetCyan            // < 500 KB/s — activo
    speedBps < 2 * 1024 * 1024L -> NetOrange         // < 2 MB/s — alto
    else                        -> NetRed            // ≥ 2 MB/s — crítico
}

/** Color para porcentaje de uso relativo */
fun trafficShareColor(fraction: Float): Color = when {
    fraction < 0.20f -> NetGreen
    fraction < 0.50f -> NetCyan
    fraction < 0.80f -> NetOrange
    else             -> NetRed
}
