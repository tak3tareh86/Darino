package com.example.ui.screens.reports.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.OnlinePrediction
import androidx.compose.material.icons.rounded.QueryStats
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.ui.screens.reports.model.VehicleComparisonData
import com.example.ui.screens.reports.model.VehicleExpenseBreakdownItem
import com.example.ui.screens.reports.model.VehicleExpenseReportData
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.WarningAmberLight

@Composable
fun VehicleExpenseAnalysisCard(
    data: VehicleExpenseReportData,
    onVehicleClick: (VehicleComparisonData) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Layered3DCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("vehicle_expense_analysis_card"),
        shape = RoundedCornerShape(RadiusLG),
        backgroundColor = MaterialTheme.colorScheme.surface,
        elevation = 3.dp,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with 3D Car Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DirectionsCar,
                            contentDescription = null,
                            tint = WarningAmberLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "تحلیل هزینه‌های خودرویی",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "تفکیک سوخت، تعمیرات، سرویس و مقایسه",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 3D Car mini thumb
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(RadiusMD),
                            ambientColor = WarningAmberLight.copy(alpha = 0.3f),
                            spotColor = WarningAmberLight.copy(alpha = 0.5f)
                        )
                        .clip(RoundedCornerShape(RadiusMD))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    WarningAmberLight.copy(alpha = if (isDark) 0.35f else 0.15f),
                                    WarningAmberLight.copy(alpha = if (isDark) 0.15f else 0.05f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = if (isDark) 0.3f else 0.8f),
                                    WarningAmberLight.copy(alpha = 0.25f)
                                )
                            ),
                            shape = RoundedCornerShape(RadiusMD)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_3d_car),
                        contentDescription = "خودرو",
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(RadiusMD)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Predictive Insights & Cost Per KM Row (ADDED)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Cost per KM card
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(RadiusMD),
                    color = EmeraldPrimaryLight.copy(alpha = 0.12f),
                    border = BorderStroke(0.8.dp, EmeraldPrimaryLight.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QueryStats,
                            contentDescription = null,
                            tint = EmeraldPrimaryLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = "هزینه هر کیلومتر حرکت",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "۲,۴۵۰ تومان / ک‌م",
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                color = EmeraldPrimaryLight
                            )
                        }
                    }
                }

                // Predictive service card
                Surface(
                    modifier = Modifier.weight(1.1f),
                    shape = RoundedCornerShape(RadiusMD),
                    color = InfoIndigoLight.copy(alpha = 0.12f),
                    border = BorderStroke(0.8.dp, InfoIndigoLight.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OnlinePrediction,
                            contentDescription = null,
                            tint = InfoIndigoLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = "پیش‌بینی تعویض روغن بعدی",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "۳۵ روز دیگر (حدودی)",
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                color = InfoIndigoLight
                            )
                        }
                    }
                }
            }

            // Total Vehicle Expense Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(RadiusMD),
                color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "مجموع هزینه‌های خودرویی",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = data.totalVehicleExpenseFormatted,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = WarningAmberLight
                            )
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = WarningAmberLight.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = data.vehicleExpenseSharePercent,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = WarningAmberLight,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Routine vs Repair Ratio Progress Bar (ADDED)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "نسبت مخارج مصرفی روتین به تعمیرات",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "۶۵٪ مصرفی • ۳۵٪ تعمیرات",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = WarningAmberLight
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    // Routine (65%)
                    Box(
                        modifier = Modifier
                            .weight(0.65f)
                            .fillMaxHeight()
                            .background(EmeraldPrimaryLight)
                    )
                    // Repair (35%)
                    Box(
                        modifier = Modifier
                            .weight(0.35f)
                            .fillMaxHeight()
                            .background(ExpenseRoseLight)
                    )
                }
            }

            // Breakdown Grid (Fuel, Maint, Service, Ins, Tires)
            Text(
                text = "تفکیک سرفصل‌های هزینه‌ای:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                data.breakdown.forEach { item ->
                    VehicleBreakdownRow(item = item)
                }
            }

            // Multi-Vehicle Comparison Section
            if (data.vehicleComparisons.size > 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CompareArrows,
                        contentDescription = null,
                        tint = EmeraldPrimaryLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "مقایسه مالی خودروها",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    data.vehicleComparisons.forEach { vehicle ->
                        VehicleComparisonItemCard(
                            vehicle = vehicle,
                            onClick = { onVehicleClick(vehicle) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VehicleBreakdownRow(
    item: VehicleExpenseBreakdownItem,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Soft3DIcon(
                imageRes = item.iconRes,
                contentDescription = item.title,
                size = 30.dp,
                accentColor = item.color,
                containerShape = RoundedCornerShape(8.dp)
            )

            Text(
                text = item.title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = item.amountFormatted,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                shape = CircleShape,
                color = item.color.copy(alpha = 0.12f)
            ) {
                Text(
                    text = item.percentageFormatted,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = item.color,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun VehicleComparisonItemCard(
    vehicle: VehicleComparisonData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMD))
            .clickable { onClick() },
        shape = RoundedCornerShape(RadiusMD),
        color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.7f) else Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Vehicle Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = vehicle.imageRes),
                        contentDescription = vehicle.vehicleName,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = vehicle.vehicleName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${vehicle.modelYear} • ${vehicle.totalKmDriven}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Total Cost
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = "مجموع هزینه:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = vehicle.totalCostFormatted,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = WarningAmberLight
                        )
                    )
                }
            }

            // Sub-cost breakdown pills (Fuel, Maint, Ins)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MiniComparisonTag(
                    label = "سوخت",
                    value = vehicle.fuelCostFormatted,
                    color = EmeraldPrimaryLight,
                    modifier = Modifier.weight(1f)
                )
                MiniComparisonTag(
                    label = "تعمیر و سرویس",
                    value = vehicle.maintenanceCostFormatted,
                    color = WarningAmberLight,
                    modifier = Modifier.weight(1f)
                )
                MiniComparisonTag(
                    label = "بیمه و سایر",
                    value = vehicle.insuranceCostFormatted,
                    color = InfoIndigoLight,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MiniComparisonTag(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.5f) else Color.White,
        border = BorderStroke(0.8.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                color = color
            )
        }
    }
}
