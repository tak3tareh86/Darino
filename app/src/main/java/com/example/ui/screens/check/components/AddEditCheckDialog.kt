package com.example.ui.screens.check.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.SharedPersianDatePickerDialog
import com.example.ui.screens.check.model.Check
import com.example.ui.screens.check.model.CheckStatus
import com.example.ui.screens.check.model.CheckType
import com.example.util.PersianCalendarHelper
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCheckDialog(
    check: Check? = null,
    onDismiss: () -> Unit,
    onConfirm: (Check) -> Unit
) {
    var type by remember { mutableStateOf(check?.type ?: CheckType.RECEIVED) }
    var amount by remember { mutableStateOf(check?.amount?.toString() ?: "") }
    var checkNumber by remember { mutableStateOf(check?.checkNumber ?: "") }
    var sayadId by remember { mutableStateOf(check?.sayadId ?: "") }
    var bankName by remember { mutableStateOf(check?.bankName ?: "") }
    var branch by remember { mutableStateOf(check?.branch ?: "") }
    var holderName by remember { mutableStateOf(check?.holderName ?: "") }
    var receiverName by remember { mutableStateOf(check?.receiverName ?: "") }
    var issueDate by remember { mutableStateOf(check?.issueDate ?: System.currentTimeMillis()) }
    var dueDate by remember { mutableStateOf(check?.dueDate ?: (System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000)) }
    var notes by remember { mutableStateOf(check?.notes ?: "") }
    var status by remember { mutableStateOf(check?.status ?: CheckStatus.REGISTERED) }

    var showIssueDatePicker by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = if (check == null) "ثبت چک جدید" else "ویرایش چک",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "بستن")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                val amountValue = amount.toLongOrNull() ?: 0L
                                if (amountValue > 0 && checkNumber.isNotBlank() && bankName.isNotBlank()) {
                                    onConfirm(
                                        Check(
                                            id = check?.id ?: 0,
                                            userId = check?.userId ?: "",
                                            type = type,
                                            amount = amountValue,
                                            checkNumber = checkNumber,
                                            sayadId = sayadId.ifBlank { null },
                                            bankName = bankName,
                                            branch = branch.ifBlank { null },
                                            holderName = holderName,
                                            receiverName = receiverName,
                                            issueDate = issueDate,
                                            dueDate = dueDate,
                                            status = status,
                                            notes = notes.ifBlank { null },
                                            accountId = check?.accountId,
                                            createdAt = check?.createdAt ?: System.currentTimeMillis(),
                                            updatedAt = System.currentTimeMillis()
                                        )
                                    )
                                }
                            }
                        ) {
                            Text("ذخیره", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Type Toggle
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = type == CheckType.RECEIVED,
                        onClick = { type = CheckType.RECEIVED },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = { Icon(Icons.Rounded.CallReceived, null) }
                    ) {
                        Text(CheckType.RECEIVED.title)
                    }
                    SegmentedButton(
                        selected = type == CheckType.PAID,
                        onClick = { type = CheckType.PAID },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = { Icon(Icons.Rounded.CallMade, null) }
                    ) {
                        Text(CheckType.PAID.title)
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { char -> char.isDigit() } },
                    label = { Text("مبلغ چک (ریال)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Rounded.Payments, null) },
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = checkNumber,
                        onValueChange = { checkNumber = it },
                        label = { Text("شماره چک") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = sayadId,
                        onValueChange = { sayadId = it },
                        label = { Text("شناسه صیادی") },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("نام بانک") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Rounded.AccountBalance, null) },
                    singleLine = true
                )

                OutlinedTextField(
                    value = branch,
                    onValueChange = { branch = it },
                    label = { Text("شعبه (اختیاری)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = holderName,
                    onValueChange = { holderName = it },
                    label = { Text(if (type == CheckType.RECEIVED) "صادرکننده / صاحب حساب" else "صاحب حساب ما") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Rounded.Person, null) },
                    singleLine = true
                )

                OutlinedTextField(
                    value = receiverName,
                    onValueChange = { receiverName = it },
                    label = { Text(if (type == CheckType.RECEIVED) "ذینفع (ما)" else "دریافت‌کننده / ذینفع") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Rounded.PersonOutline, null) },
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Issue Date
                    OutlinedCard(
                        onClick = { showIssueDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("تاریخ صدور", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = PersianCalendarHelper.fromEpochMillis(issueDate).toFormattedDate(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Due Date
                    OutlinedCard(
                        onClick = { showDueDatePicker = true },
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("تاریخ سررسید", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = PersianCalendarHelper.fromEpochMillis(dueDate).toFormattedDate(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (check != null) {
                    Text("وضعیت چک", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CheckStatus.entries.forEach { s ->
                            FilterChip(
                                selected = status == s,
                                onClick = { status = s },
                                label = { Text(s.title) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("توضیحات") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (showIssueDatePicker) {
            val dateParts = PersianCalendarHelper.fromEpochMillis(issueDate)
            SharedPersianDatePickerDialog(
                initialYear = dateParts.year,
                initialMonth = dateParts.month,
                initialDay = dateParts.day,
                onConfirm = { y, m, d ->
                    issueDate = PersianCalendarHelper.jalaliToEpochMillis(y, m, d, 9, 0)
                    showIssueDatePicker = false
                },
                onDismiss = { showIssueDatePicker = false }
            )
        }

        if (showDueDatePicker) {
            val dateParts = PersianCalendarHelper.fromEpochMillis(dueDate)
            SharedPersianDatePickerDialog(
                initialYear = dateParts.year,
                initialMonth = dateParts.month,
                initialDay = dateParts.day,
                onConfirm = { y, m, d ->
                    dueDate = PersianCalendarHelper.jalaliToEpochMillis(y, m, d, 9, 0)
                    showDueDatePicker = false
                },
                onDismiss = { showDueDatePicker = false }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}
