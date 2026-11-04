package com.example.ui.screens.settings.subviews

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.window.Dialog
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import android.graphics.Bitmap
import android.graphics.Canvas
import com.caverock.androidsvg.SVG
import org.json.JSONObject
import com.example.data.sms.UserBankSmsRule
import com.example.data.sms.UserBankSmsRuleMatcher
import com.example.data.sms.UserBankSmsRuleRepository
import com.example.data.sms.SMS_PATTERN_LOOKBACK_OPTIONS

private object BankLogoRepository {
    private val assetFiles = listOf("bank-logos-1.json", "bank-logos-2.json", "bank-logos-3.json", "bank-logos-4.json", "bank-logos-5.json", "bank-logos-6.json")
    @Volatile private var cached: Map<String, String>? = null
    fun get(context: android.content.Context): Map<String, String> {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: buildMap {
                assetFiles.forEach { file ->
                    runCatching {
                        val raw = context.assets.open(file).bufferedReader().use { it.readText() }
                        val json = JSONObject(raw)
                        val keys = json.keys()
                        while (keys.hasNext()) { val key = keys.next(); put(key, json.getString(key)) }
                    }
                }
            }.also { cached = it }
        }
    }
}
private fun sanitizeMarkerInput(value: String): String = value.filter { it.isLetterOrDigit() || it == '/' || it == '_' || it == '-' || it == '+' }

private fun renderBankLogo(source: String): Bitmap? = runCatching {
    val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
    val svg = SVG.getFromString(source)
    svg.setDocumentWidth("256px")
    svg.setDocumentHeight("256px")
    svg.renderToCanvas(Canvas(bitmap))
    bitmap
}.getOrNull()

