package com.example.calendar.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.domain.CalendarDateUtils
import com.example.calendar.domain.model.FinancialEvent
import com.example.calendar.domain.model.FinancialEventStatus
import com.example.calendar.domain.model.FinancialEventType
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.WarningAmberLight

@Composable
fun EventCard(
    event: FinancialEvent,
    onClick: () -> Unit,
    onToggleStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val isPaid = event.status == FinancialEventStatus.PAID
    val isOverdue = event.status == FinancialEventStatus.OVERDUE || event.isOverdue
    val isDueSoon = event.isDueSoon && !isPaid && !isOverdue

    val cardBg = when {
        isPaid -> if (isDark) Color(0xFF0F241C) else Color(0xFFF0FDF4)
        isOverdue -> if (isDark) Color(0xFF261217) else Color(0xFFFFF1F2)
        isDueSoon -> if (isDark) Color(0xFF241C12) else Color(0xFFFFFBEB)
        else -> if (isDark) Color(0xFF151D2C) else Color(0xFFFFFFFF)
    }

    val borderColor = when {
        isPaid -> EmeraldPrimaryLight.copy(alpha = 0.35f)
        isOverdue -> ExpenseRoseLight.copy(alpha = 0.45f)
        isDueSoon -> WarningAmberLight.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
    }

    Layered3DCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("event_card_${event.id}"),
        backgroundColor = cardBg,
        borderColor = borderColor,
        elevation = if (isOverdue || isDueSoon) 3.dp else 2.dp,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Special State Alert Banners
            if (isOverdue) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ExpenseRoseLight.copy(alpha = if (isDark) 0.2f else 0.12f),
                    border = BorderStroke(1.dp, ExpenseRoseLight.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = ExpenseRoseLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "این پرداخت انجام نشده است (عقب افتاده)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ExpenseRoseLight
                        )
                    }
                }
            } else if (isDueSoon) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = WarningAmberLight.copy(alpha = if (isDark) 0.2f else 0.12f),
                    border = BorderStroke(1.dp, WarningAmberLight.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccessTime,
                            contentDescription = null,
                            tint = WarningAmberLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "سررسید نزدیک است",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = WarningAmberLight
                        )
                    }
                }
            }

            // Main Row: 3D Icon + Title + Amount + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 3D Category Icon
                Soft3DIcon(
                    imageRes = event.type.icon3dRes,
                    contentDescription = event.type.title,
                    size = 38.dp,
                    accentColor = event.type.primaryColor,
                    containerShape = RoundedCornerShape(RadiusMD)
                )

                // Title + Description / Type tag
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (event.description.isNotBlank()) {
                        Text(
                            text = event.description,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Date and Time Chips
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = CalendarDateUtils.toPersianDisplay(event.date),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!event.time.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = event.displayTimePersian ?: "",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Amount & Status Action
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (event.formattedAmount != null) {
                        Text(
                            text = event.formattedAmount ?: "",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            ),
                            color = if (isPaid) EmeraldPrimaryLight else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Interactive Status Chip
                    Surface(
                        onClick = onToggleStatus,
                        shape = RoundedCornerShape(20.dp),
                        color = when (event.status) {
                            FinancialEventStatus.PAID -> EmeraldPrimaryLight.copy(alpha = if (isDark) 0.25f else 0.15f)
                            FinancialEventStatus.OVERDUE -> ExpenseRoseLight.copy(alpha = if (isDark) 0.25f else 0.15f)
                            FinancialEventStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = BorderStroke(
                            1.dp,
                            when (event.status) {
                                FinancialEventStatus.PAID -> EmeraldPrimaryLight.copy(alpha = 0.5f)
                                FinancialEventStatus.OVERDUE -> ExpenseRoseLight.copy(alpha = 0.5f)
                                FinancialEventStatus.PENDING -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            }
                        ),
                        modifier = Modifier.testTag("status_chip_${event.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isPaid) Icons.Rounded.CheckCircle else Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = event.status.color,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = event.status.title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                ),
                                color = event.status.color
                            )
                        }
                    }
                }
            }

            // Bottom Footer: Reminder Before badge + Category tag
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Chip
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = event.type.primaryColor.copy(alpha = if (isDark) 0.18f else 0.10f)
                ) {
                    Text(
                        text = event.type.title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp
                        ),
                        color = event.type.primaryColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Alert Before indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "هشدار: ${event.reminderBefore.label}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}
