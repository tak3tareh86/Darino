package com.example.vehicle.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.rounded.ExpandMore
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
import com.example.vehicle.domain.VehicleMaintenanceCatalog

private data class ServiceOption(val title: String, val type: ServiceType)

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
        isReminderEnabled: Boolean,
        offsets: List<com.example.reminder.domain.PredefinedOffset>
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val todayJalali = remember { com.example.util.PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate() }
    var selectedType by remember { mutableStateOf(ServiceType.OIL_CHANGE) }
    var title by remember { mutableStateOf(ServiceType.OIL_CHANGE.title) }
    var date by remember { mutableStateOf(todayJalali) }
    var mileageText by remember { mutableStateOf(if (vehicle.currentMileage > 0) vehicle.currentMileage.toString() else "") }
    var costText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Custom service intervals must be entered explicitly; no universal default is assumed.
    var customIntervalKmText by remember { mutableStateOf("") }
    var customIntervalMonthsText by remember { mutableStateOf("") }

    // Next Reminder settings
    var isReminderEnabled by remember { mutableStateOf(true) }
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
    val calcKm = if (requiresManualInterval) {
        if (customIntervalKm > 0) currentKm.coerceAtLeast(0) + customIntervalKm else 0
    } else recommendedKm
    val calcDate = if (requiresManualInterval) {
        if (customIntervalMonths > 0) com.example.util.PersianCalendarHelper.addMonthsToPersianDate(date, customIntervalMonths) else ""
    } else recommendedDate

    var nextServiceDateText by remember { mutableStateOf(calcDate) }
    var isNextDateManuallyEdited by remember { mutableStateOf(false) }

    LaunchedEffect(calcDate) {
        if (!isNextDateManuallyEdited) {
            nextServiceDateText = calcDate
        }
    }

    val serviceTypeOptions: List<ServiceOption> = remember {
        listOf(
            ServiceOption("تعویض روغن موتور", ServiceType.OIL_CHANGE),
            ServiceOption("تعویض فیلترها", ServiceType.FILTERS),
            ServiceOption("تعویض لنت ترمز", ServiceType.BRAKE_PADS),
            ServiceOption("تعویض لاستیک ها", ServiceType.TIRE_CHANGE),
            ServiceOption("تعویض باتری", ServiceType.BATTERY),
            ServiceOption("تعویض روغن گیربکس", ServiceType.TRANSMISSION_OIL_CHANGE),
            ServiceOption("تعویض روغن هیدرولیک فرمان", ServiceType.HYDRAULIC_STEERING_OIL_CHANGE),
            ServiceOption("تعویض روغن ترمز", ServiceType.BRAKE_FLUID_CHANGE),
            ServiceOption("تعویض تسمه تایم", ServiceType.TIMING_BELT_CHANGE),
            ServiceOption("تعویض تسمه دینام", ServiceType.ALTERNATOR_BELT_CHANGE),
            ServiceOption("سرویس سفارشی", ServiceType.CUSTOM)
        )
    }
    var showServiceTypeDialog by remember { mutableStateOf(false) }

    // Notification offset options (7 days, 3 days, 1 day before due date)
    var remind1DayBefore by remember { mutableStateOf(true) }
    var remind3DaysBefore by remember { mutableStateOf(true) }
    var remind1WeekBefore by remember { mutableStateOf(true) }

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

            // Service Type Selector
            Text(
                text = "نوع سرویس انجام شده",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedCard(
                onClick = { showServiceTypeDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Soft3DIcon(
                            imageRes = selectedType.iconRes,
                            contentDescription = "نوع سرویس",
                            size = 28.dp,
                            accentColor = MaterialTheme.colorScheme.primary
                        )
                        val matchedOpt = serviceTypeOptions.find { it.type == selectedType }
                        Text(
                            text = matchedOpt?.title ?: selectedType.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = "انتخاب نوع سرویس",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (showServiceTypeDialog) {
                AlertDialog(
                    onDismissRequest = { showServiceTypeDialog = false },
                    title = { Text("انتخاب نوع سرویس", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                    text = {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(serviceTypeOptions) { item ->
                                val displayName = item.title
                                val type = item.type
                                val isSelected = selectedType == type
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedType = type
                                            title = displayName
                                            customIntervalKmText = ""
                                            customIntervalMonthsText = ""
                                            showServiceTypeDialog = false
                                        },
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Soft3DIcon(
                                            imageRes = type.iconRes,
                                            contentDescription = displayName,
                                            size = 24.dp,
                                            accentColor = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = displayName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showServiceTypeDialog = false }) {
                            Text("بستن")
                        }
                    }
                )
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
                    PersianDateInputField(
                        value = nextServiceDateText,
                        onValueChange = {
                            nextServiceDateText = it
                            isNextDateManuallyEdited = true
                        },
                        label = "تاریخ سرویس بعدی (شمسی)",
                        placeholder = "۱۴۰۵/۰۷/۱۵",
                        dialogTitle = "انتخاب تاریخ سرویس بعدی",
                        testTag = "next_service_date_input"
                    )

                    if (isReminderEnabled) {
                        Text(
                            text = "زمان‌بندی ارسال اعلان‌های یادآور پیش از سررسید:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = remind1WeekBefore,
                                onClick = { remind1WeekBefore = !remind1WeekBefore },
                                label = { Text("۷ روز قبل", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = remind3DaysBefore,
                                onClick = { remind3DaysBefore = !remind3DaysBefore },
                                label = { Text("۳ روز قبل", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = remind1DayBefore,
                                onClick = { remind1DayBefore = !remind1DayBefore },
                                label = { Text("۱ روز قبل", fontSize = 11.sp) }
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
                                append("تاریخ پیشنهادی: ")
                                append(IranianPhoneUtils.convertDigitsToPersian(calcDate))
                            }
                            append(" ثبت خواهد شد.")
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

            // Submit Button
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val cost = IranianAmountUtils.parseAmountToLong(costText)
                        val mileage = IranianPhoneUtils.convertDigitsToEnglish(mileageText).filter { it.isDigit() }.toIntOrNull() ?: vehicle.currentMileage
                        val selectedOffsets = buildList {
                            if (remind1WeekBefore) add(com.example.reminder.domain.PredefinedOffset.BEFORE_7_DAYS)
                            if (remind3DaysBefore) add(com.example.reminder.domain.PredefinedOffset.BEFORE_3_DAYS)
                            if (remind1DayBefore) add(com.example.reminder.domain.PredefinedOffset.BEFORE_1_DAY)
                            add(com.example.reminder.domain.PredefinedOffset.AT_TIME)
                        }
                        onAddService(
                            title,
                            selectedType,
                            date,
                            mileage,
                            cost,
                            description,
                            if (nextServiceDateText.isNotBlank()) nextServiceDateText else null,
                            if (calcKm > 0) calcKm else null,
                            isReminderEnabled,
                            selectedOffsets
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
