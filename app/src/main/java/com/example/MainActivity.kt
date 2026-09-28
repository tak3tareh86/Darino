package com.example

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calendar.presentation.FinancialCalendarScreen
import com.example.data.network.NetworkStateMonitor
import com.example.data.security.AppLockState
import com.example.data.security.SessionManager
import com.example.data.sync.SyncWorker
import com.example.data.backup.WeeklyBackupWorker
import com.example.ui.screens.auth.AuthBottomSheet
import com.example.ui.screens.auth.AuthViewModel
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.finance.FinancialScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.components.BottomNavItem
import com.example.ui.screens.home.components.HomeBottomNavigation
import com.example.ui.screens.installments.InstallmentsScreen
import com.example.ui.screens.lock.AppLockViewModel
import com.example.ui.screens.lock.LockedAppScreen
import com.example.reminder.presentation.NotificationCenterScreen
import com.example.reminder.presentation.ReminderScreen
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.financial_health.presentation.FinancialHealthScreen
import com.example.ui.screens.settings.model.AppThemeMode
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.screens.vehicle.VehicleServicesScreen
import com.example.ui.screens.subscription.SubscriptionGate
import com.example.ui.screens.subscription.SubscriptionScreen
import com.example.ui.screens.subscription.SubscriptionViewModel
import com.example.ui.theme.FinanceManagerTheme

class MainActivity : FragmentActivity() {

    private var networkMonitor: NetworkStateMonitor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize secure session manager
        SessionManager.init(this)

        // 2. Schedule periodic background sync worker with network constraints
        SyncWorker.enqueuePeriodic(this)
        WeeklyBackupWorker.enqueueWeekly(this)

        // 3. Monitor network connectivity and auto-trigger sync when back online
        networkMonitor = NetworkStateMonitor(this) {
            SyncWorker.enqueueOneTime(this)
        }

