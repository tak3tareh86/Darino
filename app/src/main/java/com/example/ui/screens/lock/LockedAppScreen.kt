package com.example.ui.screens.lock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.example.data.security.UnlockMethod
import com.example.ui.screens.lock.components.NumericKeypad
import com.example.ui.screens.lock.components.PatternGridLock
import com.example.ui.screens.lock.components.PinDotsDisplay
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.WarningAmberLight

/**
 * Premium Locked Screen displayed on subsequent app launches when app lock is enabled.
 * Supports:
 * - Dynamic default unlock method (PIN, Pattern, Biometric)
 * - Method switcher buttons («ورود با اثر انگشت», «استفاده از PIN», «استفاده از الگو»)
 * - Rate limiting and lockout state
 * - Recovery login using Phone + OTP
 */
@Composable
fun LockedAppScreen(
    viewModel: AppLockViewModel,
    onRecoverWithOtp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    LaunchedEffect(uiState.activeUnlockMethod) {
        if (uiState.activeUnlockMethod == UnlockMethod.BIOMETRIC && activity != null) {
            viewModel.triggerBiometricPrompt(activity)
        }
    }

    // Internal input buffers
    var currentPinInput by remember { mutableStateOf("") }
    val currentPatternInput = remember { mutableStateListOf<Int>() }

    val backgroundBrush = remember(isDark) {
        if (isDark) {
            Brush.verticalGradient(
                listOf(
                    Color(0xFF0B101B),
                    Color(0xFF0F172A),
                    Color(0xFF070B12)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0xFFF8FAFC),
                    Color(0xFFF1F5F9),
                    Color(0xFFE2E8F0)
                )
            )
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("locked_app_screen"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header: App Lock Status & Brand Logo
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        com.example.ui.components.DarinoLogoMark(
                            size = 56.dp,
                            showShadow = true
                        )

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimaryLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (uiState.activeUnlockMethod) {
                                    UnlockMethod.PIN -> Icons.Rounded.Pin
                                    UnlockMethod.PATTERN -> Icons.Rounded.GridOn
                                    UnlockMethod.BIOMETRIC -> Icons.Rounded.Fingerprint
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Text(
                        text = "دارینو قفل است",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )

                    if (uiState.userPhone.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "حساب:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = uiState.userPhone,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = EmeraldPrimaryLight
                                )
                            }
                        }
                    }

                    // Error or Temporary Lockout Banner
                    AnimatedVisibility(visible = uiState.isTemporarilyLocked || uiState.errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(RadiusMD),
                            color = if (uiState.isTemporarilyLocked) WarningAmberLight.copy(alpha = 0.18f) else ExpenseRoseLight.copy(alpha = 0.15f),
                            border = BorderStroke(
                                1.dp,
                                if (uiState.isTemporarilyLocked) WarningAmberLight.copy(alpha = 0.5f) else ExpenseRoseLight.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (uiState.isTemporarilyLocked) WarningAmberLight else ExpenseRoseLight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (uiState.isTemporarilyLocked) {
                                        "ورود موقتاً مسدود شد (${uiState.lockoutSecondsRemaining} ثانیه)"
                                    } else {
                                        uiState.errorMessage ?: ""
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = if (uiState.isTemporarilyLocked) WarningAmberLight else ExpenseRoseLight
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Middle: Active Unlock Interface (PIN / Pattern / Biometric)
                Crossfade(
                    targetState = uiState.activeUnlockMethod,
                    label = "UnlockMethodCrossfade"
                ) { method ->
                    when (method) {
                        UnlockMethod.PIN -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "رمز عبور را وارد کنید",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                PinDotsDisplay(
                                    pinLength = currentPinInput.length,
                                    maxDigits = 4,
                                    isError = uiState.errorMessage != null
                                )

                                NumericKeypad(
                                    onDigitClick = { digit ->
                                        if (currentPinInput.length < 4 && !uiState.isTemporarilyLocked) {
                                            currentPinInput += digit
                                            if (currentPinInput.length == 4) {
                                                viewModel.submitPin(currentPinInput)
                                                currentPinInput = ""
                                            }
                                        }
                                    },
                                    onDeleteClick = {
                                        if (currentPinInput.isNotEmpty()) {
                                            currentPinInput = currentPinInput.dropLast(1)
                                        }
                                    },
                                    showBiometricButton = false,
                                    onBiometricClick = {}
                                )
                            }
                        }

                        UnlockMethod.PATTERN -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "الگوی امنیتی را رسم کنید",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                PatternGridLock(
                                    selectedPoints = currentPatternInput.toList(),
                                    onPatternChange = { points ->
                                        currentPatternInput.clear()
                                        currentPatternInput.addAll(points)
                                    },
                                    onPatternComplete = { points ->
                                        viewModel.submitPattern(points)
                                        currentPatternInput.clear()
                                    },
                                    isError = uiState.errorMessage != null,
                                    enabled = !uiState.isTemporarilyLocked,
                                    modifier = Modifier.size(280.dp)
                                )
                            }
                        }

                        UnlockMethod.BIOMETRIC -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldPrimaryLight.copy(alpha = 0.12f))
                                        .clickable { if (activity != null) viewModel.triggerBiometricPrompt(activity) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (uiState.isAuthenticatingBiometric) {
                                        CircularProgressIndicator(
                                            color = EmeraldPrimaryLight,
                                            strokeWidth = 3.dp,
                                            modifier = Modifier.size(54.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.Fingerprint,
                                            contentDescription = "لمس حسگر",
                                            tint = EmeraldPrimaryLight,
                                            modifier = Modifier.size(56.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = if (uiState.isAuthenticatingBiometric) "در حال خواندن اثر انگشت..." else "حسگر اثر انگشت را لمس کنید",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )

                                Button(
                                    onClick = { if (activity != null) viewModel.triggerBiometricPrompt(activity) },
                                    shape = RoundedCornerShape(RadiusMD),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimaryLight),
                                    modifier = Modifier.testTag("retry_biometric_button")
                                ) {
                                    Text("احراز هویت مجدد", color = Color.White, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Actions: Emergency Recovery Flow: Re-login with Username & Password
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(RadiusMD),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RadiusMD))
                            .clickable(onClick = onRecoverWithOtp)
                            .testTag("account_recovery_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LockReset,
                                    contentDescription = null,
                                    tint = EmeraldPrimaryLight,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "ورود مجدد با نام کاربری و رمز عبور",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Login,
                                contentDescription = null,
                                tint = EmeraldPrimaryLight,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
