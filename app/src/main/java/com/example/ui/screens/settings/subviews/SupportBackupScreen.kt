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
import androidx.compose.material3.Switch
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
    var selectedBackupTabIndex by remember { mutableIntStateOf(0) }
    val backupTabs = listOf("پشتیبان‌گیری", "بازیابی", "اشتراک‌گذاری")

    // Backup state
    val prefs = remember { context.getSharedPreferences("darino_backup_prefs", android.content.Context.MODE_PRIVATE) }
    var dailyAutoBackupEnabled by remember { mutableStateOf(prefs.getBoolean("auto_backup_daily", false)) }
    var weeklyAutoBackupEnabled by remember { mutableStateOf(prefs.getBoolean("auto_backup_weekly", false)) }
    var backupFolderUri by remember { mutableStateOf(com.example.data.backup.BackupFolderStorage.selectedFolderUri(context)) }
    var dailyAutoBackupStatus by remember { mutableStateOf("در حال بررسی وضعیت زمان‌بندی…") }
    var weeklyAutoBackupStatus by remember { mutableStateOf("در حال بررسی وضعیت زمان‌بندی…") }

    fun formatAutomaticBackupStatus(frequency: String, enabled: Boolean, state: androidx.work.WorkInfo.State?): String {
        val lastResult = prefs.getString("auto_backup_last_result_$frequency", null)
        val lastError = prefs.getString("auto_backup_last_error_$frequency", null)
        val stateLabel = when {
            !enabled -> "غیرفعال"
            state == androidx.work.WorkInfo.State.RUNNING -> "در حال اجرا"
            lastResult == "failed" -> "آخرین اجرا ناموفق بوده؛ تلاش بعدی در انتظار است"
            lastResult == "skipped" -> "آخرین نوبت بدون تهیه نسخه پشتیبان رد شده است"
            state == androidx.work.WorkInfo.State.ENQUEUED -> "زمان‌بندی شده و در انتظار اجرا"
            state == androidx.work.WorkInfo.State.BLOCKED -> "در انتظار اجرای پیش‌نیاز"
            state == androidx.work.WorkInfo.State.FAILED -> "اجرای ثبت‌شده ناموفق"
            state == androidx.work.WorkInfo.State.CANCELLED -> "زمان‌بندی لغو شده"
            state == androidx.work.WorkInfo.State.SUCCEEDED -> "اجرا با موفقیت ثبت شد"
            else -> "زمان‌بندی در WorkManager پیدا نشد"
        }
        val lastSuccess = prefs.getLong("auto_backup_last_success_$frequency", 0L)
        val successText = if (lastSuccess == 0L) {
            "هنوز اجرای موفقی ثبت نشده است"
        } else {
            val date = com.example.util.PersianCalendarHelper.fromEpochMillis(lastSuccess)
            val dateText = com.example.util.IranianPhoneUtils.convertDigitsToPersian(
                "${date.year}/${date.month.toString().padStart(2, '0')}/${date.day.toString().padStart(2, '0')}"
            )
            val timeText = com.example.util.IranianPhoneUtils.convertDigitsToPersian(
                java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(lastSuccess))
            )
            "آخرین موفق: $dateText - $timeText"
        }
        val errorText = lastError?.takeIf { it.isNotBlank() }?.let { " • علت آخرین وضعیت: $it" }.orEmpty()
        return "$stateLabel • $successText$errorText"
    }

    fun refreshAutomaticBackupStatus() {
        scope.launch {
            val statuses = withContext(Dispatchers.IO) {
                val workManager = androidx.work.WorkManager.getInstance(context)
                val dailyInfo = runCatching {
                    workManager.getWorkInfosForUniqueWork(com.example.data.backup.WeeklyBackupWorker.DAILY_UNIQUE_WORK_NAME).get().firstOrNull()
                }.getOrNull()
                val weeklyInfo = runCatching {
                    workManager.getWorkInfosForUniqueWork(com.example.data.backup.WeeklyBackupWorker.UNIQUE_WORK_NAME).get().firstOrNull()
                }.getOrNull()
                Pair(
                    formatAutomaticBackupStatus("daily", prefs.getBoolean("auto_backup_daily", false), dailyInfo?.state),
                    formatAutomaticBackupStatus("weekly", prefs.getBoolean("auto_backup_weekly", false), weeklyInfo?.state)
                )
            }
            dailyAutoBackupStatus = statuses.first
            weeklyAutoBackupStatus = statuses.second
        }
    }

    // A screen opened while the Activity is already RESUMED will not receive a new
    // ON_RESUME event, so load the status once when this composable enters composition.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        refreshAutomaticBackupStatus()
    }

    val backupFolderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { selectedUri ->
        if (selectedUri != null) {
            try {
                com.example.data.backup.BackupFolderStorage.saveSelectedFolder(
                    context,
                    selectedUri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                backupFolderUri = selectedUri
                com.example.data.backup.WeeklyBackupWorker.syncAutomaticBackups(context)
                refreshAutomaticBackupStatus()
                Toast.makeText(context, "پوشه ذخیره پشتیبان انتخاب شد.", Toast.LENGTH_SHORT).show()
            } catch (error: Exception) {
                Log.e("SupportBackupScreen", "Could not persist backup folder", error)
                Toast.makeText(context, error.message ?: "انتخاب پوشه پشتیبان ناموفق بود.", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun setAutomaticBackupEnabled(frequency: String, enabled: Boolean) {
        if (enabled && !com.example.data.backup.BackupFolderStorage.hasWritableSelectedFolder(context)) {
            Toast.makeText(context, "برای فعال‌سازی بک‌آپ خودکار، ابتدا پوشه ذخیره را انتخاب کنید.", Toast.LENGTH_LONG).show()
            backupFolderPickerLauncher.launch(null)
            return
        }
        val preferenceKey = if (frequency == "daily") "auto_backup_daily" else "auto_backup_weekly"
        try {
            check(prefs.edit().putBoolean(preferenceKey, enabled).commit()) {
                "ذخیره تنظیم پشتیبان‌گیری خودکار ناموفق بود."
            }
            if (frequency == "daily") {
                if (enabled) com.example.data.backup.WeeklyBackupWorker.enqueueDaily(context)
                else com.example.data.backup.WeeklyBackupWorker.cancelDaily(context)
                dailyAutoBackupEnabled = enabled
            } else {
                if (enabled) com.example.data.backup.WeeklyBackupWorker.enqueueWeekly(context)
                else com.example.data.backup.WeeklyBackupWorker.cancelWeekly(context)
                weeklyAutoBackupEnabled = enabled
            }
            val frequencyLabel = if (frequency == "daily") "روزانه" else "هفتگی"
            refreshAutomaticBackupStatus()
            val statusLabel = if (enabled) "فعال" else "غیرفعال"
            Toast.makeText(context, "پشتیبان‌گیری خودکار $frequencyLabel $statusLabel شد.", Toast.LENGTH_SHORT).show()
        } catch (error: Exception) {
            Log.e("SupportBackupScreen", "Could not update $frequency automatic backup setting", error)
            Toast.makeText(context, error.message ?: "تغییر تنظیم پشتیبان‌گیری ناموفق بود.", Toast.LENGTH_LONG).show()
        }
    }
    fun currentBackupTimestampKey(): String {
        val userId = com.example.data.security.SessionManager.userId
            ?: com.example.data.security.AuthSessionManager(context).getActiveSession()?.userId
        return userId?.let { "last_backup_time_${com.example.data.backup.BackupUserScope.tag(it)}" } ?: "last_backup_time"
    }
    fun readLastBackupTimestamp(): Long {
        val key = currentBackupTimestampKey()
        return prefs.getLong(key, 0L)
    }
    var lastBackupDate by remember {
        val lastTime = readLastBackupTimestamp()
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
        val lastTime = readLastBackupTimestamp()
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
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshLastBackupDate()
                dailyAutoBackupEnabled = prefs.getBoolean("auto_backup_daily", false)
                weeklyAutoBackupEnabled = prefs.getBoolean("auto_backup_weekly", false)
                backupFolderUri = com.example.data.backup.BackupFolderStorage.selectedFolderUri(context)
                refreshAutomaticBackupStatus()
            }
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
    var restorePassphrase by remember { mutableStateOf("") }
    var pendingEncryptedRestorePayload by remember { mutableStateOf<ByteArray?>(null) }
    var shareAfterEncryption by remember { mutableStateOf(false) }
    var showRestoreConfirmation by remember { mutableStateOf(false) }
    var showEmergencyRestoreConfirmation by remember { mutableStateOf(false) }
    var showAutomaticRestoreConfirmation by remember { mutableStateOf(false) }
    var pendingRestoreBackup by remember { mutableStateOf<com.example.data.backup.DarinoBackup?>(null) }
    var backupSummaryInfo by remember { mutableStateOf("") }
    var showShareNameDialog by remember { mutableStateOf(false) }
    var shareFilename by remember { mutableStateOf(buildBackupFilename().removeSuffix(".darino")) }
    var pendingSharePayload by remember { mutableStateOf<String?>(null) }

    fun updateBackupTimestamp(summary: String) {
        val nowTime = System.currentTimeMillis()
        prefs.edit().putLong("last_backup_time", nowTime).putLong(currentBackupTimestampKey(), nowTime).apply()
        val dt = com.example.util.PersianCalendarHelper.fromEpochMillis(nowTime)
        lastBackupDate = com.example.util.IranianPhoneUtils.convertDigitsToPersian(
            "${dt.year}/${dt.month.toString().padStart(2, '0')}/${dt.day.toString().padStart(2, '0')}"
        )
        backupSummaryInfo = summary
        showBackupSuccessDialog = true
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val payload = context.contentResolver.openInputStream(uri)?.use { input ->
                        readBoundedBytes(input, MAX_PORTABLE_BACKUP_BYTES + 1024)
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
                shareAfterEncryption = false
                backupPassphrase = ""
                backupPassphraseConfirm = ""
            },
            title = { Text("رمزگذاری فایل پشتیبان") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("برای محافظت از اطلاعات مالی، فایل پشتیبان با این رمزگذاری می‌شود. رمز را در جای امن نگه دارید؛ بدون آن بازیابی ممکن نیست.")
                    OutlinedTextField(
                        value = backupPassphrase,
                        onValueChange = { backupPassphrase = it.filter(Char::isDigit).take(6) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("رمز پشتیبان (۶ رقم)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation()
                    )
                    OutlinedTextField(
                        value = backupPassphraseConfirm,
                        onValueChange = { backupPassphraseConfirm = it.filter(Char::isDigit).take(6) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("تکرار رمز ۶ رقمی") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = backupPassphrase.length == 6 && backupPassphrase.all(Char::isDigit) && backupPassphrase == backupPassphraseConfirm,
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
                                val jsonBytes = json.toByteArray(Charsets.UTF_8)
                                require(jsonBytes.size <= MAX_PORTABLE_BACKUP_BYTES) {
                                    "حجم پشتیبان از حد مجاز فایل قابل‌بازیابی بیشتر است."
                                }
                                val payload = withContext(Dispatchers.Default) {
                                    com.example.data.backup.BackupEncryption.encrypt(
                                        jsonBytes,
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
                                    val filename = buildManualBackupFilename()
                                    withContext(Dispatchers.IO) {
                                        com.example.data.backup.BackupFolderStorage.saveBackup(
                                            context,
                                            filename,
                                            payload
                                        )
                                    }
                                    updateBackupTimestamp(
                                        "تعداد کل رکوردها: ${backup.metadata.recordCount} | نسخه: ${backup.metadata.backupVersion}"
                                    )
                                }
                            } catch (e: Exception) {
                                isBackingUp = false
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
                    shareAfterEncryption = false
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
                    label = { Text("رمز ۶ رقمی (برای فایل‌های قدیمی، رمز قبلی)") },
                    visualTransformation = PasswordVisualTransformation()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = (restorePassphrase.length == 6 && restorePassphrase.all(Char::isDigit)) || restorePassphrase.length >= 12,
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

    if (showEmergencyRestoreConfirmation) {
        SettingsConfirmationDialog(
            isOpen = true,
            title = "بازیابی نسخه اضطراری",
            message = "آخرین نسخه اضطراری رمزگذاری‌شده‌ای که پیش از جایگزینی اطلاعات ذخیره شده است، روی حساب فعلی بازیابی می‌شود. این نسخه فقط روی همین دستگاه قابل بازیابی است.",
            confirmButtonText = "بازیابی نسخه اضطراری",
            isDanger = true,
            onConfirm = {
                showEmergencyRestoreConfirmation = false
                scope.launch {
                    isRestoring = true
                    try {
                        com.example.data.backup.LocalBackupRepository(context).restoreLatestEmergencyBackup()
                        Toast.makeText(context, "نسخه اضطراری با موفقیت بازیابی شد.", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "خطا در بازیابی نسخه اضطراری: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        isRestoring = false
                    }
                }
            },
            onDismiss = { showEmergencyRestoreConfirmation = false }
        )
    }

    if (showAutomaticRestoreConfirmation) {
        SettingsConfirmationDialog(
            isOpen = true,
            title = "بازیابی آخرین پشتیبان خودکار",
            message = "جدیدترین نسخه روزانه یا هفتگی حساب فعلی از پوشه انتخاب‌شده بازیابی می‌شود. این نسخه به کلید رمزگذاری همین دستگاه وابسته است و پیش از جایگزینی، از وضعیت فعلی یک نسخه اضطراری ساخته خواهد شد.",
            confirmButtonText = "بازیابی پشتیبان خودکار",
            isDanger = true,
            onConfirm = {
                showAutomaticRestoreConfirmation = false
                scope.launch {
                    isRestoring = true
                    try {
                        backupRepo.restoreLatestAutomaticBackup()
                        Toast.makeText(context, "پشتیبان خودکار با موفقیت بازیابی شد.", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "خطا در بازیابی پشتیبان خودکار: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        isRestoring = false
                    }
                }
            },
            onDismiss = { showAutomaticRestoreConfirmation = false }
        )
    }

    if (showRestoreConfirmation && pendingRestoreBackup != null) {
        SettingsConfirmationDialog(
            isOpen = true,
            title = "تأیید بازیابی پشتیبان",
            message = "اطلاعات موجود با محتوای فایل پشتیبان جایگزین می‌شود. پیش از شروع، یک نسخه اضطراری رمزگذاری‌شده از وضعیت فعلی ذخیره خواهد شد. تعداد رکوردهای فایل: ${pendingRestoreBackup?.metadata?.recordCount ?: 0}",
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
            // Main tabs: data backup and support contact.
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
                    // Compact overview cards. Both cards intentionally share equal width and height.
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f).height(148.dp),
                                shape = RoundedCornerShape(RadiusMD),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CloudDone,
                                            contentDescription = null,
                                            tint = EmeraldPrimaryLight,
                                            modifier = Modifier.size(21.dp)
                                        )
                                        Text(
                                            "آخرین وضعیت بکاپ",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Text(
                                        text = lastBackupDate,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f).height(148.dp),
                                shape = RoundedCornerShape(RadiusMD),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Text(
                                        "پوشه ذخیره",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = backupFolderUri?.let {
                                            com.example.data.backup.BackupFolderStorage.folderLabel(it)
                                        } ?: "هنوز انتخاب نشده",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    OutlinedButton(
                                        onClick = { backupFolderPickerLauncher.launch(null) },
                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                        shape = RoundedCornerShape(RadiusSM)
                                    ) {
                                        Text(
                                            if (backupFolderUri == null) "انتخاب پوشه" else "تغییر پوشه",
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Backup actions are separated into three UI tabs; callbacks and state remain unchanged.
                    item {
                        TabRow(
                            selectedTabIndex = selectedBackupTabIndex,
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            backupTabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedBackupTabIndex == index,
                                    onClick = { selectedBackupTabIndex = index },
                                    text = {
                                        Text(
                                            text = title,
                                            fontSize = 12.sp,
                                            fontWeight = if (selectedBackupTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selectedBackupTabIndex == index) EmeraldPrimaryLight else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                )
                            }
                        }
                    }

                    when (selectedBackupTabIndex) {
                        0 -> {
                            // Automatic local backups.
                            item {
                                SettingsSectionCard(title = "پشتیبان‌گیری خودکار محلی") {
                                    Text(
                                        text = "نسخه‌های پشتیبان روی دستگاه و بدون نیاز به اینترنت یا سرور ذخیره می‌شوند.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("بک‌آپ خودکار روزانه", fontWeight = FontWeight.SemiBold)
                                            Text(
                                                "برنامه‌ریزی روزانه؛ زمان اجرا ممکن است با محدودیت‌های اندروید کمی جابه‌جا شود (۳۰ نسخه)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = dailyAutoBackupEnabled,
                                            onCheckedChange = { enabled -> setAutomaticBackupEnabled("daily", enabled) },
                                            modifier = Modifier.testTag("auto_backup_daily_switch")
                                        )
                                    }
                                    Text(
                                        text = dailyAutoBackupStatus,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (dailyAutoBackupStatus.contains("ناموفق") || dailyAutoBackupStatus.contains("رد شده") || dailyAutoBackupStatus.contains("پیدا نشد")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("بک‌آپ خودکار هفتگی", fontWeight = FontWeight.SemiBold)
                                            Text(
                                                "برنامه‌ریزی هفتگی؛ زمان اجرا ممکن است با محدودیت‌های اندروید کمی جابه‌جا شود (۱۲ نسخه)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = weeklyAutoBackupEnabled,
                                            onCheckedChange = { enabled -> setAutomaticBackupEnabled("weekly", enabled) },
                                            modifier = Modifier.testTag("auto_backup_weekly_switch")
                                        )
                                    }
                                    Text(
                                        text = weeklyAutoBackupStatus,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (weeklyAutoBackupStatus.contains("ناموفق") || weeklyAutoBackupStatus.contains("رد شده") || weeklyAutoBackupStatus.contains("پیدا نشد")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                    )
                                    TextButton(
                                        onClick = { refreshAutomaticBackupStatus() },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("بررسی دوباره وضعیت")
                                    }
                                }
                            }

                            // Manual backup.
                            item {
                                SettingsSectionCard(title = "پشتیبان‌گیری دستی") {
                                    Button(
                                        onClick = {
                                            if (!isBackingUp) {
                                                if (!com.example.data.backup.BackupFolderStorage.hasWritableSelectedFolder(context)) {
                                                    backupFolderUri = null
                                                    Toast.makeText(context, "دسترسی پوشه پشتیبان معتبر نیست؛ لطفاً پوشه ذخیره را انتخاب کنید.", Toast.LENGTH_LONG).show()
                                                    backupFolderPickerLauncher.launch(null)
                                                } else {
                                                    shareAfterEncryption = false
                                                    backupPassphrase = ""
                                                    backupPassphraseConfirm = ""
                                                    showBackupPasswordDialog = true
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("create_backup_button"),
                                        shape = RoundedCornerShape(RadiusMD),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimaryLight),
                                        enabled = !isBackingUp
                                    ) {
                                        if (isBackingUp) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("در حال ایجاد فایل پشتیبان...")
                                        } else {
                                            Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("ایجاد فایل پشتیبان جدید", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // Restore actions retain their original handlers and confirmation flows.
                            item {
                                SettingsSectionCard(title = "بازیابی اطلاعات") {
                                    OutlinedButton(
                                        onClick = { restoreBackupLauncher.launch(arrayOf("application/octet-stream", "application/json", "*/*")) },
                                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("restore_backup_button"),
                                        enabled = !isRestoring,
                                        shape = RoundedCornerShape(RadiusMD),
                                        border = BorderStroke(1.dp, EmeraldPrimaryLight)
                                    ) {
                                        Icon(Icons.Rounded.CloudDownload, contentDescription = null, tint = EmeraldPrimaryLight, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("بازیابی از فایل پشتیبان قبلی (Restore)", color = EmeraldPrimaryLight, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { showEmergencyRestoreConfirmation = true },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        enabled = !isRestoring,
                                        shape = RoundedCornerShape(RadiusMD),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Icon(Icons.Rounded.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("بازیابی آخرین نسخه اضطراری دستگاه", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { showAutomaticRestoreConfirmation = true },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        enabled = !isRestoring,
                                        shape = RoundedCornerShape(RadiusMD),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Icon(Icons.Rounded.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("بازیابی آخرین پشتیبان خودکار روزانه/هفتگی", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        else -> {
                            // Share / transfer backup.
                            item {
                                SettingsSectionCard(title = "اشتراک‌گذاری و انتقال بکاپ") {
                                    Text(
                                        text = "فایل پشتیبان رمزگذاری‌شده را برای نگهداری یا انتقال به فضای دیگری به اشتراک بگذارید.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            if (!isBackingUp) {
                                                shareAfterEncryption = true
                                                backupPassphrase = ""
                                                backupPassphraseConfirm = ""
                                                showBackupPasswordDialog = true
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(RadiusMD),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Icon(Icons.Rounded.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("اشتراک‌گذاری فایل بک‌آپ (ارسال به تلگرام، درایو)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }

                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(RadiusMD),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                ) {
                                    Text(
                                        text = "نکته امنیتی: فایل‌های پشتیبان با رمز عبور شما رمزگذاری می‌شوند. رمز را در جای امن نگه دارید؛ برای بازیابی فایل، همان رمز لازم است.",
                                        modifier = Modifier.padding(14.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Support contact tab.
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

private const val MAX_PORTABLE_BACKUP_BYTES = 64 * 1024 * 1024

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

private fun buildManualBackupFilename(): String {
    val (year, month, day) = com.example.calendar.domain.CalendarDateUtils.getCurrentJalaliDate()
    val time = java.text.SimpleDateFormat("HHmmssSSS", java.util.Locale.ROOT).format(java.util.Date())
    return "Manual_${year}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}_$time.darino"
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
