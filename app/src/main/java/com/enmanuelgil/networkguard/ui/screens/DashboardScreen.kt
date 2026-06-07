package com.enmanuelgil.networkguard.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enmanuelgil.networkguard.model.*
import com.enmanuelgil.networkguard.ui.theme.*

@Composable
fun DashboardScreen(
    state: NetworkState,
    alerts: List<NetworkAlert>,
    isLoading: Boolean,
    onClearAlerts: () -> Unit
) {
    // LazyColumn — seguro para recomposición múltiple (lección de StorageCleaner)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NetBackground),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Header ─────────────────────────────────────────────────────────
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "NetworkGuard",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = NetTextPrimary
                    )
                    Text(
                        "Monitor de tráfico de red",
                        fontSize = 12.sp,
                        color = NetTextSecondary
                    )
                }
                NetworkStatusBadge(state)
            }
        }

        // ── Loading ─────────────────────────────────────────────────────────
        if (isLoading) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = NetPurple,
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 3.dp
                        )
                        Text(
                            "Iniciando monitor de red…",
                            fontSize = 14.sp,
                            color = NetTextSecondary
                        )
                    }
                }
            }
            return@LazyColumn
        }

        // ── Velocímetro de red (el elemento visual más distintivo) ──────────
        item { NetworkSpeedGauge(state) }

        // ── Stats rápidas en fila ────────────────────────────────────────────
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NetStatChip(
                    Modifier.weight(1f),
                    "↓ Descarga",
                    state.rxBytesTotal.formatBytes(),
                    NetCyan
                )
                NetStatChip(
                    Modifier.weight(1f),
                    "↑ Subida",
                    state.txBytesTotal.formatBytes(),
                    NetGreen
                )
                NetStatChip(
                    Modifier.weight(1f),
                    "Apps activas",
                    "${state.activeAppsCount}",
                    NetPurpleLight
                )
            }
        }

        // ── Tipo de conexión ─────────────────────────────────────────────────
        item { ConnectionInfoCard(state) }

        // ── Alertas (si las hay) ──────────────────────────────────────────────
        if (alerts.isNotEmpty()) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "⚠ Alertas (${alerts.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NetOrange
                    )
                    TextButton(onClick = onClearAlerts) {
                        Text("Limpiar", fontSize = 12.sp, color = NetTextSecondary)
                    }
                }
            }
            items(alerts.takeLast(5), key = { "${it.packageName}_${it.alertType}_${it.timestampMs}" }) { alert ->
                AlertCard(alert)
            }
        }

        // ── Tips de seguridad ────────────────────────────────────────────────
        item { SecurityTipsCard() }
    }
}

// ── Velocímetro gráfico con arcos animados ────────────────────────────────────
@Composable
fun NetworkSpeedGauge(state: NetworkState) {
    val maxSpeed = 10 * 1024 * 1024L  // 10 MB/s como referencia máxima

    val rxFraction = (state.rxSpeedBps.toFloat() / maxSpeed).coerceIn(0f, 1f)
    val txFraction = (state.txSpeedBps.toFloat() / maxSpeed).coerceIn(0f, 1f)

    // Animaciones seguras con label explícito
    val animRx by animateFloatAsState(
        targetValue = rxFraction,
        animationSpec = tween(600, easing = EaseOutCubic),
        label = "rxArc"
    )
    val animTx by animateFloatAsState(
        targetValue = txFraction,
        animationSpec = tween(600, easing = EaseOutCubic),
        label = "txArc"
    )

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NetSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Velocidad en tiempo real",
                fontSize = 13.sp,
                color = NetTextSecondary,
                fontWeight = FontWeight.Medium
            )

            // Canvas con GUARDIA de tamaño — previene crash en primera medición
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                    // *** GUARDIA OBLIGATORIA — lección de StorageCleaner ***
                    if (size.width <= 0f || size.height <= 0f) return@Canvas

                    val cx       = size.width / 2f
                    val cy       = size.height * 0.85f
                    val radius   = minOf(size.width / 2f, size.height) * 0.78f
                    val stroke   = 18.dp.toPx()
                    val strokeIn = 10.dp.toPx()

                    // Fondo semicircular externo (RX — descarga)
                    drawArc(
                        color = NetCyan.copy(alpha = 0.08f),
                        startAngle = 180f, sweepAngle = 180f,
                        useCenter = false,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                        topLeft = Offset(cx - radius, cy - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                    )
                    // Arco progreso RX
                    val rxSweep = (180f * animRx).coerceAtLeast(if (animRx > 0f) 3f else 0f)
                    if (rxSweep > 0f) {
                        drawArc(
                            color = NetCyan,
                            startAngle = 180f, sweepAngle = rxSweep,
                            useCenter = false,
                            style = Stroke(stroke, cap = StrokeCap.Round),
                            topLeft = Offset(cx - radius, cy - radius),
                            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                        )
                    }

                    val innerR = radius * 0.70f
                    // Fondo semicircular interno (TX — subida)
                    drawArc(
                        color = NetGreen.copy(alpha = 0.08f),
                        startAngle = 180f, sweepAngle = 180f,
                        useCenter = false,
                        style = Stroke(strokeIn, cap = StrokeCap.Round),
                        topLeft = Offset(cx - innerR, cy - innerR),
                        size = androidx.compose.ui.geometry.Size(innerR * 2, innerR * 2)
                    )
                    // Arco progreso TX
                    val txSweep = (180f * animTx).coerceAtLeast(if (animTx > 0f) 3f else 0f)
                    if (txSweep > 0f) {
                        drawArc(
                            color = NetGreen,
                            startAngle = 180f, sweepAngle = txSweep,
                            useCenter = false,
                            style = Stroke(strokeIn, cap = StrokeCap.Round),
                            topLeft = Offset(cx - innerR, cy - innerR),
                            size = androidx.compose.ui.geometry.Size(innerR * 2, innerR * 2)
                        )
                    }
                }

                // Texto central dentro del arco
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 60.dp)
                ) {
                    if (!state.isConnected) {
                        Text("SIN RED", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NetRed)
                    } else {
                        Text(
                            state.rxSpeedBps.formatSpeed(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = NetCyan
                        )
                        Text("↓   ↑", fontSize = 11.sp, color = NetTextSecondary)
                        Text(
                            state.txSpeedBps.formatSpeed(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NetGreen
                        )
                    }
                }
            }

            // Leyenda
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem("● Descarga", NetCyan)
                LegendItem("● Subida", NetGreen)
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Text(label, fontSize = 12.sp, color = color, fontWeight = FontWeight.Medium)
}

