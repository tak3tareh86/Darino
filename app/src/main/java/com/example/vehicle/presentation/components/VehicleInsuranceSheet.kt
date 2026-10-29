package com.example.vehicle.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.PersianAmountInputField
import com.example.ui.components.PersianDateInputField
import com.example.ui.components.Soft3DIcon
import com.example.util.IranianAmountUtils
import com.example.util.IranianPhoneUtils
import com.example.vehicle.data.VehicleEntity
import com.example.vehicle.data.VehicleInsuranceEntity
import com.example.vehicle.data.VehicleInspectionEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleInsuranceSheet(
    vehicle: VehicleEntity,
    onSaveInsurance: (company: String, type: String, startDate: String, endDate: String, amount: Long, policyNumber: String, reminderEnabled: Boolean, reminderTime: String, reminderDays: List<Int>, recordId: String?) -> Unit,
    onSaveInspection: (lastInspectionDate: String, expiryDate: String, cost: Long, status: String, centerName: String, reminderEnabled: Boolean, reminderTime: String, reminderDays: List<Int>, recordId: String?) -> Unit,
    editingInsurance: VehicleInsuranceEntity? = null,
    editingInspection: VehicleInspectionEntity? = null,
    onDismiss: () -> Unit
) {
    val todayPdt = remember { com.example.util.PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()) }
    val todayJalali = remember { todayPdt.toFormattedDate() }
    val nextYearJalali = remember { com.example.util.IranianDateUtils.createFormattedDate(todayPdt.year + 1, todayPdt.month, todayPdt.day) }
    var selectedSection by rememberSaveable(editingInsurance?.id, editingInspection?.id) {
        mutableIntStateOf(
            when {
                editingInspection != null -> 2
                editingInsurance?.type?.contains("بدنه") == true -> 1
                else -> 0
            }
        )
    }
    var insuranceCompany by remember(editingInsurance) { mutableStateOf(editingInsurance?.company ?: "بیمه ایران") }
    var startDate by remember(editingInsurance) { mutableStateOf(editingInsurance?.startDate ?: todayJalali) }
    var endDate by remember(editingInsurance) { mutableStateOf(editingInsurance?.endDate ?: nextYearJalali) }
    var insuranceAmountText by remember(editingInsurance) { mutableStateOf(editingInsurance?.amount?.takeIf { it > 0 }?.let { IranianPhoneUtils.convertDigitsToPersian(it.toString()) } ?: "") }
    var policyNumber by remember(editingInsurance) { mutableStateOf(editingInsurance?.policyNumber ?: "") }
    var lastInspectionDate by remember(editingInspection) { mutableStateOf(editingInspection?.lastInspectionDate ?: todayJalali) }
    var inspectionExpiryDate by remember(editingInspection) { mutableStateOf(editingInspection?.expiryDate ?: nextYearJalali) }
    var inspectionCostText by remember(editingInspection) { mutableStateOf(editingInspection?.cost?.takeIf { it > 0 }?.let { IranianPhoneUtils.convertDigitsToPersian(it.toString()) } ?: "") }
    var inspectionStatus by remember(editingInspection) { mutableStateOf(editingInspection?.status ?: "معتبر (گواهی معاینه فنی)") }
    var centerName by remember(editingInspection) { mutableStateOf(editingInspection?.centerName ?: "") }
    var insuranceReminderEnabled by remember(editingInsurance) { mutableStateOf(editingInsurance?.reminderEnabled ?: true) }
    var inspectionReminderEnabled by remember(editingInspection) { mutableStateOf(editingInspection?.reminderEnabled ?: true) }
    var insuranceReminderTime by remember(editingInsurance) { mutableStateOf(editingInsurance?.reminderTime ?: "۰۹:۰۰") }
    var inspectionReminderTime by remember(editingInspection) { mutableStateOf(editingInspection?.reminderTime ?: "۰۹:۰۰") }
    var insuranceReminderDays by remember(editingInsurance) {
        mutableStateOf(editingInsurance?.reminderDays?.toSet()?.takeIf { it.isNotEmpty() } ?: setOf(30, 15, 7, 1, 0))
    }
    var inspectionReminderDays by remember(editingInspection) {
        mutableStateOf(editingInspection?.reminderDays?.toSet()?.takeIf { it.isNotEmpty() } ?: setOf(30, 15, 7, 1, 0))
    }

    val companies = listOf("بیمه ایران", "بیمه آسیا", "بیمه دانا", "بیمه البرز", "بیمه پاسارگاد", "بیمه سامان")
    val insuranceType = if (selectedSection == 1) "بیمه بدنه" else "شخص ثالث"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("vehicle_insurance_sheet")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 28.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Soft3DIcon(imageRes = R.drawable.img_3d_insurance, contentDescription = "مدیریت بیمه و مدارک خودرو", size = 36.dp, accentColor = Color(0xFFF59E0B))
                    Column {
                        Text("بیمه و مدارک خودرو", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("${vehicle.brand} ${vehicle.model}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "بستن") }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            PrimaryTabRow(selectedTabIndex = selectedSection, modifier = Modifier.fillMaxWidth()) {
                Tab(selected = selectedSection == 0, onClick = { selectedSection = 0 }, modifier = Modifier.testTag("insurance_form_tab_third_party"), text = { Text("شخص ثالث", maxLines = 1) })
                Tab(selected = selectedSection == 1, onClick = { selectedSection = 1 }, modifier = Modifier.testTag("insurance_form_tab_body"), text = { Text("بیمه بدنه", maxLines = 1) })
                Tab(selected = selectedSection == 2, onClick = { selectedSection = 2 }, modifier = Modifier.testTag("insurance_form_tab_inspection"), text = { Text("معاینه فنی", maxLines = 1) })
            }

            if (selectedSection != 2) {
                Text("شرکت بیمه‌گر", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    companies.chunked(3).forEach { rowCompanies ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            rowCompanies.forEach { company ->
                                FilterChip(
                                    selected = insuranceCompany == company,
                                    onClick = { insuranceCompany = company },
                                    label = { Text(company, fontSize = 10.sp, maxLines = 1) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowCompanies.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = insuranceCompany !in companies,
                            onClick = { if (insuranceCompany in companies) insuranceCompany = "" },
                            label = { Text("سایر", fontSize = 10.sp, maxLines = 1) },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.weight(2f))
                    }
                    if (insuranceCompany !in companies) {
                        OutlinedTextField(
                            value = insuranceCompany,
                            onValueChange = { insuranceCompany = it },
                            label = { Text("نام شرکت بیمه‌گر") },
                            placeholder = { Text("نام شرکت موردنظر را وارد کنید") },
                            modifier = Modifier.fillMaxWidth().testTag("insurance_custom_company_input"),
                            singleLine = true
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PersianDateInputField(value = startDate, onValueChange = { startDate = it }, label = "تاریخ شروع", placeholder = "۱۴۰۵/۰۱/۰۱", dialogTitle = "انتخاب تاریخ شروع بیمه‌نامه", modifier = Modifier.weight(1f), testTag = "insurance_start_date_input")
                    PersianDateInputField(value = endDate, onValueChange = { endDate = it }, label = "تاریخ انقضا", placeholder = "۱۴۰۶/۰۱/۰۱", dialogTitle = "انتخاب تاریخ انقضای بیمه‌نامه", modifier = Modifier.weight(1f), testTag = "insurance_end_date_input")
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PersianAmountInputField(value = insuranceAmountText, onValueChange = { insuranceAmountText = it }, label = "حق بیمه پرداختی", unitLabel = "تومان", placeholder = "مثال: ۲,۵۰۰,۰۰۰", showWordsPreview = false, modifier = Modifier.weight(1.1f), testTag = "insurance_cost_input")
                    OutlinedTextField(value = policyNumber, onValueChange = { policyNumber = it }, label = { Text("شماره بیمه‌نامه") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                VehicleReminderSettings(
                    enabled = insuranceReminderEnabled, onEnabledChange = { insuranceReminderEnabled = it },
                    time = insuranceReminderTime, onTimeChange = { insuranceReminderTime = it },
                    days = insuranceReminderDays, onDaysChange = { insuranceReminderDays = it },
                    tag = "insurance_reminder"
                )
                Button(
                    onClick = { onSaveInsurance(insuranceCompany, insuranceType, startDate, endDate, IranianAmountUtils.parseAmountToLong(insuranceAmountText), policyNumber, insuranceReminderEnabled, insuranceReminderTime, insuranceReminderDays.sortedDescending(), editingInsurance?.id) },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_insurance_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text(if (editingInsurance != null) "ذخیره ویرایش بیمه‌نامه" else "ثبت $insuranceType", fontWeight = FontWeight.Bold)
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PersianDateInputField(value = lastInspectionDate, onValueChange = { lastInspectionDate = it }, label = "تاریخ آخرین معاینه", placeholder = "۱۴۰۵/۰۱/۰۱", dialogTitle = "انتخاب تاریخ آخرین معاینه فنی", modifier = Modifier.weight(1f), testTag = "inspection_last_date_input")
                    PersianDateInputField(value = inspectionExpiryDate, onValueChange = { inspectionExpiryDate = it }, label = "تاریخ انقضا", placeholder = "۱۴۰۶/۰۱/۰۱", dialogTitle = "انتخاب تاریخ انقضای معاینه فنی", modifier = Modifier.weight(1f), testTag = "inspection_expiry_date_input")
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PersianAmountInputField(value = inspectionCostText, onValueChange = { inspectionCostText = it }, label = "هزینه معاینه فنی", unitLabel = "تومان", placeholder = "مثال: ۱۸۰,۰۰۰", showWordsPreview = false, modifier = Modifier.weight(1f).height(56.dp), testTag = "inspection_cost_input")
                    OutlinedTextField(value = centerName, onValueChange = { centerName = it }, label = { Text("مرکز معاینه فنی") }, modifier = Modifier.weight(1.2f).height(56.dp), singleLine = true)
                }
                OutlinedTextField(value = inspectionStatus, onValueChange = { inspectionStatus = it }, label = { Text("وضعیت گواهی معاینه فنی") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                VehicleReminderSettings(
                    enabled = inspectionReminderEnabled, onEnabledChange = { inspectionReminderEnabled = it },
                    time = inspectionReminderTime, onTimeChange = { inspectionReminderTime = it },
                    days = inspectionReminderDays, onDaysChange = { inspectionReminderDays = it },
                    tag = "inspection_reminder"
                )
                Button(
                    onClick = { onSaveInspection(lastInspectionDate, inspectionExpiryDate, IranianAmountUtils.parseAmountToLong(inspectionCostText), inspectionStatus, centerName, inspectionReminderEnabled, inspectionReminderTime, inspectionReminderDays.sortedDescending(), editingInspection?.id) },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_inspection_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text(if (editingInspection != null) "ذخیره ویرایش معاینه فنی" else "ذخیره معاینه فنی", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehicleReminderSettings(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    time: String,
    onTimeChange: (String) -> Unit,
    days: Set<Int>,
    onDaysChange: (Set<Int>) -> Unit,
    tag: String
) {
    var showTimePicker by remember { mutableStateOf(false) }
    val normalizedTime = IranianPhoneUtils.convertDigitsToEnglish(time).trim()
    val timeParts = normalizedTime.split(":")
    val initialHour = timeParts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
    val initialMinute = timeParts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0

    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("یادآور سررسید", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text(if (enabled) "هشدارها طبق زمان‌های انتخابی تنظیم می‌شوند" else "یادآور خاموش است و آلارمی ثبت نمی‌شود", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = enabled, onCheckedChange = onEnabledChange, modifier = Modifier.testTag("${tag}_enabled"))
            }
            if (enabled) {
                OutlinedTextField(
                    value = time,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("ساعت هشدار (۲۴ ساعته)") },
                    placeholder = { Text("۰۹:۰۰") },
                    trailingIcon = { Icon(Icons.Rounded.Schedule, contentDescription = "انتخاب ساعت", tint = MaterialTheme.colorScheme.primary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().clickable { showTimePicker = true }.testTag("${tag}_time")
                )
                Text("زمان‌های هشدار پیش از انقضا", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                val options = listOf(30 to "۳۰ روز", 15 to "۱۵ روز", 7 to "۷ روز", 1 to "۱ روز", 0 to "سررسید")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    options.forEach { (value, label) ->
                        FilterChip(
                            selected = value in days,
                            onClick = { onDaysChange(if (value in days) days - value else days + value) },
                            label = { Text(label, fontSize = 8.sp, maxLines = 1) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (days.isEmpty()) Text("حداقل یک زمان هشدار انتخاب کن.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("انتخاب ساعت یادآور", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ساعت و دقیقه را در قالب ۲۴ ساعته انتخاب کنید", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    TimePicker(state = pickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val selected = "%02d:%02d".format(java.util.Locale.ROOT, pickerState.hour, pickerState.minute)
                    onTimeChange(IranianPhoneUtils.convertDigitsToPersian(selected))
                    showTimePicker = false
                }) { Text("تأیید") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("انصراف") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}
