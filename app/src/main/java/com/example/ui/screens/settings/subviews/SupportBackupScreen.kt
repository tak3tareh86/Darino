package com.example.ui.screens.settings.subviews

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.util.Log
import kotlinx.coroutines.launch
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ui.screens.settings.components.SettingsConfirmationDialog
import com.example.ui.screens.settings.components.SettingsHeader
import com.example.ui.screens.settings.components.SettingsSectionCard
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM

@Composable
fun SupportBackupScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val backupRepo = remember { com.example.data.backup.LocalBackupRepository(context) }
    val backupManager = remember { com.example.domain.backup.BackupManager(backupRepo) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("پشتیبان‌گیری داده‌ها", "ارتباط با پشتیبانی")

    // Backup state
    val prefs = remember { context.getSharedPreferences("darino_backup_prefs", android.content.Context.MODE_PRIVATE) }
    var lastBackupDate by remember {
        val lastTime = prefs.getLong("last_backup_time", 0L)
        mutableStateOf(
            if (lastTime == 0L) {
                "تاکنون بک‌آپی گرفته نشده"
            } else {
                val dt = com.example.util.PersianCalendarHelper.fromEpochMillis(lastTime)
                com.example.util.IranianPhoneUtils.convertDigitsToPersian("${dt.year}/${dt.month.toString().padStart(2, '0')}/${dt.day.toString().padStart(2, '0')}")
            }
        )
    }

    val refreshLastBackupDate = {
        val lastTime = prefs.getLong("last_backup_time", 0L)
        lastBackupDate = if (lastTime == 0L) {
            "تاکنون بک‌آپی گرفته نشده"
        } else {
            val dt = com.example.util.PersianCalendarHelper.fromEpochMillis(lastTime)
            com.example.util.IranianPhoneUtils.convertDigitsToPersian(
                "${dt.year}/${dt.month.toString().padStart(2, '0')}/${dt.day.toString().padStart(2, '0')}"
            )
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshLastBackupDate()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var isBackingUp by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var showBackupSuccessDialog by remember { mutableStateOf(false) }
    var showRestoreConfirmation by remember { mutableStateOf(false) }
    var pendingRestoreBackup by remember { mutableStateOf<com.example.data.backup.DarinoBackup?>(null) }
    var backupSummaryInfo by remember { mutableStateOf("") }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            isBackingUp = true
            scope.launch {
                try {
                    val backup = backupManager.createBackup()
                    // Serialize and validate before touching the user-selected document.
                    val jsonString = com.example.data.backup.BackupFileCodec.encode(backup)
                    val payload = jsonString.toByteArray(Charsets.UTF_8)
                    if (payload.isEmpty()) throw java.io.IOException("محتوای پشتیبان خالی است.")

                    val stagedFile = java.io.File.createTempFile("darino-backup-", ".json", context.cacheDir)
                    try {
                        stagedFile.outputStream().buffered().use { stream ->
                            stream.write(payload)
                            stream.flush()
                        }
                        val stagedJson = stagedFile.inputStream().bufferedReader(Charsets.UTF_8).use { it.readText() }
                        val stagedBackup = com.example.data.backup.BackupFileCodec.decode(stagedJson)
                        if (stagedBackup.metadata.checksum != backup.metadata.checksum) {
                            throw java.io.IOException("بررسی فایل موقت پشتیبان ناموفق بود.")
                        }

                        val outputStream = context.contentResolver.openOutputStream(uri, "w")
                            ?: throw java.io.IOException("امکان نوشتن در فایل انتخاب‌شده وجود ندارد.")
                        outputStream.buffered().use { stream ->
                            stream.write(payload)
                            stream.flush()
                        }

                        val savedJson = context.contentResolver.openInputStream(uri)
                            ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                            ?: throw java.io.IOException("فایل نوشته شد اما امکان خواندن مجدد آن وجود ندارد.")
                        val savedBackup = com.example.data.backup.BackupFileCodec.decode(savedJson)
                        if (savedJson.toByteArray(Charsets.UTF_8).size != payload.size ||
                            savedBackup.metadata.checksum != backup.metadata.checksum) {
                            throw java.io.IOException("حجم یا صحت فایل ذخیره‌شده با نسخه اصلی مطابقت ندارد.")
                        }
                    } finally {
                        stagedFile.delete()
                    }

                    val nowTime = System.currentTimeMillis()
                    prefs.edit().putLong("last_backup_time", nowTime).apply()
                    val dt = com.example.util.PersianCalendarHelper.fromEpochMillis(nowTime)
                    lastBackupDate = com.example.util.IranianPhoneUtils.convertDigitsToPersian("${dt.year}/${dt.month.toString().padStart(2, '0')}/${dt.day.toString().padStart(2, '0')}")

                    backupSummaryInfo = "تعداد کل رکوردها: ${backup.metadata.recordCount} | نسخه: ${backup.metadata.backupVersion}"
                    isBackingUp = false
                    showBackupSuccessDialog = true
                } catch (e: Exception) {
                    isBackingUp = false
                    Log.e("SupportBackupScreen", "Backup creation failed", e)
                    runCatching { android.provider.DocumentsContract.deleteDocument(context.contentResolver, uri) }
                    Toast.makeText(context, "خطا در ایجاد پشتیبان: ${e.message ?: "خطای ناشناخته"}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val jsonString = context.contentResolver.openInputStream(uri)
                        ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                        ?: throw java.io.IOException("امکان خواندن فایل انتخاب‌شده وجود ندارد.")
                    val backup = com.example.data.backup.BackupFileCodec.decode(jsonString)
                    pendingRestoreBackup = backup
                    showRestoreConfirmation = true
                } catch (e: Exception) {
                    Log.e("SupportBackupScreen", "Backup file reading failed", e)
                    Toast.makeText(context, "خطا در خواندن پشتیبان: ${e.message ?: "فایل نامعتبر است"}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Support state
    var ticketSubject by remember { mutableStateOf("") }
    var ticketMessage by remember { mutableStateOf("") }

    // Single source of truth for the support mailbox used by every contact action.
    val defaultSupportGmail = "support.financeapp@gmail.com"

    fun openSupportEmail(subject: String, message: String = "") {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.fromParts("mailto", defaultSupportGmail, null)
            putExtra(Intent.EXTRA_EMAIL, arrayOf(defaultSupportGmail))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            if (message.isNotBlank()) putExtra(Intent.EXTRA_TEXT, message)
        }
        try {
            context.startActivity(Intent.createChooser(emailIntent, "انتخاب برنامه ایمیل"))
            Toast.makeText(
                context,
                "پیش‌نویس ایمیل آماده شد؛ برای ارسال نهایی، دکمه ارسال را در برنامه ایمیل بزنید.",
                Toast.LENGTH_LONG
            ).show()
        } catch (_: android.content.ActivityNotFoundException) {
            Toast.makeText(
                context,
                "هیچ برنامه ایمیلی روی دستگاه پیدا نشد. می‌توانید به این نشانی ایمیل بزنید: $defaultSupportGmail",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Log.e("SupportBackupScreen", "Unable to open support email app", e)
            Toast.makeText(context, "باز کردن برنامه ایمیل ناموفق بود.", Toast.LENGTH_LONG).show()
        }
    }

    if (showBackupSuccessDialog) {
        SettingsConfirmationDialog(
            isOpen = true,
            title = "پشتیبان‌گیری موفق",
            message = "نسخه پشتیبان شامل حساب‌ها، چک‌ها، تراکنش‌ها، اقساط، خودروها، یادآورها و رویدادهای مالی ذخیره و دوباره بررسی شد. فایل JSON رمزگذاری نشده است؛ آن را در محل امن نگه دارید.\n$backupSummaryInfo",
            confirmButtonText = "متوجه شدم",
            isDanger = false,
            onConfirm = { showBackupSuccessDialog = false },
            onDismiss = { showBackupSuccessDialog = false }
        )
    }

    if (showRestoreConfirmation && pendingRestoreBackup != null) {
        SettingsConfirmationDialog(
            isOpen = true,
            title = "تأیید بازیابی پشتیبان",
            message = "اطلاعات ذخیره‌شده مربوط به حساب فعلی با محتوای فایل پشتیبان جایگزین می‌شود. داده‌های عمومی دستگاه که به حساب کاربری وابسته نیستند، حذف نخواهند شد. پیش از ادامه، از اطلاعات فعلی نسخه پشتیبان بگیرید.",
            confirmButtonText = "بازیابی و جایگزینی",
            isDanger = true,
            onConfirm = {
                val backupToRestore = pendingRestoreBackup
                pendingRestoreBackup = null
                showRestoreConfirmation = false
                if (backupToRestore != null) {
                    scope.launch {
                        isRestoring = true
                        try {
                            backupManager.restoreBackup(
                                backupToRestore,
                                com.example.data.backup.RestoreMode.REPLACE
                            )
                            Toast.makeText(context, "اطلاعات با موفقیت بازیابی شد.", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "خطا در بازیابی: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isRestoring = false
                        }
                    }
                }
            },
            onDismiss = {
                pendingRestoreBackup = null
                showRestoreConfirmation = false
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SettingsHeader(
                title = "پشتیبانی و بک‌آپ‌گیری",
                subtitle = "تهیه نسخه پشتیبان و ارتباط با پشتیبانی از طریق برنامه ایمیل",
                showBack = true,
                showSearch = false,
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs Row
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTabIndex == index) EmeraldPrimaryLight else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectedTabIndex == 0) {
                    // TAB 1: BACKUP & RESTORE

                    // Backup Actions Card
                    item {
                        SettingsSectionCard(title = "عملیات پشتیبان‌گیری و بازیابی") {
                            // Last Backup Status
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(RadiusMD))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudDone,
                                        contentDescription = null,
                                        tint = EmeraldPrimaryLight,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "آخرین وضعیت بک‌آپ",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "آخرین ذخیره: $lastBackupDate",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Create Backup Button
                            Button(
                                onClick = {
                                    val (y, m, d) = com.example.calendar.domain.CalendarDateUtils.getCurrentJalaliDate()
                                    val defaultFilename = "backup_${y}_${m.toString().padStart(2, '0')}_${d.toString().padStart(2, '0')}.json"
                                    createBackupLauncher.launch(defaultFilename)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("create_backup_button"),
                                shape = RoundedCornerShape(RadiusMD),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimaryLight),
                                enabled = !isBackingUp
                            ) {
                                if (isBackingUp) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("در حال ایجاد فایل پشتیبان...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudUpload,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("ایجاد فایل پشتیبان جدید (Backup)", fontWeight = FontWeight.Bold)
                                }
                            }

                            // Restore Backup Button
                            OutlinedButton(
                                onClick = {
                                    restoreBackupLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("restore_backup_button"),
                                enabled = !isRestoring,
                                shape = RoundedCornerShape(RadiusMD),
                                border = BorderStroke(1.dp, EmeraldPrimaryLight)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudDownload,
                                    contentDescription = null,
                                    tint = EmeraldPrimaryLight,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "بازیابی از فایل پشتیبان قبلی (Restore)",
                                    color = EmeraldPrimaryLight,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Share Backup File
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            val backup = backupManager.createBackup()
                                            val jsonString = com.example.data.backup.BackupFileCodec.encode(backup)

                                            val (y, m, d) = com.example.calendar.domain.CalendarDateUtils.getCurrentJalaliDate()
                                            val defaultFilename = "backup_${y}_${m.toString().padStart(2, '0')}_${d.toString().padStart(2, '0')}.json"

                                            val cacheFile = java.io.File(context.cacheDir, defaultFilename)
                                            cacheFile.outputStream().buffered().use { stream ->
                                                stream.write(jsonString.toByteArray(Charsets.UTF_8))
                                                stream.flush()
                                            }
                                            if (!cacheFile.isFile || cacheFile.length() <= 0L) {
                                                throw java.io.IOException("فایل پشتیبان برای اشتراک‌گذاری خالی است.")
                                            }

                                            val contentUri = androidx.core.content.FileProvider.getUriForFile(
                                                context,
                                                "com.example.fileprovider",
                                                cacheFile
                                            )

                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/json"
                                                putExtra(Intent.EXTRA_STREAM, contentUri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }

                                            context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری فایل پشتیبان"))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "خطا در اشتراک‌گذاری: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(RadiusMD),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "اشتراک‌گذاری فایل بک‌آپ (ارسال به تلگرام، درایو)",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                } else {
                    // TAB 2: SUPPORT (فقط جیمیل پیش‌فرض)
                    item {
                        SettingsSectionCard(title = "ارتباط با پشتیبانی از طریق ایمیل") {
                            SupportContactRow(
                                title = "نشانی ایمیل پشتیبانی",
                                subtitle = defaultSupportGmail,
                                icon = Icons.Rounded.Mail,
                                iconColor = Color(0xFFEA4335),
                                actionText = "ارسال ایمیل",
                                onClick = {
                                    openSupportEmail("درخواست پشتیبانی - نرم‌افزار حسابداری و مدیریت خودرو")
                                }
                            )
                        }
                    }

                    // Ticket / Direct Message Form
                    item {
                        SettingsSectionCard(title = "ارسال پیام به ایمیل پشتیبانی") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = ticketSubject,
                                    onValueChange = { ticketSubject = it },
                                    label = { Text("موضوع پیام") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(RadiusMD)
                                )

                                OutlinedTextField(
                                    value = ticketMessage,
                                    onValueChange = { ticketMessage = it },
                                    label = { Text("متن پیام یا گزارش شما") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3,
                                    maxLines = 5,
                                    shape = RoundedCornerShape(RadiusMD)
                                )

                                Button(
                                    onClick = {
                                        val subject = ticketSubject.trim()
                                        val message = ticketMessage.trim()
                                        if (subject.isNotEmpty() && message.isNotEmpty()) {
                                            openSupportEmail(subject, message)
                                        } else {
                                            Toast.makeText(context, "لطفاً موضوع و متن پیام را وارد کنید.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp),
                                    shape = RoundedCornerShape(RadiusMD),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimaryLight)
                                ) {
                                    Text("باز کردن پیش‌نویس ایمیل", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(36.dp))
                }
            }
        }
    }
}

@Composable
private fun SupportContactRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    actionText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMD))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(RadiusSM),
            color = iconColor.copy(alpha = 0.15f)
        ) {
            Text(
                text = actionText,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = iconColor
            )
        }
    }
}
