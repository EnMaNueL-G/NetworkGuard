package com.enmanuelgil.networkguard.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enmanuelgil.networkguard.model.AppTrafficInfo
import com.enmanuelgil.networkguard.model.formatBytes
import com.enmanuelgil.networkguard.model.formatSpeed
import com.enmanuelgil.networkguard.ui.theme.*
import com.enmanuelgil.networkguard.viewmodel.OptimizeNetResult

@Composable
fun AppsScreen(
    apps: List<AppTrafficInfo>,
    showSystemApps: Boolean,
    isLoading: Boolean,
    perUidSupported: Boolean,
    isOptimizing: Boolean,
    optimizeResult: OptimizeNetResult?,
    onToggleSystemApps: () -> Unit,
    onOptimize: () -> Unit,
    onDismissResult: () -> Unit
) {
    val filtered = remember(apps, showSystemApps) {
        if (showSystemApps) apps else apps.filter { !it.isSystemApp }
    }
    val maxBytes = remember(filtered) {
        filtered.maxOfOrNull { it.totalBytes }?.coerceAtLeast(1L) ?: 1L
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(NetBackground),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Apps por tráfico", fontSize = 20.sp,
                        fontWeight = FontWeight.Bold, color = NetTextPrimary)
                    Text("${filtered.size} apps con datos registrados",
                        fontSize = 12.sp, color = NetTextSecondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Sistema", fontSize = 11.sp, color = NetTextSecondary)
                    Switch(
                        checked = showSystemApps, onCheckedChange = { onToggleSystemApps() },
                        modifier = Modifier.height(24.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor   = NetPurple,
                            checkedTrackColor   = NetPurple.copy(alpha = 0.3f),
                            uncheckedThumbColor = NetTextSecondary,
                            uncheckedTrackColor = NetCard
                        )
                    )
                }
            }
        }

        // ── Botón de optimización ─────────────────────────────────────────────
        item { OptimizeNetworkButton(isOptimizing, filtered.size, onOptimize) }

        // ── Confirmación de optimización ──────────────────────────────────────
        optimizeResult?.let { result ->
            item {
                LaunchedEffect(result) {
                    kotlinx.coroutines.delay(4000)
                    onDismissResult()
                }
                OptimizeResultBanner(result, onDismissResult)
            }
        }

        // ── Aviso si TrafficStats no soportado en este dispositivo ────────────
        if (!perUidSupported) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NetOrange.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NetOrange.copy(alpha = 0.25f))
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("⚠️", fontSize = 18.sp)
                        Column {
                            Text("Estadísticas por app no disponibles",
                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NetOrange)
                            Text(
                                "Este dispositivo no expone tráfico por UID (común en ROMs vivo/BBK). " +
                                "Las estadísticas globales de red sí están disponibles en el panel Monitor.",
                                fontSize = 12.sp, color = NetTextSecondary, lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }

        // ── Loading ───────────────────────────────────────────────────────────
        if (isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NetPurple, modifier = Modifier.size(36.dp))
                }
            }
            return@LazyColumn
        }

        // ── Sin datos ─────────────────────────────────────────────────────────
        if (filtered.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("📡", fontSize = 42.sp)
                        Text("Sin actividad de red registrada", fontSize = 14.sp, color = NetTextSecondary)
                        Text(
                            "Los datos aparecen cuando las apps usan internet.\n" +
                            "Abre Chrome u otra app y espera 5 segundos.",
                            fontSize = 12.sp, color = NetTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
            return@LazyColumn
        }

        // ── Leyenda ───────────────────────────────────────────────────────────
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("App", fontSize = 11.sp, color = NetTextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text("↓ Recibido", fontSize = 11.sp, color = NetCyan)
                    Text("↑ Enviado", fontSize = 11.sp, color = NetGreen)
                }
            }
        }

        // ── Lista de apps ─────────────────────────────────────────────────────
        itemsIndexed(filtered, key = { _, app -> app.packageName }) { index, app ->
            AppTrafficCard(app = app, rank = index + 1, maxBytes = maxBytes)
        }
    }
}

