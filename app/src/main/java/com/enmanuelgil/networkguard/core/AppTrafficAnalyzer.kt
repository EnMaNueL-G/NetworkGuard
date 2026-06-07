package com.enmanuelgil.networkguard.core

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.TrafficStats
import com.enmanuelgil.networkguard.model.AppTrafficInfo

/**
 * AppTrafficAnalyzer — analiza el tráfico de red por aplicación usando TrafficStats.
 *
 * TrafficStats.getUidRxBytes(uid) / getUidTxBytes(uid) lee directamente
 * /proc/net/xt_qtaguid por UID sin necesitar root ni permisos especiales.
 *
 * Limitación: los contadores son acumulativos desde el último reinicio del
 * dispositivo, no solo de la sesión actual. Se usan snapshots diferenciales
 * para calcular velocidad instantánea.
 */
object AppTrafficAnalyzer {

    // Snapshots previos por UID para calcular velocidad
    private val prevRxByUid = mutableMapOf<Int, Long>()
    private val prevTxByUid = mutableMapOf<Int, Long>()
    private var prevSnapshotMs = 0L

    fun getTopTrafficApps(context: Context, topN: Int = 20): List<AppTrafficInfo> {
        val pm = context.packageManager
        val ownPkg = context.packageName

        val now = System.currentTimeMillis()
        val dtSec = if (prevSnapshotMs > 0) (now - prevSnapshotMs) / 1000.0 else 1.0

        val apps = try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (_: Exception) { return emptyList() }

        val result = mutableListOf<AppTrafficInfo>()

        for (app in apps) {
            // Excluir la propia app del ranking
            if (app.packageName == ownPkg) continue

            val uid = app.uid
            val rxBytes = safeUidTraffic { TrafficStats.getUidRxBytes(uid) }
            val txBytes = safeUidTraffic { TrafficStats.getUidTxBytes(uid) }

            // Saltar apps sin ningún tráfico registrado
            if (rxBytes == 0L && txBytes == 0L) continue

            // Calcular velocidad por diferencia desde snapshot anterior
            val prevRx = prevRxByUid[uid] ?: rxBytes
            val prevTx = prevTxByUid[uid] ?: txBytes
            val rxSpeed = ((rxBytes - prevRx).coerceAtLeast(0L) / dtSec).toLong()
            val txSpeed = ((txBytes - prevTx).coerceAtLeast(0L) / dtSec).toLong()

            val appName = try {
                pm.getApplicationLabel(app).toString()
            } catch (_: Exception) { app.packageName }

            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isActive = rxSpeed > 0L || txSpeed > 0L

            result.add(
                AppTrafficInfo(
                    packageName  = app.packageName,
                    appName      = appName,
                    rxBytes      = rxBytes,
                    txBytes      = txBytes,
                    rxSpeedBps   = rxSpeed,
                    txSpeedBps   = txSpeed,
                    isSystemApp  = isSystem,
                    isActive     = isActive
                )
            )

            // Actualizar snapshot
            prevRxByUid[uid] = rxBytes
            prevTxByUid[uid] = txBytes
        }

        prevSnapshotMs = now

        return result
            .sortedByDescending { it.totalBytes }
            .take(topN)
    }

    fun resetSnapshots() {
        prevRxByUid.clear()
        prevTxByUid.clear()
        prevSnapshotMs = 0L
    }

    private fun safeUidTraffic(block: () -> Long): Long {
        return try {
            val v = block()
            if (v == TrafficStats.UNSUPPORTED.toLong()) 0L else v.coerceAtLeast(0L)
        } catch (_: Exception) { 0L }
    }
}
