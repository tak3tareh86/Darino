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
import com.example.reminder.domain.Priority
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

@Composable
fun ReminderCard(
    reminder: ReminderEntity,
    onClick: () -> Unit,
    onToggleCompleted: () -> Unit,
    onSnoozeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompleted = reminder.status.equals("COMPLETED", ignoreCase = true)
    val isMissed = reminder.status.equals("MISSED", ignoreCase = true)

    val reminderType = ReminderType.fromKey(reminder.type)
    val priority = Priority.fromKey(reminder.priority)

    val statusColor = when {
        isCompleted -> Color(0xFF94A3B8)
        isMissed -> ExpenseRoseLight
        reminder.status.equals("DISABLED", ignoreCase = true) -> Color(0xFF64748B)
        else -> EmeraldPrimaryLight
    }

    val statusText = when {
        isCompleted -> "انجام شده"
        isMissed -> "از دست رفته"
        reminder.status.equals("DISABLED", ignoreCase = true) -> "غیرفعال"
        else -> "فعال"
    }

    Layered3DCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("reminder_card_${reminder.id}"),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: 3D Icon + Title + Priority/Status + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 3D Icon
                Soft3DIcon(
                    imageRes = reminderType.iconRes,
                    contentDescription = reminderType.title,
                    size = 46.dp,
                    accentColor = reminderType.accentColor
                )

                // Title & Category
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
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

                        if (priority == Priority.HIGH) {
                            Surface(
                                shape = RoundedCornerShape(RadiusSM),
                                color = ExpenseRoseLight.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "فوری",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        color = ExpenseRoseLight,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
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
                    }
                }

                // Complete Action Button
                IconButton(
                    onClick = onToggleCompleted,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCompleted) EmeraldPrimaryLight.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                        .testTag("btn_toggle_complete_${reminder.id}")
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.CheckCircleOutline,
                        contentDescription = if (isCompleted) "علامت به عنوان فعال" else "علامت به عنوان انجام شده",
                        tint = if (isCompleted) EmeraldPrimaryLight else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Snooze Action Button (if not completed)
                if (!isCompleted) {
                    IconButton(
                        onClick = onSnoozeClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .testTag("btn_snooze_${reminder.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Snooze,
                            contentDescription = "به تعویق انداختن",
                            tint = WarningAmberLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Row 2: Description if exists
            if (reminder.description.isNotBlank()) {
                Text(
                    text = reminder.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // Row 3: Meta details (Date, Time, Amount / Kilometer, Notification / SMS status)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Date & Time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
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
                            text = reminder.date,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Row(
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
                            text = reminder.time,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // Amount or Target Kilometer
                if (reminder.amount != null && reminder.amount > 0) {
                    Text(
                        text = MoneyFormatter.formatToman(reminder.amount),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimaryLight
                        )
                    )
                } else if (reminder.targetKilometer != null) {
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
