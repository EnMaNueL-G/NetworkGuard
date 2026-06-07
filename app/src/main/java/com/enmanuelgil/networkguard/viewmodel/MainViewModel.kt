package com.enmanuelgil.networkguard.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enmanuelgil.networkguard.core.AppTrafficAnalyzer
import com.enmanuelgil.networkguard.core.NetworkMonitor
import com.enmanuelgil.networkguard.model.AppTrafficInfo
import com.enmanuelgil.networkguard.model.NetworkAlert
import com.enmanuelgil.networkguard.model.NetworkState
import com.enmanuelgil.networkguard.model.AlertType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val _networkState  = MutableStateFlow(NetworkState())
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    private val _appTraffic    = MutableStateFlow<List<AppTrafficInfo>>(emptyList())
    val appTraffic: StateFlow<List<AppTrafficInfo>> = _appTraffic.asStateFlow()

    private val _alerts        = MutableStateFlow<List<NetworkAlert>>(emptyList())
    val alerts: StateFlow<List<NetworkAlert>> = _alerts.asStateFlow()

    private val _isLoading     = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Filtro UI: mostrar solo apps de usuario (no sistema)
    private val _showSystemApps = MutableStateFlow(false)
    val showSystemApps: StateFlow<Boolean> = _showSystemApps.asStateFlow()

    private var pollingJob: Job? = null

    init {
        startPolling()
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            var firstRun = true
            while (true) {
                try {
                    val ctx = getApplication<Application>().applicationContext

                    // Leer estado de red
                    val state = NetworkMonitor.getNetworkState(ctx)

                    // Leer tráfico por app
                    val traffic = AppTrafficAnalyzer.getTopTrafficApps(ctx, topN = 25)

                    // Actualizar activos en NetworkState
                    val activeCount = traffic.count { it.isActive }
                    _networkState.value = state.copy(activeAppsCount = activeCount)
                    _appTraffic.value   = traffic

                    // Generar alertas básicas
                    generateAlerts(traffic)

                    if (firstRun) {
                        _isLoading.value = false
                        firstRun = false
                    }
                } catch (_: Exception) {
                    _isLoading.value = false
                }
                // Polling cada 3 segundos — velocidad razonable sin drenar batería
                delay(3_000L)
            }
        }
    }

    fun toggleSystemApps() {
        _showSystemApps.value = !_showSystemApps.value
    }

    fun clearAlerts() {
        _alerts.value = emptyList()
    }

    // Detecta patrones anómalos simples en el tráfico
    private fun generateAlerts(traffic: List<AppTrafficInfo>) {
        val newAlerts = mutableListOf<NetworkAlert>()
        val existing  = _alerts.value.map { it.packageName + it.alertType }.toSet()

        for (app in traffic) {
            // App enviando más de 500 KB/s — posible subida masiva
            if (app.txSpeedBps > 512 * 1024L) {
                val key = app.packageName + AlertType.LARGE_UPLOAD
                if (key !in existing) {
                    newAlerts.add(NetworkAlert(
                        packageName = app.packageName,
                        appName     = app.appName,
                        alertType   = AlertType.LARGE_UPLOAD,
                        detail      = "Subiendo a ${(app.txSpeedBps / 1024).toInt()} KB/s"
                    ))
                }
            }
            // App de sistema con más de 50 MB en background — inusual
            if (app.isSystemApp && app.totalBytes > 50L * 1024 * 1024) {
                val key = app.packageName + AlertType.HIGH_BACKGROUND_DATA
                if (key !in existing) {
                    newAlerts.add(NetworkAlert(
                        packageName = app.packageName,
                        appName     = app.appName,
                        alertType   = AlertType.HIGH_BACKGROUND_DATA,
                        detail      = "Sistema usó ${app.totalBytes / (1024 * 1024)} MB"
                    ))
                }
            }
        }

        if (newAlerts.isNotEmpty()) {
            _alerts.value = (_alerts.value + newAlerts).takeLast(20)
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
