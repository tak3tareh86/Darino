package com.example.ui.screens.home.components

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.screens.home.domain.BankSmsSuggestion
import com.example.ui.screens.home.viewmodel.SmsPermissionState
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM
import com.example.util.IranianPhoneUtils
import com.example.util.LocalAppCurrency
import com.example.util.MoneyFormatter
import com.example.util.MoneyAmount
import com.example.util.IranianAmountUtils

@Composable
fun BankSmsAssistantCard(
    permissionState: SmsPermissionState,
    isScanning: Boolean,
    errorMessage: String?,
    queue: List<BankSmsSuggestion>,
    userAccounts: List<com.example.data.database.AccountEntity> = emptyList(),
    onTypeChange: (id: String, type: TransactionType) -> Unit,
    onAccept: (id: String, amountRials: Long?, type: TransactionType?, category: String?, account: String?, desc: String?, dest: String?, onResult: (com.example.ui.screens.home.viewmodel.SmsAcceptResult) -> Unit) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onUpdateCustomDetails: (id: String, category: String?, account: String?, destAccount: String?) -> Unit = { _, _, _, _ -> },
    onDismiss: (id: String) -> Unit,
    onRequestPermission: () -> Unit,
    liveSmsEnabled: Boolean = true,
    onManagePatterns: () -> Unit = {},
    onPermanentDeniedGoToSettings: () -> Unit,
    onRefreshScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    var editingSuggestionId by remember { mutableStateOf<String?>(null) }
    val editingSuggestion = remember(editingSuggestionId, queue) {
        editingSuggestionId?.let { id -> queue.find { it.id == id } }
    }

    if (editingSuggestion != null) {
        EditSmsTransactionDialog(
            suggestion = editingSuggestion,
            userAccounts = userAccounts,
            onDismiss = { editingSuggestionId = null },
            onConfirm = { amountRials, type, cat, acc, desc, dest, onResult ->
                onUpdateCustomDetails(editingSuggestion.id, cat, acc, dest)
                onAccept(editingSuggestion.id, amountRials, type, cat, acc, desc, dest) { result ->
                    if (result == com.example.ui.screens.home.viewmodel.SmsAcceptResult.Success ||
                        result == com.example.ui.screens.home.viewmodel.SmsAcceptResult.AlreadyExists
                    ) {
                        editingSuggestionId = null
                    }
                    onResult(result)
                }
            }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sms_financial_transactions_card")
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF0F172A) else Color.White
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (permissionState) {
                // 1. Permission Not Requested Yet -> Onboarding State
                SmsPermissionState.NOT_REQUESTED -> {
                    SmsOnboardingView(
                        isDark = isDark,
                        onRequestPermission = onRequestPermission,
                        onManagePatterns = onManagePatterns
                    )
                }

                // 2. Permission Denied -> Friendly retry prompt
                SmsPermissionState.DENIED -> {
                    SmsPermissionDeniedView(
                        isDark = isDark,
                        onRetry = onRequestPermission
                    )
                }

                // 3. Permission Permanently Denied -> Open App Settings
                SmsPermissionState.PERMANENTLY_DENIED -> {
                    SmsPermissionPermanentlyDeniedView(
                        isDark = isDark,
                        onOpenSettings = onPermanentDeniedGoToSettings
                    )
                }

                // 4. Permission Granted -> Handle Scanning, Empty, Queue & Error states
                SmsPermissionState.GRANTED -> {
                    SmsGrantedContentView(
                        isDark = isDark,
                        isScanning = isScanning,
                        liveSmsEnabled = liveSmsEnabled,
                        onRequestPermission = onRequestPermission,
                        errorMessage = errorMessage,
                        queue = queue,
                        onTypeChange = onTypeChange,
                        // Every SMS suggestion must pass through the review dialog so
                        // the user explicitly confirms the linked account(s) before saving.
                        onQuickAccept = { id ->
                            editingSuggestionId = id
                        },
                        onManagePatterns = onManagePatterns,
                        onDismiss = onDismiss,
                        onRefreshScan = onRefreshScan
                    )
                }
            }
        }
    }
}

