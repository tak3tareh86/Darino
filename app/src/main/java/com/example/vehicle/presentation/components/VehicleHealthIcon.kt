package com.example.vehicle.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.DiscFull
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Theme-aware icons used only by vehicle health presentations.
 * ServiceType.iconRes remains unchanged for forms, timeline and other screens.
 */
@Composable
fun VehicleHealthIcon(
    title: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    val icon = vehicleHealthIconFor(title)
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.padding(size * 0.19f),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

private fun vehicleHealthIconFor(title: String): ImageVector {
    val value = title.replace("ي", "ی").replace("ك", "ک")
    return when {
        value.contains("معاینه فنی") -> Icons.Outlined.FactCheck
        value.contains("بیمه") && value.contains("ثالث") -> Icons.Outlined.VerifiedUser
        value.contains("بیمه") && value.contains("بدنه") -> Icons.Outlined.Shield
        value.contains("فیلتر هوا") -> Icons.Outlined.Air
        value.contains("فیلتر بنزین") || value.contains("فیلتر سوخت") -> Icons.Outlined.LocalGasStation
        value.contains("فیلتر روغن") || value.contains("فیلترها") -> Icons.Outlined.FilterAlt
        value.contains("روغن موتور") -> Icons.Outlined.WaterDrop
        value.contains("روغن گیربکس") -> Icons.Outlined.Settings
        value.contains("لنت ترمز") -> Icons.Outlined.DiscFull
        value.contains("دیسک") || value.contains("کلاچ") -> Icons.Outlined.Sync
        value.contains("تنظیم موتور") -> Icons.Outlined.Build
        value.contains("شمع") || value.contains("وایر") -> Icons.Outlined.ElectricBolt
        value.contains("موتور") -> Icons.Outlined.Build
        value.contains("لاستیک") || value.contains("تایر") -> Icons.Outlined.DiscFull
        value.contains("باتری") -> Icons.Outlined.BatteryChargingFull
        value.contains("هیدرولیک") || value.contains("فرمان") -> Icons.Outlined.Tune
        value.contains("روغن ترمز") -> Icons.Outlined.WaterDrop
        value.contains("تسمه دینام") || value.contains("تسمه تایم") -> Icons.Outlined.Sync
        value.contains("ضدیخ") || value.contains("رادیاتور") || value.contains("خنک‌کننده") -> Icons.Outlined.Thermostat
        value.contains("سفارشی") -> Icons.Outlined.DirectionsCar
        else -> Icons.Outlined.Build
    }
}
