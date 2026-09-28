package com.example.ui.screens.vehicle.components

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.WarningAmberLight

data class VehicleQuickActionItem(
    val id: String,
    val title: String,
    val emoji: String,
    val iconRes: Int? = null,
    val accentColor: Color,
    val isPrimary: Boolean = false
)

@Composable
fun VehicleQuickActions(
    modifier: Modifier = Modifier,
    onActionClick: (String) -> Unit = {}
) {
    val actions = listOf(
        VehicleQuickActionItem("add_service", "ثبت سرویس", "🛠", R.drawable.img_3d_oil, WarningAmberLight, isPrimary = true),
        VehicleQuickActionItem("add_fuel", "ثبت باک / سوخت", "⛽", R.drawable.img_3d_fuel, EmeraldPrimaryLight),
        VehicleQuickActionItem("add_reminder", "یادآوری", "⏰", null, InfoIndigoLight),
        VehicleQuickActionItem("view_details", "جزئیات خودرو", "📋", R.drawable.img_3d_car, Color(0xFF8B5CF6))
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "دسترسی سریع",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(actions, key = { it.id }) { action ->
                VehicleActionButton(
                    action = action,
                    onClick = { onActionClick(action.id) }
                )
            }
        }
    }
}

@Composable
fun VehicleActionButton(
    action: VehicleQuickActionItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val surfaceBg = if (action.isPrimary) {
        if (isDark) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFFECFDF5)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val borderColor = if (action.isPrimary) {
        action.accentColor.copy(alpha = 0.5f)
    } else {
        if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
    }

    Surface(
        modifier = modifier
            .testTag("vehicle_action_${action.id}")
            .shadow(
                elevation = if (action.isPrimary) 6.dp else 2.dp,
                shape = RoundedCornerShape(RadiusMD),
                ambientColor = action.accentColor.copy(alpha = if (action.isPrimary) 0.35f else 0.1f),
                spotColor = action.accentColor.copy(alpha = if (action.isPrimary) 0.5f else 0.2f)
            )
            .clip(RoundedCornerShape(RadiusMD))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(RadiusMD),
        color = surfaceBg,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icon Container
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                action.accentColor.copy(alpha = if (isDark) 0.35f else 0.2f),
                                action.accentColor.copy(alpha = if (isDark) 0.15f else 0.08f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (action.iconRes != null) {
                    Image(
                        painter = painterResource(id = action.iconRes),
                        contentDescription = action.title,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = action.emoji,
                        fontSize = 20.sp
                    )
                }
            }

            Text(
                text = action.title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp
                ),
                color = if (action.isPrimary) action.accentColor else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
