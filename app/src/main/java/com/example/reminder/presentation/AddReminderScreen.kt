package com.example.reminder.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
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
import com.example.data.security.SessionManager
import com.example.reminder.data.ReminderEntity
import com.example.reminder.domain.PredefinedOffset
import com.example.reminder.domain.Priority
import com.example.reminder.domain.ReminderType
import com.example.reminder.domain.RepeatType
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM
import com.example.ui.theme.WarningAmberLight
import com.example.util.IranianPhoneUtils
import com.example.util.PersianCalendarHelper
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderScreen(
    onBackClick: () -> Unit,
    onSaveReminder: (
        reminder: ReminderEntity,
        offsets: List<PredefinedOffset>,
        repeatType: RepeatType,
        repeatInterval: Int
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPersianDate = remember {
        PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()
    }

    var selectedType by remember { mutableStateOf(ReminderType.GENERAL) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(currentPersianDate) }
    var time by remember { mutableStateOf("۰۹:۰۰") }
    var priority by remember { mutableStateOf(Priority.NORMAL) }
    var repeatType by remember { mutableStateOf(RepeatType.NONE) }
    var amountText by remember { mutableStateOf("") }
    var targetKmText by remember { mutableStateOf("") }

    // Multi-Offset selection
    val selectedOffsets = remember {
        mutableStateListOf(PredefinedOffset.AT_TIME, PredefinedOffset.BEFORE_1_DAY)
    }

    // Channels
    var notificationEnabled by remember { mutableStateOf(true) }
    var smsEnabled by remember { mutableStateOf(false) }
    val sessionPhone = remember { SessionManager.currentUser?.phoneNumber ?: "09123456789" }
    var phoneNumber by remember { mutableStateOf(sessionPhone) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ثبت یادآوری و سررسید جدید",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("btn_add_reminder_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Type Selector Grid
            Text(
                text = "دسته‌بندی یادآور",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(ReminderType.GENERAL, ReminderType.INSTALLMENT, ReminderType.VEHICLE, ReminderType.INSURANCE).forEach { type ->
                    val isSelected = selectedType == type
                    Surface(
                        shape = RoundedCornerShape(RadiusMD),
                        color = if (isSelected) type.accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) type.accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedType = type }
                            .testTag("type_selector_${type.key}")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Soft3DIcon(
                                imageRes = type.iconRes,
                                contentDescription = type.title,
                                size = 32.dp,
                                accentColor = type.accentColor
                            )
                            Text(
                                text = type.title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) type.accentColor else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }

            // 2. Title & Description
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان یادآوری") },
                placeholder = { Text("مثلاً: قسط وام مسکن یا تمدید بیمه شخص ثالث") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_reminder_title"),
                shape = RoundedCornerShape(RadiusMD),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("توضیحات تکمیلی (اختیاری)") },
                placeholder = { Text("شماره شبا، جزئیات چک یا نکات مربوطه") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_reminder_desc"),
                shape = RoundedCornerShape(RadiusMD),
                maxLines = 3
            )

            // 3. Date & Time Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("تاریخ سررسید (شمسی)") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_reminder_date"),
                    shape = RoundedCornerShape(RadiusMD),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = InfoIndigoLight) }
                )

                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("ساعت هشدار") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_reminder_time"),
                    shape = RoundedCornerShape(RadiusMD),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.AccessTime, contentDescription = null, tint = WarningAmberLight) }
                )
            }

            // 4. Amount / Target Kilometer depending on type
            if (selectedType == ReminderType.INSTALLMENT || selectedType == ReminderType.FINANCE || selectedType == ReminderType.INSURANCE) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("مبلغ به تومان (اختیاری)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_reminder_amount"),
                    shape = RoundedCornerShape(RadiusMD),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.AttachMoney, contentDescription = null, tint = EmeraldPrimaryLight) }
                )
            } else if (selectedType == ReminderType.VEHICLE || selectedType == ReminderType.MAINTENANCE) {
                OutlinedTextField(
                    value = targetKmText,
                    onValueChange = { targetKmText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("کیلومتر هدف سرویس (اختیاری)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_reminder_km"),
                    shape = RoundedCornerShape(RadiusMD),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Speed, contentDescription = null, tint = InfoIndigoLight) }
                )
            }

            // 5. Multi-Offset Alert Selector (زمان‌های ارسال هشدار)
            Text(
                text = "زمان‌های ارسال هشدار (چندانتخابی)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    PredefinedOffset.BEFORE_7_DAYS,
                    PredefinedOffset.BEFORE_3_DAYS,
                    PredefinedOffset.BEFORE_1_DAY,
                    PredefinedOffset.AT_TIME
                ).forEach { offset ->
                    val isChecked = selectedOffsets.contains(offset)
                    FilterChip(
                        selected = isChecked,
                        onClick = {
                            if (isChecked) {
                                if (selectedOffsets.size > 1) selectedOffsets.remove(offset)
                            } else {
                                selectedOffsets.add(offset)
                            }
                        },
                        label = { Text(offset.title, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 6. Priority Selector
            Text(
                text = "اولویت یادآوری",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(Priority.LOW, Priority.NORMAL, Priority.HIGH).forEach { p ->
                    val isSelected = priority == p
                    Surface(
                        shape = RoundedCornerShape(RadiusMD),
                        color = if (isSelected) p.color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) p.color else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { priority = p }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = p.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) p.color else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }

            // 7. Notification & SMS Channels
            Layered3DCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "اعلان سیستم (Push Notification)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "نمایش اعلان در نوار وضعیت گوشی",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Switch(
                            checked = notificationEnabled,
                            onCheckedChange = { notificationEnabled = it },
                            modifier = Modifier.testTag("switch_notif_enabled")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "یادآوری پیامکی (SMS Mock)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "ارسال پیامک یادآوری به شماره ثبت‌شده",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Switch(
                            checked = smsEnabled,
                            onCheckedChange = { smsEnabled = it },
                            modifier = Modifier.testTag("switch_sms_enabled")
                        )
                    }

                    if (smsEnabled) {
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("شماره دریافت‌کننده پیامک") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(RadiusMD),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Save Button
            Button(
                onClick = {
                    if (title.isBlank()) return@Button
                    val parsedAmount = amountText.toLongOrNull()
                    val parsedKm = targetKmText.toLongOrNull()

                    val reminder = ReminderEntity(
                        id = UUID.randomUUID().toString(),
                        title = title.trim(),
                        description = description.trim(),
                        type = selectedType.key,
                        sourceType = "MANUAL",
                        priority = priority.name,
                        status = "ACTIVE",
                        date = date.trim(),
                        time = time.trim(),
                        notificationEnabled = notificationEnabled,
                        smsEnabled = smsEnabled,
                        phoneNumber = if (smsEnabled) phoneNumber else null,
                        amount = parsedAmount,
                        targetKilometer = parsedKm
                    )

                    onSaveReminder(reminder, selectedOffsets.toList(), repeatType, 1)
                },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_save_reminder"),
                shape = RoundedCornerShape(RadiusMD),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimaryLight)
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ثبت و زمان‌بندی یادآور", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
