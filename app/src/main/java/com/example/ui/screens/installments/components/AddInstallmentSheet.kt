package com.example.ui.screens.installments.components

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Schedule
import com.example.ui.screens.reminder.components.PersianTimePickerDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PersianAmountInputField
import com.example.ui.components.PersianDateInputField
import com.example.ui.components.Soft3DIcon
import com.example.ui.screens.installments.model.InstallmentCategory
import com.example.ui.theme.RadiusMD
import com.example.util.IranianAmountUtils
import com.example.util.IranianDateUtils
import com.example.util.IranianPhoneUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInstallmentSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onAddConfirm: (InstallmentCategory, String, String, String, String, Boolean, String, String) -> Unit,
    initialCategory: InstallmentCategory = InstallmentCategory.BANK_LOANS,
    initialTitle: String = "",
    initialTotalAmount: String = "",
    initialInstallmentAmount: String = "",
    initialInstallmentsCount: String = "",
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember(initialCategory) { mutableStateOf(initialCategory) }
    var title by remember(initialTitle) { mutableStateOf(initialTitle) }
    var totalAmount by remember(initialTotalAmount) { mutableStateOf(initialTotalAmount) }
    var installmentAmount by remember(initialInstallmentAmount) { mutableStateOf(initialInstallmentAmount) }
    var installmentsCount by remember(initialInstallmentsCount) { mutableStateOf(initialInstallmentsCount) }
    var dueDate by remember { mutableStateOf("۱۴۰۴/۰۷/۱۵") }
    var providerOrPerson by remember { mutableStateOf("") }
    var vehicleOrNote by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var reminderEnabled by remember { mutableStateOf(false) }
    var reminderDaysBefore by remember { mutableStateOf("۱") }
    var reminderTime by remember { mutableStateOf("۰۹:۰۰") }
    var showTimePicker by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Soft3DIcon(
                        imageRes = selectedCategory.iconRes,
                        contentDescription = "قسط جدید",
                        size = 36.dp,
                        accentColor = selectedCategory.accentColor,
                        containerShape = RoundedCornerShape(10.dp)
                    )
                    Text(
                        text = "افزودن قسط و تعهد جدید",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("add_installment_close")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "بستن",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Step 1: Category Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "نوع تعهد / قسط",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InstallmentCategory.entries.forEach { cat ->
                        val isCatSelected = cat == selectedCategory
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(RadiusMD))
                                .clickable { selectedCategory = cat }
                                .testTag("select_category_${cat.id}"),
                            shape = RoundedCornerShape(RadiusMD),
                            color = if (isCatSelected) cat.accentColor.copy(alpha = if (isDark) 0.25f else 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = if (isCatSelected) androidx.compose.foundation.BorderStroke(1.2.dp, cat.accentColor) else null
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = cat.title,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = if (isCatSelected) cat.accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Common & Adaptive Form Fields
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان قسط (مثلاً: وام مسکن، بیمه بدنه)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("installment_title_input"),
                shape = RoundedCornerShape(RadiusMD),
                singleLine = true
            )

            // Category Specific Fields
            when (selectedCategory) {
                InstallmentCategory.BANK_LOANS -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = providerOrPerson,
                            onValueChange = { providerOrPerson = it },
                            label = { Text("نام بانک (مثلاً ملت)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(RadiusMD),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = vehicleOrNote,
                            onValueChange = { vehicleOrNote = it },
                            label = { Text("نوع وام (مسکن، جعاله)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(RadiusMD),
                            singleLine = true
                        )
                    }
                }
                InstallmentCategory.HOME_LOANS -> {
                    OutlinedTextField(
                        value = providerOrPerson,
                        onValueChange = { providerOrPerson = it },
                        label = { Text("طرف حساب / نام صندوق خانوادگی") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(RadiusMD),
                        singleLine = true
                    )
                }
                InstallmentCategory.CAR_INSURANCE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = providerOrPerson,
                            onValueChange = { providerOrPerson = it },
                            label = { Text("شرکت بیمه (مثلاً دانا)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(RadiusMD),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = vehicleOrNote,
                            onValueChange = { vehicleOrNote = it },
                            label = { Text("خودرو (مثلاً پژو ۲۰۶)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(RadiusMD),
                            singleLine = true
                        )
                    }
                }
                InstallmentCategory.MISC -> {
                    OutlinedTextField(
                        value = providerOrPerson,
                        onValueChange = { providerOrPerson = it },
                        label = { Text("فروشگاه یا سرویس (مثلاً دیجی‌پی، اسنپ‌پی)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(RadiusMD),
                        singleLine = true
                    )
                }
            }

            // Financial amounts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PersianAmountInputField(
                    value = totalAmount,
                    onValueChange = { totalAmount = it },
                    label = "مبلغ کل",
                    unitLabel = "تومان",
                    placeholder = "مثال: ۵۰,۰۰۰,۰۰۰",
                    showWordsPreview = false,
                    modifier = Modifier.weight(1f),
                    testTag = "installment_total_amount_input"
                )

                PersianAmountInputField(
                    value = installmentAmount,
                    onValueChange = { installmentAmount = it },
                    label = "مبلغ هر قسط",
                    unitLabel = "تومان",
                    placeholder = "مثال: ۴,۵۰۰,۰۰۰",
                    showWordsPreview = false,
                    modifier = Modifier.weight(1f),
                    testTag = "installment_monthly_amount_input"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(0.9f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "تعداد کل اقساط",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = IranianPhoneUtils.convertDigitsToPersian(installmentsCount),
                        onValueChange = {
                            val digits = IranianPhoneUtils.convertDigitsToEnglish(it).filter { ch -> ch in '0'..'9' }
                            installmentsCount = digits
                        },
                        placeholder = { Text("مثال: ۱۲") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(RadiusMD),
                        singleLine = true
                    )
                }

                PersianDateInputField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = "اولین سررسید",
                    placeholder = "۱۴۰۴/۰۷/۱۵",
                    dialogTitle = "انتخاب تاریخ سررسید قسط",
                    modifier = Modifier.weight(1.1f),
                    testTag = "installment_due_date_input"
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("توضیحات یا شماره قرارداد (اختیاری)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(RadiusMD),
                maxLines = 2
            )

            // Reminder Settings (اتصال به سیستم یادآور هوشمند)
            Surface(
                shape = RoundedCornerShape(RadiusMD),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "برای این قسط یادآوری فعال شود؟",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "ارسال هشدار پیش از موعد سررسید قسط",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                        androidx.compose.material3.Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it }
                        )
                    }

                    if (reminderEnabled) {
                        val reminderAdvanceOptions = listOf("۳ روز قبل", "۱ روز قبل", "روز سررسید")
                        var selectedAdvanceOption by remember { mutableStateOf(reminderAdvanceOptions[0]) }

                        Text(
                            text = "زمان هشدار یادآوری:",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            reminderAdvanceOptions.forEach { opt ->
                                val isSelected = opt == selectedAdvanceOption
                                androidx.compose.material3.FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedAdvanceOption = opt
                                        reminderDaysBefore = when (opt) {
                                            "۳ روز قبل" -> "۳"
                                            "۱ روز قبل" -> "۱"
                                            else -> "۰"
                                        }
                                    },
                                    label = { Text(opt, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = reminderTime,
                                onValueChange = { input ->
                                    reminderTime = IranianPhoneUtils.convertDigitsToPersian(input)
                                },
                                label = { Text("ساعت یادآوری") },
                                trailingIcon = {
                                    IconButton(onClick = { showTimePicker = true }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Schedule,
                                            contentDescription = "انتخاب ساعت",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showTimePicker = true },
                                shape = RoundedCornerShape(RadiusMD),
                                singleLine = true
                            )
                        }

                        if (showTimePicker) {
                            val parts = reminderTime.split(":")
                            val hourEng = parts.getOrNull(0)?.let { IranianPhoneUtils.convertDigitsToEnglish(it).trim().toIntOrNull() } ?: 9
                            val minEng = parts.getOrNull(1)?.let { IranianPhoneUtils.convertDigitsToEnglish(it).trim().toIntOrNull() } ?: 0

                            PersianTimePickerDialog(
                                initialHour = hourEng,
                                initialMinute = minEng,
                                onDismiss = { showTimePicker = false },
                                onConfirm = { h, m ->
                                    val formattedTime = String.format(java.util.Locale.US, "%02d:%02d", h, m)
                                    reminderTime = IranianPhoneUtils.convertDigitsToPersian(formattedTime)
                                    showTimePicker = false
                                }
                            )
                        }
                    }
                }
            }

            // Submit Button
            Button(
                onClick = {
                    val finalTitle = title.ifEmpty { "قسط جدید ${selectedCategory.title}" }
                    val finalTotal = totalAmount.ifEmpty { "۱۰,۰۰۰,۰۰۰" }
                    val finalMonthly = installmentAmount.ifEmpty { "۱,۰۰۰,۰۰۰" }
                    val finalProvider = providerOrPerson.ifEmpty { "طرف حساب پیش‌فرض" }
                    onAddConfirm(
                        selectedCategory,
                        finalTitle,
                        finalTotal,
                        finalMonthly,
                        finalProvider,
                        reminderEnabled,
                        reminderDaysBefore,
                        reminderTime
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_installment_button"),
                shape = RoundedCornerShape(RadiusMD),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Done,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "ثبت قسط جدید",
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
