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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.database.NotificationLogEntity
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusXL
import com.example.ui.theme.WarningAmberLight
import com.example.util.IranianPhoneUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    onBackClick: () -> Unit,
    viewModel: NotificationCenterViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val logs by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val backgroundBrush = remember(isDark) {
        if (isDark) {
            Brush.verticalGradient(
                colors = listOf(Color(0xFF090D16), Color(0xFF0C1322), Color(0xFF090D16))
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9), Color(0xFFE2E8F0))
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            NotificationCenterHeader(
                onBackClick = onBackClick,
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refresh
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Mark all as read bar
                if (unreadCount > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${IranianPhoneUtils.convertDigitsToPersian(unreadCount.toString())} پیام خوانده نشده",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        TextButton(
                            onClick = { viewModel.markAllAsRead() },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("علامت‌گذاری همه به عنوان خوانده‌شده", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(logs, key = { it.id }) { log ->
                        NotificationLogItem(
                            log = log,
                            onClick = {
                                if (!log.isRead) {
                                    viewModel.markAsRead(log.id)
                                }
                            }
                        )
                    }

                    if (logs.isEmpty()) {
                        item {
                            EmptyNotificationState()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCenterHeader(
    onBackClick: () -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val gradient = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF131B2E)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF134E4A)))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(bottomStart = RadiusXL, bottomEnd = RadiusXL)),
        shape = RoundedCornerShape(bottomStart = RadiusXL, bottomEnd = RadiusXL),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Back Button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable { onBackClick() }
                            .testTag("notification_center_back_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowForward,
                            contentDescription = "بازگشت",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "صندوق پیام‌ها و نوتیفیکیشن‌ها",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "تاریخچه هشدارهای اقساط، چک‌ها و سرویس‌های خودرو",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "تازه‌سازی",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationLogItem(
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
        "INSTALLMENT" -> "یادآوری قسط"
        "INSURANCE" -> "سررسید بیمه"
        "VEHICLE", "MAINTENANCE" -> "سرویس دوره‌ای"
        else -> "اعلان عمومی"
    }

    val dateFormatted = remember(log.timestamp) {
        val sdf = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.US)
        IranianPhoneUtils.convertDigitsToPersian(sdf.format(Date(log.timestamp)))
    }

    Layered3DCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("notification_item_${log.id}"),
        elevation = if (log.isRead) 1.dp else 4.dp,
        backgroundColor = if (log.isRead) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Soft3DIcon(
                imageRes = when (log.type.uppercase(Locale.ROOT)) {
                    "INSTALLMENT" -> R.drawable.img_3d_installment
                    "INSURANCE" -> R.drawable.img_3d_insurance
                    "VEHICLE", "MAINTENANCE" -> R.drawable.img_3d_car
                    else -> R.drawable.img_3d_bell_notification
                },
                contentDescription = typeLabel,
                accentColor = accentColor,
                size = 42.dp
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
                            fontWeight = if (log.isRead) FontWeight.Medium else FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (!log.isRead) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(8.dp)
                        ) {}
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = log.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor
                    )

                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyNotificationState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Soft3DIcon(
            imageRes = R.drawable.img_3d_bell_notification,
            contentDescription = "صندوق پیام خالی",
            size = 80.dp
        )
        Text(
            text = "صندوق پیام‌ها خالی است",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = "هنگام سررسید یادآورها، نوتیفیکیشن‌ها در اینجا ثبت می‌شوند.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}
