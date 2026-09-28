package com.example.reminder.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM
import com.example.ui.theme.WarningAmberLight
import com.example.util.IranianPhoneUtils

data class TodayNotificationItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val time: String,
    val type: ReminderType,
    val isUrgent: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    onBackClick: () -> Unit,
    onNotificationClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val todayNotifications = listOf(
        TodayNotificationItem(
            id = "notif_1",
            title = "سررسید قسط بانک مهر",
            subtitle = "مبلغ: ۳,۰۰۰,۰۰۰ تومان - مهلت پرداخت تا ساعت ۲۴ امروز",
            time = "۰۹:۰۰",
            type = ReminderType.INSTALLMENT,
            isUrgent = true
        ),
        TodayNotificationItem(
            id = "notif_2",
            title = "تمدید بیمه خودرو",
            subtitle = "مهلت تمدید بیمه‌نامه شخص ثالث پژو ۲۰۷ فرا رسیده است",
            time = "۱۰:۰۰",
            type = ReminderType.VEHICLE,
            isUrgent = true
        ),
        TodayNotificationItem(
            id = "notif_3",
            title = "پرداخت قبوض خدماتی",
            subtitle = "قبض برق و گاز شهری - مبلغ ۴۸۰,۰۰۰ تومان",
            time = "۱۱:۰۰",
            type = ReminderType.FINANCE
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "مرکز اعلان‌ها",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        )
                        Surface(
                            shape = CircleShape,
                            color = WarningAmberLight
                        ) {
                            Text(
                                text = IranianPhoneUtils.convertDigitsToPersian("3"),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "اعلان‌های امروز (هشدار موعد)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }

            items(todayNotifications, key = { it.id }) { notif ->
                Layered3DCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notif_item_${notif.id}"),
                    onClick = { onNotificationClick(notif.id) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Soft3DIcon(
                            imageRes = notif.type.iconRes,
                            contentDescription = notif.title,
                            size = 44.dp,
                            accentColor = notif.type.accentColor
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "🔔 ${notif.title}",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                )
                                if (notif.isUrgent) {
                                    Surface(
                                        shape = RoundedCornerShape(RadiusSM),
                                        color = Color(0xFFEF4444).copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "فوری",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFFEF4444),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                }
                            }

                            Text(
                                text = notif.subtitle,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "امروز ساعت ${IranianPhoneUtils.convertDigitsToPersian(notif.time)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Rounded.ChevronLeft,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Layered3DCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = EmeraldPrimaryLight,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "سیستم هوشمند اعلان دارینو فعال است",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "یادآوری‌ها به صورت خودکار با سررسیدهای اقساط و خودرو هماهنگ هستند.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
