package com.example.ui.screens.installments

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.Layered3DCard
import com.example.ui.components.SharedPersianDatePickerDialog
import com.example.ui.components.Soft3DIcon
import com.example.ui.screens.installments.model.InstallmentCategory
import com.example.ui.screens.installments.model.InstallmentItem
import com.example.ui.screens.settings.model.AppCurrency
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM
import com.example.util.IranianAmountUtils
import com.example.util.IranianPhoneUtils
import com.example.util.MoneyFormatter
import com.example.util.PersianCalendarHelper

@Composable
fun OverdueInstallmentsScreen(
    overdueItems: List<InstallmentItem>,
    onBackClick: () -> Unit,
    onItemClick: (InstallmentItem) -> Unit,
    onMarkAsPaid: (installmentId: String, paymentDate: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedItemForPayment by remember { mutableStateOf<InstallmentItem?>(null) }
    var currentOverdueList by remember(overdueItems) { mutableStateOf(overdueItems) }

    val categories = listOf(
        InstallmentCategory.BANK_LOANS,
        InstallmentCategory.HOME_LOANS,
        InstallmentCategory.CAR_INSURANCE,
        InstallmentCategory.MISC
    )

    val currentCategory = categories[selectedTabIndex]
    val filteredItems = remember(currentOverdueList, currentCategory) {
        currentOverdueList.filter { it.category == currentCategory }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("overdue_installments_screen"),
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 3.dp,
                        ambientColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFF0F172A).copy(alpha = 0.05f),
                        spotColor = if (isDark) Color.Black.copy(alpha = 0.6f) else Color(0xFF0F172A).copy(alpha = 0.08f)
                    ),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier.testTag("overdue_screen_back_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "بازگشت",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "اقساط سررسید گذشته (معوق)",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    color = if (isDark) Color(0xFFFECDD3) else Color(0xFF9F1239)
                                )
                                Text(
                                    text = "مجموعاً ${currentOverdueList.size} قسط سررسید گذشته نیاز به تسویه دارند",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = ExpenseRoseLight.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, ExpenseRoseLight.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Warning,
                                    contentDescription = null,
                                    tint = ExpenseRoseLight,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "${currentOverdueList.size} معوق",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = ExpenseRoseLight
                                )
                            }
                        }
                    }

                    // 4 Fixed Tabs for 4 Installment Categories (همه تب‌ها در یک صفحه و ثابت)
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {}
                    ) {
                        categories.forEachIndexed { index, cat ->
                            val catCount = currentOverdueList.count { it.category == cat }
                            val isSelected = selectedTabIndex == index
                            val tabLabel = when (cat) {
                                InstallmentCategory.BANK_LOANS -> "وام بانکی"
                                InstallmentCategory.HOME_LOANS -> "وام خانگی"
                                InstallmentCategory.CAR_INSURANCE -> "بیمه خودرو"
                                InstallmentCategory.MISC -> "متفرقه"
                            }
                            Tab(
                                selected = isSelected,
                                onClick = { selectedTabIndex = index },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = tabLabel,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 11.5.sp
                                            ),
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            maxLines = 1
                                        )
                                        if (catCount > 0) {
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Surface(
                                                shape = CircleShape,
                                                color = if (isSelected) ExpenseRoseLight else ExpenseRoseLight.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = catCount.toString(),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = if (isSelected) Color.White else ExpenseRoseLight,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filteredItems.isEmpty()) {
                item {
                    OverdueEmptyStateCard(categoryTitle = currentCategory.title)
                }
            } else {
                items(filteredItems, key = { it.id }) { item ->
                    OverdueInstallmentCard(
                        item = item,
                        onClick = { onItemClick(item) },
                        onPaidClick = { selectedItemForPayment = item }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }

    // Manual Payment Date Dialog
    selectedItemForPayment?.let { item ->
        ManualPaymentDateDialog(
            item = item,
            onDismiss = { selectedItemForPayment = null },
            onConfirmPayment = { paymentDate ->
                val idToPay = item.id
                currentOverdueList = currentOverdueList.filter { it.id != idToPay }
                onMarkAsPaid(idToPay, paymentDate)
                selectedItemForPayment = null
            }
        )
    }
}

private fun calculateAccurateOverdueText(nextPaymentDate: String): String {
    try {
        val cleanDate = IranianPhoneUtils.convertDigitsToEnglish(nextPaymentDate)
        val parts = cleanDate.split("/")
        if (parts.size == 3) {
            val y = parts[0].toIntOrNull() ?: 1404
            val m = parts[1].toIntOrNull() ?: 1
            val d = parts[2].toIntOrNull() ?: 1
            val dueMillis = PersianCalendarHelper.jalaliToEpochMillis(y, m, d, 0, 0)
            val nowMillis = System.currentTimeMillis()
            val diffDays = (nowMillis - dueMillis) / (1000L * 60 * 60 * 24)
            if (diffDays > 0) {
                return "${IranianPhoneUtils.convertDigitsToPersian(diffDays.toString())} روز گذشته"
            } else if (diffDays == 0L) {
                return "امروز سررسید شده"
            }
        }
    } catch (_: Exception) {}
    return "معوق"
}

@Composable
private fun OverdueInstallmentCard(
    item: InstallmentItem,
    onClick: () -> Unit,
    onPaidClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val accurateOverdueText = remember(item.nextPaymentDate) {
        calculateAccurateOverdueText(item.nextPaymentDate)
    }

    Layered3DCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusMD),
        backgroundColor = MaterialTheme.colorScheme.surface,
        elevation = 2.dp,
        contentPadding = PaddingValues(14.dp),
        testTag = "overdue_card_${item.id}",
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Icon + Title + Overdue Days Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Soft3DIcon(
                        imageRes = item.category.iconRes,
                        contentDescription = item.title,
                        size = 38.dp,
                        accentColor = ExpenseRoseLight,
                        containerShape = RoundedCornerShape(10.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = item.providerOrPerson,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Overdue status badge
                Surface(
                    shape = RoundedCornerShape(RadiusSM),
                    color = ExpenseRoseLight.copy(alpha = if (isDark) 0.25f else 0.12f),
                    border = BorderStroke(0.8.dp, ExpenseRoseLight.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = ExpenseRoseLight,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = accurateOverdueText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = ExpenseRoseLight
                        )
                    }
                }
            }

            // Divider / Details row: Due Date & Amount
            Surface(
                shape = RoundedCornerShape(RadiusSM),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "تاریخ سررسید: ${item.nextPaymentDate}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "${item.monthlyPaymentFormatted} تومان",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = ExpenseRoseLight
                        )
                    )
                }
            }

            // Bottom Action: «پرداخت شد» Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPaidClick,
                    shape = RoundedCornerShape(RadiusSM),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimaryLight,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("btn_paid_overdue_${item.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "پرداخت شد",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualPaymentDateDialog(
    item: InstallmentItem,
    onDismiss: () -> Unit,
    onConfirmPayment: (paymentDate: String) -> Unit
) {
    val todayPersian = remember {
        PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()
    }
    var paymentDate by remember { mutableStateOf(todayPersian) }
    var penaltyInput by remember { mutableStateOf("") }
    var showCalendarDialog by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val scrollState = rememberScrollState()

    val baseMonthlyAmount = remember(item) {
        item.totalAmount / item.totalInstallments.coerceAtLeast(1)
    }

    val parsedPenalty = remember(penaltyInput) {
        IranianAmountUtils.parseAmountToLong(penaltyInput)
    }

    val penaltyInToman = remember(parsedPenalty) {
        if (MoneyFormatter.activeCurrency == AppCurrency.RIAL) {
            parsedPenalty / 10L
        } else {
            parsedPenalty
        }
    }

    val totalPayableToman = baseMonthlyAmount + penaltyInToman
    val totalPayableFormatted = MoneyFormatter.formatToman(totalPayableToman)

    val parsedDateParts = remember(paymentDate) {
        try {
            val clean = IranianPhoneUtils.convertDigitsToEnglish(paymentDate)
            val parts = clean.split("/")
            if (parts.size == 3) {
                Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            } else {
                val now = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis())
                Triple(now.year, now.month, now.day)
            }
        } catch (_: Exception) {
            val now = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis())
            Triple(now.year, now.month, now.day)
        }
    }

    if (showCalendarDialog) {
        SharedPersianDatePickerDialog(
            initialYear = parsedDateParts.first,
            initialMonth = parsedDateParts.second,
            initialDay = parsedDateParts.third,
            title = "انتخاب تاریخ پرداخت",
            onDismiss = { showCalendarDialog = false },
            onConfirm = { y, m, d ->
                paymentDate = PersianCalendarHelper.PersianDateTime(y, m, d).toFormattedDate()
                showCalendarDialog = false
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .testTag("manual_payment_date_dialog"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimaryLight.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircleOutline,
                                contentDescription = null,
                                tint = EmeraldPrimaryLight,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = "ثبت پرداخت و تسویه قسط معوق",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Installment Summary Box
                    Surface(
                        shape = RoundedCornerShape(RadiusSM),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = MoneyFormatter.formatToman(baseMonthlyAmount),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = EmeraldPrimaryLight
                                    )
                                )
                            }
                            Text(
                                text = "سررسید اصلی: ${item.nextPaymentDate} (${calculateAccurateOverdueText(item.nextPaymentDate)})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = ExpenseRoseLight
                                )
                            )
                        }
                    }

                    // 1. Late Fee / Penalty Input (سود یا کارمزد جریمه دیرکرد)
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "سود / کارمزد جریمه دیرکرد (اختیاری):",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        OutlinedTextField(
                            value = penaltyInput,
                            onValueChange = { input ->
                                penaltyInput = IranianAmountUtils.formatWithCommas(input)
                            },
                            placeholder = {
                                Text(
                                    text = "مثلاً: ۵۰,۰۰۰",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                )
                            },
                            trailingIcon = {
                                Text(
                                    text = MoneyFormatter.getUnitLabel(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_penalty_amount"),
                            shape = RoundedCornerShape(RadiusSM),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp)
                        )
                    }

                    // 2. Total Payment Amount Box (مبلغ کل پرداختی با احتساب کارمزد)
                    Surface(
                        shape = RoundedCornerShape(RadiusSM),
                        color = EmeraldPrimaryLight.copy(alpha = if (isDark) 0.18f else 0.1f),
                        border = BorderStroke(1.dp, EmeraldPrimaryLight.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    text = "مبلغ کل پرداختی:",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "اصل قسط + جریمه معوقه",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = totalPayableFormatted,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = EmeraldPrimaryLight
                                )
                            )
                        }
                    }

                    // 3. Manual Payment Date Input
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "تاریخ پرداخت:",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        OutlinedTextField(
                            value = paymentDate,
                            onValueChange = { paymentDate = it },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_manual_payment_date"),
                            shape = RoundedCornerShape(RadiusSM),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                            trailingIcon = {
                                IconButton(
                                    onClick = { showCalendarDialog = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CalendarMonth,
                                        contentDescription = "انتخاب از تقویم",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        )

                        Text(
                            text = "این قسط با نشانگر قرمز «پرداخت با تاخیر» در سوابق ذخیره خواهد شد.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    // Dialog Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "انصراف",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                            )
                        }

                        Button(
                            onClick = {
                                val trimmed = paymentDate.trim().ifEmpty { todayPersian }
                                onConfirmPayment(trimmed)
                            },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(RadiusSM),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimaryLight,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "ثبت پرداخت",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverdueEmptyStateCard(categoryTitle: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusMD),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(EmeraldPrimaryLight.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldPrimaryLight,
                    modifier = Modifier.size(26.dp)
                )
            }

            Text(
                text = "هیچ قسط معوقی وجود ندارد",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "تمامی اقساط دسته «$categoryTitle» وضعیت مناسبی دارند یا به موقع پرداخت شده‌اند.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
