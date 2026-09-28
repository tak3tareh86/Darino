package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.screens.settings.model.AlertDeliveryPreference
import com.example.ui.screens.settings.model.AppCurrency
import com.example.ui.screens.settings.model.AppLanguage
import com.example.ui.screens.settings.model.AppThemeMode
import com.example.util.MoneyFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Central repository for all application general preferences:
 * - Language (Persian / English)
 * - Currency (Toman / Rial)
 * - Theme Mode (Light / Dark / System)
 * - Alert Delivery Preference (Both / Notification Only / SMS Only)
 */
class AppPreferencesRepository private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _language = MutableStateFlow(loadLanguage())
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _currency = MutableStateFlow(loadCurrency())
    val currency: StateFlow<AppCurrency> = _currency.asStateFlow()

    private val _alertDelivery = MutableStateFlow(loadAlertDelivery())
    val alertDelivery: StateFlow<AlertDeliveryPreference> = _alertDelivery.asStateFlow()

    init {
        // Synchronize MoneyFormatter with persisted preferences
        MoneyFormatter.activeCurrency = _currency.value
        MoneyFormatter.activeLanguage = _language.value
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
        return try {
            AppCurrency.valueOf(name)
        } catch (e: Exception) {
            AppCurrency.TOMAN
        }
    }

    private fun loadAlertDelivery(): AlertDeliveryPreference {
        val name = prefs.getString(KEY_ALERT_DELIVERY, AlertDeliveryPreference.BOTH.name) ?: AlertDeliveryPreference.BOTH.name
        return try {
            AlertDeliveryPreference.valueOf(name)
        } catch (e: Exception) {
            AlertDeliveryPreference.BOTH
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
        prefs.edit().putString(KEY_CURRENCY, curr.name).apply()
        _currency.value = curr
        MoneyFormatter.activeCurrency = curr
    }

    fun setAlertDelivery(delivery: AlertDeliveryPreference) {
        prefs.edit().putString(KEY_ALERT_DELIVERY, delivery.name).apply()
        _alertDelivery.value = delivery
    }

    companion object {
        private const val PREFS_NAME = "darino_general_preferences"
        private const val KEY_THEME_MODE = "pref_theme_mode"
        private const val KEY_LANGUAGE = "pref_language"
        private const val KEY_CURRENCY = "pref_currency"
        private const val KEY_ALERT_DELIVERY = "pref_alert_delivery"

        @Volatile
        private var instance: AppPreferencesRepository? = null

        fun getInstance(context: Context): AppPreferencesRepository {
            return instance ?: synchronized(this) {
                instance ?: AppPreferencesRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
