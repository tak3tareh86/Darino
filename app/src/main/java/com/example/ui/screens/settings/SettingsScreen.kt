package com.example.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.screens.settings.components.CalendarSelectionSheet
import com.example.ui.screens.settings.components.CurrencySelectionSheet
import com.example.ui.screens.settings.components.LanguageSelectionSheet
import com.example.ui.screens.settings.components.SearchResultList
import com.example.ui.screens.settings.components.SettingsConfirmationDialog
import com.example.ui.screens.settings.components.SettingsHeader
import com.example.ui.screens.settings.components.SettingsItem
import com.example.ui.screens.settings.components.SettingsOverviewCard
import com.example.ui.screens.settings.components.SettingsSearchBar
import com.example.ui.screens.settings.components.SettingsSectionCard
import com.example.ui.screens.settings.components.UserAccountCard
import com.example.ui.screens.settings.components.WeekStartSelectionSheet
import com.example.ui.screens.settings.model.AccentColorOption
import com.example.ui.screens.settings.model.AlertDeliveryPreference
import com.example.ui.screens.settings.model.AppCalendar
import com.example.ui.screens.settings.model.AppCurrency
import com.example.ui.screens.settings.model.AppLanguage
import com.example.ui.screens.settings.model.AppLockType
import com.example.ui.screens.settings.model.AppThemeMode
import com.example.ui.screens.settings.model.SettingsDestination
import com.example.ui.screens.settings.model.SettingsMockDataSource
import com.example.ui.screens.settings.model.WeekStartDay
import com.example.ui.screens.settings.subviews.AboutScreen
import com.example.ui.screens.settings.subviews.AccountsScreen
import com.example.ui.screens.settings.subviews.AppearanceSettingsScreen
import com.example.ui.screens.settings.subviews.AppLockSettingsScreen
import com.example.ui.screens.settings.subviews.CategoriesScreen
import com.example.ui.screens.settings.subviews.DataBackupScreen
import com.example.ui.screens.settings.subviews.DataManagementScreen
import com.example.ui.screens.settings.subviews.GeneralPreferencesScreen
import com.example.ui.screens.settings.subviews.NotificationSettingsScreen
import com.example.ui.screens.settings.subviews.ReminderPreferencesScreen
import com.example.ui.screens.settings.subviews.SecurityPrivacyScreen
import com.example.ui.screens.settings.subviews.SmsSettingsScreen
import com.example.ui.screens.settings.subviews.SupportBackupScreen
import com.example.ui.screens.settings.subviews.SupportScreen
import com.example.ui.screens.settings.subviews.VehicleManagementScreen
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.WarningAmberLight
import com.example.util.IranianPhoneUtils
import com.example.financial_health.presentation.FinancialHealthScreen
import com.example.financial_health.presentation.components.EditIncomeBottomSheet
import com.example.financial_health.viewmodel.FinancialHealthViewModel
import com.example.ui.screens.lock.AppLockViewModel
import com.example.data.security.UnlockMethod
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: AppThemeMode,
    onThemeChanged: (AppThemeMode) -> Unit,
    onBackClick: (() -> Unit)? = null,
    onOpenAuth: () -> Unit = {},
    onLogout: () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentDestination by remember { mutableStateOf(SettingsDestination.MAIN) }

    // Settings States
    var currentLanguage by remember { mutableStateOf(AppLanguage.PERSIAN) }
    var currentCurrency by remember { mutableStateOf(AppCurrency.TOMAN) }
    var currentCalendar by remember { mutableStateOf(AppCalendar.SHAMSI) }
    var currentWeekStart by remember { mutableStateOf(WeekStartDay.SATURDAY) }
    var currentAccent by remember { mutableStateOf(AccentColorOption.TEAL) }
    var currentDeliveryMode by remember { mutableStateOf(AlertDeliveryPreference.BOTH) }

    val lockViewModel: AppLockViewModel = viewModel()
    val lockUiState by lockViewModel.uiState.collectAsState()
    val securitySettings = lockUiState.securitySettings

    val currentLockType = when {
        !securitySettings.appLockEnabled -> AppLockType.NONE
        securitySettings.biometricEnabled && securitySettings.defaultUnlockMethod == UnlockMethod.BIOMETRIC -> AppLockType.BIOMETRIC
        securitySettings.pinEnabled -> AppLockType.PIN
        securitySettings.patternEnabled -> AppLockType.PIN
        else -> AppLockType.NONE
    }

    // Dialog Sheets
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showCurrencySheet by remember { mutableStateOf(false) }
    var showCalendarSheet by remember { mutableStateOf(false) }
    var showWeekStartSheet by remember { mutableStateOf(false) }
    var showIncomeSheet by remember { mutableStateOf(false) }
    val incomeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val healthViewModel: FinancialHealthViewModel = viewModel()
    val healthState by healthViewModel.uiState.collectAsState()

    // Search
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Handle Back Press when in subscreen
    BackHandler(enabled = currentDestination != SettingsDestination.MAIN || onBackClick != null) {
        if (currentDestination != SettingsDestination.MAIN) {
            currentDestination = SettingsDestination.MAIN
        } else if (onBackClick != null) {
            onBackClick()
        }
    }

    AnimatedContent(
        targetState = currentDestination,
        transitionSpec = {
            if (targetState != SettingsDestination.MAIN) {
                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> width } + fadeOut()
                )
            } else {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width } + fadeOut()
                )
            }
        },
        label = "settingsDestinationTransition"
    ) { destination ->
        when (destination) {
            SettingsDestination.MAIN -> {
                SettingsMainView(
                    isSearchActive = isSearchActive,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onSearchToggle = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) searchQuery = ""
                    },
                    currentLanguage = currentLanguage,
                    currentCurrency = currentCurrency,
                    currentCalendar = currentCalendar,
                    currentWeekStart = currentWeekStart,
                    currentTheme = currentTheme,
                    currentAccent = currentAccent,
                    currentLockType = currentLockType,
                    monthlyIncome = healthState.monthlyIncome,
                    pressurePercentage = healthState.pressurePercentage,
                    healthStatusTitle = healthState.healthStatus.title,
                    onEditIncomeClick = { showIncomeSheet = true },
                    onNavigate = { currentDestination = it },
                    onOpenLanguageSheet = { showLanguageSheet = true },
                    onOpenCurrencySheet = { showCurrencySheet = true },
                    onOpenCalendarSheet = { showCalendarSheet = true },
                    onOpenWeekStartSheet = { showWeekStartSheet = true },
                    onBackClick = onBackClick,
                    onLogout = onLogout,
                    bottomBar = bottomBar,
                    modifier = modifier
                )
            }
            SettingsDestination.GENERAL -> {
                GeneralPreferencesScreen(
                    currentLanguage = currentLanguage,
                    onLanguageSelected = { currentLanguage = it },
                    currentCurrency = currentCurrency,
                    onCurrencySelected = { currentCurrency = it },
                    currentTheme = currentTheme,
                    onThemeChanged = onThemeChanged,
                    currentDeliveryMode = currentDeliveryMode,
                    onDeliveryModeChanged = { currentDeliveryMode = it },
                    onBackClick = { currentDestination = SettingsDestination.MAIN }
                )
            }
            SettingsDestination.SUPPORT_BACKUP,
            SettingsDestination.SUPPORT,
            SettingsDestination.BACKUP -> {
                SupportBackupScreen(onBackClick = { currentDestination = SettingsDestination.MAIN })
            }
            SettingsDestination.ACCOUNTS -> {
                AccountsScreen(onBackClick = { currentDestination = SettingsDestination.MAIN })
            }
            SettingsDestination.CATEGORIES -> {
                CategoriesScreen(onBackClick = { currentDestination = SettingsDestination.MAIN })
            }
            SettingsDestination.VEHICLES -> {
                VehicleManagementScreen(onBackClick = { currentDestination = SettingsDestination.MAIN })
            }
            SettingsDestination.NOTIFICATIONS -> {
                NotificationSettingsScreen(
                    onBackClick = { currentDestination = SettingsDestination.MAIN },
                    onNavigateToReminders = { currentDestination = SettingsDestination.REMINDERS }
                )
            }
            SettingsDestination.SMS -> {
                SmsSettingsScreen(
                    onBackClick = { currentDestination = SettingsDestination.MAIN }
                )
            }
            SettingsDestination.REMINDERS -> {
                ReminderPreferencesScreen(onBackClick = { currentDestination = SettingsDestination.NOTIFICATIONS })
            }
            SettingsDestination.APPEARANCE -> {
                AppearanceSettingsScreen(
                    currentTheme = currentTheme,
                    onThemeChanged = onThemeChanged,
                    currentAccent = currentAccent,
                    onAccentChanged = { currentAccent = it },
                    onBackClick = { currentDestination = SettingsDestination.MAIN }
                )
            }
            SettingsDestination.DATA_MANAGEMENT -> {
                DataManagementScreen(onBackClick = { currentDestination = SettingsDestination.MAIN })
            }
            SettingsDestination.SECURITY -> {
                SecurityPrivacyScreen(
                    currentLockType = currentLockType,
                    onNavigateToAppLock = { currentDestination = SettingsDestination.APP_LOCK },
                    onNavigateToLockAndAuth = { currentDestination = SettingsDestination.LOCK_AND_AUTH },
                    onBackClick = { currentDestination = SettingsDestination.MAIN }
                )
            }
            SettingsDestination.APP_LOCK -> {
                AppLockSettingsScreen(
                    currentLockType = currentLockType,
                    onLockTypeSelected = { type ->
                        when (type) {
                            AppLockType.NONE -> {
                                lockViewModel.disableAppLock()
                            }
                            AppLockType.PIN -> {
                                lockViewModel.toggleAppLock(true)
                                if (!securitySettings.pinEnabled) {
                                    lockViewModel.setPin("1111")
                                }
                                lockViewModel.setDefaultUnlockMethod(UnlockMethod.PIN)
                            }
                            AppLockType.BIOMETRIC -> {
                                lockViewModel.toggleAppLock(true)
                                lockViewModel.setBiometricEnabledDirectly(true)
                                lockViewModel.setDefaultUnlockMethod(UnlockMethod.BIOMETRIC)
                            }
                        }
                    },
                    onBackClick = { currentDestination = SettingsDestination.SECURITY }
                )
            }
            SettingsDestination.LOCK_AND_AUTH -> {
                com.example.ui.screens.settings.subviews.LockAndAuthenticationScreen(
                    viewModel = lockViewModel,
                    onBackClick = { currentDestination = SettingsDestination.SECURITY }
                )
            }
            SettingsDestination.ABOUT -> {
                AboutScreen(onBackClick = { currentDestination = SettingsDestination.MAIN })
            }
            SettingsDestination.FINANCIAL_HEALTH -> {
                FinancialHealthScreen(
                    onBackClick = { currentDestination = SettingsDestination.MAIN },
                    viewModel = healthViewModel
                )
            }
        }
    }

    // Modal Sheets
    LanguageSelectionSheet(
        isOpen = showLanguageSheet,
        currentLanguage = currentLanguage,
        onLanguageSelected = { currentLanguage = it },
        onDismiss = { showLanguageSheet = false }
    )

    CurrencySelectionSheet(
        isOpen = showCurrencySheet,
        currentCurrency = currentCurrency,
        onCurrencySelected = { currentCurrency = it },
        onDismiss = { showCurrencySheet = false }
    )

    CalendarSelectionSheet(
        isOpen = showCalendarSheet,
        currentCalendar = currentCalendar,
        onCalendarSelected = { currentCalendar = it },
        onDismiss = { showCalendarSheet = false }
    )

    WeekStartSelectionSheet(
        isOpen = showWeekStartSheet,
        currentDay = currentWeekStart,
        onDaySelected = { currentWeekStart = it },
        onDismiss = { showWeekStartSheet = false }
    )

    if (showIncomeSheet) {
        EditIncomeBottomSheet(
            currentIncome = healthState.monthlyIncome,
            currentFixedExpenses = healthState.fixedExpenses,
            sheetState = incomeSheetState,
            onDismiss = { showIncomeSheet = false },
            onSave = { income, fixed ->
                healthViewModel.saveProfile(income, healthState.totalInstallments, fixed)
                showIncomeSheet = false
            }
        )
    }
}

