package com.example.reminder.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reminder.data.ReminderEntity
import com.example.reminder.domain.ReminderStatus
import com.example.reminder.domain.ReminderType
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM
import com.example.ui.theme.WarningAmberLight
import com.example.util.MoneyFormatter
import com.example.util.MoneyAmount
import com.example.ui.screens.settings.model.AppCurrency

@Composable
fun ReminderCard(
    reminder: ReminderEntity,
    vehiclePlate: String? = null,
    scheduledTriggerAt: Long? = null,
    onClick: () -> Unit,
    onToggleCompleted: () -> Unit,
    onSnoozeClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    showSnooze: Boolean = true,
    showActions: Boolean = true
) {
    var showDeleteConfirmDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val isCompleted = reminder.status.equals("COMPLETED", ignoreCase = true)
    val isMissed = reminder.status.equals("MISSED", ignoreCase = true)
    val isCancelled = reminder.status.equals("CANCELLED", ignoreCase = true)

    val reminderType = ReminderType.fromKey(reminder.type)
    val isVehicleReminder = reminder.sourceType.equals("VEHICLE", ignoreCase = true)
    val isCompletedInstallmentOccurrence =
        reminder.sourceType.equals("INSTALLMENT_OCCURRENCE", ignoreCase = true)
    val isInstallmentReminder = reminder.sourceType.equals("INSTALLMENT", ignoreCase = true) ||
        isCompletedInstallmentOccurrence

    val statusColor = when {
        isCompleted -> Color(0xFF94A3B8)
        isMissed -> ExpenseRoseLight
        reminder.status.equals("DISABLED", ignoreCase = true) || isCancelled -> Color(0xFF64748B)
        else -> EmeraldPrimaryLight
    }

    val statusText = when {
        isCompleted -> "انجام شده"
        isMissed -> "از دست رفته"
        reminder.status.equals("DISABLED", ignoreCase = true) -> "غیرفعال"
        isCancelled -> "لغوشده"
        else -> "فعال"
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("حذف یادآور", fontWeight = FontWeight.Bold) },
            text = { Text("آیا از حذف این یادآور اطمینان دارید؟") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false; onDeleteClick() }) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("انصراف") } }
        )
    }

    Layered3DCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("reminder_card_${reminder.id}"),
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Row 1: 3D Icon + Title + Status + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 3D Icon
                Soft3DIcon(
                    imageRes = reminderType.iconRes,
                    contentDescription = reminderType.title,
                    size = 36.dp,
                    accentColor = reminderType.accentColor
                )

                // Title & Category
                Column(
                    modifier = Modifier.weight(1f).padding(end = 34.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                        softWrap = true
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(RadiusSM),
                                color = reminderType.accentColor.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = reminderType.title,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        color = reminderType.accentColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(RadiusSM),
                                color = statusColor.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = statusText,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        color = statusColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }

                            if (isVehicleReminder) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CalendarToday,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = scheduledTriggerAt?.let { com.example.util.PersianCalendarHelper.fromEpochMillis(it).toFormattedDate() } ?: reminder.date,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AccessTime,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = scheduledTriggerAt?.let { com.example.util.PersianCalendarHelper.fromEpochMillis(it).toFormattedTime() } ?: reminder.time,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        // Vehicle details occupy their own layout row; they no longer float over the title/status row.
                        if (!vehiclePlate.isNullOrBlank() || reminder.targetKilometer != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 38.dp),
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                if (!vehiclePlate.isNullOrBlank()) {
                                    Text(
                                        text = vehiclePlate,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (reminder.targetKilometer != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Speed,
                                            contentDescription = null,
                                            tint = InfoIndigoLight,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "${reminder.targetKilometer} کیلومتر",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = InfoIndigoLight
                                            )
                                        )
                                    }

                                }
                            }
                        }
                    }
                }


            }

            // Keep the amount in the dedicated amount row; hide it from the description when duplicated.
            val formattedReminderAmount = reminder.amount?.takeIf { it > 0L }?.let { amount ->
                runCatching {
                    MoneyFormatter.formatExact(MoneyAmount.of(amount, AppCurrency.valueOf(reminder.amountUnit)))
                }.getOrNull()
            }
            val amountCleanedDescription = if (
                formattedReminderAmount != null && reminder.description.contains(formattedReminderAmount)
            ) {
                reminder.description
                    .replace(formattedReminderAmount, "")
                    .replace(Regex("^\\s*مبلغ\\s*(?:قسط|چک)\\s*[:：]?\\s*"), "")
                    .trim()
                    .trim('•', '-', '،', ':', '：')
                    .trim()
            } else {
                reminder.description
            }

            // Vehicle reminder descriptions show only the useful identifying detail.
            // The stored reminder title and all reminder scheduling data remain untouched.
            val displayDescription = when {
                // Installment descriptions currently contain only a second, preformatted
                // copy of the amount. The dedicated amount row above is the single source
                // of truth for this card, so do not render that duplicate description.
                reminder.type.equals("INSTALLMENT", ignoreCase = true) -> ""
                reminder.type.equals("INSURANCE", ignoreCase = true) -> {
                    amountCleanedDescription
                        .substringAfter("برای خودرو ", "")
                        .substringBefore(".")
                        .trim()
                        .ifBlank { amountCleanedDescription }
                }
                reminder.title.startsWith("یادآور سرویس:") ||
                    amountCleanedDescription.startsWith("موعد سرویس دوره‌ای:") -> {
                    val serviceDetails = amountCleanedDescription
                        .substringAfter("موعد سرویس دوره‌ای:", "")
                        .substringBefore("؛ کیلومتر هدف")
                        .trim()
                        .substringBeforeLast(" (")
                        .trim()
                    val serviceNames = serviceDetails.split("،").map { it.trim() }.filter { it.isNotBlank() }
                    when {
                        serviceNames.size > 1 -> "${serviceNames.first()} و ..."
                        serviceNames.size == 1 -> serviceNames.first()
                        else -> amountCleanedDescription
                    }
                }
                else -> amountCleanedDescription
            }

            // Row 2: Description if exists and contains information beyond the amount.
            if (displayDescription.isNotBlank()) {
                Text(
                    text = displayDescription,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Row 3: Keep reminder date and time side by side; place the amount below them.
            if (!isVehicleReminder) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = when {
                                    isCompletedInstallmentOccurrence -> "تاریخ قسط: ${reminder.date}"
                                    scheduledTriggerAt != null -> "تاریخ هشدار: ${com.example.util.PersianCalendarHelper.fromEpochMillis(scheduledTriggerAt).toFormattedDate()}"
                                    isInstallmentReminder -> "تاریخ هشدار: تنظیم نشده"
                                    else -> "تاریخ یادآور: ${reminder.date}"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = when {
                                    isCompletedInstallmentOccurrence -> "ساعت یادآور: ${reminder.time}"
                                    scheduledTriggerAt != null -> "ساعت هشدار: ${com.example.util.PersianCalendarHelper.fromEpochMillis(scheduledTriggerAt).toFormattedTime()}"
                                    isInstallmentReminder -> "ساعت هشدار: تنظیم نشده"
                                    else -> "ساعت یادآور: ${reminder.time}"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    if (formattedReminderAmount != null) {
                        Text(
                            text = formattedReminderAmount,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimaryLight
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else if (formattedReminderAmount != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = formattedReminderAmount,
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimaryLight
                        )
                    )
                }
            }
        }

        // Read-only completed installment occurrences have no parent-level actions.
        if (showActions) {
        // Actions are vertically stacked in this order: delete, complete, snooze.
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 2.dp, end = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            IconButton(
                onClick = { showDeleteConfirmDialog = true },
                modifier = Modifier
                    .size(30.dp)
                    .testTag("btn_delete_reminder_${reminder.id}")
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "حذف یادآور",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            IconButton(
                onClick = onToggleCompleted,
                modifier = Modifier
                    .size(30.dp)
                    .testTag("btn_toggle_complete_${reminder.id}")
            ) {
                Box(
                    modifier = Modifier
                        .size(21.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCompleted) EmeraldPrimaryLight.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.CheckCircleOutline,
                        contentDescription = if (isCompleted) "علامت به عنوان فعال" else "علامت به عنوان انجام شده",
                        tint = if (isCompleted) EmeraldPrimaryLight else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (!isCompleted && !isMissed && reminder.status.equals("ACTIVE", ignoreCase = true) && showSnooze) {
                IconButton(
                    onClick = onSnoozeClick,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("btn_snooze_${reminder.id}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(21.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Snooze,
                            contentDescription = "به تعویق انداختن",
                            tint = WarningAmberLight,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
        }
        }
    }
}
