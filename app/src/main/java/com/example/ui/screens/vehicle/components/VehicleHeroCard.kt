package com.example.ui.screens.vehicle.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Speed
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
import com.example.ui.components.Layered3DCard
import com.example.ui.screens.vehicle.model.VehicleData
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD

@Composable
fun VehicleHeroCard(
    vehicle: VehicleData,
    modifier: Modifier = Modifier,
    onCardClick: () -> Unit = {}
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val cardBgGradient = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF0F172A),
                Color(0xFF1E293B),
                Color(0xFF0F172A)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF1E293B),
                Color(0xFF0F172A),
                Color(0xFF1E1B4B)
            )
        )
    }

    Layered3DCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLG),
        backgroundColor = Color.Transparent,
        elevation = 10.dp,
        contentPadding = PaddingValues(0.dp),
        testTag = "vehicle_hero_card",
        onClick = onCardClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBgGradient)
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Row: Car Name + Health Status Tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = vehicle.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                            color = Color.White
                        )
                        Text(
                            text = "مدل ${vehicle.modelYear} • ${vehicle.colorName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    // Health Status Tag
                    Surface(
                        shape = CircleShape,
                        color = vehicle.healthStatus.color.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, vehicle.healthStatus.color.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = vehicle.healthStatus.iconEmoji,
                                fontSize = 11.sp
                            )
                            Text(
                                text = vehicle.healthStatus.title,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = vehicle.healthStatus.color
                            )
                        }
                    }
                }

                // Middle: 3D Visual + License Plate Tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // License Plate Card (Persian Plate Look)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Iran License Plate container
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.5.dp, Color(0xFF0F172A))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 16.dp, height = 18.dp)
                                        .background(Color(0xFF2563EB)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("I.R.", fontSize = 7.sp, color = Color.White)
                                }

                                Text(
                                    text = vehicle.licensePlate,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 12.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                )
                            }
                        }

                        // Mileage pill
                        Surface(
                            shape = RoundedCornerShape(RadiusMD),
                            color = Color.White.copy(alpha = 0.1f),
                            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Speed,
                                    contentDescription = "کارکرد",
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = vehicle.odometerFormatted,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }

                    // 3D Car Render Asset
                    Vehicle3DVisual(
                        imageRes = vehicle.carImageRes,
                        carSize = 120.dp,
                        accentGlowColor = Color(0xFF3B82F6)
                    )
                }

                // Expenses Summary Bar
                val lastExpenseText = vehicle.recentExpenses.firstOrNull()?.amountFormatted
                    ?: vehicle.timelineRecords.firstOrNull()?.costFormatted
                    ?: "۰ تومان"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(RadiusMD))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "هزینه این ماه",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = vehicle.stats.monthlyExpenseFormatted,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                            color = Color(0xFF38BDF8)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(width = 1.dp, height = 24.dp)
                            .background(Color.White.copy(alpha = 0.15f))
                    )

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "آخرین هزینه ثبت شده",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = lastExpenseText,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                            color = Color(0xFFFBBF24)
                        )
                    }
                }

                // Bottom: Health Message & View Details action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(RadiusMD))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = vehicle.healthStatus.description,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFFCBD5E1),
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "مشاهده جزئیات",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color(0xFF60A5FA)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                            contentDescription = "جزییات",
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}
