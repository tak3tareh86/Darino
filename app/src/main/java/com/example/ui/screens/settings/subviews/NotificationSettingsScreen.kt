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
import com.example.data.security.SessionManager
import com.example.reminder.data.ReminderEntity
import com.example.reminder.domain.MockSmsDispatcher
import com.example.reminder.domain.NotificationDispatcher
import com.example.ui.screens.settings.components.SettingsHeader
import com.example.ui.screens.settings.components.SettingsItem
import com.example.ui.screens.settings.components.SettingsSectionCard
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM
import com.example.ui.theme.WarningAmberLight
import com.example.util.IranianPhoneUtils
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

    var pushNotificationsEnabled by remember { mutableStateOf(true) }
    var smsNotificationsEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }

    // Quiet Hours
    var quietHoursEnabled by remember { mutableStateOf(true) }
    var quietStart by remember { mutableStateOf("۲۳:۰۰") }
    var quietEnd by remember { mutableStateOf("۰۷:۰۰") }
    var allowHighPriorityInQuiet by remember { mutableStateOf(true) }

    // Verified phone from session
    val sessionUser = remember { SessionManager.currentUser }
    val rawPhone = sessionUser?.phoneNumber ?: "09123456789"
    val maskedPhone = remember(rawPhone) { IranianPhoneUtils.maskPhoneNumber(rawPhone) }

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
                        onCheckedChange = { pushNotificationsEnabled = it }
                    )

                    SettingsItem(
                        title = "یادآوری از طریق پیامک (SMS)",
                        subtitle = "ارسال پیامک یادآوری به شماره تأییدشده",
                        iconRes = R.drawable.img_3d_shield_security,
                        iconAccentColor = InfoIndigoLight,
                        checked = smsNotificationsEnabled,
                        onCheckedChange = { smsNotificationsEnabled = it }
                    )

                    // Phone Number Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(RadiusMD))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(RadiusMD))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "شماره موبایل تأییدشده (Primary)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = maskedPhone,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(RadiusSM),
                                color = EmeraldPrimaryLight.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "تأیید با OTP",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = EmeraldPrimaryLight,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
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
                        onCheckedChange = { quietHoursEnabled = it }
                    )

                    if (quietHoursEnabled) {
                        SettingsItem(
                            title = "بازه زمانی سکوت",
                            subtitle = "شروع از ساعت $quietStart تا $quietEnd صبح",
                            iconRes = R.drawable.img_3d_calendar,
                            iconAccentColor = InfoIndigoLight,
                            value = "$quietStart - $quietEnd",
                            onClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("بازه سکوت پیش‌فرض: ۲۳:۰۰ تا ۰۷:۰۰")
                                }
                            }
                        )

                        SettingsItem(
                            title = "مجوز هشدارهای مهم و فوری",
                            subtitle = "هشدارهای با اولویت HIGH (مثل سررسید اقساط سنگین) ارسال شوند",
                            iconRes = R.drawable.img_3d_card,
                            iconAccentColor = EmeraldPrimaryLight,
                            checked = allowHighPriorityInQuiet,
                            onCheckedChange = { allowHighPriorityInQuiet = it }
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
                        onCheckedChange = { soundEnabled = it }
                    )

                    SettingsItem(
                        title = "لرزش (ویبره)",
                        subtitle = "لرزش کوتاه هنگام صدور هشدار یادآوری",
                        iconRes = R.drawable.img_3d_oil,
                        iconAccentColor = WarningAmberLight,
                        checked = vibrationEnabled,
                        onCheckedChange = { vibrationEnabled = it }
                    )
                }
            }

            // Test & Simulation Section
            item {
                SettingsSectionCard(title = "تست و بررسی عملکرد هشدارها") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                            priority = "HIGH"
                                        )
                                        dispatcher.dispatchNotification(testReminder)
                                        snackbarHostState.showSnackbar("اعلان تستی با موفقیت به نوار وضعیت ارسال شد.")
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("خطا در ارسال اعلان تست: ${e.localizedMessage}")
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(RadiusMD)
                        ) {
                            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تست اعلان Push", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    val smsDispatcher = MockSmsDispatcher()
                                    val res = smsDispatcher.sendSms(
                                        phoneNumber = rawPhone,
                                        message = "دارینو: این یک پیامک آزمایشی است. سررسید قسط شما نزدیک است.",
                                        reminderType = "TEST"
                                    )
                                    if (res.success) {
                                        snackbarHostState.showSnackbar("پیامک آزمایشی با شناسه ${res.providerMessageId} با موفقیت در صف ارسال ثبت شد.")
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(RadiusMD)
                        ) {
                            Icon(Icons.Rounded.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تست پیامک Mock", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
