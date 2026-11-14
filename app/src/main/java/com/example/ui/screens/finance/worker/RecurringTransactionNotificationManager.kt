package com.example.ui.screens.finance.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject

/**
 * Durable, user-scoped pending occurrences. Pending entries are not financial transactions:
 * only the explicit confirm action may turn one into a transaction.
 */
object RecurringTransactionNotificationManager {
    private const val PREFS_NAME = "darino_recurring_pending_notifications"
    private const val CHANNEL_ID = "recurring_transaction_confirmation"
    private const val CHANNEL_NAME = "تأیید تراکنش‌های دوره‌ای"

    private fun storageKey(userId: String) = "pending_$userId"

    @Synchronized
    fun savePending(context: Context, userId: String, occurrenceId: String, payload: JSONObject): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val current = JSONArray(prefs.getString(storageKey(userId), "[]") ?: "[]")
            for (index in 0 until current.length()) {
                if (current.optJSONObject(index)?.optString("occurrenceId") == occurrenceId) return true
            }
            current.put(JSONObject(payload.toString()))
            prefs.edit().putString(storageKey(userId), current.toString()).commit()
        } catch (_: Exception) {
            false
        }
    }

    @Synchronized
    fun getPending(context: Context, userId: String, occurrenceId: String): JSONObject? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val current = JSONArray(prefs.getString(storageKey(userId), "[]") ?: "[]")
            for (index in 0 until current.length()) {
                val item = current.optJSONObject(index) ?: continue
                if (item.optString("occurrenceId") == occurrenceId) return JSONObject(item.toString())
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    fun hasPending(context: Context, userId: String, occurrenceId: String): Boolean =
        getPending(context, userId, occurrenceId) != null

    @Synchronized
    fun removePending(context: Context, userId: String, occurrenceId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = JSONArray(prefs.getString(storageKey(userId), "[]") ?: "[]")
        val updated = JSONArray()
        for (index in 0 until current.length()) {
            val item = current.optJSONObject(index) ?: continue
            if (item.optString("occurrenceId") != occurrenceId) updated.put(item)
        }
        prefs.edit().putString(storageKey(userId), updated.toString()).apply()
    }

    fun showPending(context: Context, userId: String, occurrenceId: String) {
        val payload = getPending(context, userId, occurrenceId) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            manager.getNotificationChannel(CHANNEL_ID) == null
        ) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            )
        }

        val requestCode = occurrenceId.hashCode() and 0x3fffffff
        val confirmIntent = Intent(context, RecurringTransactionActionReceiver::class.java).apply {
            action = RecurringTransactionActionReceiver.ACTION_CONFIRM
            putExtra(RecurringTransactionActionReceiver.EXTRA_USER_ID, userId)
            putExtra(RecurringTransactionActionReceiver.EXTRA_OCCURRENCE_ID, occurrenceId)
        }
        val remindIntent = Intent(context, RecurringTransactionActionReceiver::class.java).apply {
            action = RecurringTransactionActionReceiver.ACTION_REMIND_LATER
            putExtra(RecurringTransactionActionReceiver.EXTRA_USER_ID, userId)
            putExtra(RecurringTransactionActionReceiver.EXTRA_OCCURRENCE_ID, occurrenceId)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val confirmPendingIntent = PendingIntent.getBroadcast(context, requestCode * 2, confirmIntent, flags)
        val remindPendingIntent = PendingIntent.getBroadcast(context, requestCode * 2 + 1, remindIntent, flags)

        val title = payload.optString("title").ifBlank { "تراکنش دوره‌ای" }
        val date = payload.optString("datePersian")
        val amount = payload.optLong("amount")
        val amountText = "%,d".format(amount)
        val typeTitle = if (payload.optString("type") == "INCOME") "درآمد" else "هزینه"
        val details = "$typeTitle: $title\nمبلغ: $amountText\nموعد: $date\nبرای ثبت در سوابق مالی، تأیید کنید."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("تأیید تراکنش دوره‌ای")
            .setContentText("$title • $date")
            .setStyle(NotificationCompat.BigTextStyle().bigText(details))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_menu_save, "تأیید و ثبت", confirmPendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "یادآوری فردا", remindPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId(occurrenceId), notification)
        } catch (_: SecurityException) {
            // Keep the pending occurrence durable; a later worker/app launch can retry notification.
        }
    }

    fun cancel(context: Context, occurrenceId: String) {
        NotificationManagerCompat.from(context).cancel(notificationId(occurrenceId))
    }

    fun notificationId(occurrenceId: String): Int = occurrenceId.hashCode() and 0x7fffffff
}
