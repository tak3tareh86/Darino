package com.example.ui.screens.lock

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthRepositoryProvider
import com.example.data.repository.MockAuthRepository
import com.example.data.security.AppLockState
import com.example.data.security.BiometricAvailability
import com.example.data.security.BiometricAuthManager
import com.example.data.security.LocalSecurityRepository
import com.example.data.security.LocalSecuritySettings
import com.example.data.security.LockTimeoutOption
import com.example.data.security.MockLocalSecurityRepository
import com.example.data.security.UnlockMethod
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LockUiState(
    val appLockState: AppLockState = AppLockState.Checking,
    val securitySettings: LocalSecuritySettings = LocalSecuritySettings(),
    val activeUnlockMethod: UnlockMethod = UnlockMethod.PIN,
    val isBiometricAvailable: Boolean = false,
    val biometricAvailability: BiometricAvailability = BiometricAvailability.UNSUPPORTED,
    val biometricStatusMessage: String = "",
    val isAuthenticatingBiometric: Boolean = false,
    val biometricError: String? = null,
    val failedAttempts: Int = 0,
    val isTemporarilyLocked: Boolean = false,
    val lockoutSecondsRemaining: Int = 0,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val userPhone: String = ""
)

class AppLockViewModel @JvmOverloads constructor(
    application: Application,
    private val securityRepository: LocalSecurityRepository = MockLocalSecurityRepository(application),
    private val authRepository: AuthRepository = AuthRepositoryProvider.get(application)
) : AndroidViewModel(application) {

    private val biometricAuthManager = BiometricAuthManager(application)

    private val _uiState = MutableStateFlow(LockUiState())
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    private var lockoutJob: Job? = null

    init {
        refreshBiometricStatus()
        viewModelScope.launch {
            securityRepository.securitySettings.collect { settings ->
                _uiState.value = _uiState.value.copy(
                    securitySettings = settings,
                    activeUnlockMethod = determineActiveUnlockMethod(settings)
                )
            }
        }
        checkSessionAndLockStatus()
    }

    private fun refreshBiometricStatus() {
        val availability = biometricAuthManager.checkBiometricAvailability()
        _uiState.value = _uiState.value.copy(
            isBiometricAvailable = availability == BiometricAvailability.AVAILABLE,
            biometricAvailability = availability,
            biometricStatusMessage = biometricAuthManager.getAvailabilityStatusMessage()
        )
    }

    private fun determineActiveUnlockMethod(settings: LocalSecuritySettings): UnlockMethod {
        return if (settings.defaultUnlockMethod == UnlockMethod.BIOMETRIC && settings.biometricEnabled && _uiState.value.isBiometricAvailable) {
            UnlockMethod.BIOMETRIC
        } else if (settings.defaultUnlockMethod == UnlockMethod.PATTERN && settings.patternEnabled) {
            UnlockMethod.PATTERN
        } else if (settings.pinEnabled) {
            UnlockMethod.PIN
        } else if (settings.patternEnabled) {
            UnlockMethod.PATTERN
        } else if (settings.biometricEnabled && _uiState.value.isBiometricAvailable) {
            UnlockMethod.BIOMETRIC
        } else {
            UnlockMethod.PIN
        }
    }

    fun checkSessionAndLockStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(appLockState = AppLockState.Checking)
            val isLoggedIn = authRepository.isLoggedIn()
            if (!isLoggedIn) {
                _uiState.value = _uiState.value.copy(appLockState = AppLockState.Unauthenticated)
                return@launch
            }

            val user = authRepository.getCurrentUser()
            val phone = user?.phoneNumber ?: ""
            val hasLock = securityRepository.hasAnyActiveLock()

            _uiState.value = _uiState.value.copy(
                userPhone = phone,
                appLockState = if (hasLock) AppLockState.Locked else AppLockState.Unlocked
            )
        }
    }

    fun switchUnlockMethod(method: UnlockMethod) {
        _uiState.value = _uiState.value.copy(
            activeUnlockMethod = method,
            errorMessage = null,
            biometricError = null
        )
    }

    fun triggerBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        refreshBiometricStatus()
        if (!biometricAuthManager.isBiometricAvailable()) {
            val msg = biometricAuthManager.getAvailabilityStatusMessage()
            _uiState.value = _uiState.value.copy(biometricError = msg, errorMessage = msg)
            onError?.invoke(msg)
            return
        }

        _uiState.value = _uiState.value.copy(isAuthenticatingBiometric = true, biometricError = null)

        biometricAuthManager.showBiometricPrompt(
            activity = activity,
            onSuccess = {
                _uiState.value = _uiState.value.copy(isAuthenticatingBiometric = false)
                onUnlockSuccess()
                onSuccess?.invoke()
            },
            onError = { error ->
                _uiState.value = _uiState.value.copy(
                    isAuthenticatingBiometric = false,
                    biometricError = error,
                    errorMessage = error
                )
                onError?.invoke(error)
            },
            onCanceled = {
                _uiState.value = _uiState.value.copy(
                    isAuthenticatingBiometric = false,
                    biometricError = "احراز هویت بیومتریک لغو شد."
                )
            }
        )
    }

    fun toggleBiometricWithPrompt(
        activity: FragmentActivity,
        enable: Boolean,
        onComplete: (Boolean) -> Unit
    ) {
        refreshBiometricStatus()
        if (enable) {
            if (!biometricAuthManager.isBiometricAvailable()) {
                val msg = biometricAuthManager.getAvailabilityStatusMessage()
                _uiState.value = _uiState.value.copy(errorMessage = msg)
                onComplete(false)
                return
            }

            biometricAuthManager.showBiometricPrompt(
                activity = activity,
                title = "تأیید اثر انگشت / بیومتریک",
                subtitle = "برای فعال‌سازی قفل بیومتریک، اثر انگشت خود را تأیید کنید",
                onSuccess = {
                    securityRepository.setBiometricEnabled(true)
                    securityRepository.setDefaultUnlockMethod(UnlockMethod.BIOMETRIC)
                    _uiState.value = _uiState.value.copy(successMessage = "قفل بیومتریک با موفقیت فعال شد.")
                    onComplete(true)
                },
                onError = { error ->
                    _uiState.value = _uiState.value.copy(errorMessage = error)
                    onComplete(false)
                },
                onCanceled = {
                    onComplete(false)
                }
            )
        } else {
            securityRepository.setBiometricEnabled(false)
            if (_uiState.value.securitySettings.defaultUnlockMethod == UnlockMethod.BIOMETRIC) {
                securityRepository.setDefaultUnlockMethod(UnlockMethod.PIN)
            }
            _uiState.value = _uiState.value.copy(successMessage = "قفل بیومتریک غیرفعال شد.")
            onComplete(true)
        }
    }

    fun setBiometricEnabledDirectly(enabled: Boolean) {
        securityRepository.setBiometricEnabled(enabled)
        if (enabled) {
            securityRepository.setDefaultUnlockMethod(UnlockMethod.BIOMETRIC)
        } else if (_uiState.value.securitySettings.defaultUnlockMethod == UnlockMethod.BIOMETRIC) {
            securityRepository.setDefaultUnlockMethod(UnlockMethod.PIN)
        }
    }

    fun submitPin(pin: String) {
        if (_uiState.value.isTemporarilyLocked) return

        val isValid = securityRepository.verifyPin(pin)
        if (isValid) {
            onUnlockSuccess()
        } else {
            handleFailedAttempt("رمز وارد شده صحیح نیست.")
        }
    }

    fun submitPattern(patternPoints: List<Int>) {
        if (_uiState.value.isTemporarilyLocked) return

        val isValid = securityRepository.verifyPattern(patternPoints)
        if (isValid) {
            onUnlockSuccess()
        } else {
            handleFailedAttempt("الگوی وارد شده صحیح نیست.")
        }
    }

    private fun onUnlockSuccess() {
        _uiState.value = _uiState.value.copy(
            appLockState = AppLockState.Unlocked,
            failedAttempts = 0,
            errorMessage = null,
            biometricError = null,
            isTemporarilyLocked = false
        )
    }

    private fun handleFailedAttempt(message: String) {
        val attempts = _uiState.value.failedAttempts + 1
        if (attempts >= 5) {
            startLockoutTimer()
        } else {
            _uiState.value = _uiState.value.copy(
                failedAttempts = attempts,
                errorMessage = "$message ($attempts از ۵ تلاش مجاز)"
            )
        }
    }

    private fun startLockoutTimer() {
        lockoutJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isTemporarilyLocked = true,
            lockoutSecondsRemaining = 30,
            errorMessage = "بیش از حد تلاش کردید. لطفاً ۳۰ ثانیه صبر کنید."
        )

        lockoutJob = viewModelScope.launch {
            for (sec in 30 downTo 1) {
                _uiState.value = _uiState.value.copy(lockoutSecondsRemaining = sec)
                delay(1000)
            }
            _uiState.value = _uiState.value.copy(
                isTemporarilyLocked = false,
                lockoutSecondsRemaining = 0,
                failedAttempts = 0,
                errorMessage = null
            )
        }
    }

    fun setPin(pin: String) {
        securityRepository.setPin(pin)
        securityRepository.setDefaultUnlockMethod(UnlockMethod.PIN)
        _uiState.value = _uiState.value.copy(successMessage = "رمز PIN با موفقیت ثبت گردید.")
    }

    fun disablePin() {
        securityRepository.removePin()
        if (_uiState.value.securitySettings.defaultUnlockMethod == UnlockMethod.PIN) {
            securityRepository.setDefaultUnlockMethod(UnlockMethod.PATTERN)
        }
        _uiState.value = _uiState.value.copy(successMessage = "رمز PIN غیرفعال شد.")
    }

    fun setPattern(patternPoints: List<Int>) {
        securityRepository.setPattern(patternPoints)
        securityRepository.setDefaultUnlockMethod(UnlockMethod.PATTERN)
        _uiState.value = _uiState.value.copy(successMessage = "الگوی امنیتی با موفقیت ثبت گردید.")
    }

    fun disablePattern() {
        securityRepository.removePattern()
        if (_uiState.value.securitySettings.defaultUnlockMethod == UnlockMethod.PATTERN) {
            securityRepository.setDefaultUnlockMethod(UnlockMethod.PIN)
        }
        _uiState.value = _uiState.value.copy(successMessage = "الگوی امنیتی غیرفعال شد.")
    }

    fun setDefaultUnlockMethod(method: UnlockMethod) {
        securityRepository.setDefaultUnlockMethod(method)
        _uiState.value = _uiState.value.copy(activeUnlockMethod = method)
    }

    fun toggleAppLock(enable: Boolean) {
        securityRepository.setAppLockEnabled(enable)
        if (!enable) {
            _uiState.value = _uiState.value.copy(appLockState = AppLockState.Unlocked)
        }
    }

    fun setLockTimeout(timeoutOption: LockTimeoutOption) {
        securityRepository.setLockTimeoutSeconds(timeoutOption.seconds)
        _uiState.value = _uiState.value.copy(successMessage = "زمان فعال‌سازی قفل بروزرسانی شد.")
    }

    fun disableAppLock() {
        securityRepository.removePin()
        securityRepository.removePattern()
        securityRepository.setBiometricEnabled(false)
        securityRepository.setAppLockEnabled(false)
        _uiState.value = _uiState.value.copy(
            appLockState = AppLockState.Unlocked,
            successMessage = "قفل برنامه غیرفعال شد."
        )
    }

    fun onAppBackgrounded() {
        securityRepository.recordBackgroundTimestamp(System.currentTimeMillis())
    }

    fun onAppForegrounded() {
        viewModelScope.launch {
            if (authRepository.isLoggedIn()) {
                val shouldLock = securityRepository.shouldLockAppOnResume(System.currentTimeMillis())
                if (shouldLock) {
                    _uiState.value = _uiState.value.copy(appLockState = AppLockState.Locked)
                }
            } else {
                _uiState.value = _uiState.value.copy(appLockState = AppLockState.Unauthenticated)
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null, biometricError = null)
    }
}
