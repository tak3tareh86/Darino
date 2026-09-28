package com.example.vehicle.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.IranianPhoneUtils
import com.example.vehicle.domain.VehicleMonthlyExpenseBar

/**
 * Modern Stacked Monthly Vehicle Cost Chart (نمودار هزینه خودرو)
 * Segments: Fuel (Blue), Repairs (Red), Service (Green), Insurance (Amber)
 */
@Composable
fun VehicleCostChart(
    bars: List<VehicleMonthlyExpenseBar>,
    modifier: Modifier = Modifier
) {
    val maxTotal = (bars.maxOfOrNull { it.totalAmount } ?: 5_000_000L).coerceAtLeast(1_000_000L).toFloat()
    val progress = remember { Animatable(0f) }

    LaunchedEffect(bars) {
        progress.animateTo(1f, animationSpec = tween(700))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("vehicle_cost_chart"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Chart Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            bars.forEach { bar ->
                val fuelRatio = (bar.fuelAmount / maxTotal) * progress.value
                val repairRatio = (bar.repairAmount / maxTotal) * progress.value
                val serviceRatio = (bar.serviceAmount / maxTotal) * progress.value
                val insuranceRatio = (bar.insuranceAmount / maxTotal) * progress.value

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    // Tooltip tag for current month
                    if (bar.isCurrentMonth) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = "${IranianPhoneUtils.convertDigitsToPersian((bar.totalAmount / 1_000_000L).toString())} م",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(18.dp))
                    }

                    // Stacked Bar
                    Column(
                        modifier = Modifier
                            .width(22.dp)
                            .height(110.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        if (insuranceRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((110 * insuranceRatio).dp)
                                    .background(Color(0xFFF59E0B))
                            )
                        }
                        if (repairRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((110 * repairRatio).dp)
                                    .background(Color(0xFFEF4444))
                            )
                        }
                        if (serviceRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((110 * serviceRatio).dp)
                                    .background(Color(0xFF10B981))
                            )
                        }
                        if (fuelRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((110 * fuelRatio).dp)
                                    .background(Color(0xFF3B82F6))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = bar.monthName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = if (bar.isCurrentMonth) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (bar.isCurrentMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Legend Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            ChartLegendItem(title = "سوخت", color = Color(0xFF3B82F6))
            ChartLegendItem(title = "سرویس", color = Color(0xFF10B981))
            ChartLegendItem(title = "تعمیرات", color = Color(0xFFEF4444))
            ChartLegendItem(title = "بیمه", color = Color(0xFFF59E0B))
        }
    }
}

@Composable
private fun ChartLegendItem(
    title: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
