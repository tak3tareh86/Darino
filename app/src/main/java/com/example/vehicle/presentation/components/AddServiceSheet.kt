package com.example.vehicle.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.PersianAmountInputField
import com.example.ui.components.PersianDateInputField
import com.example.ui.components.Soft3DIcon
import com.example.util.IranianAmountUtils
import com.example.util.IranianPhoneUtils
import com.example.vehicle.data.ServiceType
import com.example.vehicle.data.VehicleEntity
import com.example.vehicle.domain.VehicleManager

/**
 * Bottom sheet to record a vehicle service and schedule next smart reminder
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServiceSheet(
    vehicle: VehicleEntity,
    onAddService: (
        title: String,
        serviceType: ServiceType,
        date: String,
        mileage: Int,
        cost: Long,
        description: String,
        nextReminderDate: String?,
        nextReminderMileage: Int?,
        isReminderEnabled: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(ServiceType.OIL_CHANGE) }
    var title by remember { mutableStateOf(ServiceType.OIL_CHANGE.title) }
    var date by remember { mutableStateOf("1405/06/10") }
    var mileageText by remember { mutableStateOf(vehicle.currentMileage.toString()) }
    var costText by remember { mutableStateOf("850000") }
    var description by remember { mutableStateOf("") }

    // Next Reminder settings
    var isReminderEnabled by remember { mutableStateOf(true) }
    var reminderIntervalKm by remember { mutableIntStateOf(5000) }
    var reminderIntervalMonths by remember { mutableIntStateOf(6) }

    val currentKm = mileageText.toIntOrNull() ?: vehicle.currentMileage
    val nextKm = currentKm + reminderIntervalKm
    val (calcKm, calcDate) = VehicleManager.calculateRecommendedReminder(
        serviceType = selectedType,
        currentMileage = currentKm,
        currentShamsiDate = date
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("add_service_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
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
                        imageRes = selectedType.iconRes,
                        contentDescription = "ثبت سرویس دوره‌ای",
                        size = 36.dp,
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "ثبت سرویس دوره‌ای خودرو",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "برای خودروی ${vehicle.brand} ${vehicle.model}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "بستن",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            // Service Type Selector Chips
            Text(
                text = "نوع سرویس انجام شده",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ServiceType.values()) { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = {
                            selectedType = type
                            title = type.title
                            reminderIntervalKm = type.defaultIntervalKm
                            reminderIntervalMonths = type.defaultIntervalMonths
                        },
                        label = { Text(type.title, fontSize = 11.sp) }
                    )
                }
            }

            // Title & Cost
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان سرویس") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PersianAmountInputField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = "هزینه سرویس",
                    unitLabel = "تومان",
                    placeholder = "مثال: ۸۵۰,۰۰۰",
                    showWordsPreview = false,
                    modifier = Modifier.weight(1.2f),
                    testTag = "service_cost_input"
                )

                Column(modifier = Modifier.weight(0.9f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "کیلومتر خودرو",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = IranianPhoneUtils.convertDigitsToPersian(mileageText),
                        onValueChange = {
                            val digits = IranianPhoneUtils.convertDigitsToEnglish(it).filter { ch -> ch in '0'..'9' }
                            mileageText = digits
                        },
                        placeholder = { Text("45000") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            // Date & Description
            PersianDateInputField(
                value = date,
                onValueChange = { date = it },
                label = "تاریخ انجام سرویس (شمسی)",
                placeholder = "۱۴۰۴/۰۷/۱۵",
                dialogTitle = "انتخاب تاریخ انجام سرویس",
                testTag = "service_date_input"
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("توضیحات و برند قطعات مصرفی") },
                placeholder = { Text("مثلاً: روغن بهران سوپر رانا + فیلتر روغن اصلی") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )

            // Smart Next Reminder Box (Sections 6 & 7)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "یادآوری هوشمند سرویس بعدی",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Switch(
                            checked = isReminderEnabled,
                            onCheckedChange = { isReminderEnabled = it }
                        )
                    }

                    if (isReminderEnabled) {
                        Text(
                            text = "یادآوری در کیلومتر ${IranianPhoneUtils.convertDigitsToPersian(calcKm.toString())} (معادل ${IranianPhoneUtils.convertDigitsToPersian(reminderIntervalKm.toString())} کیلومتر بعد) یا تاریخ ${IranianPhoneUtils.convertDigitsToPersian(calcDate)} فعال خواهد شد.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Submit Button
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val cost = IranianAmountUtils.parseAmountToLong(costText)
                        val mileage = IranianPhoneUtils.convertDigitsToEnglish(mileageText).filter { it.isDigit() }.toIntOrNull() ?: vehicle.currentMileage
                        onAddService(
                            title,
                            selectedType,
                            date,
                            mileage,
                            cost,
                            description,
                            if (isReminderEnabled) calcDate else null,
                            if (isReminderEnabled) calcKm else null,
                            isReminderEnabled
                        )
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_add_service_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ثبت سرویس در پرونده خودرو",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
