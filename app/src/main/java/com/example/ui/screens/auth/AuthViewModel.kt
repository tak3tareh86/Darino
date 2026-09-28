package com.example.ui.screens.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserProfile
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthRepositoryProvider
import com.example.data.repository.PasswordRecoveryResult
import com.example.data.subscription.SubscriptionRepositoryImpl
import com.example.domain.subscription.SubscriptionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AuthTab {
    LOGIN,
    REGISTER
}

data class AuthUiState(
    val selectedTab: AuthTab = AuthTab.LOGIN,

    // Login inputs
    val loginUsername: String = "",
    val loginPassword: String = "",
    val isLoginLoading: Boolean = false,
    val loginError: String? = null,

    // Register inputs
    val registerEmail: String = "",
    val registerUsername: String = "",
    val registerPassword: String = "",
    val registerConfirmPassword: String = "",
    val isRegisterLoading: Boolean = false,
    val registerError: String? = null,

    // Session state
    val isAuthenticated: Boolean = false,
    val loggedInUser: UserProfile? = null,
    val generalErrorMessage: String? = null,
    val successMessage: String? = null,

    // Forgot Password Sheet / Recovery
    val showForgotPasswordSheet: Boolean = false,
    val forgotPasswordEmail: String = "",
    val isForgotPasswordLoading: Boolean = false,
    val forgotPasswordError: String? = null,
    val recoveredCredentials: PasswordRecoveryResult? = null
) {
    val canSubmitLogin: Boolean
        get() = loginUsername.trim().isNotBlank() && loginPassword.isNotBlank() && !isLoginLoading

    val canSubmitRegister: Boolean
        get() = registerEmail.trim().isNotBlank() &&
                registerUsername.trim().isNotBlank() &&
                registerPassword.isNotBlank() &&
                registerConfirmPassword.isNotBlank() &&
                !isRegisterLoading
}

