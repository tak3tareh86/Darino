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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.HomeRepairService
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.Layered3DCard
import com.example.ui.screens.vehicle.model.MaintenanceTimelineRecord
import com.example.ui.screens.vehicle.model.UpcomingServiceItem
import com.example.ui.screens.vehicle.model.VehicleData
import com.example.ui.screens.vehicle.model.VehicleExpenseItemData
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.WarningAmberLight

/**
 * 1) SECTION الف: سرویس‌ها
 * شامل: تعویض روغن، فیلترها، سرویس دوره‌ای، موارد مصرفی خودرو
 */
@Composable
fun SectionServicesCard(
    vehicle: VehicleData,
    modifier: Modifier = Modifier,
    onAddServiceClick: () -> Unit = {},
    onSeeAllServicesClick: () -> Unit = {}
) {
    Layered3DCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLG),
        backgroundColor = MaterialTheme.colorScheme.surface,
        elevation = 3.dp,
        contentPadding = PaddingValues(16.dp),
        testTag = "section_services_card"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(WarningAmberLight.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Build,
                            contentDescription = "سرویس‌ها",
                            tint = WarningAmberLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "الف) سرویس‌ها",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "تعویض روغن، فیلترها و موارد مصرفی",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onAddServiceClick,
                    shape = RoundedCornerShape(RadiusMD),
                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmberLight),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "ثبت سرویس",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            // Quick Consumables Status Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RadiusMD))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ConsumableStatusPill(label = "روغن موتور", status = "۵۰۰ ک‌م مانده", color = WarningAmberLight)
                ConsumableStatusPill(label = "فیلتر هوا", status = "سرویس‌شده", color = EmeraldPrimaryLight)
                ConsumableStatusPill(label = "فیلتر روغن", status = "سرویس‌شده", color = EmeraldPrimaryLight)
                ConsumableStatusPill(label = "تسمه‌تایم", status = "مرتب", color = InfoIndigoLight)
            }

            // Upcoming list items
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                vehicle.upcomingServices.take(2).forEach { service ->
                    UpcomingServiceMiniRow(service = service)
                }
            }
        }
    }
}

@Composable
private fun ConsumableStatusPill(
    label: String,
    status: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = color
        )
    }
}

@Composable
private fun UpcomingServiceMiniRow(service: UpcomingServiceItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusMD),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
        border = BorderStroke(0.8.dp, service.accentColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(id = service.iconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = service.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Surface(
                shape = CircleShape,
                color = service.accentColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = service.dueText,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = service.accentColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * 2) SECTION ب: هزینه‌ها
 * شامل: لیست هزینه‌های ثبت شده، مبلغ آخرین هزینه، مجموع هزینه‌ها
 */
@Composable
fun SectionExpensesCard(
    vehicle: VehicleData,
    modifier: Modifier = Modifier,
    onAddExpenseClick: () -> Unit = {},
    onSeeAllExpensesClick: () -> Unit = {}
) {
    val totalExpense = vehicle.stats.yearlyExpenseFormatted
    val lastExpense = vehicle.recentExpenses.firstOrNull()?.amountFormatted
        ?: vehicle.timelineRecords.firstOrNull()?.costFormatted
        ?: "۰ تومان"

    Layered3DCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLG),
        backgroundColor = MaterialTheme.colorScheme.surface,
        elevation = 3.dp,
        contentPadding = PaddingValues(16.dp),
        testTag = "section_expenses_card"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimaryLight.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Receipt,
                            contentDescription = "هزینه‌ها",
                            tint = EmeraldPrimaryLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "ب) هزینه‌ها",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "خلاصه و لیست کلیه هزینه‌های ثبت شده",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onAddExpenseClick,
                    shape = RoundedCornerShape(RadiusMD),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimaryLight),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "ثبت هزینه",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            // Amounts Overview Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RadiusMD))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "مجموع هزینه‌ها",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = totalExpense,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        color = Color(0xFF38BDF8)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(width = 1.dp, height = 30.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "مبلغ آخرین هزینه",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = lastExpense,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        color = Color(0xFFFBBF24)
                    )
                }
            }

            // Recent Expenses List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                vehicle.recentExpenses.take(3).forEach { expense ->
                    ExpenseMiniRow(expense = expense)
                }
            }
        }
    }
}