// ── Badge de estado de red ────────────────────────────────────────────────────
@Composable
fun NetworkStatusBadge(state: NetworkState) {
    val (bg, fg, text) = when {
        !state.isConnected -> Triple(NetRed.copy(0.15f), NetRed, "Sin red")
        state.networkType.name.startsWith("WIFI") -> Triple(NetCyan.copy(0.15f), NetCyan, "Wi-Fi")
        else -> Triple(NetPurple.copy(0.15f), NetPurple, "Datos")
    }
    Surface(shape = RoundedCornerShape(20.dp), color = bg) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(fg)
            )
            Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = fg)
        }
    }
}

// ── Chip de estadística ────────────────────────────────────────────────────────
@Composable
fun NetStatChip(modifier: Modifier, label: String, value: String, color: Color) {
    Card(
        modifier,
        colors = CardDefaults.cardColors(containerColor = NetCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, fontSize = 10.sp, color = NetTextSecondary)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
        }
    }
}

// ── Card info de conexión ─────────────────────────────────────────────────────
@Composable
fun ConnectionInfoCard(state: NetworkState) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NetCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Conexión activa", fontSize = 13.sp, color = NetTextSecondary, fontWeight = FontWeight.Medium)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.networkType.emoji, fontSize = 20.sp)
                    Column {
                        Text(state.networkType.label, fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold, color = NetTextPrimary)
                        if (state.wifiSsid.isNotEmpty()) {
                            Text(state.wifiSsid, fontSize = 12.sp, color = NetTextSecondary)
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("↓ ${state.rxBytesTotal.formatBytes()}", fontSize = 12.sp, color = NetCyan)
                    Text("↑ ${state.txBytesTotal.formatBytes()}", fontSize = 12.sp, color = NetGreen)
                }
            }
        }
    }
}

// ── Card de alerta ────────────────────────────────────────────────────────────
@Composable
fun AlertCard(alert: NetworkAlert) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NetOrange.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(alert.alertType.emoji, fontSize = 20.sp)
            Column(Modifier.weight(1f)) {
                Text(alert.appName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NetTextPrimary)
                Text(alert.detail, fontSize = 11.sp, color = NetOrange)
            }
        }
    }
}

// ── Tips de seguridad de red ──────────────────────────────────────────────────
@Composable
fun SecurityTipsCard() {
    val tips = listOf(
        "🔒" to "Usa VPN en redes Wi-Fi públicas",
        "📱" to "Limita el acceso a datos en apps poco usadas",
        "🔋" to "Las apps con tráfico en background consumen más batería",
        "🛡" to "Desactiva Wi-Fi y datos cuando no los uses",
        "🔍" to "Revisa apps con subidas inusualmente altas"
    )
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NetSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Consejos de seguridad", fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, color = NetPurpleLight)
            tips.forEach { (emoji, tip) ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top) {
                    Text(emoji, fontSize = 14.sp)
                    Text(tip, fontSize = 12.sp, color = NetTextSecondary)
                }
            }
        }
    }
}
