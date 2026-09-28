package com.example.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.ui.screens.home.domain.VehicleSummaryData
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM
import com.example.ui.theme.WarningAmberLight

@Composable
fun VehicleSummaryCard(
    vehicle: VehicleSummaryData?,
    onViewVehicleClick: () -> Unit,
    onAddVehicleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "وضعیت خودرو",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            if (vehicle != null) {
                TextButton(
                    onClick = onViewVehicleClick,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "مشاهده پرونده",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (vehicle == null) {
            // Small Empty State for No Vehicle
            Layered3DCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 2.dp,
                contentPadding = PaddingValues(16.dp),
                testTag = "home_empty_vehicle_card"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Soft3DIcon(
                            imageRes = R.drawable.img_3d_car,
                            contentDescription = "افزودن خودرو",
                            size = 40.dp,
                            accentColor = Color(0xFF0284C7)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "هنوز خودرویی ثبت نکرده‌اید",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "برای مدیریت سرویس‌ها، بیمه و هزینه‌های خودرو ثبت کنید.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = onAddVehicleClick,
                        shape = RoundedCornerShape(RadiusMD),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "افزودن",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        } else {
            // Compact Vehicle Card
            Layered3DCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 3.dp,
                contentPadding = PaddingValues(14.dp),
                onClick = onViewVehicleClick,
                testTag = "home_vehicle_summary_card"
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Soft3DIcon(
                                imageRes = R.drawable.img_3d_car,
                                contentDescription = vehicle.name,
                                size = 44.dp,
                                accentColor = Color(0xFF0284C7)
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = vehicle.name,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(RadiusSM),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    ) {
                                        Text(
                                            text = vehicle.modelYear,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Speed,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "کارکرد: ${vehicle.formattedMileage}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Status Indicator
                    Surface(
                        shape = RoundedCornerShape(RadiusMD),
                        color = if (vehicle.isNeedsService) {
                            WarningAmberLight.copy(alpha = 0.12f)
                        } else {
                            EmeraldPrimaryLight.copy(alpha = 0.1f)
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = if (vehicle.isNeedsService) WarningAmberLight else EmeraldPrimaryLight,
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = "وضعیت: ${vehicle.statusText}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (vehicle.isNeedsService) WarningAmberLight else EmeraldPrimaryLight
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
