package com.example.ui.screens.settings.subviews

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.screens.settings.components.SettingsHeader
import com.example.ui.screens.settings.components.SettingsSectionCard
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM

@Composable
fun ReminderPreferencesScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedInstallmentDays by remember { mutableIntStateOf(7) }
    var selectedVehicleKm by remember { mutableIntStateOf(500) }
    var selectedReminderTime by remember { mutableStateOf("۰۹:۰۰ صبح") }

    val installmentDayOptions = listOf(
        1 to "۱ روز قبل",
        3 to "۳ روز قبل",
        7 to "۷ روز قبل (پیش‌فرض)",
        10 to "۱۰ روز قبل"
    )

    val vehicleKmOptions = listOf(
        200 to "۲۰۰ کیلومتر قبل",
        500 to "۵۰۰ کیلومتر قبل (پیش‌فرض)",
        1000 to "۱۰۰۰ کیلومتر قبل"
    )

    val reminderTimeOptions = listOf(
        "۰۸:۰۰ صبح",
        "۰۹:۰۰ صبح",
        "۱۲:۰۰ ظهر",
        "۱۸:۰۰ عصر",
        "۲۱:۰۰ شب"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SettingsHeader(
                title = "ترجیحات زمان‌بندی یادآوری",
                subtitle = "تنظیم دقیق روزها، ساعات و هشدارهای کیلومتری",
                showBack = true,
                showSearch = false,
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Default Reminder Time
            item {
                SettingsSectionCard(
                    title = "ساعت ارسال اعلان‌های روزانه",
                    badge = selectedReminderTime
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "تمامی یادآوری‌های روزانه در این ساعت ارسال خواهند شد:",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            reminderTimeOptions.take(3).forEach { time ->
                                val isSelected = time == selectedReminderTime
                                ReminderSegmentChip(
                                    label = time,
                                    isSelected = isSelected,
                                    onClick = { selectedReminderTime = time },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            reminderTimeOptions.drop(3).forEach { time ->
                                val isSelected = time == selectedReminderTime
                                ReminderSegmentChip(
                                    label = time,
                                    isSelected = isSelected,
                                    onClick = { selectedReminderTime = time },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Installment Due Alert
            item {
                SettingsSectionCard(
                    title = "موعد یادآوری اقساط و چک‌ها",
                    badge = "$selectedInstallmentDays روز قبل"
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "چند روز پیش از تاریخ سررسید قسط، پیام هشدار نمایش داده شود؟",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        installmentDayOptions.forEach { (days, label) ->
                            val isSelected = days == selectedInstallmentDays
                            ReminderOptionRow(
                                title = label,
                                subtitle = if (days == 7) "بهترین زمان برای برنامه‌ریزی موجودی حساب" else null,
                                isSelected = isSelected,
                                onClick = { selectedInstallmentDays = days }
                            )
                        }
                    }
                }
            }

            // Vehicle Service Km Alert
            item {
                SettingsSectionCard(
                    title = "موعد یادآوری سرویس خودرو (بر اساس کارکرد)",
                    badge = "$selectedVehicleKm کیلومتر قبل"
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "چند کیلومتر مانده به موعد سرویس بعدی (روغن، فیلترها یا تسمه)، اعلان ارسال شود؟",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        vehicleKmOptions.forEach { (km, label) ->
                            val isSelected = km == selectedVehicleKm
                            ReminderOptionRow(
                                title = label,
                                subtitle = if (km == 500) "فرصت کافی برای هماهنگی و مراجعه به تعمیرگاه" else null,
                                isSelected = isSelected,
                                onClick = { selectedVehicleKm = km }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ReminderSegmentChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(RadiusSM))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.5f else 0.7f)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp
            ),
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ReminderOptionRow(
    title: String,
    subtitle: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMD))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(RadiusMD),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.16f else 0.10f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.3f else 0.4f)
        },
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.5.sp
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
