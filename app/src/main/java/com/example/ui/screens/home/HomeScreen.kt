package com.example.ui.screens.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.home.components.BankSmsAssistantCard
import com.example.ui.screens.home.components.BottomNavItem
import com.example.ui.screens.home.components.HomeHeader
import com.example.ui.screens.home.components.UpcomingObligationsCard
import com.example.ui.screens.home.components.UpcomingRemindersCard
import com.example.ui.screens.home.domain.ObligationType
import com.example.ui.screens.home.viewmodel.HomeDashboardViewModel
import com.example.ui.screens.home.viewmodel.SmsAcceptResult
import com.example.ui.screens.subscription.SubscriptionStatusCard
import com.example.ui.screens.subscription.SubscriptionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToTab: (BottomNavItem) -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onOpenSubscription: () -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    viewModel: HomeDashboardViewModel = viewModel(),
    subscriptionViewModel: SubscriptionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val subState by subscriptionViewModel.uiState.collectAsState()
    val smsPermissionState by viewModel.smsPermissionState.collectAsState()
    val isScanningSms by viewModel.isScanningSms.collectAsState()
    val smsErrorMessage by viewModel.smsErrorMessage.collectAsState()
    val pendingSms by viewModel.pendingSmsQueue.collectAsState()
    val userAccounts by viewModel.userAccounts.collectAsState()

    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }

    // Permission launcher for SMS runtime permission
    val activity = context as? Activity
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val permanentlyDenied = if (!isGranted && activity != null) {
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_SMS)
        } else false
        viewModel.markPermissionRequested(granted = isGranted, permanentlyDenied = permanentlyDenied)
    }

    // Open System Settings if permission is permanently denied
    val onGoToSettings: () -> Unit = {
        try {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            )
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Background gradient canvas
    val backgroundBrush = remember(isDark) {
        if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF000000),
                    Color(0xFF050A14),
                    Color(0xFF000000)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF8FAFC),
                    Color(0xFFF1F5F9),
                    Color(0xFFE2E8F0)
                )
            )
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_scaffold"),
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            bottomBar?.invoke()
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Header: Greeting + Persian Jalali Date + Notification Badge + Settings + Logo
                HomeHeader(
                    userName = uiState.userName,
                    todayDateText = uiState.todayDate,
                    unreadNotificationsCount = uiState.notificationCount,
                    onNotificationClick = onNotificationClick,
                    onSettingsClick = onSettingsClick
                )

                // Scrollable body content in exact target order
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Annual Subscription Card (Prominent, real state)
                    SubscriptionStatusCard(
                        state = subState,
                        onOpenSubscription = onOpenSubscription,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 3. SMS Financial Transactions Card (Interactive runtime permissions & processing)
                        BankSmsAssistantCard(
                            permissionState = smsPermissionState,
                            isScanning = isScanningSms,
                            errorMessage = smsErrorMessage,
                            queue = pendingSms,
                            userAccounts = userAccounts,
                            onTypeChange = { id, type ->
                                viewModel.updateSmsTransactionType(id, type)
                            },
                            onUpdateCustomDetails = { id, amount, type, cat, acc, dest ->
                                viewModel.updateSmsCustomDetails(id, amount, type, cat, acc, dest)
                            },
                            onAccept = { id, amount, type, cat, acc, desc, dest, onResult ->
                                viewModel.acceptSmsSuggestion(
                                    id = id,
                                    customType = type,
                                    customCategory = cat,
                                    customAccount = acc,
                                    customDescription = desc,
                                    destAccount = dest,
                                    customAmount = amount
                                ) { result ->
                                    scope.launch {
                                        when (result) {
                                            SmsAcceptResult.Success -> {
                                                snackbarHostState.showSnackbar("تراکنش با موفقیت ثبت گردید.")
                                            }
                                            SmsAcceptResult.AlreadyExists -> {
                                                snackbarHostState.showSnackbar("این تراکنش قبلاً ثبت شده است.")
                                            }
                                            SmsAcceptResult.TypeNotSelected -> {
                                                snackbarHostState.showSnackbar("لطفاً نوع تراکنش را مشخص کنید.")
                                            }
                                            SmsAcceptResult.InvalidAmount -> {
                                                snackbarHostState.showSnackbar("مبلغ تراکنش نامعتبر است؛ لطفاً با ویرایش پیامک، مبلغ صحیح را وارد کنید.")
                                            }
                                            SmsAcceptResult.AccountRequired -> {
                                                snackbarHostState.showSnackbar("حساب بانکی مشخص نیست؛ لطفاً با زدن دکمه ویرایش، حساب را انتخاب کنید.")
                                            }
                                            SmsAcceptResult.DestinationAccountRequired -> {
                                                snackbarHostState.showSnackbar("حساب مقصد انتقال مشخص نیست؛ لطفاً با زدن دکمه ویرایش، حساب مقصد را مشخص کنید.")
                                            }
                                            SmsAcceptResult.Failed -> {
                                                snackbarHostState.showSnackbar("خطا در ثبت تراکنش. لطفاً دوباره تلاش کنید.")
                                            }
                                        }
                                    }
                                    onResult(result)
                                }
                            },
                            onDismiss = { id ->
                                viewModel.dismissSmsSuggestion(id)
                                scope.launch {
                                    snackbarHostState.showSnackbar("پیامک از صف بررسی خارج شد.")
                                }
                            },
                            onRequestPermission = {
                                permissionLauncher.launch(Manifest.permission.READ_SMS)
                            },
                            onPermanentDeniedGoToSettings = onGoToSettings,
                            onRefreshScan = {
                                viewModel.scanInboxSms()
                            }
                        )

                        // 4. Upcoming Obligations Card (تعهدات نزدیک)
                        UpcomingObligationsCard(
                            obligations = uiState.upcomingObligations,
                            onViewAllClick = { onNavigateToTab(BottomNavItem.INSTALLMENTS) },
                            onObligationClick = { item ->
                                when (item.type) {
                                    ObligationType.INSTALLMENT -> onNavigateToTab(BottomNavItem.INSTALLMENTS)
                                    ObligationType.VEHICLE_INSURANCE -> onNavigateToTab(BottomNavItem.VEHICLE)
                                    ObligationType.BILL -> onNavigateToTab(BottomNavItem.CALENDAR)
                                    ObligationType.OTHER -> onNavigateToTab(BottomNavItem.FINANCE)
                                }
                            },
                            onMarkDoneClick = { id ->
                                viewModel.markObligationDone(id)
                                scope.launch {
                                    snackbarHostState.showSnackbar("تعهد مورد نظر با موفقیت پرداخت و تسویه شد.")
                                }
                            }
                        )

                        // 5. My Reminders Card (یادآورهای من)
                        UpcomingRemindersCard(
                            reminders = uiState.upcomingReminders,
                            onViewAllClick = { onNavigateToTab(BottomNavItem.REMINDERS) },
                            onReminderClick = {
                                onNavigateToTab(BottomNavItem.REMINDERS)
                            },
                            onMarkCompleted = { id ->
                                viewModel.markReminderCompleted(id)
                                scope.launch {
                                    snackbarHostState.showSnackbar("یادآوری انجام شد.")
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}
