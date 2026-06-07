package com.enmanuelgil.networkguard.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enmanuelgil.networkguard.ui.theme.*

@Composable
fun SettingsScreen() {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NetBackground),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        item {
            Column(Modifier.fillMaxWidth()) {
                Text("Ajustes", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NetTextPrimary)
                Text("NetworkGuard v1.0.0", fontSize = 12.sp, color = NetTextSecondary)
            }
        }

        // ── Permisos ──────────────────────────────────────────────────────────
        item {
            SettingsSection(
                title = "🔐 Permisos",
                items = listOf(
                    "Estadísticas de red" to "Concede acceso para ver el tráfico por app",
                    "Datos en background" to "NetworkGuard usa < 0.1% de CPU para el monitor",
                    "Sin root" to "Todas las lecturas usan APIs públicas de Android"
                )
            )
        }

        // ── Privacidad ────────────────────────────────────────────────────────
        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NetSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🛡 Privacidad", fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold, color = NetPurpleLight)
                    Text(
                        "NetworkGuard no recopila ni transmite ningún dato personal.\n" +
                        "Toda la información de tráfico se procesa localmente en tu dispositivo.\n" +
                        "No se realizan conexiones a servidores externos.",
                        fontSize = 12.sp, color = NetTextSecondary, lineHeight = 18.sp
                    )
                }
            }
        }

        // ── Acceso a uso de apps ──────────────────────────────────────────────
        item {
            Card(
                onClick = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    } catch (_: Exception) {}
                },
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NetPurple.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.DataUsage, null, tint = NetPurpleLight,
                        modifier = Modifier.size(24.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Acceso a uso de apps", fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold, color = NetTextPrimary)
                        Text("Opcional: mejora la correlación de tráfico por app",
                            fontSize = 11.sp, color = NetTextSecondary)
                    }
                    Icon(Icons.Default.OpenInNew, null, tint = NetTextSecondary,
                        modifier = Modifier.size(18.dp))
                }
            }
        }

        // ── Donaciones ────────────────────────────────────────────────────────
        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NetSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("💜 Apoya el Proyecto", fontSize = 16.sp,
                        fontWeight = FontWeight.Bold, color = NetPurpleLight)
                    Text(
                        "NetworkGuard es gratuito y de código abierto.\n" +
                        "Tu apoyo permite continuar el desarrollo.",
                        fontSize = 12.sp, color = NetTextSecondary,
                        textAlign = TextAlign.Center, lineHeight = 18.sp
                    )

                    // Binance Pay
                    DonationRow(
                        label   = "Binance Pay ID",
                        value   = "1140153333",
                        accent  = NetCyan,
                        context = context
                    )

                    // BSC BEP20
                    DonationRow(
                        label   = "BSC BEP20",
                        value   = "0x0a9a0d8d816ede885d1d4a5c94369a72ef86b3c1",
                        accent  = NetPurple,
                        context = context
                    )
                }
            }
        }

        // ── Acerca de ─────────────────────────────────────────────────────────
        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NetCard),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Acerca de NetworkGuard", fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold, color = NetTextPrimary)
                    AboutRow("Versión", "1.0.0")
                    AboutRow("Autor", "Enmanuel Gil")
                    AboutRow("Licencia", "MIT — Código abierto")
                    AboutRow("Android mínimo", "8.0 (API 26)")
                    AboutRow("Root requerido", "No")
                    AboutRow("Permisos especiales", "Ninguno obligatorio")
                }
            }
        }

        // ── Espacio para navegación ───────────────────────────────────────────
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun SettingsSection(title: String, items: List<Pair<String, String>>) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NetSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NetPurpleLight)
            items.forEach { (key, value) ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(key, fontSize = 13.sp, color = NetTextPrimary,
                        modifier = Modifier.weight(0.4f))
                    Text(value, fontSize = 12.sp, color = NetTextSecondary,
                        modifier = Modifier.weight(0.6f))
                }
                if (items.last().first != key) {
                    HorizontalDivider(color = NetDivider.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun DonationRow(label: String, value: String, accent: androidx.compose.ui.graphics.Color, context: Context) {
    var copied by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 11.sp, color = NetTextSecondary)
        Card(
            onClick = {
                try {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
                    copied = true
                } catch (_: Exception) {}
            },
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    value,
                    fontSize = 11.sp,
                    color = accent,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (copied) "✓ Copiado" else "Copiar",
                    fontSize = 11.sp,
                    color = if (copied) NetGreen else accent
                )
            }
        }
    }

    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(2000)
            copied = false
        }
    }
}

@Composable
private fun AboutRow(key: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(key, fontSize = 12.sp, color = NetTextSecondary)
        Text(value, fontSize = 12.sp, color = NetTextPrimary, fontWeight = FontWeight.Medium)
    }
}
