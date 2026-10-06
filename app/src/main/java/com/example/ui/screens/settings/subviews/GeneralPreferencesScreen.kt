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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.launch
import com.example.ui.screens.settings.components.SettingsHeader
import com.example.ui.screens.settings.model.AlertDeliveryPreference
import com.example.ui.screens.settings.model.AppCurrency
import com.example.ui.screens.settings.model.AppLanguage
import com.example.ui.screens.settings.model.AppThemeMode
import com.example.ui.screens.settings.model.NavigationTransitionAnimation
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
    currentDeliveryMode: AlertDeliveryPreference = AlertDeliveryPreference.BOTH,
    onDeliveryModeChanged: (AlertDeliveryPreference) -> Unit = {},
    currentNavTransition: NavigationTransitionAnimation = NavigationTransitionAnimation.DYNAMIC,
    onNavTransitionChanged: (NavigationTransitionAnimation) -> Unit = {},
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isEn = currentLanguage == AppLanguage.ENGLISH

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
                    subtitle = if (isEn) "Display language for texts and numbers" else "زبان نمایش متون و اعداد",
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
                                Toast.makeText(context, "زبان برنامه به فارسی تنظیم شد", Toast.LENGTH_SHORT).show()
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

                // 3. Day & Night Mode (حالت شب و روز)
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

                // 4. Navigation Transition Animation (انیمیشن جابجایی بین منوها - کشویی و جمع‌وجور)
                NavigationTransitionCard(
                    isEn = isEn,
                    currentTransition = currentNavTransition,
                    onTransitionChanged = onNavTransitionChanged
                )

                // 5. Developer Mode / Clean Slate Section (خام‌سازی و پاکسازی داده‌های تستی)
                CleanSlateDeveloperCard(isEn = isEn)
            }
        }
    }
}

@Composable
private fun CleanSlateDeveloperCard(
    isEn: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var dialogAction by remember { mutableStateOf("clean") } // "clean" or "restore"

    val financeRepo = com.example.ui.screens.finance.data.LocalFinanceRepository.instance
    val vehicleRepo = com.example.vehicle.data.VehicleRepository.instance
    val backupRepo = com.example.data.backup.LocalBackupRepository(context)
    val scope = rememberCoroutineScope()

    Surface(
        shape = RoundedCornerShape(RadiusLG),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isExpanded) Color(0xFFEF4444).copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CleaningServices,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isEn) "Developer Mode / Test Data" else "تنظیمات توسعه‌دهنده و داده‌های آزمایشی",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.15f),
                        ) {
                            Text(
                                text = if (isEn) "RESET" else "پاکسازی",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = Color(0xFFEF4444),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = if (isEn) "Clean slate & erase mock sample records" else "خام‌سازی برنامه و حذف داده‌های فرضی جهت ورود اطلاعات واقعی",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isEn)
                            "You can erase all sample mock entries (transactions, vehicles, loans) to start using the app with your real data, or restore them anytime for testing."
                        else
                            "با لمس دکمه زیر، تمامی اطلاعات فرضی و آزمایشی برنامه (تراکنش‌ها، خودروها، اقساط، بودجه‌ها) به صورت کامل حذف شده و برنامه آماده ثبت اطلاعات واقعی شما می‌شود.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Clean Slate Button
                        Surface(
                            shape = RoundedCornerShape(RadiusSM),
                            color = Color(0xFFEF4444),
                            modifier = Modifier
                                .weight(1.3f)
                                .clickable {
                                    dialogAction = "clean"
                                    showConfirmDialog = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEn) "Erase Test Data" else "پاکسازی داده‌های تستی",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                                    color = Color.White
                                )
                            }
                        }

                        // Restore Samples Button
                        Surface(
                            shape = RoundedCornerShape(RadiusSM),
                            color = EmeraldPrimaryLight.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, EmeraldPrimaryLight.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    dialogAction = "restore"
                                    showConfirmDialog = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SettingsBackupRestore,
                                    contentDescription = null,
                                    tint = EmeraldPrimaryLight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEn) "Restore Mock" else "بازیابی نمونه‌ها",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                                    color = EmeraldPrimaryLight
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConfirmDialog) {
        com.example.ui.screens.settings.components.SettingsConfirmationDialog(
            isOpen = true,
            title = if (dialogAction == "clean")
                (if (isEn) "Clean Slate App" else "خام‌سازی برنامه و حذف داده‌های تستی")
            else
                (if (isEn) "Restore Mock Data" else "بازیابی داده‌های تستی"),
            message = if (dialogAction == "clean")
                (if (isEn) "All sample transactions, vehicles, and loan records will be wiped out. Are you sure?"
                else "تمامی داده‌های تستی و فرضی پاک شده و برنامه به حالت خام می‌رود تا اطلاعات واقعی خود را وارد کنید. آیا ادامه می‌دهید؟")
            else
                (if (isEn) "Restore sample test data for demo purposes?"
                else "آیا داده‌های نمونه و تستی برنامه جهت دمو و بررسی مجدداً بارگذاری شوند؟"),
            confirmButtonText = if (dialogAction == "clean")
                (if (isEn) "Confirm & Wipe" else "تأیید و پاکسازی")
            else
                (if (isEn) "Restore" else "تأیید و بازیابی"),
            onConfirm = {
                showConfirmDialog = false
                scope.launch {
                    if (dialogAction == "clean") {
                        financeRepo.clearAllTransactionsData()
                        vehicleRepo.clearAllVehiclesData()
                        com.example.ui.screens.installments.data.LocalInstallmentRepository.instance.clearAllInstallments(context)
                        backupRepo.deleteAllData()
                        Toast.makeText(
                            context,
                            if (isEn) "App reset to clean slate." else "برنامه با موفقیت خام‌سازی شد و داده‌های تستی پاک شدند.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        financeRepo.restoreSampleTransactions()
                        vehicleRepo.restoreSampleVehicles()
                        Toast.makeText(
                            context,
                            if (isEn) "Sample data restored." else "داده‌های تستی و نمونه با موفقیت بازیابی شدند.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onDismiss = { showConfirmDialog = false }
        )
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
