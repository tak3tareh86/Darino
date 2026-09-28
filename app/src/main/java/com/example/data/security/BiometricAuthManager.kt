package com.example.data.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricAvailability {
    AVAILABLE,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    NONE_ENROLLED,
    UNSUPPORTED
}

class BiometricAuthManager(private val context: Context) {

    private val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK

    fun checkBiometricAvailability(): BiometricAvailability {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
            else -> BiometricAvailability.UNSUPPORTED
        }
    }

    fun isBiometricAvailable(): Boolean {
        return checkBiometricAvailability() == BiometricAvailability.AVAILABLE
    }

    fun getAvailabilityStatusMessage(): String {
        return when (checkBiometricAvailability()) {
            BiometricAvailability.AVAILABLE -> "بیومتریک روی این دستگاه فعال و آماده استفاده است."
            BiometricAvailability.NO_HARDWARE -> "بیومتریک روی این دستگاه در دسترس نیست."
            BiometricAvailability.HARDWARE_UNAVAILABLE -> "سخت‌افزار بیومتریک موقتاً در دسترس نیست."
            BiometricAvailability.NONE_ENROLLED -> "هیچ اثر انگشت یا بیومتریکی در تنظیمات دستگاه ثبت نشده است."
            BiometricAvailability.UNSUPPORTED -> "بیومتریک روی این دستگاه در دسترس نیست."
        }
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "قفل برنامه دارینو",
        subtitle: String = "برای ورود به دارینو هویت خود را تأیید کنید",
        negativeButtonText: String = "انصراف",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCanceled: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(authenticators)
            .build()

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    ) {
                        onCanceled()
                    } else {
                        onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("اثر انگشت یا بیومتریک شناسایی نشد. دوباره امتحان کنید.")
                }
            }
        )

        biometricPrompt.authenticate(promptInfo)
    }
}
