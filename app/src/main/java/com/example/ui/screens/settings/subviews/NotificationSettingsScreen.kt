package com.example.ui.screens.settings.subviews

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.preferences.AppPreferencesRepository
import com.example.reminder.data.ReminderEntity
import com.example.reminder.domain.NotificationDispatcher
import com.example.ui.screens.settings.components.SettingsHeader
import com.example.ui.screens.settings.components.SettingsItem
import com.example.ui.screens.settings.components.SettingsSectionCard
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM
import com.example.ui.theme.WarningAmberLight
import kotlinx.coroutines.launch

@Composable
fun NotificationSettingsScreen(
    onBackClick: () -> Unit,
    onNavigateToReminders: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val preferences = remember { AppPreferencesRepository.getInstance(context) }

    val pushNotificationsEnabled by preferences.notificationsEnabled.collectAsState()
    val soundEnabled by preferences.notificationSoundEnabled.collectAsState()
    val vibrationEnabled by preferences.notificationVibrationEnabled.collectAsState()

    // Quiet Hours are persisted centrally and consumed by the alarm receiver.
    val quietHoursEnabled by preferences.quietHoursEnabled.collectAsState()
    val quietStartMinute by preferences.quietHoursStartMinute.collectAsState()
    val quietEndMinute by preferences.quietHoursEndMinute.collectAsState()
    val allowHighPriorityInQuiet by preferences.allowHighPriorityInQuietHours.collectAsState()
    val quietRangeOptions = listOf(22 * 60 to 6 * 60, 23 * 60 to 7 * 60, 0 to 8 * 60)
    fun formatQuietTime(minutes: Int): String {
        val hour = (minutes.coerceIn(0, 1439) / 60).toString().padStart(2, '0')
        val minute = (minutes.coerceIn(0, 1439) % 60).toString().padStart(2, '0')
        return "$hour:$minute".map { ch ->
            if (ch in '0'..'9') ('۰'.code + (ch.code - '0'.code)).toChar() else ch
        }.joinToString("")
    }
    val quietStart = formatQuietTime(quietStartMinute)
    val quietEnd = formatQuietTime(quietEndMinute)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SettingsHeader(
                title = "تنظیمات اعلان‌ها و پیامک",
                subtitle = "مدیریت هشدارهای هوشمند، ساعات سکوت و پیامک دارینو",
                showBack = true,
                showSearch = false,
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Master Channels Section
            item {
                SettingsSectionCard(title = "کانال‌های ارتباطی و دریافت هشدار") {
                    SettingsItem(
                        title = "اعلان‌های سیستم اندروید (Push)",
                        subtitle = "نمایش اعلان در نوار وضعیت و پنل گوشی",
                        iconRes = R.drawable.img_3d_bell_notification,
                        iconAccentColor = EmeraldPrimaryLight,
                        checked = pushNotificationsEnabled,
                        onCheckedChange = {
                            preferences.setNotificationsEnabled(it)
                            if (!it) NotificationDispatcher(context).cancelAllReminderNotifications()
                        }
                    )

                }
            }

            // Quiet Hours Section
            item {
                SettingsSectionCard(
                    title = "ساعات سکوت (Quiet Hours)",
                    badge = if (quietHoursEnabled) "فعال" else "غیرفعال"
                ) {
                    SettingsItem(
                        title = "فعال‌سازی ساعات سکوت",
                        subtitle = "عدم ارسال اعلان‌های معمولی در ساعات خواب و استراحت",
                        iconRes = R.drawable.img_3d_bell_notification,
                        iconAccentColor = WarningAmberLight,
                        checked = quietHoursEnabled,
                        onCheckedChange = { preferences.setQuietHoursEnabled(it) }
                    )

                    if (quietHoursEnabled) {
                        SettingsItem(
                            title = "بازه زمانی سکوت",
                            subtitle = "شروع از ساعت $quietStart تا $quietEnd صبح",
                            iconRes = R.drawable.img_3d_calendar,
                            iconAccentColor = InfoIndigoLight,
                            value = "$quietStart - $quietEnd",
                            onClick = {
                                val currentIndex = quietRangeOptions.indexOf(quietStartMinute to quietEndMinute)
                                val nextRange = quietRangeOptions[(currentIndex + 1).mod(quietRangeOptions.size)]
                                preferences.setQuietHours(nextRange.first, nextRange.second)
                            }
                        )

                        SettingsItem(
                            title = "مجوز هشدارهای مهم و فوری",
                            subtitle = "هشدارهای با اولویت HIGH (مثل سررسید اقساط سنگین) ارسال شوند",
                            iconRes = R.drawable.img_3d_card,
                            iconAccentColor = EmeraldPrimaryLight,
                            checked = allowHighPriorityInQuiet,
                            onCheckedChange = { preferences.setAllowHighPriorityInQuietHours(it) }
                        )
                    }
                }
            }

            // Sound & Vibration Section
            item {
                SettingsSectionCard(title = "صدا و لرزش") {
                    SettingsItem(
                        title = "پخش صدای اعلان",
                        subtitle = "استفاده از زنگ پیش‌فرض اعلان‌های سیستم",
                        iconRes = R.drawable.img_3d_bell_notification,
                        iconAccentColor = EmeraldPrimaryLight,
                        checked = soundEnabled,
                        onCheckedChange = { preferences.setNotificationSoundEnabled(it) }
                    )

                    SettingsItem(
                        title = "لرزش (ویبره)",
                        subtitle = "لرزش کوتاه هنگام صدور هشدار یادآوری",
                        iconRes = R.drawable.img_3d_oil,
                        iconAccentColor = WarningAmberLight,
                        checked = vibrationEnabled,
                        onCheckedChange = { preferences.setNotificationVibrationEnabled(it) }
                    )
                }
            }

            // Test & Simulation Section
            item {
                SettingsSectionCard(title = "تست و بررسی عملکرد هشدارها") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        val dispatcher = NotificationDispatcher(context)
                                        val testReminder = ReminderEntity(
                                            title = "تست هشدار دارینو",
                                            description = "سیستم اعلان‌ها و یادآورهای هوشمند فعال و آماده به کار است.",
                                            type = "GENERAL",
                                            date = "امروز",
                                            time = "هم‌اکنون",
                                            priority = "HIGH",
                                            notificationEnabled = true
                                        )
                                        val notificationId = dispatcher.dispatchNotification(testReminder)
                                        snackbarHostState.showSnackbar(
                                            if (notificationId >= 0) {
                                                "اعلان آزمایشی ارسال شد؛ نمایش آن به مجوز اعلان اندروید بستگی دارد."
                                            } else {
                                                "اعلان ارسال نشد؛ ابتدا اعلان‌های سراسری دارینو و مجوز اعلان اندروید را فعال کنید."
                                            }
                                        )
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("خطا در ارسال اعلان تست: ${e.localizedMessage}")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(RadiusMD)
                        ) {
                            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تست اعلان Push", fontSize = 12.sp)
                        }
}
                }
            }
        }
    }
}
