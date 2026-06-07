package com.enmanuelgil.networkguard.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiManager
import android.telephony.TelephonyManager
import com.enmanuelgil.networkguard.model.NetworkState
import com.enmanuelgil.networkguard.model.NetworkType

/**
 * NetworkMonitor — lee el estado de red del dispositivo de forma segura.
 *
 * Usa únicamente APIs públicas sin root:
 * - ConnectivityManager   → tipo de red activa
 * - WifiManager           → SSID (solo en Android < 10 sin permiso de ubicación)
 * - TrafficStats          → bytes RX/TX totales del dispositivo
 *
 * Las velocidades se calculan externamente comparando snapshots consecutivos.
 */
object NetworkMonitor {

    // Snapshot previo para calcular velocidad
    private var prevRx = 0L
    private var prevTx = 0L
    private var prevTimeMs = 0L
    private var sessionStartRx = -1L
    private var sessionStartTx = -1L

    fun getNetworkState(context: Context): NetworkState {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkState()

        val network = cm.activeNetwork
        val caps    = cm.getNetworkCapabilities(network)

        val isConnected = caps != null
        val networkType = detectNetworkType(context, caps)
        val wifiSsid    = getWifiSsid(context, caps)

        // TrafficStats — leer bytes totales del dispositivo (sin root)
        val currentRx = safeTrafficRead { TrafficStats.getTotalRxBytes() }
        val currentTx = safeTrafficRead { TrafficStats.getTotalTxBytes() }
        val nowMs     = System.currentTimeMillis()

        // Inicializar sesión en primera lectura
        if (sessionStartRx < 0) {
            sessionStartRx = currentRx
            sessionStartTx = currentTx
        }

        // Calcular velocidad basada en delta respecto a lectura anterior
        val (rxSpeed, txSpeed) = if (prevTimeMs > 0 && nowMs > prevTimeMs) {
            val dtSec = (nowMs - prevTimeMs) / 1000.0
            val rxDelta = (currentRx - prevRx).coerceAtLeast(0L)
            val txDelta = (currentTx - prevTx).coerceAtLeast(0L)
            Pair(
                (rxDelta / dtSec).toLong().coerceAtLeast(0L),
                (txDelta / dtSec).toLong().coerceAtLeast(0L)
            )
        } else Pair(0L, 0L)

        // Total de sesión
        val rxSession = (currentRx - sessionStartRx).coerceAtLeast(0L)
        val txSession = (currentTx - sessionStartTx).coerceAtLeast(0L)

        // Guardar snapshot actual para siguiente llamada
        prevRx     = currentRx
        prevTx     = currentTx
        prevTimeMs = nowMs

        return NetworkState(
            isConnected    = isConnected,
            networkType    = networkType,
            wifiSsid       = wifiSsid,
            rxBytesTotal   = rxSession,
            txBytesTotal   = txSession,
            rxSpeedBps     = rxSpeed,
            txSpeedBps     = txSpeed,
            rxBytesToday   = currentRx.coerceAtLeast(0L),
            txBytesToday   = currentTx.coerceAtLeast(0L)
        )
    }

    fun resetSession() {
        sessionStartRx = -1L
        sessionStartTx = -1L
        prevTimeMs     = 0L
    }

    private fun detectNetworkType(context: Context, caps: NetworkCapabilities?): NetworkType {
        if (caps == null) return NetworkType.NONE
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)     -> NetworkType.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> detectMobileGeneration(context)
            else -> NetworkType.OTHER
        }
    }

    private fun detectMobileGeneration(context: Context): NetworkType {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            when (tm?.dataNetworkType) {
                TelephonyManager.NETWORK_TYPE_LTE,
                TelephonyManager.NETWORK_TYPE_NR      -> NetworkType.MOBILE_4G
                TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_HSPAP,
                TelephonyManager.NETWORK_TYPE_UMTS    -> NetworkType.MOBILE_3G
                TelephonyManager.NETWORK_TYPE_GPRS,
                TelephonyManager.NETWORK_TYPE_EDGE    -> NetworkType.MOBILE_2G
                else                                  -> NetworkType.MOBILE_4G
            }
        } catch (_: Exception) { NetworkType.MOBILE_4G }
    }

    private fun getWifiSsid(context: Context, caps: NetworkCapabilities?): String {
        if (caps == null || !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return ""
        return try {
            @Suppress("DEPRECATION")
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val info = wm?.connectionInfo
            info?.ssid?.replace("\"", "") ?: ""
        } catch (_: Exception) { "" }
    }

    private fun safeTrafficRead(block: () -> Long): Long {
        return try {
            val v = block()
            if (v == TrafficStats.UNSUPPORTED.toLong()) 0L else v.coerceAtLeast(0L)
        } catch (_: Exception) { 0L }
    }
}
