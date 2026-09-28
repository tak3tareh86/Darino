package com.example.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

/**
 * Helper class for Android SMS Retriever API.
 * Listens for incoming SMS containing OTP code using zero-permission Google Play Services API.
 * Automatically extracts 6-digit code and invokes callback.
 */
class SmsRetrieverHelper(
    private val context: Context,
    private val onOtpExtracted: (String) -> Unit
) {
    private var broadcastReceiver: BroadcastReceiver? = null
    private var isListening = false

    fun startListening() {
        if (isListening) return

        try {
            val client = SmsRetriever.getClient(context)
            val task = client.startSmsRetriever()

            task.addOnSuccessListener {
                Log.i(TAG, "SMS Retriever task started successfully.")
                registerReceiver()
            }

            task.addOnFailureListener { e ->
                Log.w(TAG, "Failed to start SMS Retriever task: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SMS Retriever", e)
        }
    }

    private fun registerReceiver() {
        if (isListening) return

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent == null || SmsRetriever.SMS_RETRIEVED_ACTION != intent.action) return

                val extras = intent.extras ?: return
                val status = extras.get(SmsRetriever.EXTRA_STATUS) as? Status ?: return

                when (status.statusCode) {
                    CommonStatusCodes.SUCCESS -> {
                        val message = extras.getString(SmsRetriever.EXTRA_SMS_MESSAGE)
                        Log.i(TAG, "SMS retrieved automatically via SMS Retriever API.")
                        if (!message.isNullOrBlank()) {
                            val otp = extractOtpCode(message)
                            if (otp != null && otp.length == 6) {
                                onOtpExtracted(otp)
                            }
                        }
                    }
                    CommonStatusCodes.TIMEOUT -> {
                        Log.i(TAG, "SMS Retriever timed out waiting for SMS.")
                        stopListening()
                    }
                }
            }
        }

        val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(broadcastReceiver, intentFilter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(broadcastReceiver, intentFilter)
        }
        isListening = true
    }

    fun stopListening() {
        if (!isListening) return
        try {
            broadcastReceiver?.let {
                context.unregisterReceiver(it)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Receiver already unregistered or exception: ${e.message}")
        } finally {
            broadcastReceiver = null
            isListening = false
        }
    }

    private fun extractOtpCode(message: String): String? {
        val regex = Regex("\\b\\d{6}\\b")
        val match = regex.find(message)
        return match?.value
    }

    companion object {
        private const val TAG = "SmsRetrieverHelper"
    }
}
