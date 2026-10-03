package com.example.data.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.screens.home.domain.BankSmsSuggestion
import com.example.util.MoneyFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

class BankSmsRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("darino_bank_sms_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PROCESSED_SMS_IDS = "processed_sms_ids"
        private const val KEY_DISMISSED_SMS_IDS = "dismissed_sms_ids"
        private const val KEY_PERMISSION_REQUESTED = "permission_requested_before"

        @Volatile
        private var INSTANCE: BankSmsRepository? = null

        fun getInstance(context: Context): BankSmsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BankSmsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun wasPermissionRequestedBefore(): Boolean {
        return prefs.getBoolean(KEY_PERMISSION_REQUESTED, false)
    }

    fun markPermissionRequested() {
        prefs.edit().putBoolean(KEY_PERMISSION_REQUESTED, true).apply()
    }

    fun getProcessedSmsIds(): Set<String> {
        return prefs.getStringSet(KEY_PROCESSED_SMS_IDS, emptySet()) ?: emptySet()
    }

    fun getDismissedSmsIds(): Set<String> {
        return prefs.getStringSet(KEY_DISMISSED_SMS_IDS, emptySet()) ?: emptySet()
    }

    fun markSmsProcessed(smsId: String) {
        val current = getProcessedSmsIds().toMutableSet()
        current.add(smsId)
        prefs.edit().putStringSet(KEY_PROCESSED_SMS_IDS, current).apply()
    }

    fun markSmsDismissed(smsId: String) {
        val current = getDismissedSmsIds().toMutableSet()
        current.add(smsId)
        prefs.edit().putStringSet(KEY_DISMISSED_SMS_IDS, current).apply()
    }

    suspend fun readInboxBankSms(): List<BankSmsSuggestion> = withContext(Dispatchers.IO) {
        if (!hasSmsPermission()) {
            return@withContext emptyList()
        }

        val processedIds = getProcessedSmsIds()
        val dismissedIds = getDismissedSmsIds()
        val ignoredIds = processedIds + dismissedIds

        val results = mutableListOf<BankSmsSuggestion>()

        try {
            val uri = Uri.parse("content://sms/inbox")
            val projection = arrayOf("_id", "address", "body", "date")
            val cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "date DESC LIMIT 50"
            )

            cursor?.use { c ->
                val idIdx = c.getColumnIndex("_id")
                val addressIdx = c.getColumnIndex("address")
                val bodyIdx = c.getColumnIndex("body")
                val dateIdx = c.getColumnIndex("date")

                while (c.moveToNext()) {
                    val id = if (idIdx != -1) c.getString(idIdx) else null
                    val address = if (addressIdx != -1) c.getString(addressIdx) else ""
                    val body = if (bodyIdx != -1) c.getString(bodyIdx) else ""
                    val timestamp = if (dateIdx != -1) c.getLong(dateIdx) else System.currentTimeMillis()

                    val uniqueId = id ?: "sms_${address}_$timestamp"
                    if (ignoredIds.contains(uniqueId)) {
                        continue
                    }

                    if (BankSmsParser.isPotentialBankSms(address, body)) {
                        val parsed = BankSmsParser.parse(uniqueId, address, body, timestamp)
                        if (parsed != null) {
                            results.add(parsed)
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.w("BankSmsRepository", "Permission revoked while querying SMS inbox", e)
        } catch (e: Exception) {
            Log.e("BankSmsRepository", "Error reading SMS inbox: ${e.message}", e)
        }

        return@withContext results
    }

    fun generateSimulatedSms(): BankSmsSuggestion {
        val banks = listOf("ملت", "ملی", "سامان", "پاسارگاد", "تجارت", "صادرات", "رسالت", "بلوبانک")
        val randomBank = banks.random()
        val randomAmount = Random.nextLong(25_000, 1_850_000)

        val types = listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER)
        val selectedType = types.random()

        val id = "sim_${System.currentTimeMillis()}_${Random.nextInt(100, 999)}"
        val timeNow = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val jalaliToday = com.example.util.PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()

        val typeText = when (selectedType) {
            TransactionType.EXPENSE -> "برداشت"
            TransactionType.INCOME -> "واریز"
            TransactionType.TRANSFER -> "انتقال وجه"
        }

        val formattedAmount = MoneyFormatter.formatSignedToman(randomAmount, isExpense = (selectedType == TransactionType.EXPENSE))
        val body = buildString {
            append("بانک $randomBank\n")
            append("$typeText: $formattedAmount\n")
            append("کارت: *۴۵۸۲\n")
            if (selectedType == TransactionType.TRANSFER) {
                append("به حساب: *۸۹۱۰ (شبا)\n")
            }
            append("مانده: ${MoneyFormatter.formatToman(4_200_000L)}")
        }

        val category = when (selectedType) {
            TransactionType.EXPENSE -> listOf("سوپرمارکت و خرید", "غذا و رستوران", "خودرو و سوخت", "خرید روزمره").random()
            TransactionType.INCOME -> "درآمد و واریز"
            TransactionType.TRANSFER -> "انتقال بین‌بانکی"
        }

        return BankSmsSuggestion(
            id = id,
            bankName = "بانک $randomBank",
            amount = randomAmount,
            formattedAmount = formattedAmount,
            type = selectedType,
            smsText = body,
            dateText = jalaliToday,
            timeText = timeNow,
            category = category,
            sourceAccount = "بانک $randomBank (*۴۵۸۲)",
            destinationAccount = if (selectedType == TransactionType.TRANSFER) "کارت مقصد (*۸۹۱۰)" else null,
            rawSender = "بانک $randomBank"
        )
    }
}
