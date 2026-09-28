package com.example.ui.screens.vehicle.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RadiusXL

@Composable
fun VehicleHeader(
    onAddVehicleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val headerGradient = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F172A),
                Color(0xFF1E1B4B),
                Color(0xFF0F172A)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F172A),
                Color(0xFF1E293B),
                Color(0xFF334155)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(bottomStart = RadiusXL, bottomEnd = RadiusXL),
                ambientColor = Color(0xFF3B82F6).copy(alpha = 0.2f),
                spotColor = Color(0xFF0F172A).copy(alpha = 0.4f)
            )
            .clip(RoundedCornerShape(bottomStart = RadiusXL, bottomEnd = RadiusXL))
            .background(headerGradient)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Right Side (RTL): Title & Subtitle
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "خدمات خودرویی",
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 24.sp),
                    color = Color.White
                )
                Text(
                    text = "خودروها و هزینه‌هایشان را مدیریت کنید",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1).copy(alpha = 0.85f)
                )
            }

            // Left Side (RTL): 3D Add Vehicle Button
            Surface(
                modifier = Modifier
                    .testTag("vehicle_header_add_btn")
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Color(0xFF3B82F6).copy(alpha = 0.35f),
                        spotColor = Color(0xFF2563EB).copy(alpha = 0.5f)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onAddVehicleClick),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF2563EB),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "افزودن خودرو",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )

                    Text(
                        text = "افزودن خودرو",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}
