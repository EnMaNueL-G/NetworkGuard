package com.enmanuelgil.networkguard.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.TrafficStats
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.enmanuelgil.networkguard.MainActivity
import com.enmanuelgil.networkguard.R
import kotlinx.coroutines.*

/**
 * Servicio en primer plano para monitoreo continuo de red.
 * Mantiene una notificación persistente con RX/TX en tiempo real.
 * Usa dataSync foreground type (compatible Android 14+).
 */
class NetworkMonitorService : Service() {

    companion object {
        const val CHANNEL_ID = "networkguard_monitor"
        const val NOTIF_ID   = 2001
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotification("Monitor activo…", "Iniciando…"))
        startPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_STICKY

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun startPolling() {
        scope.launch {
            while (isActive) {
                try {
                    val rx = TrafficStats.getTotalRxBytes().coerceAtLeast(0L)
                    val tx = TrafficStats.getTotalTxBytes().coerceAtLeast(0L)
                    val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(NOTIF_ID, buildNotification(
                        "↓ ${rx.fmt()}  ↑ ${tx.fmt()}",
                        "NetworkGuard activo"
                    ))
                } catch (_: Exception) {}
                delay(10_000L) // cada 10 segundos para no drenar batería
            }
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_desc)
                setShowBadge(false)
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
    }

    private fun buildNotification(title: String, text: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    private fun Long.fmt(): String = when {
        this < 1024L           -> "${this}B"
        this < 1024L * 1024    -> "${"%.0f".format(this / 1024.0)}KB"
        this < 1024L * 1024 * 1024 -> "${"%.1f".format(this / (1024.0 * 1024))}MB"
        else -> "${"%.2f".format(this / (1024.0 * 1024 * 1024))}GB"
    }
}
