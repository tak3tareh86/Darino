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
import com.example.util.MoneyFormatter

@Composable
fun BankSmsAssistantCard(
    permissionState: SmsPermissionState,
    isScanning: Boolean,
    errorMessage: String?,
    queue: List<BankSmsSuggestion>,
    userAccounts: List<com.example.data.database.AccountEntity> = emptyList(),
    onTypeChange: (id: String, type: TransactionType) -> Unit,
    onAccept: (id: String, amount: Long?, type: TransactionType?, category: String?, account: String?, desc: String?, dest: String?, onResult: (com.example.ui.screens.home.viewmodel.SmsAcceptResult) -> Unit) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onUpdateCustomDetails: (id: String, amount: Long?, type: TransactionType?, category: String?, account: String?, destAccount: String?) -> Unit = { _, _, _, _, _, _ -> },
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
            onConfirm = { amount, type, cat, acc, desc, dest, onResult ->
                onUpdateCustomDetails(editingSuggestion.id, amount, type, cat, acc, dest)
                onAccept(editingSuggestion.id, amount, type, cat, acc, desc, dest) { result ->
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
                        onEditClick = { suggestion ->
                            editingSuggestionId = suggestion.id
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
    onEditClick: (BankSmsSuggestion) -> Unit,
    onManagePatterns: () -> Unit,
    onDismiss: (id: String) -> Unit,
    onRefreshScan: () -> Unit
) {
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

                        Text(
                            text = when (item.type) {
                                TransactionType.EXPENSE -> MoneyFormatter.formatSignedToman(item.amount, isExpense = true)
                                TransactionType.INCOME -> MoneyFormatter.formatSignedToman(item.amount, isExpense = false)
                                TransactionType.TRANSFER -> MoneyFormatter.formatToman(item.amount)
                                null -> MoneyFormatter.formatToman(item.amount)
                            },
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
                        item.balanceAmount?.let { "موجودی: $it تومان" },
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

                        // Edit Before Save Button
                        OutlinedButton(
                            onClick = { onEditClick(item) },
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "ویرایش",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Register Transaction Button
                        Button(
                            onClick = { onQuickAccept(item.id) },
                            enabled = (item.type != null && !item.isTypeUncertain && item.isAmountValid && item.amount > 0L),
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
    onConfirm: (
        amount: Long,
        type: TransactionType,
        category: String,
        account: String,
        description: String,
        destAccount: String?,
        onResult: (com.example.ui.screens.home.viewmodel.SmsAcceptResult) -> Unit
    ) -> Unit
) {
    var selectedType by remember(suggestion.id) { mutableStateOf(suggestion.type) }
    var amountText by remember(suggestion.id) {
        mutableStateOf(if (suggestion.isAmountValid && suggestion.amount > 0L) IranianAmountUtils.formatWithCommas(suggestion.amount.toString()) else "")
    }
    val parsedAmount = remember(amountText) {
        IranianAmountUtils.cleanAmountDigits(amountText).toLongOrNull() ?: 0L
    }
    val isAmountValid = parsedAmount > 0L

    var category by remember(suggestion.id) { mutableStateOf(suggestion.category) }
    val activeAccounts = remember(userAccounts) {
        userAccounts.filter { it.isActive && it.deletedAt == null }
    }
    // Auto-link only when a stored account number is a full exact match.
    // Masked/partial values never trigger a guess; the user chooses manually instead.
    val recognizedBankAccountDigits = suggestion.sourceAccount
        ?.let(IranianPhoneUtils::convertDigitsToEnglish)
        ?.filter { it in '0'..'9' }
    val exactStoredAccount = activeAccounts.singleOrNull { account ->
        val storedDigits = account.accountNumberMasked
            ?.let(IranianPhoneUtils::convertDigitsToEnglish)
            ?.filter { it in '0'..'9' }
        !recognizedBankAccountDigits.isNullOrBlank() && storedDigits == recognizedBankAccountDigits
    }
    var selectedAccountId by remember(suggestion.id, activeAccounts, recognizedBankAccountDigits) {
        mutableStateOf<String?>(exactStoredAccount?.stringId)
    }
    var selectedDestinationAccountId by remember(suggestion.id, activeAccounts) {
        mutableStateOf<String?>(null)
    }
    var description by remember(suggestion.id) {
        mutableStateOf("ثبت از پیامک ${suggestion.bankName}")
    }

    var isSubmitting by remember { mutableStateOf(false) }
    var dialogError by remember { mutableStateOf<String?>(null) }

    val expenseCategories = listOf("خرید روزمره", "سوپرمارکت و خرید", "غذا و رستوران", "خودرو و سوخت", "قبوض و خدمات", "اقساط و تسهیلات", "سلامت و درمان", "سایر")
    val incomeCategories = listOf("حقوق و دستمزد", "درآمد و واریز", "سود سپرده", "یارانه و کمک‌معیشتی", "فروش کالا", "سایر")
    val transferCategories = listOf("انتقال بین‌بانکی", "کارت به کارت", "پایا", "ساتنا", "انتقال داخلی")

    val categoriesList = when (selectedType) {
        TransactionType.EXPENSE -> expenseCategories
        TransactionType.INCOME -> incomeCategories
        TransactionType.TRANSFER -> transferCategories
        null -> emptyList()
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Title
                    Text(
                        text = "ویرایش تراکنش قبل از ثبت",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )

                    // The detected type is only a suggestion; the user can correct it here.
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "نوع تراکنش",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = selectedType == TransactionType.INCOME,
                                onClick = {
                                    selectedType = TransactionType.INCOME
                                    category = incomeCategories.first()
                                    dialogError = null
                                },
                                label = { Text("درآمد", fontSize = 10.sp, maxLines = 1) }
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = selectedType == TransactionType.EXPENSE,
                                onClick = {
                                    selectedType = TransactionType.EXPENSE
                                    category = expenseCategories.first()
                                    dialogError = null
                                },
                                label = { Text("هزینه", fontSize = 10.sp, maxLines = 1) }
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = selectedType == TransactionType.TRANSFER,
                                onClick = {
                                    selectedType = TransactionType.TRANSFER
                                    category = transferCategories.first()
                                    dialogError = null
                                },
                                label = { Text("انتقال", fontSize = 10.sp, maxLines = 1) }
                            )
                        }
                    }

                    // Keep the original SMS visible in the review dialog for verification.
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Column(modifier = Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "متن کامل پیامک بانکی",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                suggestion.smsText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    // Amount is prefilled when detected and remains editable for correction.
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "مبلغ تراکنش (تومان):",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            singleLine = true,
                            isError = !isAmountValid && amountText.isNotEmpty(),
                            placeholder = { Text("مثال: 50,000", fontSize = 11.5.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (selectedType) {
                                    TransactionType.EXPENSE -> ExpenseRoseLight
                                    TransactionType.INCOME -> EmeraldPrimaryLight
                                    TransactionType.TRANSFER -> Color(0xFF3B82F6)
                                    null -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        )
                        if (isAmountValid) {
                            Text(
                                text = "معادل: ${MoneyFormatter.formatToman(parsedAmount)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        } else {
                            Text(
                                text = "مبلغ به‌درستی از پیامک استخراج نشد؛ مبلغ صحیح را وارد کنید.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    color = Color(0xFFDC2626),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    // Display the exact bank account number recognized by the saved allow-list.
                    // The user still explicitly links it to a Darino account; no account is guessed.
                    if (!suggestion.sourceAccount.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    "شماره حساب بانکی شناسایی‌شده",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    suggestion.sourceAccount.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "اگر شماره کامل در حساب دارینو ذخیره شده باشد، حساب متناظر خودکار انتخاب می‌شود؛ در غیر این صورت آن را دستی انتخاب کنید.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Link the SMS transaction to real accounts so account balances stay correct.
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (selectedType == TransactionType.TRANSFER) "حساب مبدأ" else "حساب مرتبط",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        if (activeAccounts.isEmpty()) {
                            Text(
                                text = "حساب فعال پیدا نشد؛ ابتدا یک حساب فعال بسازید.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFDC2626), fontSize = 10.sp)
                            )
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(activeAccounts) { account ->
                                    FilterChip(
                                        selected = selectedAccountId == account.stringId,
                                        onClick = {
                                            selectedAccountId = account.stringId
                                            dialogError = null
                                        },
                                        label = { Text(account.name, fontSize = 9.sp) }
                                    )
                                }
                            }
                        }
                    }

                    if (selectedType == TransactionType.TRANSFER) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "حساب مقصد",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(activeAccounts) { account ->
                                    FilterChip(
                                        selected = selectedDestinationAccountId == account.stringId,
                                        onClick = {
                                            selectedDestinationAccountId = account.stringId
                                            dialogError = null
                                        },
                                        label = { Text(account.name, fontSize = 9.sp) }
                                    )
                                }
                            }
                            Text(
                                text = "می‌توانید هر حساب فعالی را به‌عنوان مقصد انتخاب کنید؛ حتی همان حساب مبدأ.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            )
                        }
                    }

                    // Category input
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "دسته‌بندی:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp)
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(categoriesList) { cat ->
                                FilterChip(
                                    selected = category == cat,
                                    onClick = { category = cat },
                                    label = { Text(cat, fontSize = 9.sp) }
                                )
                            }
                        }
                    }

                    // The selected account IDs are validated again by the ViewModel before persistence.

                    // Error Banner if validation / registration failed (Dialog stays open so user can correct)
                    if (dialogError != null) {
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
                                    text = dialogError!!,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF991B1B),
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    val canSubmit = selectedType != null && isAmountValid && !isSubmitting

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            enabled = !isSubmitting,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("انصراف", fontSize = 11.5.sp)
                        }

                        Button(
                            onClick = {
                                val sType = selectedType ?: return@Button
                                if (!isAmountValid) return@Button
                                dialogError = null
                                val finalCat = category.trim().ifEmpty { suggestion.category }
                                val sourceId = selectedAccountId
                                if (sourceId.isNullOrBlank()) {
                                    dialogError = "لطفاً حساب مرتبط را انتخاب کنید."
                                    return@Button
                                }
                                val destinationId = if (sType == TransactionType.TRANSFER) {
                                    selectedDestinationAccountId
                                } else {
                                    null
                                }
                                if (sType == TransactionType.TRANSFER && destinationId.isNullOrBlank()) {
                                    dialogError = "لطفاً حساب مقصد را انتخاب کنید."
                                    return@Button
                                }

                                isSubmitting = true
                                onConfirm(
                                    parsedAmount,
                                    sType,
                                    finalCat,
                                    sourceId,
                                    description,
                                    destinationId
                                ) { result ->
                                    isSubmitting = false
                                    when (result) {
                                        com.example.ui.screens.home.viewmodel.SmsAcceptResult.Success,
                                        com.example.ui.screens.home.viewmodel.SmsAcceptResult.AlreadyExists -> {
                                            onDismiss()
                                        }
                                        com.example.ui.screens.home.viewmodel.SmsAcceptResult.AccountRequired -> {
                                            dialogError = "حساب بانکی مشخص نیست؛ لطفاً یک حساب معتبر انتخاب کنید."
                                        }
                                        com.example.ui.screens.home.viewmodel.SmsAcceptResult.DestinationAccountRequired -> {
                                            dialogError = "حساب مقصد انتقال نامعتبر است؛ لطفاً یک حساب فعال انتخاب کنید."
                                        }
                                        com.example.ui.screens.home.viewmodel.SmsAcceptResult.InvalidAmount -> {
                                            dialogError = "مبلغ استخراج‌شده از الگوی ثبت‌شده نامعتبر است؛ الگو و متن پیامک را بررسی کنید."
                                        }
                                        com.example.ui.screens.home.viewmodel.SmsAcceptResult.TypeNotSelected -> {
                                            dialogError = "لطفاً نوع تراکنش را مشخص کنید."
                                        }
                                        com.example.ui.screens.home.viewmodel.SmsAcceptResult.Failed -> {
                                            dialogError = "خطا در ثبت تراکنش در پایگاه داده. لطفاً دوباره تلاش کنید."
                                        }
                                    }
                                }
                            },
                            enabled = canSubmit,
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when (selectedType) {
                                    TransactionType.EXPENSE -> ExpenseRoseLight
                                    TransactionType.INCOME -> EmeraldPrimaryLight
                                    TransactionType.TRANSFER -> Color(0xFF3B82F6)
                                    null -> Color.Gray
                                }
                            )
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Text(
                                    text = "تأیید و ثبت",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
