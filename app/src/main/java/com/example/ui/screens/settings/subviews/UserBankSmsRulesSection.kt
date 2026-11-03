package com.example.ui.screens.settings.subviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.sms.UserBankSmsRule
import com.example.data.sms.UserBankSmsRuleMatcher
import com.example.data.sms.UserBankSmsRuleRepository

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
            "برای هر بانک چند سرشماره و برای هر نوع تراکنش حداقل دو نشانه وارد کنید. تمام نشانه‌های یک دسته باید در متن پیامک وجود داشته باشند.",
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
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(rule.bankName, style = MaterialTheme.typography.titleSmall)
                    Text("سرشماره‌ها: " + rule.senderAddresses.joinToString("، "), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { editing = rule; showEditor = true }) { Text("ویرایش") }
                    TextButton(onClick = { repository.delete(rule.id); rules = repository.getAll() }) { Text("حذف") }
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
    val senderList = senders.split(Regex("[,،\\n]")).map(String::trim).filter(String::isNotEmpty).distinct()
    val markerFields = listOf(income, expense, transfer).filter(String::isNotBlank)
    val validMarkers = markerFields.all { UserBankSmsRuleMatcher.parseMarkers(it) != null }
    val hasAtLeastOneCategory = markerFields.isNotEmpty()
    val valid = bank.isNotBlank() && senderList.isNotEmpty() && hasAtLeastOneCategory && validMarkers

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "تعریف بانک" else "ویرایش بانک") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(bank, { bank = it }, modifier = Modifier.fillMaxWidth(), label = { Text("نام بانک") }, singleLine = true) }
                item { OutlinedTextField(senders, { senders = it }, modifier = Modifier.fillMaxWidth(), label = { Text("سرشماره‌ها (هر خط یک مورد)") }, minLines = 2) }
                item { OutlinedTextField(income, { income = it }, modifier = Modifier.fillMaxWidth(), label = { Text("نشانه‌های واریز") }, placeholder = { Text("واریز/نشست") }, singleLine = true) }
                item { OutlinedTextField(expense, { expense = it }, modifier = Modifier.fillMaxWidth(), label = { Text("نشانه‌های برداشت") }, placeholder = { Text("برداشت/پرید") }, singleLine = true) }
                item { OutlinedTextField(transfer, { transfer = it }, modifier = Modifier.fillMaxWidth(), label = { Text("نشانه‌های انتقال وجه") }, placeholder = { Text("انتقال/کارت_به_کارت") }, singleLine = true) }
                item {
                    Text(
                        "نشانه‌ها را فقط با / جدا کنید؛ حداقل دو نشانه لازم است. فاصله و علائم نگارشی دیگر مجاز نیستند. هر سه دسته اختیاری‌اند، اما هر دسته‌ای که تکمیل شود باید معتبر باشد.",
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
                            transferMarkers = transfer
                        )
                    )
                }
            ) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