@Composable
private fun ExpenseMiniRow(expense: VehicleExpenseItemData) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusMD),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
        border = BorderStroke(0.8.dp, expense.accentColor.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(id = expense.iconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Column {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${expense.categoryTitle} • ${expense.datePersian}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = expense.amountFormatted,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.sp),
                color = expense.accentColor
            )
        }
    }
}

/**
 * 3) SECTION ج: تعمیرات
 * برای ثبت و نمایش: تعمیرات انجام شده، قطعات تعویض شده، تاریخ تعمیر
 */
@Composable
fun SectionRepairsCard(
    vehicle: VehicleData,
    modifier: Modifier = Modifier,
    onAddRepairClick: () -> Unit = {},
    onSeeAllRepairsClick: () -> Unit = {}
) {
    Layered3DCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLG),
        backgroundColor = MaterialTheme.colorScheme.surface,
        elevation = 3.dp,
        contentPadding = PaddingValues(16.dp),
        testTag = "section_repairs_card"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.HomeRepairService,
                            contentDescription = "تعمیرات",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "ج) تعمیرات",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "تعمیرات انجام شده و قطعات تعویض شده",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onAddRepairClick,
                    shape = RoundedCornerShape(RadiusMD),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "ثبت تعمیرات",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            // Timeline / Repair list
            if (vehicle.timelineRecords.isEmpty()) {
                Text(
                    text = "هنوز سابقه تعمیری برای این خودرو ثبت نشده است.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    vehicle.timelineRecords.take(2).forEach { record ->
                        RepairRecordMiniRow(record = record)
                    }
                }
            }
        }
    }
}

@Composable
private fun RepairRecordMiniRow(record: MaintenanceTimelineRecord) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusMD),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
        border = BorderStroke(0.8.dp, Color(0xFFEF4444).copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = record.costFormatted,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.sp),
                    color = Color(0xFFF87171)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تاریخ: ${record.datePersian} • ${record.odometerKmFormatted}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = record.serviceCenter,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Parts changed tags
            if (record.partsChanged.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    record.partsChanged.take(2).forEach { part ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "• $part",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 4) SECTION د: مدارک خودرو
 * برای: بیمه، معاینه فنی، مدارک مرتبط
 */
@Composable
fun SectionDocumentsCard(
    vehicle: VehicleData,
    modifier: Modifier = Modifier,
    onAddInsuranceClick: () -> Unit = {}
) {
    val insurance = vehicle.insurance

    Layered3DCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLG),
        backgroundColor = MaterialTheme.colorScheme.surface,
        elevation = 3.dp,
        contentPadding = PaddingValues(16.dp),
        testTag = "section_documents_card"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(InfoIndigoLight.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Description,
                            contentDescription = "مدارک خودرو",
                            tint = InfoIndigoLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "د) مدارک خودرو",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "وضعیت بیمه‌نامه‌ها، معاینه فنی و اسناد",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onAddInsuranceClick,
                    shape = RoundedCornerShape(RadiusMD),
                    colors = ButtonDefaults.buttonColors(containerColor = InfoIndigoLight),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "ثبت بیمه",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            // Insurance Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(RadiusMD),
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                border = BorderStroke(0.8.dp, InfoIndigoLight.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = null,
                                tint = InfoIndigoLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = insurance.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.5.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = if (insurance.isExpiringSoon) Color(0xFFF59E0B).copy(alpha = 0.15f) else EmeraldPrimaryLight.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = insurance.statusText,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (insurance.isExpiringSoon) Color(0xFFF59E0B) else EmeraldPrimaryLight,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "شرکت بیمه‌گر: ${insurance.provider} • بیمه‌نامه: ${insurance.policyNumber}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Technical Inspection & Documents Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(RadiusMD)),
                    shape = RoundedCornerShape(RadiusMD),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "معاینه فنی",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "معتبر (۱۵ روز مانده)",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = EmeraldPrimaryLight
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(RadiusMD)),
                    shape = RoundedCornerShape(RadiusMD),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "کارت & سند مالکیت",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "ثبت شده و تکمیل",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
