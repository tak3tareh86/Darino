package com.example.ui.screens.vehicle.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Layered3DCard
import com.example.ui.screens.vehicle.components.InsuranceCard
import com.example.ui.screens.vehicle.components.MaintenanceTimeline
import com.example.ui.screens.vehicle.components.RecentVehicleExpensesSection
import com.example.ui.screens.vehicle.components.UpcomingServicesSection
import com.example.ui.screens.vehicle.components.VehicleHeroCard
import com.example.ui.screens.vehicle.model.MaintenanceTimelineRecord
import com.example.ui.screens.vehicle.model.VehicleData
import com.example.ui.screens.vehicle.model.VehicleExpenseItemData
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.WarningAmberLight

@Composable
fun VehicleDetailScreen(
    vehicle: VehicleData,
    initialTab: Int = 0,
    onBackClick: () -> Unit,
    onAddFuelClick: () -> Unit,
    onAddServiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val tabTitles = listOf("خلاصه و سوابق", "سرویس‌ها", "هزینه‌ها", "تعمیرات", "بیمه")
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("vehicle_detail_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "بازگشت",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = vehicle.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = { /* Menu */ },
                    modifier = Modifier.testTag("vehicle_detail_menu_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "بیشتر",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Detail Scrollable Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Top Section: Vehicle Hero Card (Image + Specs)
            item {
                VehicleHeroCard(
                    vehicle = vehicle,
                    onCardClick = {}
                )
            }

            // Tabs Selector
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(tabTitles) { index, title ->
                        val isSelected = index == selectedTab
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(RadiusMD))
                                .clickable { selectedTab = index }
                                .testTag("detail_tab_$index"),
                            shape = RoundedCornerShape(RadiusMD),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.35f else 0.15f)
                            else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 0.8.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Tab Content Switcher
            when (selectedTab) {
                0 -> { // Overview & History Tab
                    // 2. Summary Section (بخش خلاصه: هزینه این ماه / آخرین سرویس / کیلومتر فعلی)
                    item {
                        VehicleSummaryStatsRow(vehicle = vehicle)
                    }

                    // Specs & Info Card
                    item {
                        VehicleInformationCard(vehicle = vehicle)
                    }

                    // 3. History Section (بخش سوابق: کارت‌های تاریخچه)
                    item {
                        VehicleHistorySection(vehicle = vehicle)
                    }
                }

                1 -> { // Services Tab
                    item {
                        UpcomingServicesSection(services = vehicle.upcomingServices)
                    }
                }

                2 -> { // Expenses Tab
                    item {
                        RecentVehicleExpensesSection(expenses = vehicle.recentExpenses)
                    }
                }

                3 -> { // Repairs Tab
                    item {
                        MaintenanceTimeline(records = vehicle.timelineRecords)
                    }
                }

                4 -> { // Insurance Tab
                    item {
                        InsuranceCard(insurance = vehicle.insurance)
                    }
                    item {
                        VehicleDocumentsSection()
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }

        // Bottom Action Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onAddFuelClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("detail_add_fuel_btn"),
                    shape = RoundedCornerShape(RadiusMD),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimaryLight)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalGasStation,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "ثبت سوخت",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                            color = Color.White
                        )
                    }
                }

                Button(
                    onClick = onAddServiceClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("detail_add_service_btn"),
                    shape = RoundedCornerShape(RadiusMD),
                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmberLight)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Build,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "ثبت سرویس",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Summary Section (بخش خلاصه)
 * - هزینه این ماه
 * - آخرین سرویس
 * - کیلومتر فعلی
 */
@Composable
fun VehicleSummaryStatsRow(
    vehicle: VehicleData,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Monthly Expense Card
        SummaryMetricCard(
            title = "هزینه این ماه",
            value = vehicle.stats.monthlyExpenseFormatted,
            icon = Icons.Rounded.Payments,
            accentColor = Color(0xFF38BDF8),
            modifier = Modifier.weight(1f)
        )

        // Last Service Card
        SummaryMetricCard(
            title = "آخرین سرویس",
            value = vehicle.stats.lastServiceAgo,
            icon = Icons.Rounded.CalendarToday,
            accentColor = WarningAmberLight,
            modifier = Modifier.weight(1f)
        )

        // Current Odometer Card
        SummaryMetricCard(
            title = "کیلومتر فعلی",
            value = vehicle.odometerFormatted,
            icon = Icons.Rounded.Speed,
            accentColor = EmeraldPrimaryLight,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(RadiusMD)),
        shape = RoundedCornerShape(RadiusMD),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 11.5.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

/**
 * History Section (بخش سوابق)
 * Displaying history cards with Title, Date, Amount (e.g. تعویض روغن / تاریخ: 1405/06/20 / مبلغ: 850,000 تومان)
 */
@Composable
fun VehicleHistorySection(
    vehicle: VehicleData,
    modifier: Modifier = Modifier
) {
    Layered3DCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLG),
        backgroundColor = MaterialTheme.colorScheme.surface,
        elevation = 3.dp,
        contentPadding = PaddingValues(16.dp),
        testTag = "vehicle_history_section"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "بخش سوابق و تاریخچه",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (vehicle.timelineRecords.isEmpty() && vehicle.recentExpenses.isEmpty()) {
                Text(
                    text = "هنوز سوابقی ثبت نشده است.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Combine timeline records and expenses into clean history cards
                    vehicle.timelineRecords.forEach { record ->
                        HistoryCardItem(
                            title = record.title,
                            datePersian = record.datePersian,
                            costFormatted = record.costFormatted,
                            details = "کیلومتر: ${record.odometerKmFormatted} • ${record.serviceCenter}",
                            iconRes = record.iconRes,
                            accentColor = WarningAmberLight
                        )
                    }

                    vehicle.recentExpenses.forEach { expense ->
                        HistoryCardItem(
                            title = expense.title,
                            datePersian = expense.datePersian,
                            costFormatted = expense.amountFormatted.replace("−", ""),
                            details = "دسته‌بندی: ${expense.categoryTitle} • کارکرد: ${expense.odometerKmFormatted}",
                            iconRes = expense.iconRes,
                            accentColor = expense.accentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCardItem(
    title: String,
    datePersian: String,
    costFormatted: String,
    details: String,
    iconRes: Int,
    accentColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusMD),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
        border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = title,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.5.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "تاریخ: $datePersian",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "مبلغ",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = costFormatted,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                    color = accentColor
                )
            }
        }
    }
}

@Composable
fun VehicleInformationCard(
    vehicle: VehicleData,
    modifier: Modifier = Modifier
) {
    Layered3DCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusMD),
        backgroundColor = MaterialTheme.colorScheme.surface,
        elevation = 2.dp,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "اطلاعات شناسنامه خودرو",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            InfoRow(label = "برند و مدل", value = "${vehicle.brand} • ${vehicle.name}")
            InfoRow(label = "سال ساخت", value = vehicle.modelYear)
            InfoRow(label = "شماره پلاک", value = vehicle.licensePlate)
            InfoRow(label = "رنگ بدنه", value = vehicle.colorName)
            InfoRow(label = "میانگین مصرف سوخت", value = vehicle.stats.fuelEfficiencyAvg)
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun VehicleDocumentsSection(modifier: Modifier = Modifier) {
    Layered3DCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusMD),
        backgroundColor = MaterialTheme.colorScheme.surface,
        elevation = 2.dp,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "اسناد و مدارک خودرو",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "• کارت خودرو (اسکن شده)\n• برگه سبز سند مالکیت\n• بیمه‌نامه معتبر\n• گواهی آخرین معاینه فنی",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