@Composable
private fun SmsOnboardingView(
    isDark: Boolean,
    onRequestPermission: () -> Unit,
    onManagePatterns: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(158.dp)
            .testTag("sms_onboarding_view"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0D9488).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Sms,
                    contentDescription = null,
                    tint = Color(0xFF0D9488),
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = "ثبت خودکار تراکنش‌ها",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "دستیار هوشمند پیامک‌های بانکی دارینو",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "دیگر نیازی به ثبت دستی تراکنش‌های بانکی نیست! الگوهای پیامک بانکی خود را تعریف کنید تا دارینو پیامک‌های واریز، برداشت و انتقال وجه را شناسایی کرده و تراکنش‌ها را به‌صورت خودکار به شما",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.Justify,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "نمایش دهد.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .border(
                        width = 1.dp,
                        color = Color(0xFF0D9488).copy(alpha = 0.65f),
                        shape = RoundedCornerShape(9.dp)
                    )
                    .clickable(onClick = onManagePatterns)
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = null,
                        tint = Color(0xFF0D9488),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "تنظیم الگوها",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xFF0D9488))
                    .clickable(onClick = onRequestPermission)
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "دسترسی پیامک",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SmsPermissionDeniedView(
    isDark: Boolean,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sms_permission_denied_view"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = "دسترسی به پیامک‌ها تأیید نشد",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "برای تشخیص واریز، برداشت و انتقال وجه، برنامه نیازمند مجوز خواندن پیامک است.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF59E0B),
                contentColor = Color(0xFF451A03)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
        ) {
            Text(
                text = "تلاش مجدد و اعطای دسترسی",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SmsPermissionPermanentlyDeniedView(
    isDark: Boolean,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sms_permanently_denied_view"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = "دسترسی پیامک غیرفعال است",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "لطفاً در تنظیمات سیستم، مجوز پیامک را برای دارینو فعال نمایید.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        OutlinedButton(
            onClick = onOpenSettings,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "باز کردن تنظیمات برنامه",
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp
            )
        }
    }
}

