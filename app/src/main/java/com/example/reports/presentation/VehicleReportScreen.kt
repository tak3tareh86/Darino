package com.example.reports.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.reports.domain.VehicleCostItem
import com.example.reports.domain.VehicleDetailItem
import com.example.reports.domain.VehicleSummary
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.util.IranianPhoneUtils
import java.text.NumberFormat
import java.util.Locale

/**
 * Screen 3: Vehicle Cost Report & Analysis (گزارش خودرو)
 */
@Composable
fun VehicleReportScreen(
    vehicleSummary: VehicleSummary,
    modifier: Modifier = Modifier
) {
    val currentMonthFormatted = IranianPhoneUtils.convertDigitsToPersian(
        NumberFormat.getNumberInstance(Locale.US).format(vehicleSummary.currentMonthCost)
    )
    val yearlyCostFormatted = IranianPhoneUtils.convertDigitsToPersian(
        NumberFormat.getNumberInstance(Locale.US).format(vehicleSummary.yearlyCost)
    )
    val topCostAmountFormatted = IranianPhoneUtils.convertDigitsToPersian(
        NumberFormat.getNumberInstance(Locale.US).format(vehicleSummary.topCostAmount)
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("vehicle_report_screen"),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Vehicle Overview Summary Card
        item {
            Layered3DCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp,
                borderColor = Color(0xFF3B82F6).copy(alpha = 0.25f)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Soft3DIcon(
                                imageRes = R.drawable.img_3d_car,
                                contentDescription = "مخارج و نگهداری خودروها",
                                size = 34.dp,
                                accentColor = Color(0xFF3B82F6)
                            )
                            Text(
                                text = "مخارج و نگهداری خودروها",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF3B82F6).copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${IranianPhoneUtils.convertDigitsToPersian(vehicleSummary.totalVehicles.toString())} خودرو ثبت شده",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = Color(0xFF3B82F6),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    // Row: Current Month vs Yearly Cost
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "هزینه خودرو در ماه جاری",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$currentMonthFormatted تومان",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = Color(0xFF3B82F6)
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "هزینه سالانه خودرو",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$yearlyCostFormatted تومان",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Vehicle Cost Analysis & Trends Card (Section 10)
        item {
            Layered3DCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 3.dp,
                borderColor = Color(0xFFF59E0B).copy(alpha = 0.3f)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Soft3DIcon(
                                imageRes = R.drawable.img_3d_tire,
                                contentDescription = "تحلیل هوشمند هزینه‌های خودرو",
                                size = 30.dp,
                                accentColor = Color(0xFFF59E0B)
                            )
                            Text(
                                text = "تحلیل هوشمند هزینه‌های خودرو",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowUpward,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "${IranianPhoneUtils.convertDigitsToPersian(vehicleSummary.monthlyChangePercent.toString())}٪ رشد",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = Color(0xFFEF4444)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "بیشترین هزینه خودرو:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${vehicleSummary.topCostCategory} ($topCostAmountFormatted تومان)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "روند هزینه نسبت به ماه قبل:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "افزایش ${IranianPhoneUtils.convertDigitsToPersian(vehicleSummary.monthlyChangePercent.toString())} درصد",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. Cost Breakdown Progress & Items (سوخت، تعمیرات، سرویس، بیمه، قطعات)
        item {
            Layered3DCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 3.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "تفکیک دسته‌بندی هزینه‌های خودرو",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Stacked visual progress
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        vehicleSummary.costBreakdown.forEach { item ->
                            val fraction = (item.percentage.toFloat() / 100f).coerceIn(0f, 1f)
                            if (fraction > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(fraction.coerceAtLeast(0.02f))
                                        .fillMaxHeight()
                                        .background(Color(item.colorHex))
                                )
                            }
                        }
                    }

                    // Cost Breakdown List
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        vehicleSummary.costBreakdown.forEach { item ->
                            val amountFormatted = IranianPhoneUtils.convertDigitsToPersian(
                                NumberFormat.getNumberInstance(Locale.US).format(item.amount)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Soft3DIcon(
                                        imageRes = item.iconRes,
                                        contentDescription = item.categoryName,
                                        size = 24.dp,
                                        accentColor = Color(item.colorHex)
                                    )
                                    Text(
                                        text = item.categoryName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "$amountFormatted تومان",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(item.colorHex).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "${IranianPhoneUtils.convertDigitsToPersian(item.percentage.toString())}٪",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            ),
                                            color = Color(item.colorHex),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Vehicles Detail Cards
        item {
            Text(
                text = "خودروهای تحت پوشش",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(vehicleSummary.vehicles, key = { it.id }) { vehicle ->
            VehicleCard(vehicle = vehicle)
        }
    }
}

@Composable
private fun VehicleCard(vehicle: VehicleDetailItem) {
    val monthlyFormatted = IranianPhoneUtils.convertDigitsToPersian(
        NumberFormat.getNumberInstance(Locale.US).format(vehicle.monthlyCost)
    )
    val yearlyFormatted = IranianPhoneUtils.convertDigitsToPersian(
        NumberFormat.getNumberInstance(Locale.US).format(vehicle.yearlyCost)
    )

    Layered3DCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Soft3DIcon(
                    imageRes = vehicle.iconRes,
                    contentDescription = vehicle.name,
                    size = 36.dp,
                    accentColor = Color(0xFF3B82F6)
                )
                Column {
                    Text(
                        text = vehicle.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = vehicle.plateNumber,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "ماهانه: $monthlyFormatted تومان",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = "سالانه: $yearlyFormatted تومان",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
