package com.example.ui.screens.vehicle.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.WarningAmberLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceEntrySheet(
    sheetState: SheetState,
    currentOdometer: Long,
    vehicleName: String,
    initialServiceType: String = "سرویس روغن و فیلتر",
    onDismiss: () -> Unit,
    onSubmitMaintenance: (
        serviceType: String,
        cost: String,
        odometer: String,
        serviceCenter: String,
        parts: String,
        nextReminder: String
    ) -> Unit
) {
    var selectedType by remember { mutableStateOf(initialServiceType) }
    var costInput by remember { mutableStateOf("۱,۵۰۰,۰۰۰") }
    var odometerInput by remember { mutableStateOf(currentOdometer.toString()) }
    var serviceCenterInput by remember { mutableStateOf("") }
    var partsInput by remember { mutableStateOf("") }
    var nextReminderKm by remember { mutableStateOf("۵,۰۰۰ کیلومتر دیگر") }

    val commonServices = listOf(
        "سرویس روغن و فیلتر",
        "تعویض لنت ترمز",
        "تعویض باتری",
        "سرویس تسمه تایم",
        "تعویض لاستیک",
        "تعمیر جلوبندی",
        "شمع و وایر",
        "معاینه فنی"
    )

    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 44.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Build,
                        contentDescription = "سرویس",
                        tint = WarningAmberLight,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "ثبت سرویس و تعمیر ($vehicleName)",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_maintenance_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "بستن",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Common Service Type Selector
            Text(
                text = "نوع سرویس یا تعمیر",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(commonServices) { service ->
                    val isSelected = service == selectedType
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(RadiusMD))
                            .clickable { selectedType = service }
                            .testTag("service_chip_$service"),
                        shape = RoundedCornerShape(RadiusMD),
                        color = if (isSelected) WarningAmberLight.copy(alpha = if (isDark) 0.35f else 0.15f)
                        else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 0.8.dp,
                            color = if (isSelected) WarningAmberLight else Color.Transparent
                        )
                    ) {
                        Text(
                            text = service,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) WarningAmberLight else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Cost Input Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(RadiusLG),
                        ambientColor = WarningAmberLight.copy(alpha = 0.2f),
                        spotColor = WarningAmberLight.copy(alpha = 0.35f)
                    ),
                shape = RoundedCornerShape(RadiusLG),
                color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                border = BorderStroke(1.5.dp, WarningAmberLight.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "هزینه کل سرویس / فاکتور",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        OutlinedTextField(
                            value = costInput,
                            onValueChange = { costInput = it },
                            textStyle = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 30.sp,
                                color = WarningAmberLight,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .testTag("maint_cost_input")
                        )

                        Text(
                            text = "تومان",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }

            // Odometer & Service Center Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = odometerInput,
                    onValueChange = { odometerInput = it },
                    label = { Text("کیلومتر خودرو") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("maint_odometer_input"),
                    shape = RoundedCornerShape(RadiusMD),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = serviceCenterInput,
                    onValueChange = { serviceCenterInput = it },
                    label = { Text("نام تعمیرگاه / نمایندگی") },
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("maint_center_input"),
                    shape = RoundedCornerShape(RadiusMD),
                    singleLine = true
                )
            }

            // Parts Replaced
            OutlinedTextField(
                value = partsInput,
                onValueChange = { partsInput = it },
                label = { Text("قطعات تعویض شده (با ویرگول جدا کنید)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("maint_parts_input"),
                shape = RoundedCornerShape(RadiusMD),
                singleLine = true
            )

            // Next Service Reminder Setting
            OutlinedTextField(
                value = nextReminderKm,
                onValueChange = { nextReminderKm = it },
                label = { Text("تنظیم یادآوری دوره بعدی") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("maint_reminder_input"),
                shape = RoundedCornerShape(RadiusMD),
                singleLine = true
            )

            // Mock Receipt Upload Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RadiusMD))
                    .clickable { /* UI mock */ },
                shape = RoundedCornerShape(RadiusMD),
                color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddPhotoAlternate,
                        contentDescription = "پیوست تصویر فاکتور",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "پیوست عکس فاکتور یا کارت گارانتی (اختیاری)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Submit Button
            Button(
                onClick = {
                    onSubmitMaintenance(
                        selectedType,
                        costInput,
                        odometerInput,
                        serviceCenterInput.ifBlank { "تعمیرگاه تخصصی" },
                        partsInput,
                        nextReminderKm
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(RadiusMD),
                        ambientColor = WarningAmberLight.copy(alpha = 0.3f),
                        spotColor = WarningAmberLight.copy(alpha = 0.5f)
                    )
                    .testTag("submit_maintenance_btn"),
                shape = RoundedCornerShape(RadiusMD),
                colors = ButtonDefaults.buttonColors(containerColor = WarningAmberLight)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "ثبت",
                        tint = Color.White
                    )
                    Text(
                        text = "ثبت این سرویس",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
