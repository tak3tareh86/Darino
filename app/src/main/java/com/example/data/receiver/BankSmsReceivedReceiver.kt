package com.example.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.security.SessionManager
import com.example.data.sms.BankSmsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Processes an arriving SMS against the user's saved templates, even when the
 * application process was not already running. Raw SMS content is never logged.
 */
class BankSmsReceivedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != android.provider.Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                SessionManager.init(appContext)
                val userId = SessionManager.userId ?: return@launch
                val repository = BankSmsRepository.getInstance(appContext)
                // The system delivers this broadcast immediately after receipt; a small
                // look-back accounts for provider insertion/broadcast scheduling latency.
                val newSuggestions = repository.readInboxBankSms(
                    userId,
                    receivedAfterMillis = System.currentTimeMillis() - 10 * 60 * 1000L
                )
                val processed = repository.getProcessedSmsIds(userId)
                val dismissed = repository.getDismissedSmsIds(userId)
                val current = repository.getPendingSmsSuggestions(userId)
                    .filterNot { it.id in processed || it.id in dismissed }
                val currentIds = current.mapTo(mutableSetOf()) { it.id }
                val merged = (current + newSuggestions.filterNot {
                    it.id in currentIds || it.id in processed || it.id in dismissed
                }).takeLast(50)
                repository.savePendingSmsSuggestions(userId, merged)

                // Notify an active dashboard to reload the persisted queue.
                appContext.sendBroadcast(Intent(ACTION_BANK_SMS_RECEIVED).setPackage(appContext.packageName))
            } catch (_: Exception) {
                // Never crash the receiver process or expose SMS contents in logs.
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_BANK_SMS_RECEIVED = "com.example.action.BANK_SMS_RECEIVED"
    }
}
