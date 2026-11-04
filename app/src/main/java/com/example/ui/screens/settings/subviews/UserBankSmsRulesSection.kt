package com.example.ui.screens.settings.subviews

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.sms.UserBankSmsRule
import com.example.data.sms.UserBankSmsRuleMatcher
import com.example.data.sms.UserBankSmsRuleRepository
import com.example.data.sms.SMS_PATTERN_LOOKBACK_OPTIONS

private data class BankBrand(val domain: String, val accent: Color)

private fun bankBrand(name: String): BankBrand? {
    val normalized = name.trim().replace("ي", "ی").replace("ك", "ک")
    val brands = listOf(
        listOf("ملی", "ملی ایران", "بانک ملی") to BankBrand("bmi.ir", Color(0xFF2563EB)),
        listOf("ملت", "بانک ملت") to BankBrand("bankmellat.ir", Color(0xFFEA4335)),
        listOf("سامان", "بانک سامان") to BankBrand("sb24.ir", Color(0xFF2563EB)),
        listOf("تجارت", "بانک تجارت") to BankBrand("tejaratbank.ir", Color(0xFF0F766E)),
        listOf("صادرات", "بانک صادرات") to BankBrand("bsi.ir", Color(0xFF0D9488)),
        listOf("سپاه", "بانک سپه") to BankBrand("banksepah.ir", Color(0xFF0F766E)),
        listOf("پاسارگاد", "بانک پاسارگاد") to BankBrand("bpi.ir", Color(0xFFDC2626)),
        listOf("اقتصاد نوین", "بانک اقتصاد نوین") to BankBrand("enbank.ir", Color(0xFFDC2626)),
        listOf("پارسیان", "بانک پارسیان") to BankBrand("parsian-bank.ir", Color(0xFF7C3AED)),
        listOf("رفاه", "بانک رفاه") to BankBrand("refah-bank.ir", Color(0xFF0D9488)),
        listOf("مسکن", "بانک مسکن") to BankBrand("bank-maskan.ir", Color(0xFF2563EB)),
        listOf("کشاورزی", "بانک کشاورزی") to BankBrand("bki.ir", Color(0xFF16A34A)),
        listOf("پست بانک", "پست‌بانک", "بانک پست بانک") to BankBrand("postbank.ir", Color(0xFF0D9488)),
        listOf("شهر", "بانک شهر") to BankBrand("shahr-bank.ir", Color(0xFF2563EB)),
        listOf("آینده", "بانک آینده") to BankBrand("ba24.ir", Color(0xFF7C3AED))
    )
    return brands.firstOrNull { (aliases, _) -> aliases.any { normalized.contains(it) } }?.second
}

@Composable
fun UserBankSmsRulesSection() {
    val context = LocalContext.current
    val repository = remember(context) { UserBankSmsRuleRepository(context) }
    var rules by remember { mutableStateOf(repository.getAll()) }
    var editing by remember { mutableStateOf<UserBankSmsRule?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("مدیریت بانک‌ها", style = MaterialTheme.typography.titleLarge)
            Text(
                "بانک‌ها و قواعد تشخیص پیامک‌هایشان را یک‌جا مدیریت کنید.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            onClick = { editing = null; showEditor = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .shadow(8.dp, RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier.background(
                    Brush.horizontalGradient(listOf(Color(0xFF2563EB), Color(0xFF6D4AFF), Color(0xFF8B5CF6)))
                ).padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Rounded.AccountBalance, null, tint = Color.White, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("افزودن بانک", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(18.dp))
                    Box(
                        modifier = Modifier.size(38.dp).background(Color.White.copy(alpha = 0.20f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                }
            }
        }

        if (rules.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.AccountBalance, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                    Text("هنوز بانکی اضافه نکرده‌اید", style = MaterialTheme.typography.titleSmall)
                    Text("برای شروع، روی دکمه افزودن بانک بزنید.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        rules.forEach { rule ->
            val brand = bankBrand(rule.bankName)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BankLogo(name = rule.bankName, brand = brand)
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(rule.bankName, style = MaterialTheme.typography.titleMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Phone, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    rule.senderAddresses.joinToString("، "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textDirection = TextDirection.Ltr
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            TextButton(
                                onClick = { editing = rule; showEditor = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("ویرایش", style = MaterialTheme.typography.labelMedium)
                            }
                            TextButton(
                                onClick = { repository.delete(rule.id); rules = repository.getAll() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(3.dp))
                                Text("حذف", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CalendarMonth, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(
                            "بازه بررسی: ${rule.lookbackMillis / (24L * 60L * 60L * 1000L)} روز",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.weight(1f))
                        Surface(shape = CircleShape, color = Color(0xFFDDF8E9)) {
                            Text("فعال", modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp), color = Color(0xFF07864B), style = MaterialTheme.typography.labelMedium)
                        }
                    }
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
private fun BankLogo(name: String, brand: BankBrand?) {
    val fallbackColor = brand?.accent ?: MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.size(58.dp),
        shape = RoundedCornerShape(17.dp),
        color = fallbackColor.copy(alpha = 0.09f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (brand != null) {
                AsyncImage(
                    model = "https://www.google.com/s2/favicons?domain=${brand.domain}&sz=128",
                    contentDescription = "نشان وب‌سایت بانک ${name}",
                    modifier = Modifier.size(38.dp)
                )
            } else {
                Icon(Icons.Rounded.AccountBalance, null, tint = fallbackColor, modifier = Modifier.size(30.dp))
            }
        }
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
