package com.example.ui.screens.settings.subviews

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brightness4
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import androidx.compose.material3.Button
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.screens.settings.components.SettingsHeader
import androidx.compose.material.icons.rounded.Calculate
import com.example.ui.screens.settings.model.AlertDeliveryPreference
import com.example.ui.screens.settings.model.AppCurrency
import com.example.ui.screens.settings.model.AppLanguage
import com.example.ui.screens.settings.model.AppThemeMode
import com.example.ui.screens.settings.model.NavigationTransitionAnimation
import com.example.ui.screens.settings.model.NumberDisplayMode
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM

@Composable
fun GeneralPreferencesScreen(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    currentCurrency: AppCurrency,
    onCurrencySelected: (AppCurrency) -> Unit,
    currentTheme: AppThemeMode,
    onThemeChanged: (AppThemeMode) -> Unit,
    currentNumberMode: NumberDisplayMode = NumberDisplayMode.PERSIAN,
    onNumberModeChanged: (NumberDisplayMode) -> Unit = {},
    currentDeliveryMode: AlertDeliveryPreference = AlertDeliveryPreference.NOTIFICATION_ONLY,
    onDeliveryModeChanged: (AlertDeliveryPreference) -> Unit = {},
    currentNavTransition: NavigationTransitionAnimation = NavigationTransitionAnimation.DYNAMIC,
    onNavTransitionChanged: (NavigationTransitionAnimation) -> Unit = {},
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isEn = currentLanguage == AppLanguage.ENGLISH
    fun checkFullScreenIntentAccess(): Boolean {
        if (Build.VERSION.SDK_INT < 34) return true
        return try {
            context.getSystemService(NotificationManager::class.java)?.canUseFullScreenIntent() == true
        } catch (_: Exception) {
            false
        }
    }
    fun checkExactAlarmAccess(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return try {
            context.getSystemService(android.app.AlarmManager::class.java)?.canScheduleExactAlarms() == true
        } catch (_: Exception) {
            false
        }
    }
    var canUseFullScreenIntent by remember(context) {
        mutableStateOf(checkFullScreenIntentAccess())
    }
    var canScheduleExactAlarms by remember(context) {
        mutableStateOf(checkExactAlarmAccess())
    }
    fun checkNotificationPermission(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED)
    var canPostNotifications by remember(context) {
        mutableStateOf(checkNotificationPermission())
    }
    var pendingDeliveryMode by remember { mutableStateOf<AlertDeliveryPreference?>(null) }

    fun requestDeliveryMode(mode: AlertDeliveryPreference) {
        val needsFullScreen = mode != AlertDeliveryPreference.NOTIFICATION_ONLY
        val ready = canPostNotifications && canScheduleExactAlarms &&
            (!needsFullScreen || canUseFullScreenIntent)
        if (ready) {
            pendingDeliveryMode = null
            onDeliveryModeChanged(mode)
        } else {
            // Keep the existing preference until the user grants every required access manually.
            pendingDeliveryMode = mode
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canUseFullScreenIntent = checkFullScreenIntentAccess()
                canScheduleExactAlarms = checkExactAlarmAccess()
                canPostNotifications = checkNotificationPermission()

                pendingDeliveryMode?.let { requestedMode ->
                    val needsFullScreen = requestedMode != AlertDeliveryPreference.NOTIFICATION_ONLY
                    if (canPostNotifications && canScheduleExactAlarms &&
                        (!needsFullScreen || canUseFullScreenIntent)
                    ) {
                        onDeliveryModeChanged(requestedMode)
                        pendingDeliveryMode = null
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides if (isEn) LayoutDirection.Ltr else LayoutDirection.Rtl
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                SettingsHeader(
                    title = if (isEn) "General Settings" else "تنظیمات عمومی",
                    subtitle = if (isEn) "Language, currency, theme, and animation" else "زبان، واحد پول، تم شب و انیمیشن",
                    showBack = true,
                    showSearch = false,
                    onBackClick = onBackClick
                )
            }
        ) { paddingValues ->
            // Clean scrollable layout fitting comfortably without clutter
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Language Setting (تنظیمات زبان برنامه)
                GeneralCompactCard(
                    title = if (isEn) "App Language" else "زبان برنامه",
                    subtitle = if (isEn) "Language for supported settings labels; numeral style is separate" else "زبان برچسب‌های پشتیبانی‌شده تنظیمات؛ شیوه نمایش اعداد جداگانه است",
                    icon = Icons.Rounded.Language,
                    iconColor = Color(0xFF0EA5E9)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactOptionChip(
                            title = if (isEn) "Persian" else "فارسی",
                            extra = "🇮🇷",
                            isSelected = currentLanguage == AppLanguage.PERSIAN,
                            onClick = {
                                onLanguageSelected(AppLanguage.PERSIAN)
                                Toast.makeText(context, if (isEn) "App language set to Persian" else "زبان برنامه به فارسی تنظیم شد", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CompactOptionChip(
                            title = "English",
                            extra = "🇬🇧",
                            isSelected = currentLanguage == AppLanguage.ENGLISH,
                            onClick = {
                                onLanguageSelected(AppLanguage.ENGLISH)
                                Toast.makeText(context, "Language switched to English", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 2. Currency Setting (تنظیمات واحد پول: ریال و تومان)
                GeneralCompactCard(
                    title = if (isEn) "Currency" else "واحد پول",
                    subtitle = if (isEn) "Transaction calculation unit" else "واحد محاسبه تراکنش‌ها",
                    icon = Icons.Rounded.MonetizationOn,
                    iconColor = EmeraldPrimaryLight
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactOptionChip(
                            title = if (isEn) "Toman" else "تومان",
                            extra = "🪙",
                            isSelected = currentCurrency == AppCurrency.TOMAN,
                            onClick = {
                                onCurrencySelected(AppCurrency.TOMAN)
                                Toast.makeText(
                                    context,
                                    if (isEn) "Currency set to Toman" else "واحد پول به تومان تنظیم شد (مبنای اصلی)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CompactOptionChip(
                            title = if (isEn) "Rial (IRR)" else "ریال (IRR)",
                            extra = "💰",
                            isSelected = currentCurrency == AppCurrency.RIAL,
                            onClick = {
                                onCurrencySelected(AppCurrency.RIAL)
                                Toast.makeText(
                                    context,
                                    if (isEn) "Currency set to Rial (1 Toman = 10 Rials)" else "واحد پول به ریال تنظیم شد (هر ۱ تومان = ۱۰ ریال)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 3. Number Display Setting (نحوه نمایش اعداد)
                GeneralCompactCard(
                    title = if (isEn) "Number Display" else "نحوه نمایش اعداد",
                    subtitle = if (isEn) "Choose Persian or English numerals across the app" else "فارسی یا انگلیسی بودن ارقام در سراسر برنامه",
                    icon = Icons.Rounded.Calculate,
                    iconColor = Color(0xFFF59E0B)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactOptionChip(
                            title = if (isEn) "Persian: 0123456789" else "فارسی: ۰۱۲۳۴۵۶۷۸۹",
                            extra = "🇮🇷",
                            isSelected = currentNumberMode == NumberDisplayMode.PERSIAN,
                            onClick = {
                                onNumberModeChanged(NumberDisplayMode.PERSIAN)
                                Toast.makeText(
                                    context,
                                    if (isEn) "Number display set to Persian" else "نحوه نمایش اعداد به فارسی تنظیم شد",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        CompactOptionChip(
                            title = if (isEn) "English: 0123456789" else "انگلیسی: 0123456789",
                            extra = "🇬🇧",
                            isSelected = currentNumberMode == NumberDisplayMode.ENGLISH,
                            onClick = {
                                onNumberModeChanged(NumberDisplayMode.ENGLISH)
                                Toast.makeText(
                                    context,
                                    if (isEn) "Number display set to English" else "نحوه نمایش اعداد به انگلیسی تنظیم شد",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 4. Day & Night Mode (حالت شب و روز)
                GeneralCompactCard(
                    title = if (isEn) "Day & Night Mode" else "حالت شب و روز",
                    subtitle = if (isEn) "Application visual theme" else "پوسته ظاهری برنامه",
                    icon = Icons.Rounded.Brightness4,
                    iconColor = Color(0xFF8B5CF6)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactThemeChip(
                            title = if (isEn) "Light" else "روز",
                            icon = Icons.Rounded.LightMode,
                            isSelected = currentTheme == AppThemeMode.LIGHT,
                            onClick = {
                                onThemeChanged(AppThemeMode.LIGHT)
                                Toast.makeText(
                                    context,
                                    if (isEn) "Light theme activated" else "پوسته روز (روشن) فعال شد",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CompactThemeChip(
                            title = if (isEn) "Dark" else "شب",
                            icon = Icons.Rounded.DarkMode,
                            isSelected = currentTheme == AppThemeMode.DARK,
                            onClick = {
                                onThemeChanged(AppThemeMode.DARK)
                                Toast.makeText(
                                    context,
                                    if (isEn) "Dark theme activated" else "پوسته شب (تاریک) فعال شد",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CompactThemeChip(
                            title = if (isEn) "System" else "سیستم",
                            icon = Icons.Rounded.BrightnessAuto,
                            isSelected = currentTheme == AppThemeMode.SYSTEM,
                            onClick = {
                                onThemeChanged(AppThemeMode.SYSTEM)
                                Toast.makeText(
                                    context,
                                    if (isEn) "Follow system theme activated" else "پوسته هماهنگ با سیستم فعال شد",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                GeneralCompactCard(
                    title = if (isEn) "Alert Delivery" else "روش دریافت هشدار",
                    subtitle = if (isEn) "Choose notification, full-screen alarm, or both" else "نحوه نمایش اعلان و آلارم تمام‌صفحه را انتخاب کنید",
                    icon = Icons.Rounded.NotificationsActive,
                    iconColor = Color(0xFF0EA5E9)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DeliveryModeCompactItem(
                            title = if (isEn) "Notifications only" else "فقط نوتیفیکیشن",
                            subtitle = if (isEn) "Standard Android notification" else "نمایش اعلان معمولی در نوار اعلان گوشی",
                            emoji = "🔔",
                            isSelected = currentDeliveryMode == AlertDeliveryPreference.NOTIFICATION_ONLY,
                            onClick = { requestDeliveryMode(AlertDeliveryPreference.NOTIFICATION_ONLY) }
                        )
                        DeliveryModeCompactItem(
                            title = if (isEn) "Full-screen alarm" else "آلارم تمام‌صفحه",
                            subtitle = if (isEn) "Open the reminder alarm screen at its scheduled time" else "نمایش صفحه اختصاصی هشدار در زمان تعیین‌شده",
                            emoji = "⏰",
                            isSelected = currentDeliveryMode == AlertDeliveryPreference.FULL_SCREEN_ALARM,
                            onClick = { requestDeliveryMode(AlertDeliveryPreference.FULL_SCREEN_ALARM) }
                        )
                        DeliveryModeCompactItem(
                            title = if (isEn) "Notification and full-screen alarm" else "نوتیفیکیشن و آلارم تمام‌صفحه",
                            subtitle = if (isEn) "Use both alert experiences" else "نمایش اعلان و آلارم تمام‌صفحه",
                            emoji = "🔔⏰",
                            isSelected = currentDeliveryMode == AlertDeliveryPreference.BOTH,
                            onClick = { requestDeliveryMode(AlertDeliveryPreference.BOTH) }
                        )
                        val needsFullScreenAccess = pendingDeliveryMode != null &&
                            pendingDeliveryMode != AlertDeliveryPreference.NOTIFICATION_ONLY &&
                            !canUseFullScreenIntent
                        val needsExactAlarmAccess = pendingDeliveryMode != null && !canScheduleExactAlarms
                        val needsNotificationPermission = pendingDeliveryMode != null && !canPostNotifications
                        if (pendingDeliveryMode != null) {
                            Text(
                                text = when {
                                    needsNotificationPermission && needsExactAlarmAccess && needsFullScreenAccess ->
                                        if (isEn) "To use this alert method, allow notifications, exact alarms, and full-screen alarms in Android settings. Darino will not enable these accesses automatically."
                                        else "برای استفاده از این روش هشدار، باید اعلان‌ها، آلارم دقیق و نمایش تمام‌صفحه را در تنظیمات گوشی خودتان فعال کنید. دارینو هیچ‌کدام را خودکار فعال نمی‌کند."
                                    needsNotificationPermission && needsExactAlarmAccess ->
                                        if (isEn) "Allow app notifications and exact alarms in Android settings. Darino will not enable them automatically."
                                        else "لطفاً اعلان‌های برنامه و دسترسی آلارم دقیق را دستی از تنظیمات گوشی فعال کنید. دارینو این دسترسی‌ها را خودکار فعال نمی‌کند."
                                    needsNotificationPermission && needsFullScreenAccess ->
                                        if (isEn) "Allow app notifications and full-screen alarm access in Android settings. Darino will not enable them automatically."
                                        else "لطفاً اعلان‌های برنامه و دسترسی آلارم تمام‌صفحه را دستی از تنظیمات گوشی فعال کنید. دارینو آن‌ها را خودکار فعال نمی‌کند."
                                    needsNotificationPermission ->
                                        if (isEn) "App notifications are disabled. Enable them manually in Android settings."
                                        else "اعلان‌های برنامه غیرفعال است. لطفاً آن را دستی از تنظیمات گوشی فعال کنید."
                                    needsExactAlarmAccess ->
                                        if (isEn) "Exact alarm access is required for scheduled reminders. Enable it manually in Android settings."
                                        else "برای اجرای دقیق یادآورها، دسترسی آلارم دقیق لازم است. لطفاً آن را دستی از تنظیمات گوشی فعال کنید."
                                    else ->
                                        if (isEn) "Full-screen alarm access is required for this method. Enable it manually in Android settings."
                                        else "برای این روش، دسترسی آلارم تمام‌صفحه لازم است. لطفاً آن را دستی از تنظیمات گوشی فعال کنید."
                                },
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(
                                onClick = {
                                    try {
                                        val settingsIntent = when {
                                            needsNotificationPermission -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                            }
                                            needsExactAlarmAccess -> Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            needsFullScreenAccess -> Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            else -> null
                                        }
                                        if (settingsIntent != null) {
                                            context.startActivity(settingsIntent)
                                        }
                                    } catch (_: Exception) {
                                        Toast.makeText(
                                            context,
                                            if (isEn) "Required Android settings are unavailable on this device." else "صفحه تنظیمات دسترسی موردنیاز در این دستگاه در دسترس نیست.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    when {
                                        needsNotificationPermission -> if (isEn) "Open notification settings" else "رفتن به تنظیمات اعلان‌ها"
                                        needsExactAlarmAccess -> if (isEn) "Open exact alarm settings" else "رفتن به تنظیمات آلارم دقیق"
                                        else -> if (isEn) "Open full-screen alarm settings" else "رفتن به تنظیمات آلارم تمام‌صفحه"
                                    }
                                )
                            }
                        }
                    }
                }

                // 5. Navigation Transition Animation
                NavigationTransitionCard(
                    isEn = isEn,
                    currentTransition = currentNavTransition,
                    onTransitionChanged = onNavTransitionChanged
                )
            }
        }
    }
}

@Composable
private fun NavigationTransitionCard(
    isEn: Boolean,
    currentTransition: NavigationTransitionAnimation,
    onTransitionChanged: (NavigationTransitionAnimation) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val iconColor = Color(0xFF6366F1) // Indigo / Violet

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLG),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(
            width = if (isExpanded) 1.5.dp else 1.dp,
            color = if (isExpanded) iconColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row (Clickable to toggle drawer expand/collapse)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RadiusMD))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(RadiusSM))
                            .background(iconColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SwapHoriz,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isEn) "Tab Transition Animation" else "انیمیشن جابجایی بین منوها",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            // Compact badge showing current selection
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = iconColor.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "${currentTransition.iconEmoji} ${if (isEn) currentTransition.titleEn else currentTransition.title}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = iconColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = if (isExpanded) {
                                if (isEn) "Select your preferred transition style (Collapsible)" else "سبک حرکتی مورد نظر خود را انتخاب کنید (کشویی)"
                            } else {
                                if (isEn) "Active: ${currentTransition.titleEn} (Tap to expand/change)" else "حالت فعال: ${currentTransition.title} (برای تغییر لمس کنید)"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Collapsible drawer toggle arrow
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Collapsible drawer content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val transitions = NavigationTransitionAnimation.entries
                    transitions.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { option ->
                                val isSelected = currentTransition == option
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(RadiusMD))
                                        .clickable {
                                            onTransitionChanged(option)
                                            Toast.makeText(
                                                context,
                                                if (isEn) "Transition set to ${option.titleEn}" else "انیمیشن منوها به «${option.title}» تنظیم شد",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                    shape = RoundedCornerShape(RadiusMD),
                                    color = if (isSelected) {
                                        iconColor.copy(alpha = if (isDark) 0.25f else 0.12f)
                                    } else {
                                        if (isDark) Color(0xFF1E2536) else Color(0xFFF8FAFC)
                                    },
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) iconColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = option.iconEmoji,
                                            fontSize = 14.sp
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isEn) option.titleEn else option.title,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 11.5.sp
                                                ),
                                                color = if (isSelected) iconColor else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = option.description,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 9.sp,
                                                    lineHeight = 12.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                maxLines = 1
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                tint = iconColor,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Explanatory footnote
                    Surface(
                        shape = RoundedCornerShape(RadiusSM),
                        color = iconColor.copy(alpha = 0.08f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "⚡", fontSize = 12.sp)
                            Text(
                                text = if (isEn) {
                                    "When switching between Home, Vehicle, Installments, Reminders and Reports, this transition animation will play smoothly."
                                } else {
                                    "هنگام لمس هر یک از دکمه‌های نوار پایینی و جابجایی بین منوهای اصلی، این سبک حرکتی اجرا می‌شود."
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneralCompactCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLG),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(RadiusSM))
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            content()
        }
    }
}

@Composable
private fun CompactOptionChip(
    title: String,
    extra: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(RadiusMD))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(RadiusMD),
        color = if (isSelected) EmeraldPrimaryLight.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) EmeraldPrimaryLight else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = extra, fontSize = 16.sp)
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) EmeraldPrimaryLight else MaterialTheme.colorScheme.onSurface
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "انتخاب شده",
                    tint = EmeraldPrimaryLight,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactThemeChip(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(RadiusMD))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(RadiusMD),
        color = if (isSelected) EmeraldPrimaryLight.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) EmeraldPrimaryLight else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) EmeraldPrimaryLight else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) EmeraldPrimaryLight else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DeliveryModeCompactItem(
    title: String,
    subtitle: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMD))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(RadiusMD),
        color = if (isSelected) EmeraldPrimaryLight.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) EmeraldPrimaryLight else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = emoji, fontSize = 17.sp)
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) EmeraldPrimaryLight else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = if (isSelected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) EmeraldPrimaryLight else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
