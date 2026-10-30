package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.data.database.AppDatabase
import com.example.data.database.UserPreferenceDao
import com.example.data.database.UserPreferenceEntity
import com.example.data.security.SessionManager
import com.example.ui.screens.settings.model.AlertDeliveryPreference
import com.example.ui.screens.settings.model.AppCurrency
import com.example.ui.screens.settings.model.AppLanguage
import com.example.ui.screens.settings.model.AppThemeMode
import com.example.ui.screens.settings.model.NavigationTransitionAnimation
import com.example.ui.screens.settings.model.NumberDisplayMode
import com.example.util.AppNumberFormatter
import com.example.util.MoneyFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Central repository for all application general preferences:
 * - Language (Persian / English)
 * - Currency (Toman / Rial)
 * - Number Display Mode (Persian / English - Persisted in Room user_preferences)
 * - Theme Mode (Light / Dark / System)
 * - Alert Delivery Preference (Both / Notification Only / SMS Only)
 */
class AppPreferencesRepository private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val userPreferenceDao: UserPreferenceDao by lazy {
        AppDatabase.getDatabase(context).userPreferenceDao()
    }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var observePreferenceJob: Job? = null
    private var currentActiveUserId: String? = null

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _language = MutableStateFlow(loadLanguage())
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _currency = MutableStateFlow(loadCurrency())
    val currency: StateFlow<AppCurrency> = _currency.asStateFlow()

    private val _numberDisplayMode = MutableStateFlow(NumberDisplayMode.PERSIAN)
    val numberDisplayMode: StateFlow<NumberDisplayMode> = _numberDisplayMode.asStateFlow()

    private val _alertDelivery = MutableStateFlow(loadAlertDelivery())
    val alertDelivery: StateFlow<AlertDeliveryPreference> = _alertDelivery.asStateFlow()

    private val _navTransition = MutableStateFlow(loadNavTransition())
    val navTransition: StateFlow<NavigationTransitionAnimation> = _navTransition.asStateFlow()

    fun isCleanSlatePerformed(): Boolean {
        return prefs.getBoolean(KEY_IS_CLEAN_SLATE, false)
    }

    fun setCleanSlatePerformed(performed: Boolean) {
        prefs.edit().putBoolean(KEY_IS_CLEAN_SLATE, performed).apply()
    }

    init {
        // Synchronize MoneyFormatter & AppNumberFormatter with active preferences
        MoneyFormatter.activeCurrency = _currency.value
        MoneyFormatter.activeLanguage = _language.value
        AppNumberFormatter.setMode(_numberDisplayMode.value)
        MoneyFormatter.activeNumberMode = _numberDisplayMode.value

        val initialUserId = SessionManager.userId
        if (!initialUserId.isNullOrBlank()) {
            loadUserPreferences(initialUserId)
        }
    }

    private fun loadThemeMode(): AppThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
        return try {
            AppThemeMode.valueOf(name)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    private fun loadLanguage(): AppLanguage {
        val name = prefs.getString(KEY_LANGUAGE, AppLanguage.PERSIAN.name) ?: AppLanguage.PERSIAN.name
        return try {
            AppLanguage.valueOf(name)
        } catch (e: Exception) {
            AppLanguage.PERSIAN
        }
    }

    private fun loadCurrency(): AppCurrency {
        val name = prefs.getString(KEY_CURRENCY, AppCurrency.TOMAN.name) ?: AppCurrency.TOMAN.name
        val parsed = try {
            AppCurrency.valueOf(name)
        } catch (e: Exception) {
            AppCurrency.TOMAN
        }
        val validCurrency = if (parsed == AppCurrency.RIAL) AppCurrency.RIAL else AppCurrency.TOMAN
        if (name != validCurrency.name) {
            prefs.edit().putString(KEY_CURRENCY, validCurrency.name).apply()
        }
        return validCurrency
    }

    private fun loadAlertDelivery(): AlertDeliveryPreference {
        val storedName = prefs.getString(KEY_ALERT_DELIVERY, AlertDeliveryPreference.BOTH.name)
            ?: AlertDeliveryPreference.BOTH.name
        val migrated = when (storedName) {
            AlertDeliveryPreference.NOTIFICATION_ONLY.name -> AlertDeliveryPreference.NOTIFICATION_ONLY
            AlertDeliveryPreference.FULL_SCREEN_ALARM.name -> AlertDeliveryPreference.FULL_SCREEN_ALARM
            AlertDeliveryPreference.BOTH.name -> AlertDeliveryPreference.BOTH
            // Preserve a usable alert method for users upgrading from the old SMS-only
            // or disabled choices; those choices no longer exist in the general menu.
            "SMS_ONLY", "DISABLED" -> AlertDeliveryPreference.NOTIFICATION_ONLY
            else -> AlertDeliveryPreference.NOTIFICATION_ONLY
        }
        if (storedName != migrated.name) {
            prefs.edit().putString(KEY_ALERT_DELIVERY, migrated.name).apply()
        }
        return migrated
    }

    private fun loadNavTransition(): NavigationTransitionAnimation {
        val name = prefs.getString(KEY_NAV_TRANSITION, NavigationTransitionAnimation.DYNAMIC.name) ?: NavigationTransitionAnimation.DYNAMIC.name
        return try {
            NavigationTransitionAnimation.valueOf(name)
        } catch (e: Exception) {
            NavigationTransitionAnimation.DYNAMIC
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun setLanguage(lang: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, lang.name).apply()
        _language.value = lang
        MoneyFormatter.activeLanguage = lang
    }

    fun setCurrency(curr: AppCurrency) {
        val valid = if (curr == AppCurrency.RIAL) AppCurrency.RIAL else AppCurrency.TOMAN
        prefs.edit().putString(KEY_CURRENCY, valid.name).apply()
        // Update the observable formatter before emitting the new preference value so
        // collectors never rebuild formatted UI state with the previous currency.
        MoneyFormatter.activeCurrency = valid
        _currency.value = valid
    }

    fun setAlertDelivery(delivery: AlertDeliveryPreference) {
        prefs.edit().putString(KEY_ALERT_DELIVERY, delivery.name).apply()
        _alertDelivery.value = delivery
    }

    fun setNavTransition(transition: NavigationTransitionAnimation) {
        prefs.edit().putString(KEY_NAV_TRANSITION, transition.name).apply()
        _navTransition.value = transition
    }

    /**
     * Connects reactively to the Room "user_preferences" table for the specified [userId].
     * Loads the saved NumberDisplayMode (defaulting to PERSIAN if none saved).
     * Automatically reacts to updates without leaking across different users.
     */
    fun loadUserPreferences(userId: String) {
        if (userId.isBlank()) {
            resetUserPreferences()
            return
        }
        if (currentActiveUserId == userId && observePreferenceJob?.isActive == true) {
            return
        }
        currentActiveUserId = userId
        observePreferenceJob?.cancel()
        observePreferenceJob = scope.launch {
            userPreferenceDao.getPreferenceFlow(userId).collect { entity ->
                val mode = entity?.numberDisplayMode?.let { modeName ->
                    try {
                        NumberDisplayMode.valueOf(modeName)
                    } catch (e: Exception) {
                        NumberDisplayMode.PERSIAN
                    }
                } ?: NumberDisplayMode.PERSIAN

                _numberDisplayMode.value = mode
                MoneyFormatter.activeNumberMode = mode
                withContext(Dispatchers.Main) {
                    AppNumberFormatter.setMode(mode)
                }
            }
        }
    }

    /**
     * Persists the selected [NumberDisplayMode] directly into the Room database
     * for the active user ([userId]), and applies it immediately to the reactive state
     * and global formatters (0ms UI lag).
     */
    fun setNumberDisplayMode(mode: NumberDisplayMode, userId: String? = null) {
        val effectiveUserId = userId ?: currentActiveUserId ?: SessionManager.userId
        _numberDisplayMode.value = mode
        MoneyFormatter.activeNumberMode = mode
        AppNumberFormatter.setMode(mode)

        if (!effectiveUserId.isNullOrBlank()) {
            currentActiveUserId = effectiveUserId
            scope.launch {
                try {
                    userPreferenceDao.insertOrUpdate(
                        UserPreferenceEntity(
                            userId = effectiveUserId,
                            numberDisplayMode = mode.name,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    android.util.Log.e("AppPreferencesRepo", "Failed to persist numberDisplayMode to Room", e)
                }
            }
        }
    }

    /**
     * Resets reactive number display state to default (PERSIAN) on logout or session reset.
     */
    fun resetUserPreferences() {
        observePreferenceJob?.cancel()
        observePreferenceJob = null
        currentActiveUserId = null
        _numberDisplayMode.value = NumberDisplayMode.PERSIAN
        MoneyFormatter.activeNumberMode = NumberDisplayMode.PERSIAN
        AppNumberFormatter.setMode(NumberDisplayMode.PERSIAN)
    }

    companion object {
        private const val PREFS_NAME = "darino_general_preferences"
        private const val KEY_THEME_MODE = "pref_theme_mode"
        private const val KEY_LANGUAGE = "pref_language"
        private const val KEY_CURRENCY = "pref_currency"
        private const val KEY_ALERT_DELIVERY = "pref_alert_delivery"
        private const val KEY_NAV_TRANSITION = "pref_nav_transition_animation"
        private const val KEY_IS_CLEAN_SLATE = "pref_is_clean_slate"

        @Volatile
        private var instance: AppPreferencesRepository? = null

        fun getInstance(context: Context): AppPreferencesRepository {
            return instance ?: synchronized(this) {
                instance ?: AppPreferencesRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
