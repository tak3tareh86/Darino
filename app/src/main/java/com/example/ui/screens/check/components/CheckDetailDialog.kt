package com.example.ui.screens.check.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.screens.check.model.Check
import com.example.ui.screens.check.model.CheckStatus
import com.example.ui.screens.check.model.CheckType
import com.example.util.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckDetailDialog(
    check: Check,
    accounts: List<com.example.ui.screens.finance.model.Account>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSettle: (com.example.ui.screens.finance.model.Account) -> Unit,
    onStatusChange: (CheckStatus) -> Unit
) {
    var showSettleDialog by remember { mutableStateOf(false) }
    var selectedAccount by remember { mutableStateOf<com.example.ui.screens.finance.model.Account?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = MoneyFormatter.format(check.amount),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (check.type == CheckType.RECEIVED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "${check.type.title} - ${check.bankName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                StatusBadge(status = check.status)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

            // Info Grid
            InfoRow(label = "شماره چک", value = check.checkNumber, icon = Icons.Rounded.Tag)
            if (check.sayadId != null) {
                InfoRow(label = "شناسه صیادی", value = check.sayadId, icon = Icons.Rounded.QrCode)
            }
            InfoRow(
                label = if (check.type == CheckType.RECEIVED) "صادرکننده" else "صاحب حساب",
                value = check.holderName,
                icon = Icons.Rounded.Person
            )
            InfoRow(
                label = if (check.type == CheckType.RECEIVED) "ذینفع" else "دریافت‌کننده",
                value = check.receiverName,
                icon = Icons.Rounded.PersonOutline
            )
            InfoRow(label = "تاریخ صدور", value = check.formattedIssueDate, icon = Icons.Rounded.CalendarToday)
            InfoRow(
                label = "تاریخ سررسید", 
                value = check.formattedDueDate, 
                icon = Icons.Rounded.Event,
                valueColor = if (check.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            if (check.reminderDateTime != null) {
                val reminder = com.example.util.PersianCalendarHelper.fromEpochMillis(check.reminderDateTime)
                InfoRow(
                    label = "زمان یادآور",
                    value = "${reminder.toFormattedDate()} - ساعت ${reminder.toFormattedTime()}",
                    icon = Icons.Rounded.NotificationsActive,
                    valueColor = MaterialTheme.colorScheme.primary
                )
            }
            if (check.branch != null) {
                InfoRow(label = "شعبه", value = check.branch, icon = Icons.Rounded.AccountTree)
            }
            if (!check.notes.isNullOrBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("توضیحات", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(check.notes, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Actions
            if (check.status == CheckStatus.REGISTERED || check.status == CheckStatus.PENDING) {
                Button(
                    onClick = { showSettleDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.CheckCircle, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تسویه و ثبت تراکنش", fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ویرایش")
                }
                
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حذف")
                }
            }
        }
    }

    if (showSettleDialog) {
        AlertDialog(
            onDismissRequest = { showSettleDialog = false },
            title = { Text("تسویه چک") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("حساب مورد نظر برای ثبت تراکنش تسویه را انتخاب کنید:")
                    
                    accounts.forEach { account ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedAccount == account,
                                onClick = { selectedAccount = account }
                            )
                            Text(
                                text = account.name,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedAccount?.let { 
                            onSettle(it)
                            showSettleDialog = false
                        }
                    },
                    enabled = selectedAccount != null
                ) {
                    Text("تایید و تسویه")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettleDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
        }
    }
}
