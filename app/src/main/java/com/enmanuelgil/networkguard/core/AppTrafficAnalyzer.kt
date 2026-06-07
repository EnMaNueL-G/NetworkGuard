package com.enmanuelgil.networkguard.core

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.TrafficStats
import com.enmanuelgil.networkguard.model.AppTrafficInfo

/**
 * AppTrafficAnalyzer — analiza tráfico de red por app usando TrafficStats.
 *
 * TrafficStats.getUidRxBytes(uid) lee /proc/net/xt_qtaguid por UID sin root.
 * En algunos dispositivos (vivo, Qualcomm) puede retornar UNSUPPORTED (-1).
 * Manejamos ese caso con un flag de soporte y fallback.
 */
object AppTrafficAnalyzer {

    private val prevRxByUid    = mutableMapOf<Int, Long>()
    private val prevTxByUid    = mutableMapOf<Int, Long>()
    private var prevSnapshotMs = 0L

    // Flag: true si el dispositivo soporta per-UID stats
    var isPerUidSupported = true
        private set

    fun getTopTrafficApps(context: Context, topN: Int = 25): List<AppTrafficInfo> {
        val pm     = context.packageManager
        val ownPkg = context.packageName
        val now    = System.currentTimeMillis()
        val dtSec  = if (prevSnapshotMs > 0) (now - prevSnapshotMs) / 1000.0 else 1.0

        val apps = try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (_: Exception) { return emptyList() }

        val result = mutableListOf<AppTrafficInfo>()

        // Test inicial: verificar soporte con UID 0 (kernel)
        if (prevSnapshotMs == 0L) {
            val testRx = try { TrafficStats.getUidRxBytes(1000) } catch (_: Exception) { TrafficStats.UNSUPPORTED.toLong() }
            isPerUidSupported = testRx != TrafficStats.UNSUPPORTED.toLong()
        }

        for (app in apps) {
            if (app.packageName == ownPkg) continue

            val uid     = app.uid
            val rxBytes = safeUidTraffic { TrafficStats.getUidRxBytes(uid) }
            val txBytes = safeUidTraffic { TrafficStats.getUidTxBytes(uid) }

            // Incluir apps aunque tengan 0 en primera lectura si el dispositivo soporta TrafficStats
            // (el 0 puede ser legítimo — nunca usó red)
            // Solo skip si ambos son 0 Y ya ha pasado más de un ciclo de polling
            if (rxBytes == 0L && txBytes == 0L && prevSnapshotMs > 0) continue

            val prevRx  = prevRxByUid[uid] ?: rxBytes
            val prevTx  = prevTxByUid[uid] ?: txBytes
            val rxSpeed = ((rxBytes - prevRx).coerceAtLeast(0L) / dtSec).toLong()
            val txSpeed = ((txBytes - prevTx).coerceAtLeast(0L) / dtSec).toLong()

            val appName  = try { pm.getApplicationLabel(app).toString() } catch (_: Exception) { app.packageName }
            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isActive = rxSpeed > 0L || txSpeed > 0L

            result.add(AppTrafficInfo(
                packageName = app.packageName,
                appName     = appName,
                rxBytes     = rxBytes,
                txBytes     = txBytes,
                rxSpeedBps  = rxSpeed,
                txSpeedBps  = txSpeed,
                isSystemApp = isSystem,
                isActive    = isActive
            ))

            prevRxByUid[uid] = rxBytes
            prevTxByUid[uid] = txBytes
        }

        prevSnapshotMs = now

        return result
            .filter { it.totalBytes > 0 || prevSnapshotMs == now }
            .sortedByDescending { it.totalBytes }
            .take(topN)
    }

    /**
     * Optimización de red: libera apps con alto consumo en background.
     * Retorna el número de apps afectadas.
     */
    fun optimizeNetworkUsage(context: Context, topConsumers: List<AppTrafficInfo>): Int {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return 0
        val ownPkg = context.packageName
        var killed = 0

        // Matar background de las apps con más tráfico que NO están en foreground
        for (app in topConsumers.take(5)) {
            if (app.packageName == ownPkg) continue
            try {
                am.killBackgroundProcesses(app.packageName)
                killed++
            } catch (_: Exception) {}
        }

        // Forzar GC
        try { System.gc(); Runtime.getRuntime().gc() } catch (_: Exception) {}

        return killed
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
