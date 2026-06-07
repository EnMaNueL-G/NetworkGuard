package com.enmanuelgil.networkguard.model

// ── Información general de red ──────────────────────────────────────────────

data class NetworkState(
    val isConnected: Boolean       = false,
    val networkType: NetworkType   = NetworkType.NONE,
    val wifiSsid: String           = "",
    val signalStrength: Int        = 0,      // 0-4
    val rxBytesTotal: Long         = 0L,     // bytes recibidos sesión
    val txBytesTotal: Long         = 0L,     // bytes enviados sesión
    val rxSpeedBps: Long           = 0L,     // velocidad actual recepción bytes/s
    val txSpeedBps: Long           = 0L,     // velocidad actual envío bytes/s
    val rxBytesToday: Long         = 0L,     // total diario recibido
    val txBytesToday: Long         = 0L,     // total diario enviado
    val activeAppsCount: Int       = 0       // apps con tráfico activo
)

enum class NetworkType(val label: String, val emoji: String) {
    NONE("Sin red", "📵"),
    WIFI("Wi-Fi", "📶"),
    MOBILE_4G("Datos 4G", "📡"),
    MOBILE_3G("Datos 3G", "📡"),
    MOBILE_2G("Datos 2G", "📡"),
    ETHERNET("Ethernet", "🔌"),
    OTHER("Red desconocida", "🌐")
}

// ── Tráfico por aplicación ───────────────────────────────────────────────────

data class AppTrafficInfo(
    val packageName: String,
    val appName: String,
    val rxBytes: Long,          // bytes recibidos total (sesión)
    val txBytes: Long,          // bytes enviados total (sesión)
    val rxSpeedBps: Long,       // velocidad recepción actual bytes/s
    val txSpeedBps: Long,       // velocidad envío actual bytes/s
    val isSystemApp: Boolean,
    val isActive: Boolean       // tiene tráfico en último intervalo
) {
    val totalBytes: Long get() = rxBytes + txBytes
    val totalSpeedBps: Long get() = rxSpeedBps + txSpeedBps
}

// ── Alertas ─────────────────────────────────────────────────────────────────

data class NetworkAlert(
    val packageName: String,
    val appName: String,
    val alertType: AlertType,
    val detail: String,
    val timestampMs: Long = System.currentTimeMillis()
)

enum class AlertType(val label: String, val emoji: String) {
    HIGH_BACKGROUND_DATA("Alto consumo en background", "⚠️"),
    LARGE_UPLOAD("Subida masiva detectada", "🔺"),
    LARGE_DOWNLOAD("Descarga masiva detectada", "🔻"),
    UNUSUAL_TRAFFIC("Tráfico inusual", "🔍")
}

// ── Utilidades de formato ────────────────────────────────────────────────────

fun Long.formatBytes(): String = when {
    this < 0          -> "—"
    this < 1024L      -> "${this} B"
    this < 1024L * 1024 -> "${"%.1f".format(this / 1024.0)} KB"
    this < 1024L * 1024 * 1024 -> "${"%.2f".format(this / (1024.0 * 1024))} MB"
    else -> "${"%.2f".format(this / (1024.0 * 1024 * 1024))} GB"
}

fun Long.formatSpeed(): String = when {
    this < 0          -> "0 B/s"
    this < 1024L      -> "${this} B/s"
    this < 1024L * 1024 -> "${"%.1f".format(this / 1024.0)} KB/s"
    else -> "${"%.2f".format(this / (1024.0 * 1024))} MB/s"
}
