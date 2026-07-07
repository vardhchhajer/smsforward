package com.example.smsforwarderpro.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import com.example.smsforwarderpro.ui.dashboard.DashboardScreen
import com.example.smsforwarderpro.ui.dashboard.DashboardViewModel
import com.example.smsforwarderpro.ui.destinations.DestinationsScreen
import com.example.smsforwarderpro.ui.destinations.DestinationsViewModel
import com.example.smsforwarderpro.ui.logs.LogsScreen
import com.example.smsforwarderpro.ui.logs.LogsViewModel
import com.example.smsforwarderpro.ui.rules.RulesScreen
import com.example.smsforwarderpro.ui.rules.RulesViewModel
import com.example.smsforwarderpro.ui.settings.SettingsScreen
import com.example.smsforwarderpro.ui.settings.SettingsViewModel

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val destinationsViewModel: DestinationsViewModel = hiltViewModel()
    val rulesViewModel: RulesViewModel = hiltViewModel()
    val logsViewModel: LogsViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()

    val tabs = listOf(
        TabItem("Home", Icons.Default.Dashboard),
        TabItem("Destinations", Icons.Default.Output),
        TabItem("Rules", Icons.Default.AltRoute),
        TabItem("Logs", Icons.Default.History),
        TabItem("Settings", Icons.Default.Settings)
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { Text(tab.label) },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) }
                    )
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToSettings = { selectedTab = 4 }
                )
                1 -> DestinationsScreen(
                    viewModel = destinationsViewModel
                )
                2 -> RulesScreen(
                    viewModel = rulesViewModel
                )
                3 -> LogsScreen(
                    viewModel = logsViewModel
                )
                4 -> SettingsScreen(
                    viewModel = settingsViewModel
                )
            }
        }
    }
}

data class TabItem(
    val label: String,
    val icon: ImageVector
)
