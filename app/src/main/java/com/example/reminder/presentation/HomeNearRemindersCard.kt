package com.example.reminder.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.reminder.domain.ReminderType
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM
import com.example.ui.theme.WarningAmberLight
import com.example.util.IranianPhoneUtils

data class NearReminderPreview(
    val title: String,
    val timeRemainingText: String,
    val dateText: String,
    val type: ReminderType,
    val amountFormatted: String? = null
)

@Composable
fun HomeNearRemindersCard(
    onViewAllRemindersClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nearReminders = listOf(
        NearReminderPreview(
            title = "قسط بانک مهر",
            timeRemainingText = "فردا",
            dateText = "۱۵ مهر",
            type = ReminderType.INSTALLMENT,
            amountFormatted = "۳,۰۰۰,۰۰۰ تومان"
        ),
        NearReminderPreview(
            title = "بیمه شخص ثالث خودرو",
            timeRemainingText = "۵ روز دیگر",
            dateText = "۲۲ مهر",
            type = ReminderType.VEHICLE,
            amountFormatted = "۵,۴۰۰,۰۰۰ تومان"
        ),
        NearReminderPreview(
            title = "تعویض روغن موتور دنا",
            timeRemainingText = "۸ روز دیگر",
            dateText = "۲۵ مهر",
            type = ReminderType.VEHICLE,
            amountFormatted = "۱,۸۵۰,۰۰۰ تومان"
        )
    )

    Layered3DCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_near_reminders_card"),
        onClick = onViewAllRemindersClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Soft3DIcon(
                        imageRes = R.drawable.img_3d_bell_notification,
                        contentDescription = "یادآورهای نزدیک",
                        size = 36.dp,
                        accentColor = WarningAmberLight
                    )
                    Column {
                        Text(
                            text = "یادآورهای نزدیک",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                        Text(
                            text = "تعهدات و هشدارهای مهم روزهای آینده",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                TextButton(
                    onClick = onViewAllRemindersClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "مشاهده همه",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Icon(
                        imageVector = Icons.Rounded.ChevronLeft,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                thickness = 0.8.dp
            )

            // Next 3 Reminders List
            nearReminders.forEach { reminder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(RadiusSM))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = reminder.type.accentColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(8.dp)
                        ) {}

                        Column {
                            Text(
                                text = reminder.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (reminder.amountFormatted != null) {
                                Text(
                                    text = IranianPhoneUtils.convertDigitsToPersian(reminder.amountFormatted),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    // Time remaining badge (فردا / ۵ روز دیگر)
                    Surface(
                        shape = RoundedCornerShape(RadiusSM),
                        color = if (reminder.timeRemainingText == "فردا") WarningAmberLight.copy(alpha = 0.15f)
                        else InfoIndigoLight.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccessTime,
                                contentDescription = null,
                                tint = if (reminder.timeRemainingText == "فردا") WarningAmberLight else InfoIndigoLight,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = IranianPhoneUtils.convertDigitsToPersian(reminder.timeRemainingText),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (reminder.timeRemainingText == "فردا") WarningAmberLight else InfoIndigoLight,
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