@Composable
fun UserBankSmsRulesSection() {
    val context = LocalContext.current
    val repository = remember(context) { UserBankSmsRuleRepository(context) }
    var rules by remember { mutableStateOf(repository.getAll()) }
    var editing by remember { mutableStateOf<UserBankSmsRule?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(1) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("مدیریت الگوهای پیامکی", style = MaterialTheme.typography.titleLarge)
            Text(
                "بانک‌ها را مدیریت کنید و راهنمای قوانین ثبت الگو را ببینید.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GuideTab(title = "قوانین ثبت الگو", selected = selectedTab == 1, onClick = { selectedTab = 1 }, modifier = Modifier.weight(1f))
            GuideTab(title = "مدیریت بانک‌ها", selected = selectedTab == 0, onClick = { selectedTab = 0 }, modifier = Modifier.weight(1f))
        }

        if (selectedTab == 1) {
            SmsRulesGuide()
        } else {
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
                        BankLogo(name = rule.bankName)
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
    }

    if (showEditor && selectedTab == 0) {
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
private fun BankLogo(name: String) {
    val context = LocalContext.current
    val entry = remember(name) { BankCatalog.find(name) }
    val svg = remember(entry) { entry?.let { BankLogoRepository.get(context)[it.logoKey] } }
    val bitmap = remember(svg) { svg?.let { renderBankLogo(it) } }
    Surface(
        modifier = Modifier.size(58.dp),
        shape = RoundedCornerShape(17.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (bitmap != null) {
                Image(bitmap.asImageBitmap(), contentDescription = "لوگوی بانک", modifier = Modifier.size(46.dp), contentScale = ContentScale.Fit)
            } else {
                Icon(Icons.Rounded.AccountBalance, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
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
    var bankSearch by remember { mutableStateOf("") }
    var showBankPicker by remember { mutableStateOf(false) }
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
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(value = bank, onValueChange = { bank = it }, modifier = Modifier.fillMaxWidth(), label = { Text("نام بانک یا مؤسسه") }, singleLine = true)
                        TextButton(onClick = { bankSearch = ""; showBankPicker = true }) { Text("انتخاب از فهرست بانک‌ها و مؤسسات") }
                    }
                }
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
                item { OutlinedTextField(income, { income = sanitizeMarkerInput(it) }, modifier = Modifier.fillMaxWidth(), label = { Text("نشانه‌های واریز") }, placeholder = { Text("واریز/نشست") }, singleLine = true) }
                item { OutlinedTextField(expense, { expense = sanitizeMarkerInput(it) }, modifier = Modifier.fillMaxWidth(), label = { Text("نشانه‌های برداشت") }, placeholder = { Text("برداشت/پرید") }, singleLine = true) }
                item { OutlinedTextField(transfer, { transfer = sanitizeMarkerInput(it) }, modifier = Modifier.fillMaxWidth(), label = { Text("نشانه‌های انتقال وجه") }, placeholder = { Text("انتقال/کارت_به_کارت") }, singleLine = true) }
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
                        "در این سه کادر فقط حروف و اعداد و نشانه‌های +، _، - و جداکننده / پذیرفته می‌شوند؛ فاصله و سایر علائم نگارشی قابل ورود نیستند. برای ثبت چند نشانه فقط از / استفاده کنید. _ و - هنگام تطبیق معادل‌اند و همه نشانه‌های یک دسته باید در همان پیامک وجود داشته باشند.",
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
    if (showBankPicker) {
        Dialog(onDismissRequest = { showBankPicker = false }) {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("فهرست بانک‌ها و مؤسسات", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(value = bankSearch, onValueChange = { bankSearch = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("جست‌وجوی نام بانک") })
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(420.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(BankCatalog.search(bankSearch), key = { it.id }) { entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { bank = entry.title; showBankPicker = false }.padding(horizontal = 8.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                BankLogo(name = entry.title)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.title, style = MaterialTheme.typography.bodyMedium)
                                    Text(entry.group, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    TextButton(onClick = { showBankPicker = false }, modifier = Modifier.align(Alignment.End)) { Text("بستن") }
                }
            }
        }
    }
}


@Composable
private fun GuideTab(title: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(17.dp),
        color = if (selected) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shadowElevation = if (selected) 4.dp else 0.dp
    ) {
        Box(
            modifier = Modifier.background(
                if (selected) Brush.horizontalGradient(listOf(Color(0xFF1677F2), Color(0xFF7438E8)))
                else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
            ).padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(title, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun SmsRulesGuide() {
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFFE8F0FF),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("راهنمای قوانین و قواعد ورود اطلاعات", style = MaterialTheme.typography.titleMedium, color = Color(0xFF172B70))
                Text("برای شناسایی درست پیامک‌های بانکی، نکات زیر را هنگام تعریف الگو رعایت کنید.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF26345F))
            }
        }

        GuideRuleCard(
            icon = "↔",
            title = "قوانین نشانه‌های واریز، برداشت و انتقال وجه",
            bullets = listOf(
                "تنها جداکننده مجاز بین نشانه‌ها علامت / است.",
                "فاصله و علائم دیگر مانند ، . ؛ - یا , جداکننده مجاز نیستند.",
                "حداقل دو نشانه متفاوت برای هر دسته وارد کنید.",
                "تمام نشانه‌های یک دسته باید در همان پیامک وجود داشته باشند؛ ترتیبشان مهم نیست."
            ),
            examples = listOf(
                "مثال صحیح", "واریز/پرداخت/نشست",
                "مثال نادرست", "واریز، پرداخت، نشست"
            )
        )

        GuideRuleCard(
            icon = "👤",
            title = "قوانین سرشناسه",
            bullets = listOf(
                "سرشناسه عددی: ارقام فارسی، عربی و انگلیسی معادل‌اند؛ فاصله‌ها و علامت + ابتدایی نادیده گرفته می‌شوند.",
                "سرشناسه متنی: فاصله‌های ابتدا و انتها حذف می‌شوند؛ حروف «ي» و «ی» و همچنین «ك» و «ک» معادل‌اند.",
                "در سرشناسه متنی، بزرگی و کوچکی حروف انگلیسی اهمیت دارد."
            ),
            examples = listOf(
                "مثال عددی", "0912 345 6789  =  ۰۹۱۲۳۴۵۶۷۸۹",
                "مثال متنی", "Bank Mellat  ≠  bank mellat"
            )
        )

        GuideRuleCard(
            icon = "✎",
            title = "قوانین حروف و علائم در نشانه‌ها",
            bullets = listOf(
                "حروف فارسی و عربی «ی/ي» و «ک/ك» هنگام تطبیق معادل‌اند.",
                "ارقام فارسی، عربی و انگلیسی یکسان در نظر گرفته می‌شوند.",
                "فاصله‌ها هنگام تطبیق نشانه‌ها نادیده گرفته می‌شوند.",
                "علامت‌های _ و - هنگام تطبیق معادل یکدیگرند؛ اما برای جداکردن نشانه‌ها فقط / معتبر است."
            ),
            examples = listOf(
                "نمونه معادل", "کارت_به_کارت  =  کارت-به-کارت",
                "نمونه غیرمجاز", "واریز / برداشت (فاصله در کادر مجاز نیست)"
            )
        )

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFE8F0FF),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("💡", style = MaterialTheme.typography.headlineSmall)
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("نکته مهم", color = Color(0xFF1746B5), style = MaterialTheme.typography.titleSmall)
                    Text("اگر بیش از یک دسته نشانه در پیامک تطبیق پیدا کند، نوع تراکنش نیازمند تأیید کاربر خواهد بود.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF26345F))
                }
            }
        }
    }
}

@Composable
private fun GuideRuleCard(icon: String, title: String, bullets: List<String>, examples: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(21.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Surface(shape = RoundedCornerShape(13.dp), color = Color(0xFFE8EEFF)) {
                    Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                        Text(icon, color = Color(0xFF245CE8), style = MaterialTheme.typography.titleLarge)
                    }
                }
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            }
            bullets.forEach { line ->
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("✓", color = Color(0xFF2478E8), style = MaterialTheme.typography.titleSmall)
                    Text(line, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                examples.chunked(2).forEach { pair ->
                    val correct = pair.firstOrNull()?.contains("صحیح") == true || pair.firstOrNull()?.contains("معادل") == true || pair.firstOrNull()?.contains("عددی") == true || pair.firstOrNull()?.contains("متنی") == true
                    Surface(
                        shape = RoundedCornerShape(13.dp),
                        color = if (correct) Color(0xFFE4F7EC) else Color(0xFFFFE9E9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text((if (correct) "✓ " else "✕ ") + (pair.getOrNull(0) ?: ""), color = if (correct) Color(0xFF087C48) else Color(0xFFC43D3D), style = MaterialTheme.typography.labelLarge)
                            Text(pair.getOrNull(1).orEmpty(), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
