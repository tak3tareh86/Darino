package com.example.ui.screens.vehicle.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.vehicle.model.VehicleData
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD

@Composable
fun VehicleSelector(
    vehicles: List<VehicleData>,
    selectedVehicleId: String,
    onVehicleSelect: (VehicleData) -> Unit,
    onAddNewVehicle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(RadiusMD))
                .clickable { isExpanded = !isExpanded },
            shape = RoundedCornerShape(RadiusMD),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "تغییر وضعیت نمایش",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "خودروهای شما (${vehicles.size} ثبت شده)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = if (isExpanded) "بستن" else "مشاهده لیست",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (isExpanded) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(vehicles, key = { it.id }) { vehicle ->
                    val isSelected = vehicle.id == selectedVehicleId

                    VehicleSelectorItem(
                        vehicle = vehicle,
                        isSelected = isSelected,
                        onClick = { onVehicleSelect(vehicle) }
                    )
                }
            }
        }
    }
}

@Composable
fun VehicleSelectorItem(
    vehicle: VehicleData,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFFEFF6FF)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "BgColor"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF3B82F6) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        label = "BorderColor"
    )

    Surface(
        modifier = modifier
            .testTag("vehicle_tab_${vehicle.id}")
            .shadow(
                elevation = if (isSelected) 6.dp else 2.dp,
                shape = RoundedCornerShape(RadiusMD),
                ambientColor = if (isSelected) Color(0xFF3B82F6).copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.05f),
                spotColor = if (isSelected) Color(0xFF2563EB).copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.1f)
            )
            .clip(RoundedCornerShape(RadiusMD))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(RadiusMD),
        color = backgroundColor,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Mini 3D Car thumbnail
            Image(
                painter = painterResource(id = vehicle.carImageRes),
                contentDescription = vehicle.name,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = vehicle.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                    color = if (isSelected) Color(0xFF2563EB) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${vehicle.odometerFormatted} • ${vehicle.healthStatus.iconEmoji}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
