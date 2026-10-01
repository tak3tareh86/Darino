package com.example.ui.screens.vehicle

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.vehicle.presentation.DarinoVehicleMainDashboard

/**
 * Entry point for Darino Vehicle Assistant (دستیار مدیریت خودرو در دارینو)
 */
@Composable
fun VehicleServicesScreen(
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {}
) {
    DarinoVehicleMainDashboard(
        bottomBar = bottomBar,
        modifier = modifier
    )
}
