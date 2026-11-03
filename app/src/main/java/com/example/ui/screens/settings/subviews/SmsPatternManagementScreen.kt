package com.example.ui.screens.settings.subviews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.sms.BankSmsParser
import com.example.data.sms.SmsPattern
import com.example.data.sms.SmsPatternMatcher
import com.example.data.sms.SmsPatternRepository
import com.example.data.sms.SmsPatternSlot
import com.example.data.sms.SmsPatternSlotKind
import com.example.data.sms.SmsPatternType
import com.example.ui.screens.settings.components.SettingsHeader

private enum class PatternFilter { ALL, INCOME, EXPENSE, TRANSFER }

@Composable
fun SmsPatternManagementScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { SmsPatternRepository(context) }
    var patterns by remember { mutableStateOf(repository.getPatterns()) }
    var filter by remember { mutableStateOf(PatternFilter.ALL) }
    var showEditor by remember { mutableStateOf(false) }
    var editingPattern by remember { mutableStateOf<SmsPattern?>(null) }
    var patternToDelete by remember { mutableStateOf<SmsPattern?>(null) }

    val visiblePatterns = patterns.filter { pattern ->
        when (filter) {
            PatternFilter.ALL -> true
            PatternFilter.INCOME -> pattern.type == SmsPatternType.INCOME
            PatternFilter.EXPENSE -> pattern.type == SmsPatternType.EXPENSE
            PatternFilter.TRANSFER -> pattern.type == SmsPatternType.TRANSFER
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            SettingsHeader(
                title = "مدیریت الگوهای پیامکی",
                subtitle = "نمونه پیامک‌های بانکی خود را مدیریت کنید",
                showBack = true,
                showSearch = false,
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Sms, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                "الگوهای پیامک بانکی خودتان را بسازید",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "یک پیامک واقعی واریز، برداشت یا انتقال وجه را از صندوق پیامک کپی کنید و متن کامل آن را اینجا قرار دهید. برای هر قالب بانکی می‌توانید چند نمونه جداگانه ثبت کنید.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("الگوهای ذخیره‌شده", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${patterns.size} الگو", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { editingPattern = null; showEditor = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("افزودن الگو", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = filter == PatternFilter.ALL,
                        onClick = { filter = PatternFilter.ALL },
                        label = { Text("همه") }
                    )
                    FilterChip(
                        selected = filter == PatternFilter.INCOME,
                        onClick = { filter = PatternFilter.INCOME },
                        label = { Text("واریز") },
                        leadingIcon = { Icon(Icons.Rounded.TrendingUp, null, modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = filter == PatternFilter.EXPENSE,
                        onClick = { filter = PatternFilter.EXPENSE },
                        label = { Text("برداشت") },
                        leadingIcon = { Icon(Icons.Rounded.TrendingDown, null, modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = filter == PatternFilter.TRANSFER,
                        onClick = { filter = PatternFilter.TRANSFER },
                        label = { Text("انتقال وجه") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.CompareArrows, null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            if (visiblePatterns.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(
                                if (patterns.isEmpty()) "هنوز الگویی ثبت نکرده‌اید" else "الگویی در این دسته وجود ندارد",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (patterns.isEmpty()) "برای شروع، یک نمونه پیامک واقعی از بانک خود اضافه کنید." else "دسته دیگری را انتخاب کنید یا یک الگوی جدید بسازید.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (patterns.isEmpty()) {
                                TextButton(onClick = { editingPattern = null; showEditor = true }) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.size(5.dp))
                                    Text("ثبت اولین الگو")
                                }
                            }
                        }
                    }
                }
            } else {
                items(visiblePatterns, key = { it.id }) { pattern ->
                    PatternCard(
                        pattern = pattern,
                        onEdit = { editingPattern = pattern; showEditor = true },
                        onDelete = { patternToDelete = pattern }
                    )
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text(
                            "الگوها به‌صورت محلی و رمزگذاری‌شده ذخیره می‌شوند. فقط پیامکی پذیرفته می‌شود که متن ثابت آن دقیقاً با الگو منطبق باشد و جایگاه‌های متغیرِ تشخیص‌داده‌شده و تأییدشده توسط شما در همان ترتیب قرار داشته باشند. نوع تراکنش نیز فقط از انتخاب شما در الگو گرفته می‌شود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        SmsPatternEditorDialog(
            pattern = editingPattern,
            onDismiss = { showEditor = false; editingPattern = null },
            onSave = { name, bankName, senderAddress, accountNumber, dateFormat, timeFormat, type, message, slots ->
                val existing = editingPattern
                if (existing == null) {
                    repository.create(name, type, message, slots, bankName = bankName, senderAddress = senderAddress, accountNumber = accountNumber, dateFormat = dateFormat, timeFormat = timeFormat)
                } else {
                    repository.save(existing.copy(name = name.trim(), bankName = bankName.trim(), senderAddress = senderAddress, accountNumber = accountNumber?.trim()?.takeIf { it.isNotEmpty() }, dateFormat = dateFormat, timeFormat = timeFormat, type = type, sampleMessage = message, slots = slots))
                }
                patterns = repository.getPatterns()
                showEditor = false
                editingPattern = null
            }
        )
    }

    patternToDelete?.let { pattern ->
        AlertDialog(
            onDismissRequest = { patternToDelete = null },
            title = { Text("حذف الگو؟", fontWeight = FontWeight.Bold) },
            text = { Text("الگوی «${pattern.name}» حذف شود؟ این کار قابل بازگشت نیست.") },
            confirmButton = {
                TextButton(onClick = {
                    repository.delete(pattern.id)
                    patterns = repository.getPatterns()
                    patternToDelete = null
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { patternToDelete = null }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun PatternCard(
    pattern: SmsPattern,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = when (pattern.type) {
        SmsPatternType.INCOME -> Color(0xFF059669)
        SmsPatternType.EXPENSE -> Color(0xFFE11D48)
        SmsPatternType.TRANSFER -> Color(0xFF2563EB)
    }
    val typeIcon = when (pattern.type) {
        SmsPatternType.INCOME -> Icons.Rounded.TrendingUp
        SmsPatternType.EXPENSE -> Icons.Rounded.TrendingDown
        SmsPatternType.TRANSFER -> Icons.AutoMirrored.Rounded.CompareArrows
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        typeIcon,
                        contentDescription = null,
                        tint = accent
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(pattern.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(pattern.type.title, color = accent, style = MaterialTheme.typography.labelSmall)
                    Text(
                        pattern.bankName + if (pattern.senderAddress.isNotEmpty()) {
                            " • " + pattern.senderAddress
                        } else {
                            " • شناسه فرستنده ثبت نشده؛ تشخیص خودکار غیرفعال"
                        },
                        color = if (pattern.senderAddress.isBlank()) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = "ویرایش الگو") }
                IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = "حذف الگو", tint = MaterialTheme.colorScheme.error) }
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Text(
                    pattern.sampleMessage,
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SmsPatternEditorDialog(
    pattern: SmsPattern?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String?, String, String, SmsPatternType, String, List<SmsPatternSlot>) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var name by remember(pattern?.id) { mutableStateOf(pattern?.name.orEmpty()) }
    var bankName by remember(pattern?.id) { mutableStateOf(pattern?.bankName.orEmpty()) }
    var senderAddress by remember(pattern?.id) { mutableStateOf(pattern?.senderAddress.orEmpty()) }
    var accountNumber by remember(pattern?.id) { mutableStateOf(pattern?.accountNumber.orEmpty()) }
    var dateFormat by remember(pattern?.id) { mutableStateOf(pattern?.dateFormat.orEmpty()) }
    var timeFormat by remember(pattern?.id) { mutableStateOf(pattern?.timeFormat.orEmpty()) }
    var type by remember(pattern?.id) { mutableStateOf(pattern?.type ?: SmsPatternType.INCOME) }
    var message by remember(pattern?.id) { mutableStateOf(pattern?.sampleMessage.orEmpty()) }
    val inferredSlots = remember(message) { SmsPatternMatcher.inferVariableSlots(message) }
    var compactDateOrder by remember(pattern?.id) {
        mutableStateOf(pattern?.slots?.firstOrNull {
            it.kind == SmsPatternSlotKind.DATE_YEAR_MONTH_DAY ||
                it.kind == SmsPatternSlotKind.DATE_DAY_MONTH_YEAR ||
                it.kind == SmsPatternSlotKind.DATE_MONTH_DAY_YEAR
        }?.kind ?: inferredSlots.firstOrNull {
            it.kind == SmsPatternSlotKind.DATE_YEAR_MONTH_DAY || it.kind == SmsPatternSlotKind.DATE_DAY_MONTH_YEAR
        }?.kind ?: SmsPatternSlotKind.DATE_YEAR_MONTH_DAY)
    }
    val dateSlot = inferredSlots.firstOrNull { it.kind in setOf(
        SmsPatternSlotKind.DATE, SmsPatternSlotKind.DATE_DAY_MONTH, SmsPatternSlotKind.DATE_MONTH_DAY,
        SmsPatternSlotKind.DATE_DAY_MONTH_YEAR, SmsPatternSlotKind.DATE_MONTH_DAY_YEAR, SmsPatternSlotKind.DATE_YEAR_MONTH_DAY
    ) }
    val timeSlot = inferredSlots.firstOrNull { it.kind == SmsPatternSlotKind.TIME }
    val datePartsForMask = dateSlot?.let { message.substring(it.start, it.endExclusive).split(Regex("[/.-]")) }.orEmpty()
    val dateSeparator = dateSlot?.let { Regex("[/.-]").find(message.substring(it.start, it.endExclusive))?.value } ?: "/"
    val dateYearToken = if (datePartsForMask.any { it.length == 4 }) "YYYY" else "YY"
    val normalizedDateFormat = dateFormat.uppercase().replace(" ", "")
    val supportedDateFormats = setOf(
        "YYYY/MM/DD", "YYYY-MM-DD", "YYYY.MM.DD", "YY/MM/DD", "YY-MM-DD", "YY.MM.DD",
        "DD/MM/YYYY", "DD-MM-YYYY", "DD.MM.YYYY", "DD/MM/YY", "DD-MM-YY", "DD.MM.YY",
        "MM/DD/YYYY", "MM-DD-YYYY", "MM.DD.YYYY", "MM/DD/YY", "MM-DD-YY", "MM.DD.YY",
        "DD/MM", "DD-MM", "DD.MM", "MM/DD", "MM-DD", "MM.DD"
    )
    val validDateFormat = dateSlot == null || (
        normalizedDateFormat in supportedDateFormats && when (dateSlot.kind) {
            SmsPatternSlotKind.DATE -> normalizedDateFormat.startsWith("YYYY")
            SmsPatternSlotKind.DATE_DAY_MONTH_YEAR, SmsPatternSlotKind.DATE_MONTH_DAY_YEAR ->
                normalizedDateFormat.endsWith("YYYY") || normalizedDateFormat.endsWith("YY")
            SmsPatternSlotKind.DATE_DAY_MONTH -> normalizedDateFormat.startsWith("DD")
            SmsPatternSlotKind.DATE_MONTH_DAY -> normalizedDateFormat.startsWith("MM")
            else -> true
        }
    )
    val validTimeFormat = timeSlot == null || timeFormat.uppercase().replace(" ", "") in setOf("HH:MM", "H:MM", "HH:MM:SS", "H:MM:SS")
    val dateKindFromFormat = when (dateFormat.uppercase().replace(" ", "")) {
        "YYYY/MM/DD", "YYYY-MM-DD", "YYYY.MM.DD" -> SmsPatternSlotKind.DATE
        "YY/MM/DD", "YY-MM-DD", "YY.MM.DD" -> SmsPatternSlotKind.DATE_YEAR_MONTH_DAY
        "DD/MM/YYYY", "DD-MM-YYYY", "DD.MM.YYYY", "DD/MM/YY", "DD-MM-YY", "DD.MM.YY" -> SmsPatternSlotKind.DATE_DAY_MONTH_YEAR
        "MM/DD/YYYY", "MM-DD-YYYY", "MM.DD.YYYY", "MM/DD/YY", "MM-DD-YY", "MM.DD.YY" -> SmsPatternSlotKind.DATE_MONTH_DAY_YEAR
        "DD/MM", "DD-MM", "DD.MM" -> SmsPatternSlotKind.DATE_DAY_MONTH
        "MM/DD", "MM-DD", "MM.DD" -> SmsPatternSlotKind.DATE_MONTH_DAY
        else -> compactDateOrder
    }
    LaunchedEffect(message, dateSlot, timeSlot) {
        if (dateSlot != null && dateFormat.isBlank()) {
            val rawDate = message.substring(dateSlot.start, dateSlot.endExclusive)
            val parts = rawDate.split(Regex("[/.-]"))
            val separator = Regex("[/.-]").find(rawDate)?.value ?: "/"
            dateFormat = when (dateSlot.kind) {
                SmsPatternSlotKind.DATE_YEAR_MONTH_DAY -> "YY" + separator + "MM" + separator + "DD"
                SmsPatternSlotKind.DATE_DAY_MONTH_YEAR -> "DD" + separator + "MM" + separator + if (parts.lastOrNull()?.length == 4) "YYYY" else "YY"
                SmsPatternSlotKind.DATE_MONTH_DAY_YEAR -> "MM" + separator + "DD" + separator + if (parts.lastOrNull()?.length == 4) "YYYY" else "YY"
                SmsPatternSlotKind.DATE_DAY_MONTH -> "DD" + separator + "MM"
                SmsPatternSlotKind.DATE_MONTH_DAY -> "MM" + separator + "DD"
                else -> (if (parts.firstOrNull()?.length == 4) "YYYY" else "YY") + separator + "MM" + separator + "DD"
            }
        }
        if (timeSlot != null && timeFormat.isBlank()) {
            val rawTime = message.substring(timeSlot.start, timeSlot.endExclusive)
            val hourToken = rawTime.substringBefore(':')
            val hourMask = if (hourToken.length == 1) "H" else "HH"
            timeFormat = hourMask + if (rawTime.count { it == ':' } == 2) ":mm:ss" else ":mm"
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (pattern == null) "افزودن الگوی پیامکی" else "ویرایش الگوی پیامکی", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("نام الگو") }, placeholder = { Text("مثلاً رسالت - واریز") })
                OutlinedTextField(value = bankName, onValueChange = { bankName = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("نام بانک یا مؤسسه") }, placeholder = { Text("مثلاً بانک قرض‌الحسنه رسالت") })
                OutlinedTextField(value = senderAddress, onValueChange = { senderAddress = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("سرشماره / شناسه فرستنده") }, placeholder = { Text("مثلاً ResalatBank") }, supportingText = { Text("اجباری؛ دقیقاً از قسمت فرستنده پیامک کپی کنید. حروف بزرگ و کوچک، فاصله‌ها، نیم‌فاصله و حروف فارسی را عیناً حفظ کنید.") })
                OutlinedTextField(value = accountNumber, onValueChange = { accountNumber = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("شماره حساب (اختیاری)") }, placeholder = { Text("در صورت نیاز وارد کنید") }, supportingText = { Text("اگر بانک شماره حساب را در پیامک درج نمی‌کند، این کادر را خالی بگذارید.") })
                Text("نوع تراکنش", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    FilterChip(selected = type == SmsPatternType.INCOME, onClick = { type = SmsPatternType.INCOME }, label = { Text("واریز") }, leadingIcon = { Icon(Icons.Rounded.TrendingUp, null, modifier = Modifier.size(16.dp)) })
                    FilterChip(selected = type == SmsPatternType.EXPENSE, onClick = { type = SmsPatternType.EXPENSE }, label = { Text("برداشت") }, leadingIcon = { Icon(Icons.Rounded.TrendingDown, null, modifier = Modifier.size(16.dp)) })
                    FilterChip(selected = type == SmsPatternType.TRANSFER, onClick = { type = SmsPatternType.TRANSFER }, label = { Text("انتقال") }, leadingIcon = { Icon(Icons.AutoMirrored.Rounded.CompareArrows, null, modifier = Modifier.size(16.dp)) })
                }
                Text("قالب تاریخ شمسی و ساعت", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value = dateFormat, onValueChange = { dateFormat = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("فرمت تاریخ شمسی") }, placeholder = { Text("مثلاً YYYY/MM/DD یا YY/MM/DD") }, supportingText = { Text("نمونه‌ها: 1405/08/05 و 05/08/05؛ در نمونه دوم ترتیب سال/ماه/روز را مشخص کنید.") })
                OutlinedTextField(value = timeFormat, onValueChange = { timeFormat = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("فرمت ساعت") }, placeholder = { Text("مثلاً HH:mm") })
                if (dateSlot != null && dateSlot.kind in setOf(SmsPatternSlotKind.DATE_YEAR_MONTH_DAY, SmsPatternSlotKind.DATE_DAY_MONTH_YEAR, SmsPatternSlotKind.DATE_MONTH_DAY_YEAR)) {
                    Text("ترتیب اجزای تاریخ نمونه را مشخص کنید:", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (dateSlot.kind == SmsPatternSlotKind.DATE_YEAR_MONTH_DAY) FilterChip(selected = dateKindFromFormat == SmsPatternSlotKind.DATE_YEAR_MONTH_DAY, onClick = {
                                compactDateOrder = SmsPatternSlotKind.DATE_YEAR_MONTH_DAY
                                dateFormat = dateYearToken + dateSeparator + "MM" + dateSeparator + "DD"
                            }, label = { Text("سال/ماه/روز") })
                        FilterChip(selected = dateKindFromFormat == SmsPatternSlotKind.DATE_DAY_MONTH_YEAR, onClick = {
                            compactDateOrder = SmsPatternSlotKind.DATE_DAY_MONTH_YEAR
                            dateFormat = "DD" + dateSeparator + "MM" + dateSeparator + dateYearToken
                        }, label = { Text("روز/ماه/سال") })
                        FilterChip(selected = dateKindFromFormat == SmsPatternSlotKind.DATE_MONTH_DAY_YEAR, onClick = {
                            compactDateOrder = SmsPatternSlotKind.DATE_MONTH_DAY_YEAR
                            dateFormat = "MM" + dateSeparator + "DD" + dateSeparator + dateYearToken
                        }, label = { Text("ماه/روز/سال") })
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("متن کامل پیامک نمونه", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { clipboardManager.getText()?.text?.takeIf { it.isNotBlank() }?.let { message = it; dateFormat = ""; timeFormat = "" } }, enabled = !clipboardManager.getText()?.text.isNullOrBlank()) {
                        Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.size(4.dp)); Text("جای‌گذاری")
                    }
                }
                OutlinedTextField(value = message, onValueChange = { message = it; dateFormat = ""; timeFormat = "" }, modifier = Modifier.fillMaxWidth(), minLines = 4, maxLines = 8, placeholder = { Text("کل پیامک بانکی را عیناً جای‌گذاری کنید؛ خطوط و علائم را تغییر ندهید.") }, supportingText = { Text(message.length.toString() + " نویسه") })
                if (inferredSlots.isNotEmpty()) {
                    Text("تحلیل اولیه دارینو", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                    inferredSlots.forEach { slot ->
                        val kind = if (slot.kind in setOf(SmsPatternSlotKind.DATE_YEAR_MONTH_DAY, SmsPatternSlotKind.DATE_DAY_MONTH_YEAR, SmsPatternSlotKind.DATE_MONTH_DAY_YEAR)) compactDateOrder else slot.kind
                        Text("• " + kind.title + ": " + message.substring(slot.start, slot.endExclusive), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (message.isNotBlank()) {
                    Text("دارینو هنوز بخش متغیری پیدا نکرده؛ نمونه پیامک و وجود مبلغ را بررسی کنید.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Text("مبلغ و موجودی با تعداد رقم متغیر و جداکننده هزارگان سازگار تحلیل می‌شوند. تاریخ شمسی و ساعت بر اساس قالب نمونه و تنظیمات بالا پردازش می‌شوند. متن ثابت، فاصله‌ها و علائم نگارشی تا حد امکان حفظ می‌شوند.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dateAdjustedSlots = inferredSlots.map { slot ->
                        if (slot.kind in setOf(SmsPatternSlotKind.DATE, SmsPatternSlotKind.DATE_YEAR_MONTH_DAY, SmsPatternSlotKind.DATE_DAY_MONTH_YEAR, SmsPatternSlotKind.DATE_MONTH_DAY_YEAR, SmsPatternSlotKind.DATE_DAY_MONTH, SmsPatternSlotKind.DATE_MONTH_DAY)) slot.copy(kind = dateKindFromFormat) else slot
                    }
                    val slots = SmsPatternMatcher.configureAllowedAccountNumbers(
                        message,
                        dateAdjustedSlots,
                        listOfNotNull(accountNumber.trim().takeIf { it.isNotEmpty() }),
                        type
                    )
                    onSave(name.trim(), bankName.trim(), senderAddress, accountNumber.trim().takeIf { it.isNotEmpty() }, dateFormat, timeFormat, type, message, slots)
                },
                enabled = name.isNotBlank() && bankName.isNotBlank() && senderAddress.isNotBlank() && message.isNotBlank() &&
                    inferredSlots.count { it.kind == SmsPatternSlotKind.AMOUNT_RIAL || it.kind == SmsPatternSlotKind.AMOUNT_TOMAN } == 1 &&
                    validDateFormat && validTimeFormat,
                shape = RoundedCornerShape(10.dp)
            ) { Text("تحلیل و ذخیره الگو") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
