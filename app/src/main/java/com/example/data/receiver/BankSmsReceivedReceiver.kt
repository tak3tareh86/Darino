package com.example.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives the system SMS notification only to trigger the app's existing inbox scan.
 * SMS content is deliberately not parsed, persisted, or logged here.
 */
class BankSmsReceivedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != android.provider.Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val refreshIntent = Intent(ACTION_BANK_SMS_RECEIVED).setPackage(context.packageName)
        context.sendBroadcast(refreshIntent)
    }

    companion object {
        const val ACTION_BANK_SMS_RECEIVED = "com.example.action.BANK_SMS_RECEIVED"
    }
}
