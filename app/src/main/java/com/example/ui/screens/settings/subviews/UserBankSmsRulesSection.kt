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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.ui.text.style.TextOverflow
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
private fun normalizeSmsRuleSearch(value: String): String = value.trim().map { ch -> when (ch) { in '۰'..'۹' -> ('0'.code + (ch - '۰')).toChar(); in '٠'..'٩' -> ('0'.code + (ch - '٠')).toChar(); else -> ch } }.joinToString("").lowercase().replace("ي", "ی").replace("ك", "ک")

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
fun UserBankSmsRulesSection(selectedTab: Int, onSelectedTabChange: (Int) -> Unit, addBankRequest: Int) {
    val context = LocalContext.current
    val repository = remember(context) { UserBankSmsRuleRepository(context) }
    var rules by remember { mutableStateOf(repository.getAll()) }
    var editing by remember { mutableStateOf<UserBankSmsRule?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    androidx.compose.runtime.LaunchedEffect(addBankRequest) { if (addBankRequest > 0) { editing = null; showEditor = true } }
    val filteredRules = remember(rules, searchQuery) { val query = normalizeSmsRuleSearch(searchQuery); if (query.isBlank()) rules else rules.filter { rule -> (listOf(rule.bankName) + rule.textSenderAddresses + rule.numericSenderAddresses).any { normalizeSmsRuleSearch(it).contains(query) } } }

    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("مدیریت الگوهای پیامکی", style = MaterialTheme.typography.titleLarge)
            Text(
                "بانک‌ها را مدیریت کنید و راهنمای قوانین ثبت الگو را ببینید.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GuideTab(title = "مدیریت بانک‌ها", selected = selectedTab == 0, onClick = { onSelectedTabChange(0) }, modifier = Modifier.weight(1f))
            GuideTab(title = "قوانین ثبت الگو", selected = selectedTab == 1, onClick = { onSelectedTabChange(1) }, modifier = Modifier.weight(1f))
        }

        if (selectedTab == 1) {
            SmsRulesGuide()
        } else {
        OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("جست‌وجوی بانک یا سرشناسه") }, placeholder = { Text("نام بانک، سرشناسه متنی یا عددی") }, leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) }, trailingIcon = { if (searchQuery.isNotEmpty()) androidx.compose.material3.IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Rounded.Close, contentDescription = "پاک کردن جست‌وجو") } })

        if (filteredRules.isEmpty()) {
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
                    Text(if (rules.isEmpty()) "هنوز بانکی اضافه نکرده‌اید" else "موردی پیدا نشد", style = MaterialTheme.typography.titleSmall)
                    Text(if (rules.isEmpty()) "برای افزودن بانک، از دکمه + پایین صفحه استفاده کنید." else "نام بانک یا سرشناسه دیگری را جست‌وجو کنید.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        filteredRules.chunked(2).forEach { rowRules ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowRules.forEach { rule ->
                    Card(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        shape = RoundedCornerShape(19.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 9.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            // Actions are moved to the top corners; callbacks remain unchanged.
                            Box(modifier = Modifier.fillMaxWidth().height(30.dp)) {
                                androidx.compose.material3.IconButton(
                                    onClick = { repository.delete(rule.id); rules = repository.getAll() },
                                    modifier = Modifier.align(Alignment.TopEnd).size(30.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.DeleteOutline,
                                        contentDescription = "حذف",
                                        modifier = Modifier.size(17.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                                androidx.compose.material3.IconButton(
                                    onClick = { editing = rule; showEditor = true },
                                    modifier = Modifier.align(Alignment.TopStart).size(30.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Edit,
                                        contentDescription = "ویرایش",
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }

                            BankLogo(name = rule.bankName)
                            Text(
                                rule.bankName,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (rule.textSenderAddresses.isNotEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        "نوع سرشماره: متنی",
                                        modifier = Modifier.fillMaxWidth(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Start
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(
                                            "سرشماره:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            rule.textSenderAddresses.joinToString("، "),
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Content),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Start,
                                            softWrap = true
                                        )
                                    }
                                }
                            }

                            if (rule.numericSenderAddresses.isNotEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        "نوع سرشماره: عددی",
                                        modifier = Modifier.fillMaxWidth(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Start
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(
                                            "سرشماره:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            rule.numericSenderAddresses.joinToString("، "),
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Start,
                                            softWrap = true
                                        )
                                    }
                                }
                            }

                            // Preserve the separator and the review/active information directly below sender details.
                            Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Rounded.CalendarMonth, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "بررسی: ${rule.lookbackMillis / (24L * 60L * 60L * 1000L)} روز",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(Modifier.weight(1f))
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                                    Text(
                                        "فعال",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
                if (rowRules.size == 1) Spacer(Modifier.weight(1f))
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
        modifier = Modifier.size(46.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (bitmap != null) {
                Image(bitmap.asImageBitmap(), contentDescription = "لوگوی بانک", modifier = Modifier.size(36.dp), contentScale = ContentScale.Fit)
            } else {
                Icon(Icons.Rounded.AccountBalance, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
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
    var textSenders by remember(initial?.id) { mutableStateOf(initial?.textSenderAddresses?.joinToString("\n").orEmpty()) }
    var numericSenders by remember(initial?.id) { mutableStateOf(initial?.numericSenderAddresses?.joinToString("\n").orEmpty()) }
    var income by remember(initial?.id) { mutableStateOf(initial?.incomeMarkers.orEmpty()) }
    var expense by remember(initial?.id) { mutableStateOf(initial?.expenseMarkers.orEmpty()) }
    var transfer by remember(initial?.id) { mutableStateOf(initial?.transferMarkers.orEmpty()) }
    var lookbackDays by remember(initial?.id) { mutableStateOf((initial?.lookbackMillis ?: 24L * 60L * 60L * 1000L).let { (it / (24L * 60L * 60L * 1000L)).toInt().coerceIn(1, 90) }) }
    var lookbackMenuExpanded by remember { mutableStateOf(false) }
    val textSenderList = textSenders.split(Regex("[,،\\n]")).map(String::trim).filter(String::isNotEmpty).distinct()
    val numericSenderList = numericSenders.split(Regex("[,،\\n]")).map(String::trim).filter(String::isNotEmpty).distinct()
    val numericSendersValid = numericSenderList.all { UserBankSmsRuleMatcher.isNumericSender(it) }
    val textSendersValid = textSenderList.all { !UserBankSmsRuleMatcher.isNumericSender(it) }
    val markerInputs = listOf(income, expense, transfer)
    val markerInputsValid = markerInputs.all { marker ->
        marker.isBlank() || UserBankSmsRuleMatcher.parseMarkers(marker) != null
    }
    val hasAtLeastOneValidMarker = markerInputs.any { marker ->
        marker.isNotBlank() && UserBankSmsRuleMatcher.parseMarkers(marker) != null
    }
    val valid = bank.isNotBlank() && (textSenderList.isNotEmpty() || numericSenderList.isNotEmpty()) &&
        numericSendersValid && textSendersValid && markerInputsValid && hasAtLeastOneValidMarker

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "تعریف بانک" else "ویرایش بانک") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = bank,
                            onValueChange = { bank = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("نام بانک یا مؤسسه") },
                            singleLine = true,
                            trailingIcon = {
                                androidx.compose.material3.IconButton(onClick = {
                                    bankSearch = ""
                                    showBankPicker = !showBankPicker
                                }) {
                                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = "نمایش فهرست بانک‌ها")
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = showBankPicker,
                            onDismissRequest = { showBankPicker = false },
                            modifier = Modifier.width(320.dp).heightIn(max = 440.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = bankSearch,
                                    onValueChange = { bankSearch = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    label = { Text("جست‌وجوی نام بانک") },
                                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) }
                                )
                                val matchingBanks = BankCatalog.search(bankSearch)
                                if (matchingBanks.isEmpty()) {
                                    Text(
                                        "بانکی با این نام پیدا نشد.",
                                        modifier = Modifier.padding(8.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp).verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        matchingBanks.chunked(2).forEach { bankRow ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                bankRow.forEach { entry ->
                                                    Surface(
                                                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable {
                                                            bank = entry.title
                                                            showBankPicker = false
                                                        },
                                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                                        shape = RoundedCornerShape(10.dp)
                                                    ) {
                                                        Text(
                                                            entry.title,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                                            style = MaterialTheme.typography.labelMedium,
                                                            textAlign = TextAlign.Center,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                                if (bankRow.size == 1) Spacer(Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = textSenders,
                        onValueChange = { textSenders = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("سرشماره متنی (هر خط یک مورد)") },
                        placeholder = { Text("مثلاً Blu یا BankMellat") },
                        minLines = 2,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = TextDirection.Ltr,
                            textAlign = TextAlign.Start
                        )
                    )
                }
                item {
                    OutlinedTextField(
                        value = numericSenders,
                        onValueChange = { numericSenders = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("سرشماره عددی (هر خط یک مورد)") },
                        placeholder = { Text("مثلاً 0999 998 7641") },
                        minLines = 2,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = TextDirection.Ltr,
                            textAlign = TextAlign.Start
                        ),
                        isError = !numericSendersValid
                    )
                    if (!numericSendersValid) {
                        Text("در کادر عددی فقط ارقام، فاصله و + ابتدایی مجاز است.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    if (!textSendersValid) {
                        Text("مقدار عددی را در کادر سرشماره عددی وارد کنید.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("پرکردن یکی از دو کادر کافی است؛ اگر هر دو را وارد کنید، تطبیق با هرکدام پذیرفته می‌شود.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item { OutlinedTextField(income, { income = sanitizeMarkerInput(it) }, modifier = Modifier.fillMaxWidth(), label = { Text("نشانه‌های واریز") }, placeholder = { Text("واریز/نشست") }, singleLine = true) }
                item { OutlinedTextField(expense, { expense = sanitizeMarkerInput(it) }, modifier = Modifier.fillMaxWidth(), label = { Text("نشانه‌های برداشت") }, placeholder = { Text("برداشت/پرید") }, singleLine = true) }
                item { OutlinedTextField(transfer, { transfer = sanitizeMarkerInput(it) }, modifier = Modifier.fillMaxWidth(), label = { Text("نشانه‌های انتقال وجه") }, placeholder = { Text("انتقال/کارت_به_کارت") }, singleLine = true) }
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(11.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("بررسی پیامک‌های قدیمی", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "جست‌وجو در پیامک‌های $lookbackDays روز گذشته",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box {
                                TextButton(onClick = { lookbackMenuExpanded = true }) {
                                    Text("$lookbackDays روز")
                                    Icon(
                                        Icons.Rounded.CalendarMonth,
                                        contentDescription = "انتخاب بازه",
                                        modifier = Modifier.padding(start = 4.dp).size(16.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = lookbackMenuExpanded,
                                    onDismissRequest = { lookbackMenuExpanded = false }
                                ) {
                                    SMS_PATTERN_LOOKBACK_OPTIONS.forEach { millis ->
                                        val days = (millis / (24L * 60L * 60L * 1000L)).toInt()
                                        DropdownMenuItem(
                                            text = { Text("$days روز") },
                                            onClick = {
                                                lookbackDays = days
                                                lookbackMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Text(
                        "پرکردن حداقل یکی از سه کادر نشانه کافی است. کادرهای دیگر می‌توانند خالی بمانند؛ اما هر کادر تکمیل‌شده باید حداقل دو نشانه متفاوت با جداکننده / داشته باشد. در نشانه‌ها فقط حروف، اعداد و علائم +، _ و - مجازند.",
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
                            textSenderAddresses = textSenderList,
                            numericSenderAddresses = numericSenderList,
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


@Composable
private fun GuideTab(title: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shadowElevation = if (selected) 4.dp else 0.dp
    ) {
        Box(
            modifier = Modifier.background(
                if (selected) Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)))
                else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
            ).padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(title, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private data class SmsGuideGroup(
    val icon: String,
    val title: String,
    val shortTitle: String,
    val bullets: List<String>,
    val examples: List<String>
)

@Composable
private fun SmsRulesGuide() {
    val groups = listOf(
        SmsGuideGroup(
            icon = "↔",
            title = "قوانین نشانه‌های واریز، برداشت و انتقال وجه",
            shortTitle = "نشانه‌ها",
            bullets = listOf(
                "تنها جداکننده مجاز بین نشانه‌ها علامت / است.",
                "فاصله و علائم دیگر مانند ، . ؛ - یا , جداکننده مجاز نیستند.",
                "حداقل دو نشانه متفاوت برای هر دسته وارد کنید.",
                "تمام نشانه‌های یک دسته باید در همان پیامک وجود داشته باشند؛ ترتیبشان مهم نیست."
            ),
            examples = listOf("مثال صحیح", "واریز/پرداخت/نشست", "مثال نادرست", "واریز، پرداخت، نشست")
        ),
        SmsGuideGroup(
            icon = "👤",
            title = "قوانین سرشناسه",
            shortTitle = "سرشناسه",
            bullets = listOf(
                "سرشناسه عددی: ارقام فارسی، عربی و انگلیسی معادل‌اند؛ فاصله‌ها و علامت + ابتدایی نادیده گرفته می‌شوند.",
                "سرشناسه متنی: فاصله‌های ابتدا و انتها حذف می‌شوند؛ حروف «ي» و «ی» و همچنین «ك» و «ک» معادل‌اند.",
                "در سرشناسه متنی، بزرگی و کوچکی حروف انگلیسی اهمیت دارد."
            ),
            examples = listOf("مثال عددی", "0912 345 6789  =  ۰۹۱۲۳۴۵۶۷۸۹", "مثال متنی", "Bank Mellat  ≠  bank mellat")
        ),
        SmsGuideGroup(
            icon = "✎",
            title = "قوانین حروف و علائم در نشانه‌ها",
            shortTitle = "حروف و علائم",
            bullets = listOf(
                "حروف فارسی و عربی «ی/ي» و «ک/ك» هنگام تطبیق معادل‌اند.",
                "ارقام فارسی، عربی و انگلیسی یکسان در نظر گرفته می‌شوند.",
                "فاصله‌ها هنگام تطبیق نشانه‌ها نادیده گرفته می‌شوند.",
                "علامت‌های _ و - هنگام تطبیق معادل یکدیگرند؛ اما برای جداکردن نشانه‌ها فقط / معتبر است."
            ),
            examples = listOf("نمونه معادل", "کارت_به_کارت  =  کارت-به-کارت", "نمونه غیرمجاز", "واریز / برداشت (فاصله در کادر مجاز نیست)")
        )
    )
    var selectedGroup by remember { mutableStateOf<SmsGuideGroup?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("راهنمای قوانین و قواعد ورود اطلاعات", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("برای مشاهده جزئیات، یکی از مجموعه قوانین زیر را انتخاب کنید.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            groups.forEach { group ->
                Surface(
                    onClick = { selectedGroup = group },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(group.icon, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Text(
                            group.shortTitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("💡", style = MaterialTheme.typography.headlineSmall)
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("نکته مهم", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall)
                    Text("اگر بیش از یک دسته نشانه در پیامک تطبیق پیدا کند، نوع تراکنش نیازمند تأیید کاربر خواهد بود.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    selectedGroup?.let { group ->
        Dialog(onDismissRequest = { selectedGroup = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            group.title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = { selectedGroup = null }) {
                            Text("بستن")
                        }
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        GuideRuleCard(
                            icon = group.icon,
                            title = group.title,
                            bullets = group.bullets,
                            examples = group.examples
                        )
                    }
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
                Surface(shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                        Text(icon, color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.titleLarge)
                    }
                }
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            }
            bullets.forEach { line ->
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("✓", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall)
                    Text(line, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                examples.chunked(2).forEach { pair ->
                    val correct = pair.firstOrNull()?.contains("صحیح") == true || pair.firstOrNull()?.contains("معادل") == true || pair.firstOrNull()?.contains("عددی") == true || pair.firstOrNull()?.contains("متنی") == true
                    Surface(
                        shape = RoundedCornerShape(13.dp),
                        color = if (correct) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                (if (correct) "✓ " else "✕ ") + (pair.getOrNull(0) ?: ""),
                                color = if (correct) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                pair.getOrNull(1).orEmpty(),
                                color = if (correct) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
