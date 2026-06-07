package com.enmanuelgil.networkguard.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enmanuelgil.networkguard.core.AppTrafficAnalyzer
import com.enmanuelgil.networkguard.core.NetworkMonitor
import com.enmanuelgil.networkguard.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OptimizeNetResult(
    val killedApps: Int    = 0,
    val message: String    = ""
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val _networkState   = MutableStateFlow(NetworkState())
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    private val _appTraffic     = MutableStateFlow<List<AppTrafficInfo>>(emptyList())
    val appTraffic: StateFlow<List<AppTrafficInfo>> = _appTraffic.asStateFlow()

    private val _alerts         = MutableStateFlow<List<NetworkAlert>>(emptyList())
    val alerts: StateFlow<List<NetworkAlert>> = _alerts.asStateFlow()

    private val _isLoading      = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isOptimizing   = MutableStateFlow(false)
    val isOptimizing: StateFlow<Boolean> = _isOptimizing.asStateFlow()

    private val _optimizeResult = MutableStateFlow<OptimizeNetResult?>(null)
    val optimizeResult: StateFlow<OptimizeNetResult?> = _optimizeResult.asStateFlow()

    private val _showSystemApps = MutableStateFlow(false)
    val showSystemApps: StateFlow<Boolean> = _showSystemApps.asStateFlow()

    // ¿El dispositivo soporta TrafficStats por UID?
    private val _perUidSupported = MutableStateFlow(true)
    val perUidSupported: StateFlow<Boolean> = _perUidSupported.asStateFlow()

    private var pollingJob: Job? = null

    init { startPolling() }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            var firstRun = true
            while (true) {
                try {
                    val ctx     = getApplication<Application>().applicationContext
                    val state   = NetworkMonitor.getNetworkState(ctx)
                    val traffic = AppTrafficAnalyzer.getTopTrafficApps(ctx, topN = 25)

                    _perUidSupported.value = AppTrafficAnalyzer.isPerUidSupported
                    val activeCount = traffic.count { it.isActive }
                    _networkState.value = state.copy(activeAppsCount = activeCount)
                    _appTraffic.value   = traffic

                    generateAlerts(traffic)

                    if (firstRun) {
                        _isLoading.value = false
                        firstRun = false
                    }
                } catch (_: Exception) {
                    _isLoading.value = false
                }
                delay(3_000L)
            }
        }
    }

    fun optimizeNetwork() {
        if (_isOptimizing.value) return
        viewModelScope.launch {
            _isOptimizing.value   = true
            _optimizeResult.value = null

            val ctx      = getApplication<Application>().applicationContext
            val topApps  = _appTraffic.value
            val killed   = AppTrafficAnalyzer.optimizeNetworkUsage(ctx, topApps)

            val msg = when {
                killed > 0 -> "$killed apps en background detenidas. Red liberada."
                else       -> "Sistema en buen estado. Sin procesos innecesarios."
            }
            _optimizeResult.value = OptimizeNetResult(killedApps = killed, message = msg)
            _isOptimizing.value   = false
        }
    }

    fun dismissOptimizeResult() { _optimizeResult.value = null }

    fun toggleSystemApps() { _showSystemApps.value = !_showSystemApps.value }

    fun clearAlerts() { _alerts.value = emptyList() }

    private fun generateAlerts(traffic: List<AppTrafficInfo>) {
        val newAlerts = mutableListOf<NetworkAlert>()
        val existing  = _alerts.value.map { it.packageName + it.alertType }.toSet()

        for (app in traffic) {
            if (app.txSpeedBps > 512 * 1024L) {
                val key = app.packageName + AlertType.LARGE_UPLOAD
                if (key !in existing) {
                    newAlerts.add(NetworkAlert(app.packageName, app.appName, AlertType.LARGE_UPLOAD,
                        "Subiendo a ${app.txSpeedBps / 1024} KB/s"))
                }
            }
            if (app.isSystemApp && app.totalBytes > 50L * 1024 * 1024) {
                val key = app.packageName + AlertType.HIGH_BACKGROUND_DATA
                if (key !in existing) {
                    newAlerts.add(NetworkAlert(app.packageName, app.appName, AlertType.HIGH_BACKGROUND_DATA,
                        "Sistema usó ${app.totalBytes / (1024 * 1024)} MB"))
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
