package com.example.reminder.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.reminder.data.ReminderEntity
import com.example.reminder.domain.ReminderType
import com.example.reminder.domain.SnoozeOption
import com.example.ui.components.Layered3DCard
import com.example.ui.components.PersianDateInputField
import com.example.ui.components.Soft3DIcon
import com.example.ui.screens.reminder.components.PersianTimeInputField
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM
import com.example.ui.theme.WarningAmberLight
import com.example.util.IranianPhoneUtils
import com.example.util.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDetailScreen(
    reminder: ReminderEntity,
    onBackClick: () -> Unit,
    onToggleCompleted: () -> Unit,
    onSnooze: (option: SnoozeOption, customTargetMillis: Long?) -> Unit,
    onDelete: () -> Unit,
    onUpdateDateTime: (newDate: String, newTime: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSnoozeDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showEditDateTimeDialog by remember { mutableStateOf(false) }

    var editDate by remember { mutableStateOf(reminder.date) }
    var editTime by remember { mutableStateOf(reminder.time) }

    val isCompleted = reminder.status == "COMPLETED"
    val reminderType = when (reminder.type) {
        "FINANCE" -> ReminderType.FINANCE
        "INSTALLMENT" -> ReminderType.INSTALLMENT
        "VEHICLE" -> ReminderType.VEHICLE
        else -> ReminderType.PERSONAL
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "جزئیات یادآور",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "حذف یادآور",
                            tint = ExpenseRoseLight
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header 3D Card
            Layered3DCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Soft3DIcon(
                        imageRes = reminderType.iconRes,
                        contentDescription = reminderType.title,
                        size = 64.dp,
                        accentColor = reminderType.accentColor
                    )

                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(RadiusSM),
                            color = reminderType.accentColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = reminderType.title,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = reminderType.accentColor
                                )
                            )
                        }

                        val statusColor = if (isCompleted) Color(0xFF64748B) else EmeraldPrimaryLight
                        Surface(
                            shape = RoundedCornerShape(RadiusSM),
                            color = statusColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = if (isCompleted) "انجام شده" else "فعال",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = statusColor
                                )
                            )
                        }
                    }

                    if (reminder.description.isNotBlank()) {
                        Text(
                            text = reminder.description,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            // Attributes Card
            Layered3DCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "مشخصات زمان‌بندی و مقادیر",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )

                    DetailRow(
                        icon = Icons.Rounded.CalendarToday,
                        label = "تاریخ سررسید",
                        value = IranianPhoneUtils.convertDigitsToPersian(reminder.date)
                    )

                    DetailRow(
                        icon = Icons.Rounded.AccessTime,
                        label = "ساعت یادآوری",
                        value = IranianPhoneUtils.convertDigitsToPersian(reminder.time)
                    )

                    DetailRow(
                        icon = Icons.Rounded.Repeat,
                        label = "وضعیت",
                        value = when (reminder.status) {
                            "ACTIVE" -> "فعال"
                            "COMPLETED" -> "انجام شده"
                            "MISSED" -> "از دست رفته"
                            else -> "غیرفعال"
                        }
                    )

                    if (reminder.amount != null && reminder.amount > 0) {
                        DetailRow(
                            icon = Icons.Rounded.AttachMoney,
                            label = "مبلغ تعهد",
                            value = MoneyFormatter.formatToman(reminder.amount)
                        )
                    }

                    if (reminder.targetKilometer != null && reminder.targetKilometer > 0) {
                        DetailRow(
                            icon = Icons.Rounded.Speed,
                            label = "کیلومتر هدف",
                            value = "${IranianPhoneUtils.convertDigitsToPersian("%,d".format(reminder.targetKilometer))} کیلومتر"
                        )
                    }
                }
            }

            // Quick Operations Buttons
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Complete Button
                Button(
                    onClick = {
                        onToggleCompleted()
                        onBackClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("detail_complete_btn"),
                    shape = RoundedCornerShape(RadiusMD),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant else EmeraldPrimaryLight,
                        contentColor = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Rounded.Undo else Icons.Rounded.CheckCircle,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCompleted) "فعال‌سازی مجدد یادآور" else "علامت‌گذاری به عنوان انجام شده",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Snooze & Reschedule Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showSnoozeDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("detail_snooze_btn"),
                        shape = RoundedCornerShape(RadiusMD)
                    ) {
                        Icon(imageVector = Icons.Rounded.Snooze, contentDescription = null, tint = WarningAmberLight)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تعویق (Snooze)")
                    }

                    OutlinedButton(
                        onClick = { showEditDateTimeDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("detail_edit_time_btn"),
                        shape = RoundedCornerShape(RadiusMD)
                    ) {
                        Icon(imageVector = Icons.Rounded.EditCalendar, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تغییر زمان")
                    }
                }
            }
        }
    }

    // Snooze Dialog
    if (showSnoozeDialog) {
        SnoozeReminderDialog(
            reminderTitle = reminder.title,
            onDismiss = { showSnoozeDialog = false },
            onSnoozePreset = { option ->
                onSnooze(option, null)
                showSnoozeDialog = false
                onBackClick()
            },
            onSnoozeCustom = { targetMillis ->
                onSnooze(SnoozeOption.CUSTOM, targetMillis)
                showSnoozeDialog = false
                onBackClick()
            }
        )
    }

    // Edit Date/Time Dialog
    if (showEditDateTimeDialog) {
        AlertDialog(
            onDismissRequest = { showEditDateTimeDialog = false },
            title = { Text("تغییر زمان سررسید", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PersianDateInputField(
                        value = editDate,
                        onValueChange = { editDate = it },
                        label = "تاریخ جدید (شمسی)",
                        placeholder = "۱۴۰۴/۰۱/۰۱"
                    )
                    PersianTimeInputField(
                        value = editTime,
                        onValueChange = { editTime = it },
                        label = "ساعت جدید",
                        testTag = "input_edit_reminder_time"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateDateTime(editDate, editTime)
                        showEditDateTimeDialog = false
                        onBackClick()
                    }
                ) {
                    Text("ذخیره زمان جدید")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDateTimeDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("حذف یادآور", fontWeight = FontWeight.Bold) },
            text = { Text("آیا از حذف این یادآور اطمینان دارید؟") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete()
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRoseLight)
                ) {
                    Text("بله، حذف شود")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}
