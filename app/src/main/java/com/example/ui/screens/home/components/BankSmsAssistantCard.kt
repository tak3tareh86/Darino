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
                        onRequestPermission = onRequestPermission
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
                        errorMessage = errorMessage,
                        queue = queue,
                        onTypeChange = onTypeChange,
                        onQuickAccept = { id ->
                            onAccept(id, null, null, null, null, null, null) { }
                        },
                        onEditClick = { suggestion ->
                            editingSuggestionId = suggestion.id
                        },
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
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sms_onboarding_view"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) Color(0xFF131D31) else Color(0xFFF1F5F9),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "با دسترسی به پیامک‌های بانکی، دارینو می‌تواند تراکنش‌های مالی شما را شناسایی کند.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.padding(12.dp)
            )
        }

        Button(
            onClick = onRequestPermission,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0D9488),
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .testTag("btn_request_sms_permission")
        ) {
            Icon(
                imageVector = Icons.Rounded.Security,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "فعال کردن دسترسی پیامک",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                )
            )
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
                    text = "برای تشخیص واریز و برداشت‌ها، برنامه نیازمند مجوز خواندن پیامک است.",
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
    errorMessage: String?,
    queue: List<BankSmsSuggestion>,
    onTypeChange: (id: String, type: TransactionType) -> Unit,
    onQuickAccept: (id: String) -> Unit,
    onEditClick: (BankSmsSuggestion) -> Unit,
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
                        text = "هیچ پیامک مالی جدیدی یافت نشد",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "تراکنش‌های پیامکی شما به‌روز هستند.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
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
                            text = item.formattedAmount,
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
                            text = "⚠️ لطفاً نوع تراکنش را مشخص کنید:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFD97706)
                        )
                    }

                    // Interactive Transaction Type Selector: [هزینه | درآمد | انتقال]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TransactionTypeChip(
                            title = "هزینه",
                            icon = Icons.Rounded.TrendingDown,
                            isSelected = item.type == TransactionType.EXPENSE,
                            activeColor = ExpenseRoseLight,
                            onClick = { onTypeChange(item.id, TransactionType.EXPENSE) },
                            modifier = Modifier.weight(1f)
                        )
                        TransactionTypeChip(
                            title = "درآمد",
                            icon = Icons.Rounded.TrendingUp,
                            isSelected = item.type == TransactionType.INCOME,
                            activeColor = EmeraldPrimaryLight,
                            onClick = { onTypeChange(item.id, TransactionType.INCOME) },
                            modifier = Modifier.weight(1f)
                        )
                        TransactionTypeChip(
                            title = "انتقال",
                            icon = Icons.AutoMirrored.Rounded.CompareArrows,
                            isSelected = item.type == TransactionType.TRANSFER,
                            activeColor = Color(0xFF3B82F6),
                            onClick = { onTypeChange(item.id, TransactionType.TRANSFER) },
                            modifier = Modifier.weight(1f)
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

                    // Original SMS snippet
                    Text(
                        text = item.smsText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

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
                                text = "ثبت تراکنش",
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
    val activeUserAccounts = remember(userAccounts) {
        userAccounts.filter { it.isActive && it.deletedAt == null }
    }

    var selectedType by remember(suggestion.id) { mutableStateOf(suggestion.type) }
    var amountText by remember(suggestion.id) {
        mutableStateOf(if (suggestion.isAmountValid && suggestion.amount > 0L) suggestion.amount.toString() else "")
    }
    val parsedAmount = remember(amountText) {
        val clean = IranianPhoneUtils.convertDigitsToEnglish(amountText)
            .replace(",", "").replace("،", "").replace("٫", "").replace("٬", "").replace(".", "").trim()
        clean.toLongOrNull() ?: 0L
    }
    val isAmountValid = parsedAmount > 0L

    var category by remember(suggestion.id) { mutableStateOf(suggestion.category) }
    var description by remember(suggestion.id) {
        mutableStateOf("ثبت از پیامک ${suggestion.bankName}")
    }

    // Resolves initial source account strictly: by stringId, unique exact name, unique card digits, or unique bank name
    var selectedSourceAccountId by remember(suggestion.id, activeUserAccounts) {
        val initial = activeUserAccounts.firstOrNull { acc ->
            acc.stringId == suggestion.sourceAccount
        } ?: activeUserAccounts.firstOrNull { acc ->
            !suggestion.sourceAccount.isNullOrBlank() && acc.name.equals(suggestion.sourceAccount, ignoreCase = true)
        } ?: run {
            val targetDigits = suggestion.sourceAccount?.filter { it.isDigit() } ?: ""
            if (targetDigits.length >= 4) {
                val matches = activeUserAccounts.filter { acc ->
                    val accDigits = acc.accountNumberMasked?.filter { it.isDigit() } ?: ""
                    accDigits.length >= 4 && accDigits.takeLast(4) == targetDigits.takeLast(4)
                }
                if (matches.size == 1) matches.first() else null
            } else null
        } ?: run {
            if (!suggestion.bankName.isNullOrBlank()) {
                val cleanHint = suggestion.bankName.replace("بانک", "").trim()
                val matches = activeUserAccounts.filter { acc ->
                    val cleanBank = acc.bankName?.replace("بانک", "")?.trim() ?: ""
                    cleanHint.isNotEmpty() && cleanBank.equals(cleanHint, ignoreCase = true)
                }
                if (matches.size == 1) matches.first() else null
            } else null
        }
        mutableStateOf(initial?.stringId)
    }

    // Resolves initial destination account for transfer
    var selectedDestAccountId by remember(suggestion.id, activeUserAccounts) {
        val initial = activeUserAccounts.firstOrNull { acc ->
            acc.stringId == suggestion.destinationAccount
        } ?: activeUserAccounts.firstOrNull { acc ->
            !suggestion.destinationAccount.isNullOrBlank() && acc.name.equals(suggestion.destinationAccount, ignoreCase = true)
        }
        mutableStateOf(initial?.stringId)
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

                    // Editable Amount Input
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "مبلغ تراکنش (تومان):",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = {
                                amountText = it
                                dialogError = null
                            },
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
                                text = "⚠️ لطفاً مبلغ معتبر (بزرگتر از صفر تومان) وارد کنید.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    color = Color(0xFFDC2626),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    // Type Selector
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "نوع تراکنش:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TransactionTypeChip(
                                title = "هزینه",
                                icon = Icons.Rounded.TrendingDown,
                                isSelected = selectedType == TransactionType.EXPENSE,
                                activeColor = ExpenseRoseLight,
                                onClick = {
                                    selectedType = TransactionType.EXPENSE
                                    category = "خرید روزمره"
                                    dialogError = null
                                },
                                modifier = Modifier.weight(1f)
                            )
                            TransactionTypeChip(
                                title = "درآمد",
                                icon = Icons.Rounded.TrendingUp,
                                isSelected = selectedType == TransactionType.INCOME,
                                activeColor = EmeraldPrimaryLight,
                                onClick = {
                                    selectedType = TransactionType.INCOME
                                    category = "درآمد و واریز"
                                    dialogError = null
                                },
                                modifier = Modifier.weight(1f)
                            )
                            TransactionTypeChip(
                                title = "انتقال",
                                icon = Icons.AutoMirrored.Rounded.CompareArrows,
                                isSelected = selectedType == TransactionType.TRANSFER,
                                activeColor = Color(0xFF3B82F6),
                                onClick = {
                                    selectedType = TransactionType.TRANSFER
                                    category = "انتقال بین‌بانکی"
                                    dialogError = null
                                },
                                modifier = Modifier.weight(1f)
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

                    // Real Account Selection (Strictly active, undeleted accounts of current user)
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = if (selectedType == TransactionType.TRANSFER) "حساب مبدأ (از):" else "حساب بانکی:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        if (activeUserAccounts.isEmpty()) {
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
                                        text = "هیچ حساب بانکی فعالی در برنامه یافت نشد. لطفاً ابتدا در بخش حساب‌ها یک حساب تعریف کنید.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF991B1B),
                                            fontWeight = FontWeight.Medium
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(activeUserAccounts, key = { it.stringId }) { acc ->
                                    val isSelected = selectedSourceAccountId == acc.stringId
                                    val chipLabel = if (!acc.bankName.isNullOrBlank() && !acc.name.contains(acc.bankName!!)) {
                                        "${acc.name} (${acc.bankName})"
                                    } else acc.name
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedSourceAccountId = acc.stringId
                                            if (selectedDestAccountId == acc.stringId) {
                                                selectedDestAccountId = null
                                            }
                                            dialogError = null
                                        },
                                        label = {
                                            Text(
                                                chipLabel,
                                                fontSize = 9.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        leadingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        } else null
                                    )
                                }
                            }
                            if (selectedSourceAccountId == null) {
                                Text(
                                    text = "⚠️ لطفاً یک حساب بانکی معتبر انتخاب کنید.",
                                    fontSize = 10.sp,
                                    color = Color(0xFFDC2626),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Destination Account (Only for TRANSFER - must be distinct and valid)
                    if (selectedType == TransactionType.TRANSFER) {
                        val availableDestAccounts = remember(activeUserAccounts, selectedSourceAccountId) {
                            activeUserAccounts.filter { it.stringId != selectedSourceAccountId }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "حساب مقصد (به):",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            if (availableDestAccounts.isEmpty()) {
                                Text(
                                    text = "⚠️ برای انتقال، به حداقل دو حساب بانکی فعال نیاز است. لطفاً حساب دیگری در برنامه تعریف کنید.",
                                    fontSize = 10.sp,
                                    color = Color(0xFFDC2626),
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    items(availableDestAccounts, key = { it.stringId }) { acc ->
                                        val isSelected = selectedDestAccountId == acc.stringId
                                        val chipLabel = if (!acc.bankName.isNullOrBlank() && !acc.name.contains(acc.bankName!!)) {
                                            "${acc.name} (${acc.bankName})"
                                        } else acc.name
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                selectedDestAccountId = acc.stringId
                                                dialogError = null
                                            },
                                            label = {
                                                Text(
                                                    chipLabel,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            leadingIcon = if (isSelected) {
                                                {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            } else null
                                        )
                                    }
                                }
                                if (selectedDestAccountId == null) {
                                    Text(
                                        text = "⚠️ لطفاً حساب مقصد انتقال را انتخاب کنید (متفاوت از حساب مبدأ).",
                                        fontSize = 10.sp,
                                        color = Color(0xFFDC2626),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

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

                    val canSubmit = (selectedType != null) &&
                            isAmountValid &&
                            (selectedSourceAccountId != null) &&
                            (selectedType != TransactionType.TRANSFER || (selectedDestAccountId != null && selectedDestAccountId != selectedSourceAccountId)) &&
                            !isSubmitting

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
                                val srcId = selectedSourceAccountId
                                if (srcId == null) {
                                    dialogError = "لطفاً یک حساب بانکی معتبر انتخاب کنید."
                                    return@Button
                                }
                                val destId = if (sType == TransactionType.TRANSFER) selectedDestAccountId else null
                                if (sType == TransactionType.TRANSFER && (destId == null || destId == srcId)) {
                                    dialogError = "حساب مقصد انتقال نامعتبر یا یکسان با حساب مبدأ است؛ لطفاً حساب مقصد دیگری انتخاب کنید."
                                    return@Button
                                }

                                isSubmitting = true
                                dialogError = null
                                val finalCat = category.trim().ifEmpty { suggestion.category }

                                onConfirm(
                                    parsedAmount,
                                    sType,
                                    finalCat,
                                    srcId,
                                    description,
                                    destId
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
                                            dialogError = "حساب مقصد انتقال نامعتبر یا یکسان با مبدأ است؛ لطفاً حساب مقصد دیگری انتخاب کنید."
                                        }
                                        com.example.ui.screens.home.viewmodel.SmsAcceptResult.InvalidAmount -> {
                                            dialogError = "مبلغ تراکنش نامعتبر است؛ لطفاً مبلغ صحیح را وارد کنید."
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
