package com.example.ui.screens.settings.subviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.example.data.sms.UserBankSmsRule
import com.example.data.sms.UserBankSmsRuleMatcher
import com.example.data.sms.UserBankSmsRuleRepository
import com.example.data.sms.SMS_PATTERN_LOOKBACK_OPTIONS

@Composable
fun UserBankSmsRulesSection() {
    val context = LocalContext.current
    val repository = remember(context) { UserBankSmsRuleRepository(context) }
    var rules by remember { mutableStateOf(repository.getAll()) }
    var editing by remember { mutableStateOf<UserBankSmsRule?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("تشخیص پیامک با سرشماره و نشانه‌های دلخواه", style = MaterialTheme.typography.titleMedium)
        Text(
            "برای هر بانک چند سرشماره ثبت کنید. هر سه کادر واریز، برداشت و انتقال باید حداقل دو نشانه داشته باشند و تمام نشانه‌های همان کادر باید در یک پیامک پیدا شوند.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = { editing = null; showEditor = true }) { Text("افزودن بانک") }
        rules.forEach { rule ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Column(
                    Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(rule.bankName, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.weight(1f))
                        TextButton(
                            onClick = { editing = rule; showEditor = true },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 7.dp, vertical = 0.dp)
                        ) { Text("ویرایش", style = MaterialTheme.typography.labelMedium) }
                        TextButton(
                            onClick = { repository.delete(rule.id); rules = repository.getAll() },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 7.dp, vertical = 0.dp)
                        ) { Text("حذف", style = MaterialTheme.typography.labelMedium) }
                    }
                    Text(
                        "سرشماره‌ها: " + rule.senderAddresses.joinToString("، "),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2
                    )
                    Text(
                        "بازه بررسی: ${rule.lookbackMillis / (24L * 60L * 60L * 1000L)} روز",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showEditor) {
        RuleEditorDialog(
            initial = editing,
            onDismiss = { showEditor = false },
            onSave = { rule ->
                repository.save(rule)
                rules = repository.getAll()
                showEditor = false
            }
        )
    }
}

@Composable
private fun RuleEditorDialog(
    initial: UserBankSmsRule?,
    onDismiss: () -> Unit,
    onSave: (UserBankSmsRule) -> Unit
) {
    var bank by remember(initial?.id) { mutableStateOf(initial?.bankName.orEmpty()) }
    var senders by remember(initial?.id) { mutableStateOf(initial?.senderAddresses?.joinToString("\n").orEmpty()) }
    var income by remember(initial?.id) { mutableStateOf(initial?.incomeMarkers.orEmpty()) }
    var expense by remember(initial?.id) { mutableStateOf(initial?.expenseMarkers.orEmpty()) }
    var transfer by remember(initial?.id) { mutableStateOf(initial?.transferMarkers.orEmpty()) }
    var lookbackDays by remember(initial?.id) { mutableStateOf((initial?.lookbackMillis ?: 24L * 60L * 60L * 1000L).let { (it / (24L * 60L * 60L * 1000L)).toInt().coerceIn(1, 90) }) }
    var lookbackMenuExpanded by remember { mutableStateOf(false) }
    val senderList = senders.split(Regex("[,،\\n]")).map(String::trim).filter(String::isNotEmpty).distinct()
    val validMarkers = listOf(income, expense, transfer).all { UserBankSmsRuleMatcher.parseMarkers(it) != null }
    val valid = bank.isNotBlank() && senderList.isNotEmpty() && validMarkers

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "تعریف بانک" else "ویرایش بانک") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(bank, { bank = it }, modifier = Modifier.fillMaxWidth(), label = { Text("نام بانک") }, singleLine = true) }
                item {
                    OutlinedTextField(
                        value = senders,
                        onValueChange = { senders = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("سرشماره‌ها (هر خط یک مورد)") },
                        minLines = 2,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = TextDirection.Ltr,
                            textAlign = TextAlign.Start
                        )
                    )
                }
                item { OutlinedTextField(income, { income = it }, modifier = Modifier.fillMaxWidth(), label = { Text("نشانه‌های واریز") }, placeholder = { Text("واریز/نشست") }, singleLine = true) }
                item { OutlinedTextField(expense, { expense = it }, modifier = Modifier.fillMaxWidth(), label = { Text("نشانه‌های برداشت") }, placeholder = { Text("برداشت/پرید") }, singleLine = true) }
                item { OutlinedTextField(transfer, { transfer = it }, modifier = Modifier.fillMaxWidth(), label = { Text("نشانه‌های انتقال وجه") }, placeholder = { Text("انتقال/کارت_به_کارت") }, singleLine = true) }
                item {
                    Column {
                        Text("بازه بررسی پیامک‌های قدیمی")
                        TextButton(onClick = { lookbackMenuExpanded = true }) { Text("$lookbackDays روز") }
                        DropdownMenu(expanded = lookbackMenuExpanded, onDismissRequest = { lookbackMenuExpanded = false }) {
                            SMS_PATTERN_LOOKBACK_OPTIONS.forEach { millis ->
                                val days = (millis / (24L * 60L * 60L * 1000L)).toInt()
                                DropdownMenuItem(text = { Text("$days روز") }, onClick = { lookbackDays = days; lookbackMenuExpanded = false })
                            }
                        }
                    }
                }
                item {
                    Text(
                        "در هر سه کادر حداقل دو نشانه وارد کنید و فقط از / برای جداکردن آن‌ها استفاده کنید. فاصله و علائم نگارشی دیگر جداکننده نیستند؛ فاصله‌ها هنگام تطبیق نادیده گرفته می‌شوند و _ و - معادل‌اند. هر دو نشانه باید در همان پیامک وجود داشته باشند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = {
                    onSave(
                        UserBankSmsRule(
                            id = initial?.id ?: java.util.UUID.randomUUID().toString(),
                            bankName = bank.trim(),
                            senderAddresses = senderList,
                            incomeMarkers = income,
                            expenseMarkers = expense,
                            transferMarkers = transfer,
                            lookbackMillis = lookbackDays.toLong() * 24L * 60L * 60L * 1000L
                        )
                    )
                }
            ) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
