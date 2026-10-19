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
    onDismiss: () -> Unit,
    existingService: VehicleServiceEntity? = null,
    onUpdateService: ((String, String, ServiceType, String, Int, Long, String, String?, Int?, Boolean) -> Unit)? = null
) {
    val todayJalali = remember { com.example.util.PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate() }
    var selectedType by remember(vehicle, existingService) { mutableStateOf(existingService?.serviceType ?: ServiceType.OIL_CHANGE) }
    var title by remember(vehicle, existingService) { mutableStateOf(existingService?.title ?: ServiceType.OIL_CHANGE.title) }
    var date by remember(vehicle, existingService) { mutableStateOf(existingService?.date ?: todayJalali) }
    var mileageText by remember(vehicle, existingService) { mutableStateOf((existingService?.mileage ?: vehicle.currentMileage).takeIf { it > 0 }?.toString() ?: "") }
    var costText by remember(vehicle, existingService) { mutableStateOf(existingService?.cost?.toString() ?: "") }
    var description by remember(vehicle, existingService) { mutableStateOf(existingService?.description ?: "") }

    // When editing, retain the service's existing interval instead of silently replacing it
    // with a different catalog recommendation.
    val savedIntervalKm = existingService?.let { service ->
        service.nextReminderMileage?.minus(service.mileage)?.coerceAtLeast(0) ?: 0
    } ?: 0
    val savedIntervalMonths = existingService?.let { service ->
        val start = IranianPhoneUtils.convertDigitsToEnglish(service.date).filter(Char::isDigit)
        val due = IranianPhoneUtils.convertDigitsToEnglish(service.nextReminderDate.orEmpty()).filter(Char::isDigit)
        if (start.length >= 6 && due.length >= 6) {
            (((due.take(4).toInt() - start.take(4).toInt()) * 12) +
                (due.substring(4, 6).toInt() - start.substring(4, 6).toInt())).coerceAtLeast(0)
        } else 0
    } ?: 0
    var customIntervalKmText by remember(vehicle, existingService) { mutableStateOf(savedIntervalKm.takeIf { it > 0 }?.toString() ?: "") }
    var customIntervalMonthsText by remember(vehicle, existingService) { mutableStateOf(savedIntervalMonths.takeIf { it > 0 }?.toString() ?: "") }

    var isReminderEnabled by remember(vehicle, existingService) { mutableStateOf(existingService?.isReminderEnabled ?: true) }
    val currentKm = mileageText.toIntOrNull() ?: vehicle.currentMileage
    val interval = remember(vehicle.brand, vehicle.model, vehicle.year, vehicle.engine, vehicle.transmission, selectedType) {
        VehicleMaintenanceCatalog.resolve(vehicle, selectedType)
    }
    val recommendedKm = when {
        existingService != null && currentKm == existingService.mileage && existingService.nextReminderMileage != null -> existingService.nextReminderMileage
        existingService != null && savedIntervalKm > 0 -> currentKm.coerceAtLeast(0) + savedIntervalKm
        interval.intervalKm > 0 -> currentKm.coerceAtLeast(0) + interval.intervalKm
        else -> 0
    }
    val recommendedDate = when {
        existingService != null && date == existingService.date && !existingService.nextReminderDate.isNullOrBlank() -> existingService.nextReminderDate!!
        existingService != null && savedIntervalMonths > 0 -> com.example.util.PersianCalendarHelper.addMonthsToPersianDate(date, savedIntervalMonths)
        interval.intervalMonths > 0 -> com.example.util.PersianCalendarHelper.addMonthsToPersianDate(date, interval.intervalMonths)
        else -> ""
    }
    val customIntervalKm = IranianPhoneUtils.convertDigitsToEnglish(customIntervalKmText).toIntOrNull()?.coerceAtLeast(0) ?: 0
    val customIntervalMonths = IranianPhoneUtils.convertDigitsToEnglish(customIntervalMonthsText).toIntOrNull()?.coerceAtLeast(0) ?: 0
    val requiresManualInterval = selectedType == ServiceType.CUSTOM || !interval.isUsable
    val calcKm = if (requiresManualInterval) {
        if (customIntervalKm > 0) currentKm.coerceAtLeast(0) + customIntervalKm else 0
    } else recommendedKm
    val calcDate = if (requiresManualInterval) {
        if (customIntervalMonths > 0) com.example.util.PersianCalendarHelper.addMonthsToPersianDate(date, customIntervalMonths) else ""
    } else recommendedDate

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
                        val dueDate = if (isReminderEnabled && calcDate.isNotBlank()) calcDate else null
                        val dueKm = if (isReminderEnabled && calcKm > 0) calcKm else null
                        if (existingService != null && onUpdateService != null) {
                            onUpdateService(existingService.id, title.trim(), selectedType, date, mileage, cost, description.trim(), dueDate, dueKm, isReminderEnabled)
                        } else {
                            onAddService(title.trim(), selectedType, date, mileage, cost, description.trim(), dueDate, dueKm, isReminderEnabled)
                        }
                    }
                },
                enabled = title.isNotBlank() && (!isReminderEnabled || calcKm > 0 || calcDate.isNotBlank()),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_add_service_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (existingService == null) "ثبت سرویس در پرونده خودرو" else "ذخیره تغییرات سرویس",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