        setContent {
            var currentThemeMode by remember { mutableStateOf(AppThemeMode.SYSTEM) }
            val isDark = when (currentThemeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            FinanceManagerTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigationContainer(
                        currentThemeMode = currentThemeMode,
                        onThemeModeChanged = { currentThemeMode = it }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        networkMonitor?.unregister()
    }
}

@Composable
fun AppNavigationContainer(
    currentThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    onThemeModeChanged: (AppThemeMode) -> Unit = {}
) {
    var isSplashFinished by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf(BottomNavItem.HOME) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isNotificationsOpen by remember { mutableStateOf(false) }
    var isFinancialHealthOpen by remember { mutableStateOf(false) }
    var isSubscriptionOpen by remember { mutableStateOf(false) }
    var showAuthSheet by remember { mutableStateOf(false) }

    val authViewModel: AuthViewModel = viewModel()
    val authUiState by authViewModel.uiState.collectAsState()

    val lockViewModel: AppLockViewModel = viewModel()
    val lockUiState by lockViewModel.uiState.collectAsState()

    val subscriptionViewModel: SubscriptionViewModel = viewModel()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                lockViewModel.onAppBackgrounded()
            } else if (event == Lifecycle.Event.ON_START) {
                lockViewModel.onAppForegrounded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 1. Splash Screen Phase
    if (!isSplashFinished) {
        SplashScreen(
            onSplashFinished = {
                isSplashFinished = true
                lockViewModel.checkSessionAndLockStatus()
            }
        )
        return
    }

    // 2. Unauthenticated user -> Login Screen (Phone + OTP)
    if (!authUiState.isAuthenticated || lockUiState.appLockState is AppLockState.Unauthenticated) {
        LoginScreen(
            onLoginSuccess = {
                lockViewModel.checkSessionAndLockStatus()
            },
            viewModel = authViewModel
        )
        return
    }

    // 3. Authenticated user with Active Lock -> LockedAppScreen (PIN / Pattern / Biometric)
    if (lockUiState.appLockState is AppLockState.Locked) {
        LockedAppScreen(
            viewModel = lockViewModel,
            onRecoverWithOtp = {
                authViewModel.logout()
                lockViewModel.checkSessionAndLockStatus()
            }
        )
        return
    }

    // 4. Authenticated and Unlocked -> Main Application
    SubscriptionGate(subscriptionViewModel = subscriptionViewModel) {
        val showBottomBar = !isSubscriptionOpen && !isSettingsOpen && !isNotificationsOpen && !isFinancialHealthOpen

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showBottomBar) {
                    HomeBottomNavigation(
                        selectedItem = currentTab,
                        onItemSelected = { tab ->
                            currentTab = tab
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 720.dp)
                ) {
                    if (isSubscriptionOpen) {
                        SubscriptionScreen(
                            onBackClick = { isSubscriptionOpen = false },
                            viewModel = subscriptionViewModel
                        )
                    } else if (isSettingsOpen) {
                        SettingsScreen(
                            currentTheme = currentThemeMode,
                            onThemeChanged = onThemeModeChanged,
                            onBackClick = { isSettingsOpen = false },
                            onOpenAuth = { showAuthSheet = true },
                            onLogout = {
                                authViewModel.logout()
                                lockViewModel.checkSessionAndLockStatus()
                                isSettingsOpen = false
                            }
                        )
                    } else if (isNotificationsOpen) {
                        NotificationCenterScreen(
                            onBackClick = { isNotificationsOpen = false }
                        )
                    } else if (isFinancialHealthOpen) {
                        FinancialHealthScreen(
                            onBackClick = { isFinancialHealthOpen = false }
                        )
                    } else {
                        Crossfade(
                            targetState = currentTab,
                            animationSpec = tween(durationMillis = 200),
                            label = "MainTabCrossfade"
                        ) { targetTab ->
                            when (targetTab) {
                                BottomNavItem.HOME -> {
                                    HomeScreen(
                                        onNavigateToTab = { tab ->
                                            currentTab = tab
                                        },
                                        onCategoryClick = { categoryId ->
                                            when {
                                                categoryId.contains("car", ignoreCase = true) || categoryId.contains("vehicle", ignoreCase = true) -> {
                                                    currentTab = BottomNavItem.VEHICLE
                                                }
                                                categoryId.contains("installment", ignoreCase = true) || categoryId.contains("loan", ignoreCase = true) -> {
                                                    currentTab = BottomNavItem.INSTALLMENTS
                                                }
                                                else -> {
                                                    currentTab = BottomNavItem.FINANCE
                                                }
                                            }
                                        },
                                        onQuickActionClick = { actionId ->
                                            when {
                                                actionId.contains("car", ignoreCase = true) || actionId.contains("fuel", ignoreCase = true) || actionId.contains("service", ignoreCase = true) -> {
                                                    currentTab = BottomNavItem.VEHICLE
                                                }
                                                actionId.contains("installment", ignoreCase = true) || actionId.contains("loan", ignoreCase = true) -> {
                                                    currentTab = BottomNavItem.INSTALLMENTS
                                                }
                                                actionId.contains("calendar", ignoreCase = true) || actionId.contains("تقویم", ignoreCase = true) -> {
                                                    currentTab = BottomNavItem.CALENDAR
                                                }
                                                actionId.contains("report", ignoreCase = true) || actionId.contains("chart", ignoreCase = true) -> {
                                                    currentTab = BottomNavItem.REPORTS
                                                }
                                                actionId.contains("health", ignoreCase = true) || actionId.contains("سلامت", ignoreCase = true) -> {
                                                    isFinancialHealthOpen = true
                                                }
                                                else -> {
                                                    currentTab = BottomNavItem.FINANCE
                                                }
                                            }
                                        },
                                        onNavigateToFinancialHealth = {
                                            isFinancialHealthOpen = true
                                        },
                                        onNotificationClick = {
                                            isNotificationsOpen = true
                                        },
                                        onSettingsClick = {
                                            isSettingsOpen = true
                                        },
                                        onOpenSubscription = {
                                            isSubscriptionOpen = true
                                        },
                                        bottomBar = {},
                                        subscriptionViewModel = subscriptionViewModel
                                    )
                                }
                                BottomNavItem.FINANCE -> {
                                    FinancialScreen(
                                        currentNavTab = currentTab,
                                        onNavigateToTab = { tab ->
                                            currentTab = tab
                                        },
                                        bottomBar = {}
                                    )
                                }
                                BottomNavItem.CALENDAR -> {
                                    FinancialCalendarScreen()
                                }
                                BottomNavItem.INSTALLMENTS -> {
                                    InstallmentsScreen(
                                        bottomBar = {}
                                    )
                                }
                                BottomNavItem.VEHICLE -> {
                                    VehicleServicesScreen(
                                        bottomBar = {}
                                    )
                                }
                                BottomNavItem.REPORTS -> {
                                    ReportsScreen(
                                        onNavigateToHome = { currentTab = BottomNavItem.HOME },
                                        onNavigateToFinancial = { currentTab = BottomNavItem.FINANCE },
                                        onNavigateToInstallments = { currentTab = BottomNavItem.INSTALLMENTS },
                                        onNavigateToVehicles = { currentTab = BottomNavItem.VEHICLE },
                                        bottomBar = {}
                                    )
                                }
                                BottomNavItem.REMINDERS -> {
                                    ReminderScreen(
                                        bottomBar = {}
                                    )
                                }
                            }
                        }
                    }
                }

                // Global Auth Bottom Sheet
                if (showAuthSheet) {
                    AuthBottomSheet(
                        authViewModel = authViewModel,
                        onDismissRequest = { showAuthSheet = false }
                    )
                }
            }
        }
    }
}
