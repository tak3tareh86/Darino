package com.example.ui.screens.vehicle.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.RadiusMD

@Composable
fun VehicleEmptyState(
    onAddVehicleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 3D Empty Garage Visual Asset
        Image(
            painter = painterResource(id = R.drawable.img_3d_empty_garage),
            contentDescription = "پارکینگ خالی",
            modifier = Modifier
                .size(160.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
                .clip(RoundedCornerShape(24.dp)),
            contentScale = ContentScale.Crop
        )

        Text(
            text = "هنوز خودرویی اضافه نکرده‌اید",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Text(
            text = "خودروی خود را اضافه کنید تا هزینه‌ها، سرویس‌ها و یادآوری‌ها را به آسانی مدیریت کنید.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Button(
            onClick = onAddVehicleClick,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(48.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(RadiusMD),
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
                .testTag("empty_state_add_vehicle_btn"),
            shape = RoundedCornerShape(RadiusMD),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "افزودن خودرو",
                    tint = Color.White
                )
                Text(
                    text = "افزودن اولین خودرو",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                    color = Color.White
                )
            }
        }
    }
}
