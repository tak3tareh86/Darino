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
import com.example.vehicle.data.VehicleServiceEntity
import com.example.vehicle.domain.VehicleMaintenanceCatalog

/**
 * Bottom sheet to record or edit a vehicle service and its next reminder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServiceSheet(
    vehicle: VehicleEntity,
    initialService: VehicleServiceEntity? = null,
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
    onUpdateService: ((
        serviceId: String,
        title: String,
        serviceType: ServiceType,
        date: String,
        mileage: Int,
        cost: Long,
        description: String,
        nextReminderDate: String?,
        nextReminderMileage: Int?,
        isReminderEnabled: Boolean
    ) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val todayJalali = remember { com.example.util.PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate() }
    var selectedType by remember(initialService?.id) { mutableStateOf(initialService?.serviceType ?: ServiceType.OIL_CHANGE) }
    var title by remember(initialService?.id) { mutableStateOf(initialService?.title ?: ServiceType.OIL_CHANGE.title) }
    var date by remember(initialService?.id) { mutableStateOf(initialService?.date ?: todayJalali) }
    var mileageText by remember(initialService?.id) {
        mutableStateOf(
            initialService?.mileage?.toString()
                ?: vehicle.currentMileage.takeIf { it > 0 }?.toString().orEmpty()
        )
    }
    var costText by remember(initialService?.id) { mutableStateOf(initialService?.cost?.toString() ?: "") }
    var description by remember(initialService?.id) { mutableStateOf(initialService?.description ?: "") }

    // Custom service intervals must be entered explicitly; no universal default is assumed.
    var customIntervalKmText by remember(initialService?.id) {
        mutableStateOf(
            initialService?.let { service ->
                val resolved = VehicleMaintenanceCatalog.resolve(vehicle, service.serviceType)
                if (resolved.intervalKm == 0 && service.nextReminderMileage != null) {
                    (service.nextReminderMileage - service.mileage).coerceAtLeast(0).toString()
                } else ""
            } ?: ""
        )
    }
    var customIntervalMonthsText by remember(initialService?.id) { mutableStateOf("") }

    // Next Reminder settings
    var isReminderEnabled by remember(initialService?.id) { mutableStateOf(initialService?.isReminderEnabled ?: true) }
    val currentKm = mileageText.toIntOrNull() ?: vehicle.currentMileage
    val interval = remember(vehicle.brand, vehicle.model, vehicle.year, selectedType) {
        VehicleMaintenanceCatalog.resolve(vehicle, selectedType)
    }
    val recommendedKm = if (interval.intervalKm > 0) currentKm.coerceAtLeast(0) + interval.intervalKm else 0
    val recommendedDate = if (interval.intervalMonths > 0) {
        com.example.util.PersianCalendarHelper.addMonthsToPersianDate(date, interval.intervalMonths)
    } else ""
    val customIntervalKm = IranianPhoneUtils.convertDigitsToEnglish(customIntervalKmText).toIntOrNull()?.coerceAtLeast(0) ?: 0
    val customIntervalMonths = IranianPhoneUtils.convertDigitsToEnglish(customIntervalMonthsText).toIntOrNull()?.coerceAtLeast(0) ?: 0
    val requiresManualInterval = selectedType == ServiceType.CUSTOM || !interval.isUsable
    val preserveExistingReminder = initialService != null &&
        selectedType == initialService.serviceType &&
        date == initialService.date &&
        currentKm == initialService.mileage
    val calculatedKm = if (requiresManualInterval) {
        if (customIntervalKm > 0) currentKm.coerceAtLeast(0) + customIntervalKm else 0
    } else recommendedKm
    val calculatedDate = if (requiresManualInterval) {
        if (customIntervalMonths > 0) com.example.util.PersianCalendarHelper.addMonthsToPersianDate(date, customIntervalMonths) else ""
    } else recommendedDate
    val calcKm = if (preserveExistingReminder) initialService?.nextReminderMileage ?: calculatedKm else calculatedKm
    val calcDate = if (preserveExistingReminder) initialService?.nextReminderDate ?: calculatedDate else calculatedDate

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

            Text(
                text = "برای دیدن انواع دیگر سرویس، فهرست را افقی حرکت دهید.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            customIntervalKmText = ""
                            customIntervalMonthsText = ""
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

                OutlinedTextField(
                    value = IranianPhoneUtils.convertDigitsToPersian(mileageText),
                    onValueChange = {
                        val digits = IranianPhoneUtils.convertDigitsToEnglish(it).filter { ch -> ch in '0'..'9' }
                        mileageText = digits
                    },
                    label = { Text("کیلومتر خودرو") },
                    placeholder = { Text("۴۵۰۰۰") },
                    modifier = Modifier.weight(0.9f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
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
                        if (requiresManualInterval) {
                            Text(
                                text = if (selectedType == ServiceType.CUSTOM) "فاصلهٔ یادآوری سرویس سفارشی را مطابق دفترچهٔ خودرو یا نظر متخصص وارد کنید." else "برای این مدل و سرویس، بازهٔ اختصاصیِ مستند در بانک دارینو موجود نیست. فاصله را از دفترچهٔ همان خودرو وارد کنید؛ از بازهٔ خودروی دیگری استفاده نکنید.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = IranianPhoneUtils.convertDigitsToPersian(customIntervalKmText),
                                    onValueChange = { customIntervalKmText = IranianPhoneUtils.convertDigitsToEnglish(it).filter(Char::isDigit) },
                                    label = { Text("فاصله (کیلومتر)") },
                                    placeholder = { Text("مثلاً ۵۰۰۰") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = IranianPhoneUtils.convertDigitsToPersian(customIntervalMonthsText),
                                    onValueChange = { customIntervalMonthsText = IranianPhoneUtils.convertDigitsToEnglish(it).filter(Char::isDigit) },
                                    label = { Text("فاصله (ماه)") },
                                    placeholder = { Text("مثلاً ۶") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                        }
                        Text(
                            text = buildString {
                                if (calcKm > 0) {
                                    append("یادآوری کیلومتری: ")
                                    append(IranianPhoneUtils.convertDigitsToPersian(calcKm.toString()))
                                    append(" کیلومتر")
                                }
                                if (calcDate.isNotBlank()) {
                                    if (isNotEmpty()) append(" یا ")
                                    append("تاریخ تقریبی: ")
                                    append(IranianPhoneUtils.convertDigitsToPersian(calcDate))
                                }
                                append(" فعال خواهد شد.")
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "مبنای بازه: " + interval.source + "\nاگر دفترچهٔ خودروی شما بازهٔ دیگری دارد، همان ملاک نهایی است.",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        val reminderDate = if (isReminderEnabled && calcDate.isNotBlank()) calcDate else null
                        val reminderMileage = if (isReminderEnabled && calcKm > 0) calcKm else null
                        if (initialService != null && onUpdateService != null) {
                            onUpdateService(
                                initialService.id,
                                title,
                                selectedType,
                                date,
                                mileage,
                                cost,
                                description,
                                reminderDate,
                                reminderMileage,
                                isReminderEnabled
                            )
                        } else {
                            onAddService(
                                title,
                                selectedType,
                                date,
                                mileage,
                                cost,
                                description,
                                reminderDate,
                                reminderMileage,
                                isReminderEnabled
                            )
                        }
                    }
                },
                enabled = title.isNotBlank() && (!isReminderEnabled || calcKm > 0 || calcDate.isNotBlank()),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag(if (initialService == null) "submit_add_service_btn" else "submit_edit_service_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (initialService == null) "ثبت سرویس در پرونده خودرو" else "ذخیره تغییرات سرویس",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