@Composable
private fun SmsGrantedContentView(
    isDark: Boolean,
    isScanning: Boolean,
    liveSmsEnabled: Boolean,
    onRequestPermission: () -> Unit,
    errorMessage: String?,
    queue: List<BankSmsSuggestion>,
    onTypeChange: (id: String, type: TransactionType) -> Unit,
    onQuickAccept: (id: String) -> Unit,
    onManagePatterns: () -> Unit,
    onDismiss: (id: String) -> Unit,
    onRefreshScan: () -> Unit
) {
    val appCurrency = LocalAppCurrency.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header Row: Status badge + actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0D9488).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Sms,
                        contentDescription = null,
                        tint = Color(0xFF0D9488),
                        modifier = Modifier.size(14.dp)
                    )
                }

                Text(
                    text = "ثبت خودکار تراکنش‌ها",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (queue.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0D9488),
                        contentColor = Color.White
                    ) {
                        Text(
                            text = "${IranianPhoneUtils.convertDigitsToPersian(queue.size.toString())} پیامک جدید",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onRefreshScan,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "بررسی مجدد",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (!liveSmsEnabled) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(9.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "برای شناسایی پیامک بلافاصله پس از دریافت، مجوز دریافت پیامک را فعال کنید.",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = onRequestPermission,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) { Text("فعال‌سازی", fontSize = 10.sp) }
                }
            }
        }

        // Parsing / Reading Error state
        if (errorMessage != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFEF2F2),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.5.sp,
                            color = Color(0xFF991B1B)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Loading Scanning State
        if (isScanning) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF0D9488)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "در حال پایش پیامک‌های بانکی...",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        } else if (queue.isEmpty()) {
            // No new financial SMS
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0xFF131D31) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MarkEmailRead,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "پیامکی مطابق الگوهای شما پیدا نشد",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "اگر پیامک بانکی جدیدی دارید، الگوهای ذخیره‌شده را بررسی یا تکمیل کنید.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        textAlign = TextAlign.Center
                    )
                    TextButton(
                        onClick = onManagePatterns,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مدیریت الگوهای پیامکی", fontSize = 10.5.sp)
                    }
                }
            }
        } else {
            // New Financial SMS Available -> Pending Queue Item Card
            val current = queue.first()

            AnimatedContent(
                targetState = current,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "sms_item_transition"
            ) { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isDark) Color(0xFF131D31) else Color(0xFFF8FAFC),
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            1.dp,
                            if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Top Row: Bank name + Amount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = item.bankName,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (queue.size > 1) {
                                Text(
                                    text = "(۱ از ${IranianPhoneUtils.convertDigitsToPersian(queue.size.toString())})",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }

                        val convertedAmount = MoneyFormatter.formatExact(
                            MoneyAmount.of(item.amount, item.amountUnit),
                            targetCurrency = appCurrency
                        )
                        val signedAmount = when (item.type) {
                            TransactionType.EXPENSE -> "−$convertedAmount"
                            TransactionType.INCOME -> "+$convertedAmount"
                            TransactionType.TRANSFER, null -> convertedAmount
                        }
                        Text(
                            text = signedAmount,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = when (item.type) {
                                    TransactionType.EXPENSE -> ExpenseRoseLight
                                    TransactionType.INCOME -> EmeraldPrimaryLight
                                    TransactionType.TRANSFER -> Color(0xFF3B82F6)
                                    null -> Color.Gray
                                }
                            )
                        )
                    }

                    if (!item.isAmountValid || item.amount <= 0L) {
                        Text(
                            text = "⚠️ مبلغ تراکنش نامشخص است؛ لطفاً با زدن دکمه ویرایش، مبلغ را وارد کنید.",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFDC2626)
                        )
                    }

                    if (item.isTypeUncertain) {
                        Text(
                            text = "⚠️ لطفاً نوع تراکنش را بررسی و تأیید کنید:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFD97706)
                        )
                    }

                    if (!item.parseError.isNullOrBlank()) {
                        Text(
                            text = item.parseError.orEmpty(),
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Transfer notes if present
                    if (item.type == TransactionType.TRANSFER && (!item.sourceAccount.isNullOrBlank() || !item.destinationAccount.isNullOrBlank())) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF3B82F6).copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (!item.sourceAccount.isNullOrBlank()) {
                                    Text(
                                        text = "مبدأ: ${item.sourceAccount}",
                                        fontSize = 9.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (!item.destinationAccount.isNullOrBlank()) {
                                    Text(
                                        text = "مقصد: ${item.destinationAccount}",
                                        fontSize = 9.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Show the complete original SMS so the user can review the source text.
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Text(
                            text = item.smsText,
                            modifier = Modifier.padding(9.dp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    // Suggested Category & Date/Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "دسته‌بندی: ${item.category}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "${item.dateText} | ${item.timeText}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    val extractedDetails = listOfNotNull(
                        item.sourceAccount?.takeIf { it.isNotBlank() }?.let { "مبدأ: $it" },
                        item.destinationAccount?.takeIf { it.isNotBlank() }?.let { "مقصد: $it" },
                        item.balanceAmount?.let { "موجودی: ${MoneyFormatter.formatExact(MoneyAmount.of(it, item.balanceAmountUnit ?: item.amountUnit))}" },
                        item.trackingCode?.takeIf { it.isNotBlank() }?.let { "پیگیری: $it" }
                    ).joinToString(" | ")
                    if (extractedDetails.isNotBlank()) {
                        Text(
                            text = extractedDetails,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 9.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Action Buttons Row: [نادیده گرفتن | ویرایش | ثبت تراکنش]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Dismiss / Ignore Button
                        OutlinedButton(
                            onClick = { onDismiss(item.id) },
                            modifier = Modifier
                                .weight(0.9f)
                                .height(32.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "نادیده گرفتن",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Register Transaction Button
                        Button(
                            onClick = { onQuickAccept(item.id) },
                            // Open review even when parsing is incomplete so the user can correct it.
                            modifier = Modifier
                                .weight(1.3f)
                                .height(32.dp)
                                .testTag("btn_register_sms_transaction"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when (item.type) {
                                    TransactionType.EXPENSE -> ExpenseRoseLight
                                    TransactionType.INCOME -> EmeraldPrimaryLight
                                    TransactionType.TRANSFER -> Color(0xFF3B82F6)
                                    null -> Color.Gray
                                },
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "بررسی و ثبت",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionTypeChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) activeColor else Color.Transparent,
        border = BorderStroke(1.dp, if (isSelected) activeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
        modifier = modifier.height(28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = if (isSelected) Color.White else activeColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun EditSmsTransactionDialog(
    suggestion: BankSmsSuggestion,
    userAccounts: List<com.example.data.database.AccountEntity> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (Long, TransactionType, String, String, String, String?, (com.example.ui.screens.home.viewmodel.SmsAcceptResult) -> Unit) -> Unit
) {
    val appCurrency = LocalAppCurrency.current
    val amountUnit = MoneyFormatter.getUnitLabel(appCurrency)
    var selectedType by remember(suggestion.id) { mutableStateOf(suggestion.type) }
    var amountText by remember(suggestion.id) {
        mutableStateOf(
            if (suggestion.amountCandidates.filter { it > 0L }.distinct().size > 1) ""
            else if (suggestion.isAmountValid && suggestion.amount > 0L) IranianAmountUtils.formatDecimalWithCommas(MoneyAmount.of(suggestion.amount, suggestion.amountUnit).inUnit(appCurrency).value.stripTrailingZeros().toPlainString())
            else ""
        )
    }
    val amountDecimal = remember(amountText) { IranianAmountUtils.parseAmountToBigDecimal(amountText) }
    // Convert the user's app-currency entry to canonical Rials exactly.
    // Fractional Rials and overflow are rejected rather than rounded.
    val amountRials = remember(amountDecimal, appCurrency) {
        amountDecimal?.let { runCatching { MoneyAmount(it, appCurrency).asRialsLongExact() }.getOrNull() } ?: 0L
    }
    val validAmount = amountDecimal != null && amountDecimal.signum() > 0 && amountRials > 0L
    val amountOptions = remember(
        suggestion.id, suggestion.amount, suggestion.isAmountValid,
        suggestion.balanceAmount, suggestion.balanceAmountUnit, suggestion.amountUnit, suggestion.amountCandidates, suggestion.amountCandidateUnits
    ) {
        buildList {
            // Show every distinct grouped number extracted from the original SMS.
            // The user chooses the transaction amount; do not silently preselect
            // a guessed value when the message contains multiple candidates.
            suggestion.amountCandidates.forEachIndexed { candidateIndex, candidate ->
                if (candidate <= 0L) return@forEachIndexed
                val candidateUnit = suggestion.amountCandidateUnits.getOrNull(candidateIndex)
                    ?: if (candidate == suggestion.balanceAmount) suggestion.balanceAmountUnit ?: suggestion.amountUnit else suggestion.amountUnit
                val label = when {
                    candidate == suggestion.amount && candidateUnit == suggestion.amountUnit -> "مبلغ پیشنهادی"
                    candidate == suggestion.balanceAmount && candidateUnit == (suggestion.balanceAmountUnit ?: suggestion.amountUnit) -> "موجودی/مانده"
                    else -> "عدد گروه‌بندی‌شده"
                }
                add(MoneyAmount.of(candidate, candidateUnit).inUnit(appCurrency).value to label)
            }
            if (suggestion.isAmountValid && suggestion.amount > 0L) add(MoneyAmount.of(suggestion.amount, suggestion.amountUnit).inUnit(appCurrency).value to "مبلغ پیشنهادی")
            suggestion.balanceAmount?.takeIf { it > 0L }?.let { add(MoneyAmount.of(it, suggestion.balanceAmountUnit ?: suggestion.amountUnit).inUnit(appCurrency).value to "موجودی/مانده") }
        }.distinctBy { it.first }
    }
    var category by remember(suggestion.id) { mutableStateOf(suggestion.category) }
    val accounts = remember(userAccounts) { userAccounts.filter { it.isActive && it.deletedAt == null } }
    val sourceDigits = suggestion.sourceAccount?.let(IranianPhoneUtils::convertDigitsToEnglish)?.filter { it in '0'..'9' }
    val exactAccount = accounts.singleOrNull { a ->
        val digits = a.accountNumberMasked?.let(IranianPhoneUtils::convertDigitsToEnglish)?.filter { it in '0'..'9' }
        !sourceDigits.isNullOrBlank() && digits == sourceDigits
    }
    var accountId by remember(suggestion.id, accounts, sourceDigits) { mutableStateOf<String?>(exactAccount?.stringId) }
    var destinationId by remember(suggestion.id, accounts) { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var accountMenuExpanded by remember(suggestion.id) { mutableStateOf(false) }
    var destinationMenuExpanded by remember(suggestion.id) { mutableStateOf(false) }
    var categoryMenuExpanded by remember(suggestion.id) { mutableStateOf(false) }
    val expenseCats=listOf("خرید روزمره","سوپرمارکت و خرید","غذا و رستوران","خودرو و سوخت","قبوض و خدمات","اقساط و تسهیلات","سلامت و درمان","سایر")
    val incomeCats=listOf("حقوق و دستمزد","درآمد و واریز","سود سپرده","یارانه و کمک‌معیشتی","فروش کالا","سایر")
    val transferCats=listOf("انتقال بین‌بانکی","کارت به کارت","پایا","ساتنا","انتقال داخلی")
    val cats=when(selectedType){TransactionType.EXPENSE->expenseCats;TransactionType.INCOME->incomeCats;TransactionType.TRANSFER->transferCats;null->emptyList()}

    androidx.compose.ui.window.Dialog(onDismissRequest=onDismiss,properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)) {
        Card(
            modifier=Modifier.fillMaxWidth().widthIn(max=420.dp).heightIn(max=660.dp).padding(horizontal=14.dp),
            shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier=Modifier.fillMaxWidth().heightIn(max=660.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth().padding(start=15.dp,end=6.dp,top=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                    Column {
                        Text("بررسی و ثبت تراکنش",style=MaterialTheme.typography.titleMedium.copy(fontWeight=FontWeight.Bold))
                        Text(suggestion.bankName,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick=onDismiss,enabled=!submitting){Icon(Icons.Rounded.Close,contentDescription="بستن")}
                }
                Surface(Modifier.fillMaxWidth().padding(horizontal=12.dp),shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.primaryContainer.copy(alpha=.45f)) {
                    Column(Modifier.padding(horizontal=10.dp,vertical=7.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Text("مبلغ تراکنش (عدد را به $amountUnit وارد کنید)",style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.Bold)
                        OutlinedTextField(
                             value = amountText,
                             onValueChange = { input ->
                                 amountText = input
                                 error = null
                             },
                             singleLine = true,
                             keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                             isError = !validAmount && amountText.isNotEmpty(),
                             placeholder = { Text("مبلغ را به $amountUnit وارد کنید") },
                             modifier = Modifier.fillMaxWidth(),
                             shape = RoundedCornerShape(9.dp),
                             textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                         )
                        if(amountOptions.isNotEmpty()) LazyRow(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                            items(amountOptions){(v,label)->FilterChip(selected=amountDecimal?.compareTo(v)==0,onClick={amountText=IranianAmountUtils.formatDecimalWithCommas(v.stripTrailingZeros().toPlainString());error=null},label={Text(label + ": " + IranianAmountUtils.formatDecimalWithCommas(v.stripTrailingZeros().toPlainString()) + " " + amountUnit,fontSize=10.sp)})}
                        }
                        if(!validAmount) Text("مبلغ پیامک معتبر نیست؛ مبلغ صحیح را وارد کنید.",style=MaterialTheme.typography.bodySmall,color=Color(0xFFDC2626))
                    }
                }
                Column(Modifier.weight(1f,fill=false).verticalScroll(rememberScrollState()).padding(horizontal=13.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
                    Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Text("نوع تراکنش",style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.Bold)
                        Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                            listOf(TransactionType.INCOME to "درآمد",TransactionType.EXPENSE to "هزینه",TransactionType.TRANSFER to "انتقال").forEach{(t,label)->
                                FilterChip(modifier=Modifier.weight(1f),selected=selectedType==t,onClick={selectedType=t;category=when(t){TransactionType.INCOME->incomeCats.first();TransactionType.EXPENSE->expenseCats.first();TransactionType.TRANSFER->transferCats.first()};error=null},label={Text(label,maxLines=1)})
                            }
                        }
                    }
                    Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            val selectedAccountName = accounts.firstOrNull { it.stringId == accountId }?.name.orEmpty()
                            OutlinedTextField(
                                value = selectedAccountName,
                                onValueChange = {},
                                readOnly = true,
                                enabled = accounts.isNotEmpty(),
                                singleLine = true,
                                label = { Text(if (selectedType == TransactionType.TRANSFER) "حساب مبدأ" else "حساب مرتبط", maxLines = 1) },
                                placeholder = { Text("انتخاب حساب", maxLines = 1) },
                                trailingIcon = {
                                    IconButton(onClick = { accountMenuExpanded = true }, enabled = accounts.isNotEmpty()) {
                                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = "انتخاب حساب")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(9.dp),
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                            DropdownMenu(
                                expanded = accountMenuExpanded,
                                onDismissRequest = { accountMenuExpanded = false },
                                modifier = Modifier.widthIn(min = 150.dp, max = 230.dp)
                            ) {
                                accounts.forEach { account ->
                                    DropdownMenuItem(
                                        text = { Text(account.name, maxLines = 1) },
                                        onClick = {
                                            accountId = account.stringId
                                            accountMenuExpanded = false
                                            error = null
                                        }
                                    )
                                }
                            }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = {
                                    category = it
                                    error = null
                                },
                                singleLine = true,
                                label = { Text("دسته‌بندی", maxLines = 1) },
                                placeholder = { Text("انتخاب یا ورود دستی", maxLines = 1) },
                                trailingIcon = {
                                    IconButton(onClick = { categoryMenuExpanded = true }) {
                                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = "انتخاب دسته‌بندی")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(9.dp),
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                            DropdownMenu(
                                expanded = categoryMenuExpanded,
                                onDismissRequest = { categoryMenuExpanded = false },
                                modifier = Modifier.widthIn(min = 150.dp, max = 230.dp)
                            ) {
                                cats.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(item, maxLines = 1) },
                                        onClick = {
                                            category = item
                                            categoryMenuExpanded = false
                                            error = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                    if (accounts.isEmpty()) {
                        Text(
                            "حساب فعالی پیدا نشد؛ ابتدا یک حساب فعال ایجاد کنید.",
                            color = Color(0xFFDC2626),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(9.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .4f)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "متن کامل پیام بانکی",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                suggestion.smsText,
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    if (selectedType == TransactionType.TRANSFER) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val selectedDestinationName = accounts.firstOrNull { it.stringId == destinationId }?.name.orEmpty()
                            OutlinedTextField(
                                value = selectedDestinationName,
                                onValueChange = {},
                                readOnly = true,
                                enabled = accounts.isNotEmpty(),
                                singleLine = true,
                                label = { Text("حساب مقصد") },
                                placeholder = { Text("انتخاب حساب مقصد") },
                                trailingIcon = {
                                    IconButton(onClick = { destinationMenuExpanded = true }, enabled = accounts.isNotEmpty()) {
                                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = "انتخاب حساب مقصد")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(9.dp),
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                            DropdownMenu(
                                expanded = destinationMenuExpanded,
                                onDismissRequest = { destinationMenuExpanded = false },
                                modifier = Modifier.widthIn(min = 180.dp, max = 280.dp)
                            ) {
                                accounts.filterNot { it.stringId == accountId }.forEach { account ->
                                    DropdownMenuItem(
                                        text = { Text(account.name, maxLines = 1) },
                                        onClick = {
                                            destinationId = account.stringId
                                            destinationMenuExpanded = false
                                            error = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                    if (!suggestion.sourceAccount.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(9.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .25f)
                        ) {
                            Text(
                                "شماره حساب بانکی شناسایی‌شده: ${suggestion.sourceAccount}",
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if(error!=null) Surface(shape=RoundedCornerShape(9.dp),color=Color(0xFFFEF2F2),border=BorderStroke(1.dp,Color(0xFFFCA5A5)),modifier=Modifier.fillMaxWidth()){
                        Row(Modifier.padding(8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
                            Icon(Icons.Rounded.Info,null,tint=Color(0xFFDC2626),modifier=Modifier.size(16.dp))
                            Text(error!!,style=MaterialTheme.typography.bodySmall,color=Color(0xFF991B1B))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=9.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    OutlinedButton(onClick=onDismiss,enabled=!submitting,modifier=Modifier.weight(1f),shape=RoundedCornerShape(9.dp)){Text("انصراف")}
                    Button(
                        onClick={
                            val t=selectedType
                            if(t==null){error="لطفاً نوع تراکنش را مشخص کنید.";return@Button}
                            if(!validAmount){error="لطفاً مبلغ معتبر وارد کنید.";return@Button}
                            val src=accountId
                            if(src.isNullOrBlank()){error="لطفاً حساب مرتبط را انتخاب کنید.";return@Button}
                            val dst=if(t==TransactionType.TRANSFER)destinationId else null
                            if(t==TransactionType.TRANSFER&&dst.isNullOrBlank()){error="لطفاً حساب مقصد را انتخاب کنید.";return@Button}
                            submitting=true;error=null
                            onConfirm(amountRials, t, category.trim().ifEmpty { suggestion.category }, src, "ثبت از پیامک " + suggestion.bankName, dst) { result ->
                                submitting=false
                                when(result){
                                    com.example.ui.screens.home.viewmodel.SmsAcceptResult.Success,com.example.ui.screens.home.viewmodel.SmsAcceptResult.AlreadyExists->onDismiss()
                                    com.example.ui.screens.home.viewmodel.SmsAcceptResult.AccountRequired->error="لطفاً یک حساب معتبر انتخاب کنید."
                                    com.example.ui.screens.home.viewmodel.SmsAcceptResult.DestinationAccountRequired->error="لطفاً حساب مقصد معتبری انتخاب کنید."
                                    com.example.ui.screens.home.viewmodel.SmsAcceptResult.InvalidAmount->error="مبلغ استخراج‌شده نامعتبر است؛ مبلغ را بررسی کنید."
                                    com.example.ui.screens.home.viewmodel.SmsAcceptResult.TypeNotSelected->error="لطفاً نوع تراکنش را مشخص کنید."
                                    com.example.ui.screens.home.viewmodel.SmsAcceptResult.Failed->error="خطا در ثبت تراکنش؛ دوباره تلاش کنید."
                                }
                            }
                        },
                        enabled=selectedType!=null&&validAmount&&!submitting,
                        modifier=Modifier.weight(1.35f),shape=RoundedCornerShape(9.dp),
                        colors=ButtonDefaults.buttonColors(containerColor=when(selectedType){TransactionType.EXPENSE->ExpenseRoseLight;TransactionType.INCOME->EmeraldPrimaryLight;TransactionType.TRANSFER->Color(0xFF3B82F6);null->Color.Gray})
                    ){
                        if(submitting) CircularProgressIndicator(Modifier.size(16.dp),strokeWidth=2.dp,color=Color.White) else Text("تأیید و ثبت",fontWeight=FontWeight.Bold)
                    }
                }
            }
        }
    }
}
}
