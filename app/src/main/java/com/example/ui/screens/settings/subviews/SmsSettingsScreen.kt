package com.example.ui.screens.settings.subviews

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.sms.BackendSecureSmsAdapter
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.ui.screens.settings.components.SettingsHeader
import com.example.ui.screens.settings.components.SettingsItem
import com.example.ui.screens.settings.components.SettingsSectionCard
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusXL
import com.example.ui.theme.WarningAmberLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsSettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isSmsEnabled by remember { mutableStateOf(true) }
    var phoneNumber by remember { mutableStateOf("09123456789") }
    var isPhoneVerified by remember { mutableStateOf(true) }
    var showOtpDialog by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }
    var otpCooldown by remember { mutableStateOf(0) }
    var isSendingTestSms by remember { mutableStateOf(false) }

    val maskedPhoneNumber = remember(phoneNumber) {
        if (phoneNumber.length >= 10) {
            phoneNumber.substring(0, 4) + "••••" + phoneNumber.takeLast(3)
        } else {
            phoneNumber
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SettingsHeader(
                title = "تنظیمات پیامک و پیام‌رسانی",
                subtitle = "تأیید شماره، اپراتورهای ایرانی و هشدارهای پیامکی",
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
            // General SMS Toggle Card
            item {
                SettingsSectionCard(title = "سرویس پیامک هوشمند") {
                    SettingsItem(
                        title = "فعال‌سازی ارسال پیامک‌های یادآوری",
                        subtitle = "دریافت پیامک سررسید اقساط و موعد بیمه در زمان معین",
                        iconRes = R.drawable.img_3d_bell_notification,
                        iconAccentColor = EmeraldPrimaryLight,
                        checked = isSmsEnabled,
                        onCheckedChange = { isSmsEnabled = it }
                    )
                }
            }

            // Phone verification & operator status
            item {
                SettingsSectionCard(title = "شماره همراه و اعتبارسنجی") {
                    Layered3DCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Soft3DIcon(
                                        imageRes = R.drawable.img_3d_shield_security,
                                        contentDescription = "امنیت",
                                        size = 38.dp,
                                        accentColor = if (isPhoneVerified) EmeraldPrimaryLight else WarningAmberLight
                                    )
                                    Column {
                                        Text(
                                            text = if (isPhoneVerified) "شماره موبایل تأیید شده" else "شماره موبایل تأیید نشده",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = maskedPhoneNumber,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isPhoneVerified) EmeraldPrimaryLight.copy(alpha = 0.15f) else WarningAmberLight.copy(alpha = 0.15f),
                                    border = BorderStroke(0.5.dp, if (isPhoneVerified) EmeraldPrimaryLight.copy(alpha = 0.3f) else WarningAmberLight.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = if (isPhoneVerified) "✓ فعال و تأییدشده" else "نیازمند تأیید",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (isPhoneVerified) EmeraldPrimaryLight else WarningAmberLight,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        showOtpDialog = true
                                        otpCooldown = 60
                                        scope.launch {
                                            snackbarHostState.showSnackbar("کد تأیید به شماره $phoneNumber ارسال شد.")
                                            while (otpCooldown > 0) {
                                                delay(1000)
                                                otpCooldown--
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.PhoneIphone, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isPhoneVerified) "تغییر شماره" else "تأیید شماره")
                                }

                                Button(
                                    onClick = {
                                        isSendingTestSms = true
                                        scope.launch {
                                            val adapter = BackendSecureSmsAdapter()
                                            val res = adapter.sendSms(phoneNumber, "تست سیستم پیامک مالی و یادآور با موفقیت انجام شد.")
                                            isSendingTestSms = false
                                            if (res.success) {
                                                snackbarHostState.showSnackbar("پیامک تست با موفقیت از طریق سامانه ارسال شد.")
                                            } else {
                                                snackbarHostState.showSnackbar("خطا در ارسال پیامک تست.")
                                            }
                                        }
                                    },
                                    enabled = isPhoneVerified && !isSendingTestSms,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (isSendingTestSms) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("ارسال پیام تست")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Supported Iranian Operators
            item {
                SettingsSectionCard(title = "اپراتورهای پشتیبانی‌شده در سراسر کشور") {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "سامانه پیامک هوشمند با زیرساخت کلیه اپراتورهای مخابراتی کشور یکپارچه است:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            OperatorBadge(title = "همراه اول", code = "MCI", color = Color(0xFF00A2E8))
                            OperatorBadge(title = "ایرانسل", code = "MTN", color = Color(0xFFFFCC00))
                            OperatorBadge(title = "رایتل", code = "Rightel", color = Color(0xFF8B1D61))
                            OperatorBadge(title = "شاتل موبایل", code = "Shatel", color = Color(0xFF10B981))
                        }
                    }
                }
            }

            // Quiet Hours & Delivery Policy
            item {
                SettingsSectionCard(title = "ساعات سکوت (Quiet Hours)") {
                    SettingsItem(
                        title = "عدم ارسال پیام در ساعات استراحت",
                        subtitle = "جلوگیری از ارسال اعلان‌های صوتی و پیامک بین ساعت ۲۳:۰۰ تا ۰۸:۰۰",
                        iconRes = R.drawable.img_3d_bell_notification,
                        iconAccentColor = InfoIndigoLight,
                        checked = true,
                        onCheckedChange = {}
                    )

                    SettingsItem(
                        title = "استثنا برای اقساط با اولویت بالا",
                        subtitle = "ارسال فوری هشدارهای سررسید فردا حتی در زمان سکوت",
                        iconRes = R.drawable.img_3d_installment,
                        iconAccentColor = WarningAmberLight,
                        checked = true,
                        onCheckedChange = {}
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Phone & OTP Verification Dialog
    if (showOtpDialog) {
        AlertDialog(
            onDismissRequest = { showOtpDialog = false },
            title = {
                Text(
                    text = "تأیید شماره تلفن همراه",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "کد ۵ رقمی ارسال‌شده به شماره $phoneNumber را وارد نمایید:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("شماره همراه") },
                        placeholder = { Text("09123456789") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { if (it.length <= 5) otpCode = it },
                        label = { Text("کد تأیید OTP") },
                        placeholder = { Text("۱۲۳۴۵") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (otpCooldown > 0) {
                        Text(
                            text = "امکان ارسال مجدد پس از $otpCooldown ثانیه",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isPhoneVerified = true
                        showOtpDialog = false
                        otpCode = ""
                        scope.launch {
                            snackbarHostState.showSnackbar("شماره همراه با موفقیت تأیید شد.")
                        }
                    },
                    enabled = phoneNumber.isNotBlank()
                ) {
                    Text("تأیید و ثبت")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOtpDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
private fun OperatorBadge(
    title: String,
    code: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(RadiusMD),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = code,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}
