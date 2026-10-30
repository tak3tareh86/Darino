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
    companion object { private const val LOCKOUT_DURATION_MILLIS = 30_000L }

    init {
        refreshBiometricStatus()
        viewModelScope.launch {
            securityRepository.securitySettings.collect { settings ->
                _uiState.value = _uiState.value.copy(
                    securitySettings = settings,
                    activeUnlockMethod = determineActiveUnlockMethod(settings)
                )
                val selectedAvailable = when (settings.defaultUnlockMethod) {
                    UnlockMethod.PIN -> settings.pinEnabled
                    UnlockMethod.PATTERN -> settings.patternEnabled
                    UnlockMethod.BIOMETRIC -> settings.biometricEnabled && _uiState.value.isBiometricAvailable
                }
                if ((settings.pinEnabled || settings.patternEnabled || settings.biometricEnabled) && !selectedAvailable) {
                    fallbackUnlockMethod(settings.defaultUnlockMethod)?.let(securityRepository::setDefaultUnlockMethod)
                }
            }
        }
        _uiState.value = _uiState.value.copy(failedAttempts = securityRepository.getFailedAttempts())
        restoreLockoutState()
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
                fallbackUnlockMethod(UnlockMethod.BIOMETRIC)?.let(securityRepository::setDefaultUnlockMethod)
            }
            _uiState.value = _uiState.value.copy(successMessage = "قفل بیومتریک غیرفعال شد.")
            onComplete(true)
        }
    }

    fun setBiometricEnabledDirectly(enabled: Boolean) {
        if (enabled && !_uiState.value.isBiometricAvailable) return
        securityRepository.setBiometricEnabled(enabled)
        if (enabled) {
            securityRepository.setDefaultUnlockMethod(UnlockMethod.BIOMETRIC)
        } else if (_uiState.value.securitySettings.defaultUnlockMethod == UnlockMethod.BIOMETRIC) {
            fallbackUnlockMethod(UnlockMethod.BIOMETRIC)?.let(securityRepository::setDefaultUnlockMethod)
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
        securityRepository.resetFailedAttempts()
        _uiState.value = _uiState.value.copy(appLockState = AppLockState.Unlocked, failedAttempts = 0,
            errorMessage = null, biometricError = null, isTemporarilyLocked = false, lockoutSecondsRemaining = 0)
    }

    private fun handleFailedAttempt(message: String) {
        val attempts = securityRepository.incrementFailedAttempts()
        _uiState.value = _uiState.value.copy(failedAttempts = attempts)
        if (attempts >= 5) {
            val until = System.currentTimeMillis() + LOCKOUT_DURATION_MILLIS
            securityRepository.setLockoutUntilTimestamp(until)
            startLockoutTimer(until)
        } else _uiState.value = _uiState.value.copy(errorMessage = "$message ($attempts از ۵ تلاش مجاز)")
    }

    private fun restoreLockoutState() {
        val now = System.currentTimeMillis()
        val until = securityRepository.getLockoutUntilTimestamp()
        when {
            until > now -> startLockoutTimer(until)
            until > 0L -> {
                securityRepository.resetFailedAttempts()
                _uiState.value = _uiState.value.copy(failedAttempts = 0)
            }
            securityRepository.getFailedAttempts() >= 5 -> {
                val newUntil = now + LOCKOUT_DURATION_MILLIS
                securityRepository.setLockoutUntilTimestamp(newUntil)
                startLockoutTimer(newUntil)
            }
            else -> _uiState.value = _uiState.value.copy(failedAttempts = securityRepository.getFailedAttempts())
        }
    }

    private fun startLockoutTimer(until: Long) {
        lockoutJob?.cancel()
        val remaining = ((until - System.currentTimeMillis() + 999L) / 1000L).toInt().coerceAtLeast(0)
        if (remaining == 0) {
            securityRepository.resetFailedAttempts()
            _uiState.value = _uiState.value.copy(isTemporarilyLocked = false, failedAttempts = 0, lockoutSecondsRemaining = 0)
            return
        }
        _uiState.value = _uiState.value.copy(isTemporarilyLocked = true, lockoutSecondsRemaining = remaining,
            failedAttempts = 5, errorMessage = "بیش از حد تلاش کردید. لطفاً تا پایان زمان مسدودی صبر کنید.")
        lockoutJob = viewModelScope.launch {
            while (true) {
                val left = ((until - System.currentTimeMillis() + 999L) / 1000L).toInt()
                if (left <= 0) break
                _uiState.value = _uiState.value.copy(lockoutSecondsRemaining = left)
                delay(minOf(1000L, until - System.currentTimeMillis()).coerceAtLeast(1L))
            }
            securityRepository.resetFailedAttempts()
            _uiState.value = _uiState.value.copy(isTemporarilyLocked = false, lockoutSecondsRemaining = 0,
                failedAttempts = 0, errorMessage = null)
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
            fallbackUnlockMethod(UnlockMethod.PIN)?.let(securityRepository::setDefaultUnlockMethod)
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
            fallbackUnlockMethod(UnlockMethod.PATTERN)?.let(securityRepository::setDefaultUnlockMethod)
        }
        _uiState.value = _uiState.value.copy(successMessage = "الگوی امنیتی غیرفعال شد.")
    }

    fun setDefaultUnlockMethod(method: UnlockMethod) {
        val settings = _uiState.value.securitySettings
        val available = when (method) {
            UnlockMethod.PIN -> settings.pinEnabled
            UnlockMethod.PATTERN -> settings.patternEnabled
            UnlockMethod.BIOMETRIC -> settings.biometricEnabled && _uiState.value.isBiometricAvailable
        }
        if (!available) return
        securityRepository.setDefaultUnlockMethod(method)
        _uiState.value = _uiState.value.copy(activeUnlockMethod = method)
    }

    private fun fallbackUnlockMethod(excluding: UnlockMethod): UnlockMethod? {
        val settings = _uiState.value.securitySettings
        return listOf(UnlockMethod.PIN, UnlockMethod.PATTERN, UnlockMethod.BIOMETRIC).firstOrNull { method ->
            method != excluding && when (method) {
                UnlockMethod.PIN -> settings.pinEnabled
                UnlockMethod.PATTERN -> settings.patternEnabled
                UnlockMethod.BIOMETRIC -> settings.biometricEnabled && _uiState.value.isBiometricAvailable
            }
        }
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
        securityRepository.resetFailedAttempts()
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
