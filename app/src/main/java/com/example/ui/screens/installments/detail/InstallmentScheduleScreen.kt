package com.example.ui.screens.installments.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.Layered3DCard
import com.example.ui.components.PersianAmountInputField
import com.example.ui.components.PersianDateInputField
import com.example.ui.screens.installments.components.InstallmentStatusBadge
import com.example.ui.screens.installments.model.InstallmentItem
import com.example.ui.screens.installments.model.InstallmentMockDataSource
import com.example.ui.screens.installments.model.PaymentHistoryItem
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusMD
import com.example.util.IranianAmountUtils
import kotlinx.coroutines.launch
import com.example.util.MoneyFormatter

@Composable
fun InstallmentScheduleScreen(
    installment: InstallmentItem,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentItem by remember(installment) { mutableStateOf(installment) }
    var editingScheduleItem by remember { mutableStateOf<PaymentHistoryItem?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.testTag("schedule_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "بازگشت",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "برنامه پرداخت اقساط",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = currentItem.title,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "جدول اقساط و موعد سررسیدها (${currentItem.totalInstallments} قسط)",
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.5.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(currentItem.paymentHistory, key = { it.id }) { scheduleItem ->
                ScheduleTimelineItem(
                    item = scheduleItem,
                    onEditClick = { editingScheduleItem = scheduleItem }
                )
            }

            item {
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }

    // Edit Schedule Item Dialog
    if (editingScheduleItem != null) {
        val targetItem = editingScheduleItem!!
        var amountStr by remember { mutableStateOf(IranianAmountUtils.formatWithCommas(targetItem.amount.toString())) }
        var dueDateStr by remember { mutableStateOf(targetItem.dueDate) }
        var noteStr by remember { mutableStateOf(targetItem.note ?: "") }

        Dialog(
            onDismissRequest = { editingScheduleItem = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .widthIn(max = 350.dp)
                    .fillMaxWidth(0.92f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ویرایش قسط شماره ${targetItem.installmentNumber}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    PersianAmountInputField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = "مبلغ این قسط",
                        useOuterHeader = false
                    )

                    PersianDateInputField(
                        value = dueDateStr,
                        onValueChange = { dueDateStr = it },
                        label = "تاریخ سررسید قسط",
                        useOuterHeader = false,
                        showSubLabel = false
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { editingScheduleItem = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("انصراف")
                        }
                        Button(
                            onClick = {
                                val parsedLong = IranianAmountUtils.parseAmountToLong(amountStr)
                                val success = InstallmentMockDataSource.updateScheduleItem(
                                    installmentId = currentItem.id,
                                    scheduleItemId = targetItem.id,
                                    newAmountLong = parsedLong,
                                    newDueDate = dueDateStr,
                                    newNote = targetItem.note,
                                    context = context
                                )
                                if (success) {
                                    val updatedAll = InstallmentMockDataSource.allInstallments
                                    val found = updatedAll.find { it.id == currentItem.id }
                                    if (found != null) {
                                        currentItem = found
                                        scope.launch {
                                            try {
                                                com.example.reminder.domain.ReminderManager(context).syncInstallmentReminder(
                                                    installmentId = found.id,
                                                    title = found.title,
                                                    amount = found.totalAmount / found.totalInstallments.coerceAtLeast(1),
                                                    dueDatePersian = found.nextPaymentDate,
                                                    dueTimePersian = "۰۹:۰۰"
                                                )
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                }
                                editingScheduleItem = null
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("ذخیره تغییرات")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleTimelineItem(
    item: PaymentHistoryItem,
    onEditClick: () -> Unit
) {
    Layered3DCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusMD),
        backgroundColor = MaterialTheme.colorScheme.surface,
        elevation = 1.5.dp,
        contentPadding = PaddingValues(12.dp)
    ) {
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
                // Circle timeline indicator
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (item.isPaidLate) ExpenseRoseLight.copy(alpha = 0.2f) else item.status.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${item.installmentNumber}",
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp, fontWeight = if (item.isPaidLate) FontWeight.Bold else FontWeight.Normal),
                        color = if (item.isPaidLate) ExpenseRoseLight else item.status.color
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (item.isPaidLate) "قسط شماره ${item.installmentNumber} (با تاخیر)" else "قسط شماره ${item.installmentNumber}",
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp, fontWeight = if (item.isPaidLate) FontWeight.Bold else FontWeight.Medium),
                        color = if (item.isPaidLate) ExpenseRoseLight else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "سررسید: ${item.dueDate}" + (item.paidDate?.let { " • پرداخت: $it" } ?: ""),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = if (item.isPaidLate) ExpenseRoseLight.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.amountFormatted,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                        color = if (item.isPaidLate) ExpenseRoseLight else MaterialTheme.colorScheme.onSurface
                    )
                    if (item.isPaidLate) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ExpenseRoseLight.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "پرداخت بعد از موعد",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = ExpenseRoseLight,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        InstallmentStatusBadge(status = item.status, compact = true)
                    }
                }

                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "ویرایش قسط",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
