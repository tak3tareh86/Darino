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
 * - Alert Delivery Preference (Notification Only / Full-screen Alarm / Both)
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

    // Global master switches are opt-in. Per-reminder channel selection is also required.
    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, false))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()
    private val _smsNotificationsEnabled = MutableStateFlow(prefs.getBoolean(KEY_SMS_NOTIFICATIONS_ENABLED, false))
    val smsNotificationsEnabled: StateFlow<Boolean> = _smsNotificationsEnabled.asStateFlow()
    private val _notificationSoundEnabled = MutableStateFlow(prefs.getBoolean(KEY_NOTIFICATION_SOUND_ENABLED, true))
    val notificationSoundEnabled: StateFlow<Boolean> = _notificationSoundEnabled.asStateFlow()
    private val _notificationVibrationEnabled = MutableStateFlow(prefs.getBoolean(KEY_NOTIFICATION_VIBRATION_ENABLED, true))
    val notificationVibrationEnabled: StateFlow<Boolean> = _notificationVibrationEnabled.asStateFlow()

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
        MoneyFormatter.activeLanguage = AppLanguage.PERSIAN
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

    private fun loadLanguage(): AppLanguage = AppLanguage.PERSIAN

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
        val storedName = prefs.getString(KEY_ALERT_DELIVERY, null)
        val migrationDone = prefs.getBoolean(KEY_ALERT_DELIVERY_V2_MIGRATED, false)

        val selected = if (!migrationDone) {
            // The old BOTH value meant SMS + notification, not full-screen alarm.
            // Never silently opt existing users into an intrusive full-screen alert.
            val migrated = when (storedName) {
                AlertDeliveryPreference.NOTIFICATION_ONLY.name -> AlertDeliveryPreference.NOTIFICATION_ONLY
                AlertDeliveryPreference.FULL_SCREEN_ALARM.name -> AlertDeliveryPreference.FULL_SCREEN_ALARM
                else -> AlertDeliveryPreference.NOTIFICATION_ONLY
            }
            prefs.edit()
                .putString(KEY_ALERT_DELIVERY, migrated.name)
                .putBoolean(KEY_ALERT_DELIVERY_V2_MIGRATED, true)
                .apply()
            migrated
        } else {
            storedName?.let {
                try {
                    AlertDeliveryPreference.valueOf(it)
                } catch (_: IllegalArgumentException) {
                    AlertDeliveryPreference.NOTIFICATION_ONLY
                }
            } ?: AlertDeliveryPreference.NOTIFICATION_ONLY
        }
        return selected
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

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
        _notificationsEnabled.value = enabled
    }

    fun setSmsNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SMS_NOTIFICATIONS_ENABLED, enabled).apply()
        _smsNotificationsEnabled.value = enabled
    }

    fun setNotificationSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_SOUND_ENABLED, enabled).apply()
        _notificationSoundEnabled.value = enabled
    }

    fun setNotificationVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_VIBRATION_ENABLED, enabled).apply()
        _notificationVibrationEnabled.value = enabled
    }

    fun setAlertDelivery(delivery: AlertDeliveryPreference) {
        prefs.edit()
            .putString(KEY_ALERT_DELIVERY, delivery.name)
            .putBoolean(KEY_ALERT_DELIVERY_V2_MIGRATED, true)
            .apply()
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
        private const val KEY_ALERT_DELIVERY_V2_MIGRATED = "pref_alert_delivery_v2_migrated"
        private const val KEY_NOTIFICATIONS_ENABLED = "pref_notifications_enabled"
        private const val KEY_SMS_NOTIFICATIONS_ENABLED = "pref_sms_notifications_enabled"
        private const val KEY_NOTIFICATION_SOUND_ENABLED = "pref_notification_sound_enabled"
        private const val KEY_NOTIFICATION_VIBRATION_ENABLED = "pref_notification_vibration_enabled"
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