@Composable
private fun SettingsMainView(
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: () -> Unit,
    currentLanguage: AppLanguage,
    currentCurrency: AppCurrency,
    currentCalendar: AppCalendar,
    currentWeekStart: WeekStartDay,
    currentTheme: AppThemeMode,
    currentAccent: AccentColorOption,
    currentLockType: AppLockType,
    monthlyIncome: Long = 25_000_000L,
    pressurePercentage: Float = 35f,
    healthStatusTitle: String = "مناسب",
    onEditIncomeClick: () -> Unit = {},
    onNavigate: (SettingsDestination) -> Unit,
    onOpenLanguageSheet: () -> Unit,
    onOpenCurrencySheet: () -> Unit,
    onOpenCalendarSheet: () -> Unit,
    onOpenWeekStartSheet: () -> Unit,
    onBackClick: (() -> Unit)? = null,
    onLogout: () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    if (showLogoutConfirmDialog) {
        SettingsConfirmationDialog(
            isOpen = true,
            title = "خروج از حساب کاربری",
            message = "آیا اطمینان دارید که می‌خواهید از حساب کاربری خود خارج شوید؟ برای استفاده مجدد نیاز به ورود با شماره همراه خواهید داشت.",
            confirmButtonText = "خروج از حساب",
            isDanger = true,
            onConfirm = {
                showLogoutConfirmDialog = false
                onLogout()
            },
            onDismiss = { showLogoutConfirmDialog = false }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = bottomBar,
        topBar = {
            SettingsHeader(
                title = "تنظیمات و مدیریت",
                subtitle = "شخصی‌سازی، مدیریت حساب‌ها و گاراژ",
                showBack = onBackClick != null,
                onBackClick = onBackClick ?: {},
                showSearch = true,
                isSearchActive = isSearchActive,
                onSearchToggle = onSearchToggle
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar (Animated)
            if (isSearchActive) {
                SettingsSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    onClearQuery = { onSearchQueryChange("") },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (isSearchActive && searchQuery.isNotBlank()) {
                // Filter search items
                val filteredItems = remember(searchQuery) {
                    SettingsMockDataSource.searchableItems.filter {
                        it.title.contains(searchQuery.trim(), ignoreCase = true) ||
                                it.subtitle.contains(searchQuery.trim(), ignoreCase = true) ||
                                it.section.contains(searchQuery.trim(), ignoreCase = true)
                    }
                }

                SearchResultList(
                    results = filteredItems,
                    onItemClick = { destination ->
                        onNavigate(destination)
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Top User Account Card (کادر حساب کاربری بالای منوی تنظیمات)
                    item {
                        UserAccountCard(
                            onLogoutClick = { showLogoutConfirmDialog = true }
                        )
                    }

                    // 2. Main Settings: General Settings & Support & Backup
                    item {
                        SettingsSectionCard(title = "مدیریت مالی و تعهدات") {
                            SettingsItem(
                                title = "درآمد ماهانه من",
                                subtitle = "تنظیم و ویرایش درآمد ماهانه جهت تحلیل سلامت مالی (${IranianPhoneUtils.convertDigitsToPersian("%,d".format(monthlyIncome))} تومان)",
                                vectorIcon = Icons.Rounded.MonetizationOn,
                                iconAccentColor = Color(0xFF10B981),
                                onClick = onEditIncomeClick
                            )

                            SettingsItem(
                                title = "سلامت تعهدات مالی",
                                subtitle = "شاخص فشار ماهانه (${IranianPhoneUtils.convertDigitsToPersian(pressurePercentage.toInt().toString())}٪ - $healthStatusTitle) و پیش‌بینی ماه آینده",
                                iconRes = R.drawable.img_3d_analytics,
                                iconAccentColor = Color(0xFF3B82F6),
                                onClick = { onNavigate(SettingsDestination.FINANCIAL_HEALTH) }
                            )
                        }
                    }

                    item {
                        SettingsSectionCard(title = "تنظیمات و امکانات") {
                            SettingsItem(
                                title = "تنظیمات عمومی",
                                subtitle = "زبان (${currentLanguage.title})، واحد پول (${currentCurrency.title})، حالت شب و روز (${currentTheme.title})، روش ارسال هشدارها",
                                vectorIcon = Icons.Rounded.Tune,
                                iconAccentColor = Color(0xFF0EA5E9),
                                onClick = { onNavigate(SettingsDestination.GENERAL) }
                            )

                            SettingsItem(
                                title = "پشتیبانی و بک‌آپ‌گیری",
                                subtitle = "تهیه نسخه پشتیبان از اطلاعات، بازیابی فایل، راهنما و ارتباط با پشتیبانی",
                                iconRes = R.drawable.img_3d_cloud_backup,
                                iconAccentColor = EmeraldPrimaryLight,
                                onClick = { onNavigate(SettingsDestination.SUPPORT_BACKUP) }
                            )
                        }
                    }

                    // 3. Security Section
                    item {
                        SettingsSectionCard(title = "امنیت و دسترسی") {
                            SettingsItem(
                                title = "قفل و احراز هویت",
                                subtitle = "تنظیم رمز ورود عددی (PIN)، الگو، اثر انگشت، روش پیش‌فرض و حریم خصوصی مبالغ",
                                iconRes = R.drawable.img_3d_shield_security,
                                iconAccentColor = EmeraldPrimaryLight,
                                onClick = { onNavigate(SettingsDestination.LOCK_AND_AUTH) }
                            )
                        }
                    }

                    // Bottom Padding
                    item {
                        Spacer(modifier = Modifier.height(84.dp))
                    }
                }
            }
        }
    }
}
