package com.example.calendar.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
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
import com.example.calendar.domain.CalendarDateUtils
import com.example.calendar.domain.model.FinancialEvent
import com.example.calendar.domain.model.FinancialEventStatus
import com.example.calendar.domain.model.FinancialEventType
import com.example.calendar.domain.model.ReminderBeforeOption
import com.example.ui.components.PersianAmountInputField
import com.example.ui.components.PersianDateInputField
import com.example.ui.components.Soft3DIcon
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusXL
import com.example.util.IranianAmountUtils
import com.example.util.IranianPhoneUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFinancialEventSheet(
    sheetState: SheetState,
    initialType: FinancialEventType = FinancialEventType.INSTALLMENT,
    initialDate: String,
    onDismiss: () -> Unit,
    onConfirm: (FinancialEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedType by remember(initialType) { mutableStateOf(initialType) }
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var dateText by remember(initialDate) { mutableStateOf(CalendarDateUtils.toPersianDisplay(initialDate)) }
    var timeText by remember { mutableStateOf("۱۰:۰۰") }
    var description by remember { mutableStateOf("") }
    var selectedReminderBefore by remember { mutableStateOf(ReminderBeforeOption.ONE_DAY) }

    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = RadiusXL, topEnd = RadiusXL),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier.testTag("add_financial_event_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "بستن",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "افزودن رویداد مالی جدید",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 1. Type Selector (Chips with 3D Icons)
            Text(
                text = "دسته‌بندی رویداد",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FinancialEventType.entries.forEach { type ->
                    val isSelected = type == selectedType
                    Surface(
                        onClick = { selectedType = type },
                        shape = RoundedCornerShape(RadiusMD),
                        color = if (isSelected) {
                            type.primaryColor.copy(alpha = if (isDark) 0.28f else 0.15f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) type.primaryColor else Color.Transparent
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Soft3DIcon(
                                imageRes = type.icon3dRes,
                                contentDescription = type.title,
                                size = 26.dp,
                                accentColor = type.primaryColor
                            )
                            Text(
                                text = type.title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) type.primaryColor else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 2. Title Field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان رویداد (مثال: قسط بانک مهر، بیمه خودرو)") },
                placeholder = {
                    Text(
                        when (selectedType) {
                            FinancialEventType.INSTALLMENT -> "مثال: قسط وام مسکن یا خرید لپ‌تاپ"
                            FinancialEventType.VEHICLE -> "مثال: تمدید بیمه یا تعویض روغن"
                            FinancialEventType.EXPENSE -> "مثال: شارژ ساختمان یا پرداخت قبض"
                            FinancialEventType.REMINDER -> "مثال: یادآوری چک یا تمدید قرارداد"
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("event_title_input"),
                shape = RoundedCornerShape(RadiusMD),
                singleLine = true
            )

            // 3. Amount Field
            if (selectedType != FinancialEventType.REMINDER) {
                PersianAmountInputField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = "مبلغ به تومان",
                    placeholder = "مثال: ۳,۰۰۰,۰۰۰",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_amount_input")
                )
            }

            // 4. Date and Time Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PersianDateInputField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = "تاریخ سررسید (شمسی)",
                    placeholder = "۱۴۰۴/۰۱/۰۱",
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("event_date_input")
                )

                OutlinedTextField(
                    value = timeText,
                    onValueChange = { timeText = it },
                    label = { Text("ساعت") },
                    placeholder = { Text("۱۰:۰۰") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .weight(0.8f)
                        .testTag("event_time_input"),
                    shape = RoundedCornerShape(RadiusMD),
                    singleLine = true
                )
            }

            // 5. Reminder Before Option (Section 10 of specs)
            Text(
                text = "یادآوری قبل از سررسید",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReminderBeforeOption.entries.forEach { option ->
                    val isSelected = option == selectedReminderBefore
                    Surface(
                        onClick = { selectedReminderBefore = option },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.25f else 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 6. Description / Notes
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("توضیحات و یادداشت (اختیاری)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("event_description_input"),
                shape = RoundedCornerShape(RadiusMD),
                minLines = 2,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Confirm Submit Button
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val amountLong = IranianAmountUtils.parseAmountToLong(amountText)
                        val newEvent = FinancialEvent(
                            id = "user_ev_${System.currentTimeMillis()}",
                            title = title.trim(),
                            description = description.trim(),
                            type = selectedType,
                            amount = amountLong,
                            date = CalendarDateUtils.normalizeDate(dateText),
                            time = timeText.trim(),
                            reminderBefore = selectedReminderBefore,
                            status = FinancialEventStatus.PENDING
                        )
                        onConfirm(newEvent)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_add_event_button"),
                shape = RoundedCornerShape(RadiusMD),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ثبت در تقویم مالی",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
