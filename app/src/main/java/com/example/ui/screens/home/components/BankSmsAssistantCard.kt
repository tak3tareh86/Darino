package com.example.ui.screens.home.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.screens.home.domain.BankSmsSuggestion
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun BankSmsAssistantCard(
    queue: List<BankSmsSuggestion>,
    onAccept: (id: String, category: String?, account: String?) -> Unit,
    onDismiss: (String) -> Unit,
    onSimulateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (queue.isEmpty()) return

    val currentSuggestion = queue.first()
    val remainingCount = queue.size - 1
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val context = LocalContext.current

    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasSmsPermission = isGranted
    }

    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        ConfirmSmsDialog(
            suggestion = currentSuggestion,
            onDismiss = { showConfirmDialog = false },
            onConfirm = { cat, acc ->
                onAccept(currentSuggestion.id, cat, acc)
                showConfirmDialog = false
            }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bank_sms_assistant_card"),
        shape = RoundedCornerShape(RadiusMD),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF0F131E) else Color(0xFFFFFFFF)
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF232D42) else Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 7.dp)
        ) {
            // Header: Icon + Title + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(
                                if (isDark) Color(0xFF16252C) else Color(0xFFE6F4EA),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Sms,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF14B8A6) else EmeraldPrimaryLight,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "دستیار هوشمند پیامک",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (remainingCount > 0) {
                            Text(
                                text = "۱ از ${queue.size} پیامک در صف بررسی",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = if (isDark) Color(0xFF6366F1) else Color(0xFF4F46E5)
                            )
                        } else {
                            Text(
                                text = "تراکنش بانکی موقت نیازمند تأیید شما",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // User Permission Control Banner if SMS permission is not granted
            if (!hasSmsPermission) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(RadiusSM),
                    color = if (isDark) Color(0xFF2E1A08) else Color(0xFFFFF7ED),
                    border = BorderStroke(1.dp, Color(0xFFF97316).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = Color(0xFFF97316),
                            modifier = Modifier.size(16.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "دسترسی پیامک بانکی",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp,
                                    color = if (isDark) Color(0xFFFDBA74) else Color(0xFFC2410C)
                                )
                            )
                            Text(
                                text = "برای خواندن خودکار پیامک‌ها مجوز را فعال نمایید.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 9.sp,
                                    color = if (isDark) Color(0xFFFED7AA) else Color(0xFFEA580C)
                                )
                            )
                        }
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.READ_SMS) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF97316),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 7.dp, vertical = 1.dp),
                            shape = RoundedCornerShape(RadiusSM),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text("اعطا", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Sliding animation when top SMS suggestion changes
            AnimatedContent(
                targetState = currentSuggestion,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "sms_queue_transition"
            ) { suggestion ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isDark) Color(0xFF161E2E) else Color(0xFFF8FAFC),
                            RoundedCornerShape(RadiusSM)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    // Bank Name, Amount, Type (Withdrawal vs Deposit)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(RadiusSM),
                                color = if (suggestion.isExpense) {
                                    if (isDark) Color(0xFF3F161C) else Color(0xFFFFECEE)
                                } else {
                                    if (isDark) Color(0xFF0F3A22) else Color(0xFFE6F9EE)
                                },
                                modifier = Modifier.padding(1.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = if (suggestion.isExpense) Icons.Rounded.TrendingDown else Icons.Rounded.TrendingUp,
                                        contentDescription = null,
                                        tint = if (suggestion.isExpense) ExpenseRoseLight else EmeraldPrimaryLight,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = if (suggestion.isExpense) "برداشت" else "واریز",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                        color = if (suggestion.isExpense) ExpenseRoseLight else EmeraldPrimaryLight
                                    )
                                }
                            }

                            Text(
                                text = suggestion.bankName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = suggestion.formattedAmount,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (suggestion.isExpense) ExpenseRoseLight else EmeraldPrimaryLight
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Raw SMS Content representation (compact 2 lines)
                    Text(
                        text = suggestion.smsText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 9.5.sp,
                            lineHeight = 13.5.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    // Category + Date & Time Row inside Card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "دسته: ${suggestion.category}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 8.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            )
                        }

                        // Date and Time displayed clearly inside card
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "${suggestion.dateText} | ${suggestion.timeText}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Reject Button
                OutlinedButton(
                    onClick = { onDismiss(currentSuggestion.id) },
                    modifier = Modifier.weight(1f).height(28.dp),
                    shape = RoundedCornerShape(RadiusSM),
                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEF4444)
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "رد کردن",
                        color = Color(0xFFEF4444),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    )
                }

                // Confirm and Smart Register Button
                Button(
                    onClick = { showConfirmDialog = true },
                    modifier = Modifier.weight(1.3f).height(28.dp),
                    shape = RoundedCornerShape(RadiusSM),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF14B8A6) else EmeraldPrimaryLight,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "تأیید و ثبت هوشمند",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmSmsDialog(
    suggestion: BankSmsSuggestion,
    onDismiss: () -> Unit,
    onConfirm: (category: String, account: String) -> Unit
) {
    var category by remember(suggestion.id) { mutableStateOf(suggestion.category) }
    var accountName by remember(suggestion.id) { mutableStateOf(suggestion.bankName) }

    val expenseCategories = listOf("خرید روزمره", "سوپرمارکت", "قبوض و خدمات", "خودرو و بنزین", "اقساط", "سایر")
    val incomeCategories = listOf("حقوق و دستمزد", "درآمد جانبی", "سود سپرده", "فروش کالا", "سایر")
    val defaultAccounts = listOf("بانک ملت", "بانک ملی", "بانک صادرات", "بانک سامان", "بانک تجارت", "بانک پاسارگاد", "بلوبانک", "کیف پول")

    val categoriesList = if (suggestion.isExpense) expenseCategories else incomeCategories
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

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
                    .widthIn(max = 330.dp)
                    .offset(y = (-75).dp) // Shifted upwards so it centers perfectly in the screen
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (suggestion.isExpense) Icons.Rounded.TrendingDown else Icons.Rounded.TrendingUp,
                        contentDescription = null,
                        tint = if (suggestion.isExpense) Color(0xFFEF4444) else EmeraldPrimaryLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (suggestion.isExpense) "ثبت هوشمند برداشت" else "ثبت هوشمند واریز",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                }

                // Summary Box with Amount, Date & Time
                Surface(
                    shape = RoundedCornerShape(RadiusSM),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = suggestion.bankName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                )
                            )
                            Text(
                                text = suggestion.formattedAmount,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = if (suggestion.isExpense) Color(0xFFEF4444) else EmeraldPrimaryLight
                                )
                            )
                        }
                        // Date and Time
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "تاریخ: ${suggestion.dateText} | ساعت: ${suggestion.timeText}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }

                // Category Input
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = if (suggestion.isExpense) "نوع هزینه (دسته‌بندی):" else "نوع درآمد (دسته‌بندی):",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        )
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(RadiusSM),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(categoriesList) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 9.5.sp) }
                            )
                        }
                    }
                }

                // Account / Card Input
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = if (suggestion.isExpense) "کارت / حساب مبدأ:" else "حساب مقصد:",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        )
                    )
                    OutlinedTextField(
                        value = accountName,
                        onValueChange = { accountName = it },
                        placeholder = { Text("نام بانک یا حساب را وارد کنید...", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("sms_account_name_input"),
                        shape = RoundedCornerShape(RadiusSM),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(defaultAccounts) { acc ->
                            FilterChip(
                                selected = accountName == acc,
                                onClick = { accountName = acc },
                                label = { Text(acc, fontSize = 9.5.sp) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("انصراف", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp))
                    }

                    Button(
                        onClick = {
                            val chosenCat = category.trim().ifEmpty { suggestion.category }
                            val chosenAcc = accountName.trim().ifEmpty { suggestion.bankName }
                            onConfirm(chosenCat, chosenAcc)
                        },
                        modifier = Modifier.weight(1.4f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (suggestion.isExpense) Color(0xFFEF4444) else EmeraldPrimaryLight,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(RadiusSM)
                    ) {
                        Text(
                            text = if (suggestion.isExpense) "ثبت قطعی برداشت" else "ثبت قطعی واریز",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
}