// ── Botón de optimización de red ──────────────────────────────────────────────
@Composable
fun OptimizeNetworkButton(isOptimizing: Boolean, appCount: Int, onOptimize: () -> Unit) {
    val color = if (isOptimizing) NetOrange else NetPurple
    Card(
        onClick = { if (!isOptimizing) onOptimize() },
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                if (isOptimizing) Icons.Default.HourglassTop else Icons.Default.NetworkCheck,
                null, tint = color, modifier = Modifier.size(28.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    if (isOptimizing) "Optimizando red…" else "Optimizar Red",
                    fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color
                )
                Text(
                    if (isOptimizing) "Deteniendo procesos con alto consumo"
                    else "Detiene background de las ${ if (appCount > 5) "top 5 apps" else "apps" } con más tráfico",
                    fontSize = 12.sp, color = NetTextSecondary
                )
            }
            if (!isOptimizing) {
                Icon(Icons.Default.PlayArrow, null, tint = color.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ── Banner de confirmación ────────────────────────────────────────────────────
@Composable
fun OptimizeResultBanner(result: OptimizeNetResult, onDismiss: () -> Unit) {
    val ok = result.killedApps > 0
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (ok) NetGreen.copy(alpha = 0.10f) else NetCyan.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, (if (ok) NetGreen else NetCyan).copy(alpha = 0.25f))
    ) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                if (ok) Icons.Default.CheckCircle else Icons.Default.Info,
                null,
                tint = if (ok) NetGreen else NetCyan,
                modifier = Modifier.size(26.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    if (ok) "✅ Red optimizada" else "ℹ️ Análisis completado",
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NetTextPrimary
                )
                Text(result.message, fontSize = 12.sp,
                    color = if (ok) NetGreen else NetCyan)
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(22.dp)) {
                Icon(Icons.Default.Close, null, tint = NetTextSecondary, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ── Card por app ─────────────────────────────────────────────────────────────
@Composable
fun AppTrafficCard(app: AppTrafficInfo, rank: Int, maxBytes: Long) {
    val totalFraction = (app.totalBytes.toFloat() / maxBytes).coerceIn(0f, 1f)
    val animFraction by animateFloatAsState(
        targetValue = totalFraction,
        animationSpec = tween(500, easing = EaseOutCubic),
        label = "traffic_${app.packageName}"
    )
    val barColor = trafficShareColor(totalFraction)

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (app.isActive) NetSurface else NetCard
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(shape = RoundedCornerShape(8.dp),
                    color = NetPurple.copy(alpha = if (rank <= 3) 0.25f else 0.1f)) {
                    Text("#$rank", Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = if (rank <= 3) NetPurpleLight else NetTextSecondary)
                }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(app.appName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = NetTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (app.isActive) {
                            Surface(shape = RoundedCornerShape(4.dp),
                                color = NetGreen.copy(alpha = 0.15f)) {
                                Text("ACTIVA", Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NetGreen)
                            }
                        }
                        if (app.isSystemApp) {
                            Surface(shape = RoundedCornerShape(4.dp),
                                color = NetTextSecondary.copy(alpha = 0.1f)) {
                                Text("SYS", Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    fontSize = 9.sp, color = NetTextSecondary)
                            }
                        }
                    }
                    Text(app.packageName, fontSize = 10.sp, color = NetTextSecondary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(app.rxBytes.formatBytes(), fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold, color = NetCyan)
                    Text(app.txBytes.formatBytes(), fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold, color = NetGreen)
                }
            }

            // Barra de progreso proporcional
            Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                .background(NetDivider)) {
                Box(Modifier.fillMaxWidth(animFraction).height(4.dp)
                    .clip(RoundedCornerShape(2.dp)).background(barColor))
            }

            // Velocidades actuales
            if (app.rxSpeedBps > 0L || app.txSpeedBps > 0L) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (app.rxSpeedBps > 0L)
                        Text("↓ ${app.rxSpeedBps.formatSpeed()}", fontSize = 11.sp,
                            color = NetCyan.copy(alpha = 0.8f))
                    if (app.txSpeedBps > 0L)
                        Text("↑ ${app.txSpeedBps.formatSpeed()}", fontSize = 11.sp,
                            color = NetGreen.copy(alpha = 0.8f))
                }
            }
        }
    }
}