class AuthViewModel @JvmOverloads constructor(
    application: Application,
    private val authRepository: AuthRepository = AuthRepositoryProvider.get(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkInitialSession()
    }

    private fun checkInitialSession() {
        viewModelScope.launch {
            val isLogged = authRepository.isLoggedIn()
            if (isLogged) {
                val user = authRepository.getCurrentUser()
                if (user != null) {
                    try {
                        SubscriptionManager(SubscriptionRepositoryImpl.getInstance(getApplication())).ensureTrialStarted(user.id)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w("AuthViewModel", "Non-fatal trial initialization issue during session restore", e)
                    }
                }
                _uiState.value = _uiState.value.copy(
                    isAuthenticated = true,
                    loggedInUser = user
                )
            }
        }
    }

    fun selectTab(tab: AuthTab) {
        _uiState.value = _uiState.value.copy(
            selectedTab = tab,
            loginError = null,
            registerError = null,
            generalErrorMessage = null,
            successMessage = null
        )
    }

    // Preserves exact uppercase/lowercase characters
    fun onLoginUsernameChanged(username: String) {
        val clean = username.trim().filter { it.isLetterOrDigit() || it == '_' }
        _uiState.value = _uiState.value.copy(
            loginUsername = clean,
            loginError = null,
            generalErrorMessage = null
        )
    }

    fun onLoginPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(
            loginPassword = password,
            loginError = null,
            generalErrorMessage = null
        )
    }

    fun onRegisterEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(
            registerEmail = email.trim(),
            registerError = null,
            generalErrorMessage = null
        )
    }

    // Preserves exact uppercase/lowercase characters
    fun onRegisterUsernameChanged(username: String) {
        val clean = username.trim().filter { it.isLetterOrDigit() || it == '_' }
        _uiState.value = _uiState.value.copy(
            registerUsername = clean,
            registerError = null,
            generalErrorMessage = null
        )
    }

    fun onRegisterPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(
            registerPassword = password,
            registerError = null,
            generalErrorMessage = null
        )
    }

    fun onRegisterConfirmPasswordChanged(confirmPassword: String) {
        _uiState.value = _uiState.value.copy(
            registerConfirmPassword = confirmPassword,
            registerError = null,
            generalErrorMessage = null
        )
    }

    fun login() {
        val state = _uiState.value
        val username = state.loginUsername.trim() // Preserves exact case
        val password = state.loginPassword

        if (username.isBlank()) {
            _uiState.value = state.copy(loginError = "لطفاً نام کاربری را وارد کنید.")
            return
        }

        if (password.isBlank()) {
            _uiState.value = state.copy(loginError = "لطفاً رمز عبور را وارد کنید.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoginLoading = true,
                loginError = null,
                generalErrorMessage = null
            )

            val result = authRepository.login(username, password)
            result.fold(
                onSuccess = { user ->
                    try {
                        SubscriptionManager(SubscriptionRepositoryImpl.getInstance(getApplication())).ensureTrialStarted(user.id)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w("AuthViewModel", "Non-fatal trial initialization issue during login", e)
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoginLoading = false,
                        isAuthenticated = true,
                        loggedInUser = user,
                        successMessage = "ورود با موفقیت انجام شد."
                    )
                },
                onFailure = { error ->
                    val msg = error.localizedMessage ?: "نام کاربری یا رمز عبور اشتباه است."
                    _uiState.value = _uiState.value.copy(
                        isLoginLoading = false,
                        loginError = msg,
                        generalErrorMessage = msg
                    )
                }
            )
        }
    }

    fun register() {
        val state = _uiState.value
        val email = state.registerEmail.trim().lowercase()
        val username = state.registerUsername.trim() // Preserves exact case
        val password = state.registerPassword
        val confirmPassword = state.registerConfirmPassword

        if (email.isBlank()) {
            _uiState.value = state.copy(registerError = "لطفاً جیمیل یا ایمیل خود را وارد کنید.")
            return
        }

        val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        if (!email.matches(emailRegex)) {
            _uiState.value = state.copy(registerError = "فرمت ایمیل نامعتبر است (مثال: user@gmail.com).")
            return
        }

        if (username.isBlank()) {
            _uiState.value = state.copy(registerError = "لطفاً نام کاربری را وارد کنید.")
            return
        }

        if (username.length < 3) {
            _uiState.value = state.copy(registerError = "نام کاربری حداقل باید ۳ کاراکتر باشد.")
            return
        }

        if (!username.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            _uiState.value = state.copy(registerError = "نام کاربری فقط می‌تواند شامل حروف انگلیسی، اعداد و زیرخط باشد.")
            return
        }

        if (password.length < 8) {
            _uiState.value = state.copy(registerError = "رمز عبور باید حداقل ۸ کاراکتر باشد.")
            return
        }

        if (password != confirmPassword) {
            _uiState.value = state.copy(registerError = "رمز عبور و تکرار آن یکسان نیستند.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRegisterLoading = true,
                registerError = null,
                generalErrorMessage = null
            )

            val result = authRepository.register(username, password, email)
            result.fold(
                onSuccess = { user ->
                    try {
                        SubscriptionManager(SubscriptionRepositoryImpl.getInstance(getApplication())).ensureTrialStarted(user.id)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w("AuthViewModel", "Non-fatal trial initialization issue during registration", e)
                    }
                    _uiState.value = _uiState.value.copy(
                        isRegisterLoading = false,
                        isAuthenticated = true,
                        loggedInUser = user,
                        successMessage = "حساب کاربری با موفقیت ساخته شد."
                    )
                },
                onFailure = { error ->
                    val msg = error.localizedMessage ?: "خطا در ثبت‌نام کاربر."
                    _uiState.value = _uiState.value.copy(
                        isRegisterLoading = false,
                        registerError = msg,
                        generalErrorMessage = msg
                    )
                }
            )
        }
    }

    fun onForgotPasswordEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(
            forgotPasswordEmail = email.trim(),
            forgotPasswordError = null
        )
    }

    fun showForgotPasswordSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(
            showForgotPasswordSheet = show,
            forgotPasswordEmail = "",
            forgotPasswordError = null,
            recoveredCredentials = null
        )
    }

    fun recoverPassword(onSuccess: (PasswordRecoveryResult) -> Unit) {
        val email = _uiState.value.forgotPasswordEmail.trim().lowercase()
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(forgotPasswordError = "لطفاً ایمیل یا جیمیل خود را وارد کنید.")
            return
        }
        val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        if (!email.matches(emailRegex)) {
            _uiState.value = _uiState.value.copy(forgotPasswordError = "فرمت ایمیل نامعتبر است (مثال: user@gmail.com).")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isForgotPasswordLoading = true,
                forgotPasswordError = null
            )
            val result = authRepository.recoverPasswordByEmail(email)
            result.fold(
                onSuccess = { recovery ->
                    _uiState.value = _uiState.value.copy(
                        isForgotPasswordLoading = false,
                        recoveredCredentials = recovery,
                        loginUsername = recovery.username,
                        loginPassword = recovery.newTemporaryPassword
                    )
                    onSuccess(recovery)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isForgotPasswordLoading = false,
                        forgotPasswordError = error.localizedMessage ?: "خطا در بازیابی رمز عبور."
                    )
                }
            )
        }
    }

    fun logout() {
        // Immediately reset UI state so screen switches to login screen in 0ms without delay
        _uiState.value = AuthUiState(
            isAuthenticated = false,
            loggedInUser = null
        )
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
