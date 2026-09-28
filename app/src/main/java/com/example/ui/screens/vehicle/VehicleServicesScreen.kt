package com.example.ui.screens.vehicle

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.vehicle.presentation.VehicleDashboardScreen

/**
 * Entry point for Darino Vehicle Assistant (دستیار مدیریت خودرو در دارینو)
 * Provides comprehensive vehicle dossier, health status, smart reminders, timeline, and financial analytics.
 */
@Composable
fun VehicleServicesScreen(
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {}
) {
    VehicleDashboardScreen(
        bottomBar = bottomBar,
        modifier = modifier
    )
}
