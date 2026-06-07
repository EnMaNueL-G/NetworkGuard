package com.enmanuelgil.networkguard

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowWidthSizeClass
import com.enmanuelgil.networkguard.ui.screens.*
import com.enmanuelgil.networkguard.ui.theme.*
import com.enmanuelgil.networkguard.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Solicitar notificaciones en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        // Iniciar servicio de monitoreo en background
        try {
            val svc = android.content.Intent(this, com.enmanuelgil.networkguard.service.NetworkMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(svc)
            else startService(svc)
        } catch (_: Exception) {}

        setContent {
            NetworkGuardTheme {
                NetworkGuardApp(viewModel)
            }
        }
    }
}

data class NavItem(val label: String, val icon: ImageVector)

@Composable
fun NetworkGuardApp(viewModel: MainViewModel) {
    val networkState  by viewModel.networkState.collectAsStateWithLifecycle()
    val appTraffic    by viewModel.appTraffic.collectAsStateWithLifecycle()
    val alerts        by viewModel.alerts.collectAsStateWithLifecycle()
    val isLoading     by viewModel.isLoading.collectAsStateWithLifecycle()
    val showSystem    by viewModel.showSystemApps.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }

    val navItems = listOf(
        NavItem("Monitor",  Icons.Default.NetworkCheck),
        NavItem("Apps",     Icons.Default.Apps),
        NavItem("Ajustes",  Icons.Default.Settings)
    )

    // Detección de tablet para NavigationRail vs NavigationBar
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val isTablet = adaptiveInfo.windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT

    Box(Modifier.fillMaxSize().background(NetBackground)) {
        if (isTablet) {
            // ── Layout tablet: NavigationRail lateral ────────────────────────
            Row(Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = NetSurface,
                    modifier = Modifier.width(80.dp)
                ) {
                    Spacer(Modifier.weight(1f))
                    navItems.forEachIndexed { i, item ->
                        NavigationRailItem(
                            selected  = selectedTab == i,
                            onClick   = { selectedTab = i },
                            icon      = { Icon(item.icon, contentDescription = item.label) },
                            label     = { Text(item.label) },
                            colors    = NavigationRailItemDefaults.colors(
                                selectedIconColor   = NetPurple,
                                selectedTextColor   = NetPurple,
                                indicatorColor      = NetPurple.copy(alpha = 0.12f),
                                unselectedIconColor = NetTextSecondary,
                                unselectedTextColor = NetTextSecondary
                            )
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
                TabContent(
                    selectedTab, networkState, appTraffic, alerts,
                    isLoading, showSystem, viewModel,
                    Modifier.weight(1f)
                )
            }
        } else {
            // ── Layout teléfono: NavigationBar inferior ──────────────────────
            Scaffold(
                containerColor = NetBackground,
                bottomBar = {
                    NavigationBar(containerColor = NetSurface) {
                        navItems.forEachIndexed { i, item ->
                            NavigationBarItem(
                                selected  = selectedTab == i,
                                onClick   = { selectedTab = i },
                                icon      = { Icon(item.icon, contentDescription = item.label) },
                                label     = { Text(item.label) },
                                colors    = NavigationBarItemDefaults.colors(
                                    selectedIconColor   = NetPurple,
                                    selectedTextColor   = NetPurple,
                                    indicatorColor      = NetPurple.copy(alpha = 0.12f),
                                    unselectedIconColor = NetTextSecondary,
                                    unselectedTextColor = NetTextSecondary
                                )
                            )
                        }
                    }
                }
            ) { innerPadding ->
                TabContent(
                    selectedTab, networkState, appTraffic, alerts,
                    isLoading, showSystem, viewModel,
                    Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun TabContent(
    tab: Int,
    networkState: com.enmanuelgil.networkguard.model.NetworkState,
    appTraffic: List<com.enmanuelgil.networkguard.model.AppTrafficInfo>,
    alerts: List<com.enmanuelgil.networkguard.model.NetworkAlert>,
    isLoading: Boolean,
    showSystem: Boolean,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize()) {
        when (tab) {
            0 -> DashboardScreen(
                state       = networkState,
                alerts      = alerts,
                isLoading   = isLoading,
                onClearAlerts = { viewModel.clearAlerts() }
            )
            1 -> AppsScreen(
                apps             = appTraffic,
                showSystemApps   = showSystem,
                onToggleSystemApps = { viewModel.toggleSystemApps() },
                isLoading        = isLoading
            )
            2 -> SettingsScreen()
        }
    }
}
