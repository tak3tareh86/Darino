package com.example.ui.screens.settings.subviews

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.security.SecurityValidators
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.example.data.security.LockTimeoutOption
import com.example.data.security.UnlockMethod
import com.example.ui.screens.lock.AppLockViewModel
import com.example.ui.screens.lock.components.NumericKeypad
import com.example.ui.screens.lock.components.PatternGridLock
import com.example.ui.screens.lock.components.PinDotsDisplay
import com.example.ui.screens.settings.components.SettingsHeader
import com.example.ui.screens.settings.components.SettingsSectionCard
import com.example.ui.theme.ButtonShape
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM
import com.example.ui.theme.WarningAmberLight

/**
 * Complete, Professional "قفل و احراز هویت" Settings View.
 * Contains:
 * 1. Security Status Section (Active state, verified phone, current method, mock timestamp)
 * 2. PIN Activation, Verification, Change & Deactivation (Bottom Sheets)
 * 3. Pattern Activation & Verification (3x3 grid)
 * 4. Biometric Mock Activation & Hardware Status
 * 5. Default Unlock Method Picker (PIN / Pattern / Biometric)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockAndAuthenticationScreen(
    viewModel: AppLockViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    val prefs = remember { context.getSharedPreferences("darino_security_prefs", android.content.Context.MODE_PRIVATE) }
    var isPrivacyModeEnabled by remember { mutableStateOf(prefs.getBoolean("privacy_mode_enabled", false)) }
    var showBalanceOnHome by remember { mutableStateOf(prefs.getBoolean("show_balance_on_home", true)) }

    // Sheet visibility states
    var showCreatePinSheet by remember { mutableStateOf(false) }
    var showChangePinSheet by remember { mutableStateOf(false) }
    var showDisablePinSheet by remember { mutableStateOf(false) }

    var showCreatePatternSheet by remember { mutableStateOf(false) }
    var showDisablePatternSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SettingsHeader(
                title = "قفل و احراز هویت",
                subtitle = "مدیریت رمز ورود، اثر انگشت، الگو و امنیت ورود",
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
            // Combined Authentication Methods (PIN, Pattern, Biometric) in a single compact card
            item {
                UnifiedAuthenticationMethodsCard(
                    pinEnabled = uiState.securitySettings.pinEnabled,
                    patternEnabled = uiState.securitySettings.patternEnabled,
                    biometricEnabled = uiState.securitySettings.biometricEnabled,
                    isBiometricAvailable = uiState.isBiometricAvailable,
                    biometricStatusMessage = uiState.biometricStatusMessage,
                    isDark = isDark,
                    onCreatePinClick = { showCreatePinSheet = true },
                    onChangePinClick = { showChangePinSheet = true },
                    onDisablePinClick = { showDisablePinSheet = true },
                    onCreatePatternClick = { showCreatePatternSheet = true },
                    onDisablePatternClick = { showDisablePatternSheet = true },
                    onToggleBiometric = { enable ->
                        if (activity != null) {
                            viewModel.toggleBiometricWithPrompt(activity, enable) { }
                        }
                    }
                )
            }

            // 4. Lock Timeout Settings
            item {
                LockTimeoutPickerCard(
                    currentTimeoutSeconds = uiState.securitySettings.lockTimeoutSeconds,
                    isDark = isDark,
                    onSelectTimeout = { option -> viewModel.setLockTimeout(option) }
                )
            }

            // 5. Default Unlock Method Selector (if multiple active)
            item {
                DefaultMethodSelectorCard(
                    settings = uiState.securitySettings,
                    isDark = isDark,
                    onSelectMethod = { viewModel.setDefaultUnlockMethod(it) }
                )
            }

            // 6. Privacy and Amounts Display Card
            item {
                PrivacyManagementCard(
                    isPrivacyModeEnabled = isPrivacyModeEnabled,
                    onPrivacyModeChange = {
                        isPrivacyModeEnabled = it
                        prefs.edit().putBoolean("privacy_mode_enabled", it).apply()
                    },
                    showBalanceOnHome = showBalanceOnHome,
                    onShowBalanceChange = {
                        showBalanceOnHome = it
                        prefs.edit().putBoolean("show_balance_on_home", it).apply()
                    },
                    isDark = isDark
                )
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Modal Bottom Sheets
    if (showCreatePinSheet) {
        CreatePinBottomSheet(
            onPinCreated = { pin ->
                viewModel.setPin(pin)
                showCreatePinSheet = false
            },
            onDismiss = { showCreatePinSheet = false }
        )
    }

    if (showChangePinSheet) {
        ChangePinBottomSheet(
            onPinChanged = { newPin ->
                viewModel.setPin(newPin)
                showChangePinSheet = false
            },
            onDismiss = { showChangePinSheet = false }
        )
    }

    if (showDisablePinSheet) {
        ConfirmActionBottomSheet(
            title = "غیرفعال‌سازی رمز ورود (PIN)",
            description = "آیا از غیرفعال‌کردن رمز عبور عددی اطمینان دارید؟ در صورت نبود روش دیگر، قفل برنامه برداشته خواهد شد.",
            confirmText = "بله، غیرفعال کن",
            onConfirm = {
                viewModel.disablePin()
                showDisablePinSheet = false
            },
            onDismiss = { showDisablePinSheet = false }
        )
    }

    if (showCreatePatternSheet) {
        CreatePatternBottomSheet(
            onPatternCreated = { pattern ->
                viewModel.setPattern(pattern)
                showCreatePatternSheet = false
            },
            onDismiss = { showCreatePatternSheet = false }
        )
    }

    if (showDisablePatternSheet) {
        ConfirmActionBottomSheet(
            title = "غیرفعال‌سازی الگوی ورود",
            description = "با غیرفعال‌سازی الگو، امکان ورود از طریق رسم مسیر ۳×۳ حذف خواهد شد.",
            confirmText = "غیرفعال‌سازی الگو",
            onConfirm = {
                viewModel.disablePattern()
                showDisablePatternSheet = false
            },
            onDismiss = { showDisablePatternSheet = false }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// Sub-Cards: UI Presentation Components
// -------------------------------------------------------------------------------------------------

@Composable
private fun SecurityStatusOverviewCard(
    isAppLockEnabled: Boolean,
    userPhone: String,
    defaultMethod: UnlockMethod,
    isDark: Boolean,
    onToggleMasterLock: (Boolean) -> Unit
) {
    SettingsSectionCard(title = "وضعیت امنیتی برنامه") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Master toggle row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isAppLockEnabled) EmeraldPrimaryLight.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAppLockEnabled) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                            contentDescription = null,
                            tint = if (isAppLockEnabled) EmeraldPrimaryLight else Color.Gray,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "قفل سراسری برنامه",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = if (isAppLockEnabled) "فعال — هنگام ورود رمز پرسیده می‌شود" else "غیرفعال — دسترسی آزاد",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isAppLockEnabled,
                    onCheckedChange = onToggleMasterLock,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = EmeraldPrimaryLight
                    ),
                    modifier = Modifier.testTag("master_lock_toggle")
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            )

            // Verified Phone Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Verified,
                        contentDescription = null,
                        tint = EmeraldPrimaryLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "شماره موبایل تأییدشده:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = userPhone.ifBlank { "09121234567" },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = EmeraldPrimaryLight
                    )
                }
            }

            // Current Active Default Method
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "روش ورود فعلی:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${defaultMethod.emoji} ${defaultMethod.title}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
            }

            // Mock Last Modified
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "آخرین بروزرسانی امنیتی:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = "امروز، ذخیره محلی امن",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun UnifiedAuthenticationMethodsCard(
    pinEnabled: Boolean,
    patternEnabled: Boolean,
    biometricEnabled: Boolean,
    isBiometricAvailable: Boolean,
    biometricStatusMessage: String,
    isDark: Boolean,
    onCreatePinClick: () -> Unit,
    onChangePinClick: () -> Unit,
    onDisablePinClick: () -> Unit,
    onCreatePatternClick: () -> Unit,
    onDisablePatternClick: () -> Unit,
    onToggleBiometric: (Boolean) -> Unit
) {
    SettingsSectionCard(title = "روش‌های قفل و احراز هویت") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 14.dp)
        ) {
            // 1. PIN Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (pinEnabled) EmeraldPrimaryLight.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Pin,
                            contentDescription = null,
                            tint = if (pinEnabled) EmeraldPrimaryLight else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "رمز عبور عددی (PIN)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = if (pinEnabled) "فعال است" else "غیرفعال",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = if (pinEnabled) EmeraldPrimaryLight else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!pinEnabled) {
                        Button(
                            onClick = onCreatePinClick,
                            shape = RoundedCornerShape(RadiusMD),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimaryLight),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("فعال‌سازی", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onChangePinClick,
                            shape = RoundedCornerShape(RadiusMD),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp).testTag("change_pin_button")
                        ) {
                            Text("تغییر", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = onDisablePinClick,
                            shape = RoundedCornerShape(RadiusMD),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRoseLight),
                            border = BorderStroke(1.dp, ExpenseRoseLight.copy(alpha = 0.4f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp).testTag("disable_pin_button")
                        ) {
                            Text("غیرفعال", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Divider 1
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            )

            // 2. Pattern Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (patternEnabled) EmeraldPrimaryLight.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GridOn,
                            contentDescription = null,
                            tint = if (patternEnabled) EmeraldPrimaryLight else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "الگوی ورود (Pattern)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = if (patternEnabled) "فعال است" else "غیرفعال",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = if (patternEnabled) EmeraldPrimaryLight else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!patternEnabled) {
                        Button(
                            onClick = onCreatePatternClick,
                            shape = RoundedCornerShape(RadiusMD),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimaryLight),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp).testTag("create_pattern_button")
                        ) {
                            Text("فعال‌سازی", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onCreatePatternClick,
                            shape = RoundedCornerShape(RadiusMD),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("تغییر", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = onDisablePatternClick,
                            shape = RoundedCornerShape(RadiusMD),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRoseLight),
                            border = BorderStroke(1.dp, ExpenseRoseLight.copy(alpha = 0.4f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("غیرفعال", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Divider 2
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            )

            // 3. Biometric Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (biometricEnabled && isBiometricAvailable) EmeraldPrimaryLight.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Fingerprint,
                            contentDescription = null,
                            tint = if (biometricEnabled && isBiometricAvailable) EmeraldPrimaryLight else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "اثر انگشت و بیومتریک",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = if (biometricEnabled && isBiometricAvailable) "فعال است" else biometricStatusMessage,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = if (biometricEnabled && isBiometricAvailable) EmeraldPrimaryLight else if (isBiometricAvailable) MaterialTheme.colorScheme.onSurfaceVariant else ExpenseRoseLight
                        )
                    }
                }

                Switch(
                    checked = biometricEnabled && isBiometricAvailable,
                    onCheckedChange = if (isBiometricAvailable) onToggleBiometric else null,
                    enabled = isBiometricAvailable,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = EmeraldPrimaryLight
                    ),
                    modifier = Modifier.testTag("biometric_toggle")
                )
            }
        }
    }
}

@Composable
private fun LockTimeoutPickerCard(
    currentTimeoutSeconds: Long,
    isDark: Boolean,
    onSelectTimeout: (LockTimeoutOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentOption = LockTimeoutOption.entries.find { it.seconds == currentTimeoutSeconds } ?: LockTimeoutOption.IMMEDIATELY

    SettingsSectionCard(title = "زمان فعال شدن دوباره قفل") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "فعال‌شدن مجدد قفل:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
                Text(
                    text = "تاخیر ورود پس از پس‌زمینه",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                Surface(
                    shape = RoundedCornerShape(RadiusMD),
                    color = EmeraldPrimaryLight.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, EmeraldPrimaryLight.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(RadiusMD))
                        .clickable { expanded = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = currentOption.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldPrimaryLight
                        )
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = EmeraldPrimaryLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    LockTimeoutOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (option == currentOption) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                onSelectTimeout(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultMethodSelectorCard(
    settings: com.example.data.security.LocalSecuritySettings,
    isDark: Boolean,
    onSelectMethod: (UnlockMethod) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentMethod = settings.defaultUnlockMethod

    SettingsSectionCard(title = "انتخاب روش پیش‌فرض ورود") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "روش پیش‌فرض ورود:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
                Text(
                    text = "روش ترجیحی بازگشایی قفل",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                Surface(
                    shape = RoundedCornerShape(RadiusMD),
                    color = EmeraldPrimaryLight.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, EmeraldPrimaryLight.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(RadiusMD))
                        .clickable { expanded = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${currentMethod.emoji} ${currentMethod.title}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldPrimaryLight
                        )
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = EmeraldPrimaryLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    UnlockMethod.values().forEach { method ->
                        val isAvailable = when (method) {
                            UnlockMethod.PIN -> settings.pinEnabled
                            UnlockMethod.PATTERN -> settings.patternEnabled
                            UnlockMethod.BIOMETRIC -> settings.biometricEnabled
                        }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${method.emoji} ${method.title}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (method == currentMethod) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isAvailable) MaterialTheme.colorScheme.onSurface else Color.Gray
                                )
                            },
                            enabled = isAvailable,
                            onClick = {
                                onSelectMethod(method)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyManagementCard(
    isPrivacyModeEnabled: Boolean,
    onPrivacyModeChange: (Boolean) -> Unit,
    showBalanceOnHome: Boolean,
    onShowBalanceChange: (Boolean) -> Unit,
    isDark: Boolean
) {
    SettingsSectionCard(title = "حریم خصوصی و نمایش مبالغ") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Privacy mode toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "حالت حریم خصوصی (Privacy Mode)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "اطلاعات مالی و مبالغ حساس را در صفحه‌ها با ستاره (••••) مخفی می‌کند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = isPrivacyModeEnabled,
                    onCheckedChange = onPrivacyModeChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = EmeraldPrimaryLight
                    )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            )

            // Balance on home toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "نمایش موجودی کل در صفحه اصلی",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "نمایش یا عدم نمایش مانده حساب در خلاصه داشبورد",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = showBalanceOnHome,
                    onCheckedChange = onShowBalanceChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = EmeraldPrimaryLight
                    )
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Sheets: PIN & Pattern Setup flows
// -------------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePinBottomSheet(
    onPinCreated: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1: Create, 2: Confirm
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (step == 1) "تعریف رمز جدید (PIN)" else "تکرار رمز جهت تأیید",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Text(
                text = if (step == 1) "یک رمز ۴ رقمی دلخواه وارد کنید:" else "لطفاً رمز واردشده را مجدداً وارد نمایید:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            PinDotsDisplay(
                pinLength = if (step == 1) firstPin.length else confirmPin.length,
                maxDigits = 4,
                isError = errorMessage != null
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = ExpenseRoseLight,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp)
                )
            }

            NumericKeypad(
                onDigitClick = { digit ->
                    if (step == 1) {
                        if (firstPin.length < 4) {
                            firstPin += digit
                            errorMessage = null
                            if (firstPin.length == 4) {
                                step = 2
                            }
                        }
                    } else {
                        if (confirmPin.length < 4) {
                            confirmPin += digit
                            errorMessage = null
                            if (confirmPin.length == 4) {
                                if (confirmPin == firstPin) {
                                    onPinCreated(confirmPin)
                                } else {
                                    errorMessage = "رمز تکرارشده یکسان نیست. لطفاً مجدداً امتحان کنید."
                                    confirmPin = ""
                                }
                            }
                        }
                    }
                },
                onDeleteClick = {
                    if (step == 1 && firstPin.isNotEmpty()) {
                        firstPin = firstPin.dropLast(1)
                    } else if (step == 2 && confirmPin.isNotEmpty()) {
                        confirmPin = confirmPin.dropLast(1)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangePinBottomSheet(
    onPinChanged: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (step == 1) "رمز ۴ رقمی جدید" else "تأیید رمز جدید",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            PinDotsDisplay(
                pinLength = if (step == 1) newPin.length else confirmPin.length,
                maxDigits = 4,
                isError = errorMessage != null
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = ExpenseRoseLight,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            NumericKeypad(
                onDigitClick = { digit ->
                    if (step == 1) {
                        if (newPin.length < 4) {
                            newPin += digit
                            errorMessage = null
                            if (newPin.length == 4) {
                                step = 2
                            }
                        }
                    } else {
                        if (confirmPin.length < 4) {
                            confirmPin += digit
                            errorMessage = null
                            if (confirmPin.length == 4) {
                                if (confirmPin == newPin) {
                                    onPinChanged(confirmPin)
                                } else {
                                    errorMessage = "رمزهای وارد شده مطابقت ندارند."
                                    confirmPin = ""
                                }
                            }
                        }
                    }
                },
                onDeleteClick = {
                    if (step == 1 && newPin.isNotEmpty()) {
                        newPin = newPin.dropLast(1)
                    } else if (step == 2 && confirmPin.isNotEmpty()) {
                        confirmPin = confirmPin.dropLast(1)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePatternBottomSheet(
    onPatternCreated: (List<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1: Draw, 2: Confirm
    val firstPattern = remember { mutableStateListOf<Int>() }
    val confirmPattern = remember { mutableStateListOf<Int>() }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (step == 1) "رسم الگوی جدید" else "تکرار الگو جهت تأیید",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Text(
                text = if (step == 1) "حداقل ۴ نقطه را به هم متصل کنید" else "الگوی قبلی را دوباره رسم کنید",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = ExpenseRoseLight,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            PatternGridLock(
                selectedPoints = if (step == 1) firstPattern.toList() else confirmPattern.toList(),
                onPatternChange = { points ->
                    if (step == 1) {
                        firstPattern.clear()
                        firstPattern.addAll(points)
                    } else {
                        confirmPattern.clear()
                        confirmPattern.addAll(points)
                    }
                },
                onPatternComplete = { points ->
                    if (step == 1) {
                        if (points.size < 4) {
                            errorMessage = "الگو باید حداقل شامل ۴ نقطه باشد."
                            firstPattern.clear()
                        } else {
                            errorMessage = null
                            step = 2
                        }
                    } else {
                        if (points == firstPattern.toList()) {
                            onPatternCreated(points)
                        } else {
                            errorMessage = "الگوها یکسان نیستند. لطفاً مجدداً امتحان کنید."
                            confirmPattern.clear()
                        }
                    }
                },
                isError = errorMessage != null,
                modifier = Modifier.size(260.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmActionBottomSheet(
    title: String,
    description: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = ExpenseRoseLight
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(RadiusMD),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("انصراف")
                }

                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(RadiusMD),
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRoseLight),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(confirmText, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
