package com.example.ui.screens.reminder

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.database.NotificationLogEntity
import com.example.ui.components.Soft3DIcon
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusXL
import com.example.ui.theme.WarningAmberLight
import com.example.util.IranianPhoneUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationCenterScreen(
    onBackClick: () -> Unit,
    onNavigateToInstallments: () -> Unit = {},
    onNavigateToVehicles: () -> Unit = {},
    onNavigateToFinance: () -> Unit = {},
    onNotificationClick: (Int) -> Unit = {},
    viewModel: NotificationCenterViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val unreadCount by viewModel.unreadCount.collectAsState()
    val logs by viewModel.notifications.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    // Semi-transparent backdrop for centered popup dialog
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable { onBackClick() }
            .testTag("notification_popup_backdrop"),
        contentAlignment = Alignment.Center
    ) {
        // Centered Popup Card
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.78f)
                .shadow(16.dp, RoundedCornerShape(RadiusXL))
                .clickable(enabled = false) { /* consume click */ }
                .testTag("notification_popup_dialog"),
            shape = RoundedCornerShape(RadiusXL),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Popup Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "صندوق پیام‌ها و نوتیفیکیشن‌ها",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "هشدارهای اقساط و سرویس‌ها",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = viewModel::refresh,
                            modifier = Modifier.size(32.dp)
                        ) {
                            if (uiState.isRefreshing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "تازه‌سازی",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowForward,
                                contentDescription = "بستن",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))

                // Mark all as read bar
                if (unreadCount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${IranianPhoneUtils.convertDigitsToPersian(unreadCount.toString())} خوانده نشده",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        TextButton(
                            onClick = { viewModel.markAllAsRead() },
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("علامت‌گذاری همه", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // List of compact notification items
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(logs, key = { it.id }) { log ->
                        CompactNotificationItem(
                            log = log,
                            onClick = {
                                if (!log.isRead) {
                                    viewModel.markAsRead(log.id)
                                }
                                onNotificationClick(log.id)
                            }
                        )
                    }

                    if (logs.isEmpty()) {
                        item {
                            CompactEmptyState()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactNotificationItem(
    log: NotificationLogEntity,
    onClick: () -> Unit
) {
    val accentColor = when (log.type.uppercase(Locale.ROOT)) {
        "INSTALLMENT" -> EmeraldPrimaryLight
        "INSURANCE" -> WarningAmberLight
        "VEHICLE", "MAINTENANCE" -> InfoIndigoLight
        else -> MaterialTheme.colorScheme.primary
    }

    val typeLabel = when (log.type.uppercase(Locale.ROOT)) {
        "INSTALLMENT" -> "قسط"
        "INSURANCE" -> "بیمه"
        "VEHICLE", "MAINTENANCE" -> "سرویس"
        else -> "اعلان"
    }

    val dateFormatted = remember(log.timestamp) {
        val sdf = SimpleDateFormat("MM/dd - HH:mm", Locale.US)
        IranianPhoneUtils.convertDigitsToPersian(sdf.format(Date(log.timestamp)))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("notification_item_${log.id}"),
        shape = RoundedCornerShape(RadiusMD),
        color = if (log.isRead) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Soft3DIcon(
                imageRes = when (log.type.uppercase(Locale.ROOT)) {
                    "INSTALLMENT" -> R.drawable.img_3d_installment
                    "INSURANCE" -> R.drawable.img_3d_insurance
                    "VEHICLE", "MAINTENANCE" -> R.drawable.img_3d_car
                    else -> R.drawable.img_3d_bell_notification
                },
                contentDescription = typeLabel,
                accentColor = accentColor,
                size = 32.dp
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )

                    if (!log.isRead) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(6.dp)
                        ) {}
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = log.message,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = accentColor
                    )

                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun CompactEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Soft3DIcon(
            imageRes = R.drawable.img_3d_bell_notification,
            contentDescription = "صندوق پیام خالی",
            size = 50.dp
        )
        Text(
            text = "صندوق پیام‌ها خالی است",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
