package com.enmanuelgil.networkguard.ui.screens

import androidx.compose.animation.core.*
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

@Composable
fun AppsScreen(
    apps: List<AppTrafficInfo>,
    showSystemApps: Boolean,
    onToggleSystemApps: () -> Unit,
    isLoading: Boolean
) {
    val filtered = remember(apps, showSystemApps) {
        if (showSystemApps) apps else apps.filter { !it.isSystemApp }
    }

    val maxBytes = remember(filtered) {
        filtered.maxOfOrNull { it.totalBytes }?.coerceAtLeast(1L) ?: 1L
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NetBackground),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Apps por tráfico", fontSize = 20.sp,
                        fontWeight = FontWeight.Bold, color = NetTextPrimary)
                    Text(
                        "${filtered.size} aplicaciones con datos",
                        fontSize = 12.sp, color = NetTextSecondary
                    )
                }
                // Toggle sistema
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Sistema", fontSize = 11.sp, color = NetTextSecondary)
                    Switch(
                        checked = showSystemApps,
                        onCheckedChange = { onToggleSystemApps() },
                        modifier = Modifier.height(24.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NetPurple,
                            checkedTrackColor = NetPurple.copy(alpha = 0.3f),
                            uncheckedThumbColor = NetTextSecondary,
                            uncheckedTrackColor = NetCard
                        )
                    )
                }
            }
        }

        // ── Loading ───────────────────────────────────────────────────────────
        if (isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NetPurple, modifier = Modifier.size(36.dp))
                }
            }
            return@LazyColumn
        }

        // ── Sin datos ─────────────────────────────────────────────────────────
        if (filtered.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("📡", fontSize = 40.sp)
                        Text("Sin tráfico detectado aún", fontSize = 14.sp, color = NetTextSecondary)
                        Text("Los datos aparecen cuando hay actividad de red",
                            fontSize = 12.sp, color = NetTextSecondary)
                    }
                }
            }
            return@LazyColumn
        }

        // ── Leyenda de columnas ───────────────────────────────────────────────
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("App", fontSize = 11.sp, color = NetTextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Text("↓ RX", fontSize = 11.sp, color = NetCyan)
                    Text("↑ TX", fontSize = 11.sp, color = NetGreen)
                }
            }
        }

        // ── Lista de apps ─────────────────────────────────────────────────────
        itemsIndexed(filtered, key = { _, app -> app.packageName }) { index, app ->
            AppTrafficCard(app = app, rank = index + 1, maxBytes = maxBytes)
        }
    }
}

@Composable
fun AppTrafficCard(app: AppTrafficInfo, rank: Int, maxBytes: Long) {
    val totalFraction = (app.totalBytes.toFloat() / maxBytes).coerceIn(0f, 1f)

    // Animación de barra — con label explícito para evitar bugs de SlotTable
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
            // Fila principal: rank + nombre + velocidades
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Número de ranking
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NetPurple.copy(alpha = if (rank <= 3) 0.25f else 0.1f)
                ) {
                    Text(
                        "#$rank",
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (rank <= 3) NetPurpleLight else NetTextSecondary
                    )
                }

                // Nombre y package
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            app.appName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NetTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (app.isActive) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = NetGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "ACTIVA",
                                    Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NetGreen
                                )
                            }
                        }
                        if (app.isSystemApp) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = NetTextSecondary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    "SYS",
                                    Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    fontSize = 9.sp,
                                    color = NetTextSecondary
                                )
                            }
                        }
                    }
                    Text(
                        app.packageName,
                        fontSize = 10.sp,
                        color = NetTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Totales RX / TX
                Column(horizontalAlignment = Alignment.End) {
                    Text(app.rxBytes.formatBytes(), fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold, color = NetCyan)
                    Text(app.txBytes.formatBytes(), fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold, color = NetGreen)
                }
            }

            // Barra de progreso proporcional
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(NetDivider)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(animFraction)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(barColor)
                )
            }

            // Velocidades actuales (si hay actividad)
            if (app.rxSpeedBps > 0L || app.txSpeedBps > 0L) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (app.rxSpeedBps > 0L) {
                        Text(
                            "↓ ${app.rxSpeedBps.formatSpeed()}",
                            fontSize = 11.sp,
                            color = NetCyan.copy(alpha = 0.8f)
                        )
                    }
                    if (app.txSpeedBps > 0L) {
                        Text(
                            "↑ ${app.txSpeedBps.formatSpeed()}",
                            fontSize = 11.sp,
                            color = NetGreen.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}
