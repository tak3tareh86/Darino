package com.example.ui.screens.settings.subviews

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
    var showBackupPasswordDialog by remember { mutableStateOf(false) }
    var showRestorePasswordDialog by remember { mutableStateOf(false) }
    var backupPassphrase by remember { mutableStateOf("") }
    var backupPassphraseConfirm by remember { mutableStateOf("") }
    var pendingBackupPassphrase by remember { mutableStateOf<String?>(null) }
    var restorePassphrase by remember { mutableStateOf("") }
    var pendingEncryptedRestorePayload by remember { mutableStateOf<ByteArray?>(null) }
    var shareAfterEncryption by remember { mutableStateOf(false) }
    var showRestoreConfirmation by remember { mutableStateOf(false) }
    var pendingRestoreBackup by remember { mutableStateOf<com.example.data.backup.DarinoBackup?>(null) }
    var backupSummaryInfo by remember { mutableStateOf("") }
    var pendingBackupPayload by remember { mutableStateOf<ByteArray?>(null) }
    var pendingBackupChecksum by remember { mutableStateOf<String?>(null) }
    var pendingBackupSummary by remember { mutableStateOf("") }
    var showShareNameDialog by remember { mutableStateOf(false) }
    var shareFilename by remember { mutableStateOf(buildBackupFilename().removeSuffix(".darino")) }
    var pendingSharePayload by remember { mutableStateOf<String?>(null) }

    fun updateBackupTimestamp(summary: String) {
        val nowTime = System.currentTimeMillis()
        prefs.edit().putLong("last_backup_time", nowTime).apply()
        val dt = com.example.util.PersianCalendarHelper.fromEpochMillis(nowTime)
        lastBackupDate = com.example.util.IranianPhoneUtils.convertDigitsToPersian(
            "${dt.year}/${dt.month.toString().padStart(2, '0')}/${dt.day.toString().padStart(2, '0')}"
        )
        backupSummaryInfo = summary
        showBackupSuccessDialog = true
    }

    val backupFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { destinationUri ->
        val payload = pendingBackupPayload
        val checksum = pendingBackupChecksum
        val summary = pendingBackupSummary
        val passphrase = pendingBackupPassphrase
        pendingBackupPayload = null
        pendingBackupChecksum = null
        pendingBackupSummary = ""
        pendingBackupPassphrase = null
        if (destinationUri == null) {
            isBackingUp = false
        } else if (payload != null && checksum != null && passphrase != null) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        writeAndVerifyBackup(context, destinationUri, payload, checksum, passphrase.toCharArray())
                    }
                    updateBackupTimestamp(summary)
                } catch (e: Exception) {
                    Log.e("SupportBackupScreen", "Backup save failed", e)
                    Toast.makeText(context, "خطا در ذخیره فایل پشتیبان: ${e.message ?: "خطای ناشناخته"}", Toast.LENGTH_LONG).show()
                } finally {
                    isBackingUp = false
                }
            }
        } else {
            isBackingUp = false
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val payload = context.contentResolver.openInputStream(uri)?.use { input ->
                        readBoundedBytes(input, 64 * 1024 * 1024)
                    } ?: throw java.io.IOException("امکان خواندن فایل انتخاب‌شده وجود ندارد.")
                    if (com.example.data.backup.BackupEncryption.isEncrypted(payload)) {
                        pendingEncryptedRestorePayload = payload
                        restorePassphrase = ""
                        showRestorePasswordDialog = true
                    } else {
                        // Backward compatibility: older unencrypted JSON backups remain importable.
                        val backup = com.example.data.backup.BackupFileCodec.decode(payload.toString(Charsets.UTF_8))
                        pendingRestoreBackup = backup
                        showRestoreConfirmation = true
                    }
                } catch (e: Exception) {
                    Log.e("SupportBackupScreen", "Backup file reading failed", e)
                    Toast.makeText(context, "خطا در خواندن پشتیبان: ${e.message ?: "فایل نامعتبر است"}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }



    // Support contact
    val defaultSupportGmail = "support.apkapp@gmail.com"

    fun openSupportEmail(subject: String) {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.fromParts("mailto", defaultSupportGmail, null)
            putExtra(Intent.EXTRA_EMAIL, arrayOf(defaultSupportGmail))
            putExtra(Intent.EXTRA_SUBJECT, subject)
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

    if (showBackupPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showBackupPasswordDialog = false
                backupPassphrase = ""
                backupPassphraseConfirm = ""
            },
            title = { Text("رمزگذاری فایل پشتیبان") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("برای محافظت از اطلاعات مالی، فایل پشتیبان با این رمزگذاری می‌شود. رمز را در جای امن نگه دارید؛ بدون آن بازیابی ممکن نیست.")
                    OutlinedTextField(
                        value = backupPassphrase,
                        onValueChange = { backupPassphrase = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("رمز پشتیبان (حداقل ۱۲ نویسه)") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                    OutlinedTextField(
                        value = backupPassphraseConfirm,
                        onValueChange = { backupPassphraseConfirm = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("تکرار رمز پشتیبان") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = backupPassphrase.length >= 12 && backupPassphrase == backupPassphraseConfirm,
                    onClick = {
                        val secret = backupPassphrase
                        showBackupPasswordDialog = false
                        backupPassphrase = ""
                        backupPassphraseConfirm = ""
                        isBackingUp = true
                        scope.launch {
                            try {
                                val backup = backupManager.createBackup()
                                val json = com.example.data.backup.BackupFileCodec.encode(backup)
                                val payload = withContext(Dispatchers.Default) {
                                    com.example.data.backup.BackupEncryption.encrypt(
                                        json.toByteArray(Charsets.UTF_8),
                                        secret.toCharArray()
                                    )
                                }
                                val verified = com.example.data.backup.BackupEncryption.decrypt(
                                    payload,
                                    secret.toCharArray()
                                ).toString(Charsets.UTF_8)
                                val decoded = com.example.data.backup.BackupFileCodec.decode(verified)
                                check(decoded.metadata.checksum == backup.metadata.checksum) {
                                    "بررسی صحت نسخه رمزگذاری‌شده پشتیبان ناموفق بود."
                                }
                                if (shareAfterEncryption) {
                                    pendingSharePayload = android.util.Base64.encodeToString(payload, android.util.Base64.NO_WRAP)
                                    shareFilename = buildBackupFilename().removeSuffix(".darino")
                                    shareAfterEncryption = false
                                    showShareNameDialog = true
                                    isBackingUp = false
                                } else {
                                    pendingBackupPayload = payload
                                    pendingBackupChecksum = backup.metadata.checksum
                                    pendingBackupPassphrase = secret
                                    pendingBackupSummary = "تعداد کل رکوردها: ${backup.metadata.recordCount} | نسخه: ${backup.metadata.backupVersion}"
                                    backupFileLauncher.launch(buildBackupFilename())
                                }
                            } catch (e: Exception) {
                                isBackingUp = false
                                pendingBackupPayload = null
                                pendingBackupChecksum = null
                                pendingBackupPassphrase = null
                                pendingBackupSummary = ""
                                Log.e("SupportBackupScreen", "Backup creation failed", e)
                                Toast.makeText(context, "خطا در آماده‌سازی پشتیبان: ${e.message ?: "خطای ناشناخته"}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) { Text("ایجاد فایل رمزگذاری‌شده") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBackupPasswordDialog = false
                    backupPassphrase = ""
                    backupPassphraseConfirm = ""
                }) { Text("انصراف") }
            }
        )
    }

    if (showRestorePasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showRestorePasswordDialog = false
                restorePassphrase = ""
                pendingEncryptedRestorePayload = null
            },
            title = { Text("رمز فایل پشتیبان") },
            text = {
                OutlinedTextField(
                    value = restorePassphrase,
                    onValueChange = { restorePassphrase = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("رمز پشتیبان") },
                    visualTransformation = PasswordVisualTransformation()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = restorePassphrase.isNotEmpty(),
                    onClick = {
                        val secret = restorePassphrase
                        val encrypted = pendingEncryptedRestorePayload
                        scope.launch {
                            try {
                                val bytes = encrypted ?: throw IllegalStateException("فایل رمزگذاری‌شده در دسترس نیست.")
                                val json = withContext(Dispatchers.Default) {
                                    com.example.data.backup.BackupEncryption.decrypt(bytes, secret.toCharArray())
                                }.toString(Charsets.UTF_8)
                                pendingRestoreBackup = com.example.data.backup.BackupFileCodec.decode(json)
                                showRestorePasswordDialog = false
                                restorePassphrase = ""
                                pendingEncryptedRestorePayload = null
                                showRestoreConfirmation = true
                            } catch (e: Exception) {
                                restorePassphrase = ""
                                showRestorePasswordDialog = true
                                Log.e("SupportBackupScreen", "Encrypted backup could not be opened", e)
                                Toast.makeText(context, "رمز اشتباه است یا فایل پشتیبان آسیب دیده است. دوباره تلاش کنید.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) { Text("باز کردن فایل") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestorePasswordDialog = false
                    restorePassphrase = ""
                    pendingEncryptedRestorePayload = null
                }) { Text("انصراف") }
            }
        )
    }

    if (showShareNameDialog) {
        AlertDialog(
            onDismissRequest = {
                showShareNameDialog = false
                pendingSharePayload = null
            },
            title = { Text("نام فایل پشتیبان") },
            text = {
                OutlinedTextField(
                    value = shareFilename,
                    onValueChange = { shareFilename = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("نام فایل") },
                    supportingText = { Text("پسوند .darino به‌صورت خودکار اضافه می‌شود.") }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = shareFilename.isNotBlank() && shareFilename.trim() !in setOf(".", "..") &&
                        shareFilename.none { it in charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|') },
                    onClick = {
                        val encodedPayload = pendingSharePayload
                        val safeName = shareFilename.trim().removeSuffix(".darino").ifBlank { "Darino backup" } + ".darino"
                        showShareNameDialog = false
                        pendingSharePayload = null
                        if (encodedPayload != null) {
                            scope.launch {
                                try {
                                    val shareRoot = java.io.File(context.cacheDir, "shared_backups").apply { mkdirs() }
                                    val shareDir = java.io.File(shareRoot, "share-${System.currentTimeMillis()}").apply { mkdirs() }
                                    withContext(Dispatchers.IO) {
                                        shareRoot.listFiles()?.filter {
                                            it.isDirectory && System.currentTimeMillis() - it.lastModified() > 24L * 60L * 60L * 1000L
                                        }?.forEach { it.deleteRecursively() }
                                    }
                                    val cacheFile = java.io.File(shareDir, safeName)
                                    withContext(Dispatchers.IO) {
                                        val encryptedBytes = android.util.Base64.decode(encodedPayload, android.util.Base64.NO_WRAP)
                                        check(com.example.data.backup.BackupEncryption.isEncrypted(encryptedBytes)) {
                                            "فایل اشتراک‌گذاری‌شده رمزگذاری‌شده نیست."
                                        }
                                        cacheFile.outputStream().buffered().use { it.write(encryptedBytes) }
                                        if (!cacheFile.isFile || cacheFile.length() <= 0L) {
                                            throw java.io.IOException("فایل پشتیبان برای اشتراک‌گذاری خالی است.")
                                        }
                                    }
                                    val contentUri = androidx.core.content.FileProvider.getUriForFile(
                                        context, "com.example.fileprovider", cacheFile
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/octet-stream"
                                        putExtra(Intent.EXTRA_STREAM, contentUri)
                                        clipData = android.content.ClipData.newUri(context.contentResolver, safeName, contentUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری فایل پشتیبان"))
                                } catch (e: Exception) {
                                    Log.e("SupportBackupScreen", "Backup sharing failed", e)
                                    Toast.makeText(context, "خطا در اشتراک‌گذاری: ${e.message ?: "خطای ناشناخته"}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                ) { Text("ادامه") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showShareNameDialog = false
                    pendingSharePayload = null
                }) { Text("انصراف") }
            }
        )
    }

    if (showBackupSuccessDialog) {
        SettingsConfirmationDialog(
            isOpen = true,
            title = "پشتیبان‌گیری موفق",
            message = "فایل پشتیبان با رمز عبوری که تعیین کردید رمزگذاری و پس از ذخیره دوباره بررسی شد. برای بازیابی، همان رمز لازم است.\n$backupSummaryInfo",
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
            message = "اطلاعات موجود با محتوای فایل پشتیبان جایگزین می‌شود. پیش از شروع، یک نسخه اضطراری رمزگذاری‌شده از وضعیت فعلی ذخیره خواهد شد. تعداد رکوردهای فایل: ${pendingRestoreBackup?.metadata?.recordCount ?: 0} | فایل‌های پیوست: ${pendingRestoreBackup?.attachments?.size ?: 0}",
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
                                    if (!isBackingUp) {
                                        backupPassphrase = ""
                                        backupPassphraseConfirm = ""
                                        showBackupPasswordDialog = true
                                    }
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
                                    restoreBackupLauncher.launch(arrayOf("application/octet-stream", "application/json", "*/*"))
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
                                    if (!isBackingUp) {
                                        shareAfterEncryption = true
                                        backupPassphrase = ""
                                        backupPassphraseConfirm = ""
                                        showBackupPasswordDialog = true
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
                    // TAB 2: SUPPORT
                    item {
                        SettingsSectionCard(title = "ارتباط با پشتیبانی از طریق ایمیل") {
                            SupportContactRow(
                                title = "نشانی ایمیل پشتیبانی",
                                subtitle = defaultSupportGmail,
                                icon = Icons.Rounded.Mail,
                                iconColor = Color(0xFFEA4335)
                            )
                        }
                    }

                    item {
                        SettingsSectionCard(title = "ارسال ایمیل") {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(
                                    onClick = {
                                        openSupportEmail("درخواست پشتیبانی - نرم‌افزار حسابداری و مدیریت خودرو")
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp),
                                    shape = RoundedCornerShape(RadiusMD),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimaryLight)
                                ) {
                                    Text("ارسال ایمیل", fontWeight = FontWeight.Bold)
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

private fun readBoundedBytes(input: java.io.InputStream, maxBytes: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    var total = 0
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        total += read
        require(total <= maxBytes) { "حجم فایل پشتیبان بیش از حد مجاز است." }
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}

private fun buildBackupFilename(): String {
    val (year, month, day) = com.example.calendar.domain.CalendarDateUtils.getCurrentJalaliDate()
    return "Darino backup ${year}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}.darino"
}

private fun findExistingBackupDocument(
    context: android.content.Context,
    treeUri: Uri,
    filename: String
): Uri? {
    val resolver = context.contentResolver
    val parentDocumentId = android.provider.DocumentsContract.getTreeDocumentId(treeUri)
    val childrenUri = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(
        treeUri,
        parentDocumentId
    )
    val projection = arrayOf(
        android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME
    )

    val cursor = resolver.query(childrenUri, projection, null, null, null)
        ?: throw java.io.IOException("امکان بررسی فایل‌های موجود در پوشه انتخاب‌شده وجود ندارد.")
    cursor.use {
        val idColumn = cursor.getColumnIndexOrThrow(
            android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID
        )
        val nameColumn = cursor.getColumnIndexOrThrow(
            android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME
        )
        while (cursor.moveToNext()) {
            if (cursor.getString(nameColumn) == filename) {
                val documentId = cursor.getString(idColumn)
                return android.provider.DocumentsContract.buildDocumentUriUsingTree(
                    treeUri,
                    documentId
                )
            }
        }
    }
    return null
}

private fun getDocumentDisplayName(
    context: android.content.Context,
    documentUri: Uri
): String? {
    val projection = arrayOf(android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME)
    context.contentResolver.query(documentUri, projection, null, null, null)?.use { cursor ->
        val nameColumn = cursor.getColumnIndex(
            android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME
        )
        if (nameColumn >= 0 && cursor.moveToFirst()) return cursor.getString(nameColumn)
    }
    return null
}

private fun writeAndVerifyBackup(
    context: android.content.Context,
    destinationUri: Uri,
    payload: ByteArray,
    expectedChecksum: String,
    passphrase: CharArray
) {
    val output = context.contentResolver.openOutputStream(destinationUri, "wt")
        ?: throw java.io.IOException("ارائه‌دهنده فایل اجازه نوشتن یا جایگزینی فایل را نمی‌دهد.")
    output.buffered().use { stream ->
        stream.write(payload)
        stream.flush()
    }

    val savedPayload = context.contentResolver.openInputStream(destinationUri)
        ?.use { it.readBytes() }
        ?: throw java.io.IOException("فایل ذخیره شد اما امکان بررسی مجدد آن وجود ندارد.")
    val savedJson = com.example.data.backup.BackupEncryption.decrypt(savedPayload, passphrase)
        .toString(Charsets.UTF_8)
    val savedBackup = com.example.data.backup.BackupFileCodec.decode(savedJson)
    if (savedPayload.size != payload.size || savedBackup.metadata.checksum != expectedChecksum) {
        throw java.io.IOException("بررسی صحت فایل ذخیره‌شده ناموفق بود.")
    }
}

@Composable
private fun SupportContactRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
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
    }
}
