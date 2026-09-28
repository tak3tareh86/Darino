package com.example.data.subscription

import android.content.Context
import android.content.SharedPreferences
import com.example.domain.subscription.SubscriptionInfo
import com.example.domain.subscription.SubscriptionStatus

class LocalSubscriptionDataSource(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "darino_subscription_prefs"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_TRIAL_START_AT = "trial_start_at"
        private const val KEY_TRIAL_END_AT = "trial_end_at"
        private const val KEY_SUB_START_AT = "sub_start_at"
        private const val KEY_SUB_END_AT = "sub_end_at"
        private const val KEY_SUB_STATUS = "sub_status"
        private const val KEY_PRODUCT_ID = "product_id"
        private const val KEY_PURCHASE_TOKEN = "purchase_token"
        private const val KEY_MARKET_ACCOUNT_STATE = "market_account_state"
        private const val KEY_LAST_SYNC_AT = "last_sync_at"
    }

    /**
     * Initializes or returns the trial start time.
     * Enforces that trialStartAt is set ONCE upon first successful registration/login and is preserved.
     */
    fun ensureTrialInitialized(userId: String): SubscriptionInfo {
        val existingUserId = prefs.getString(KEY_USER_ID, null)
        val existingTrialStart = prefs.getLong(KEY_TRIAL_START_AT, 0L)

        val trialStart: Long
        val trialEnd: Long

        if (existingTrialStart > 0L) {
            trialStart = existingTrialStart
            trialEnd = prefs.getLong(KEY_TRIAL_END_AT, trialStart + MarketConfig.TRIAL_DURATION_DAYS * 86_400_000L)
        } else {
            trialStart = System.currentTimeMillis()
            trialEnd = trialStart + MarketConfig.TRIAL_DURATION_DAYS * 86_400_000L
            prefs.edit()
                .putString(KEY_USER_ID, userId)
                .putLong(KEY_TRIAL_START_AT, trialStart)
                .putLong(KEY_TRIAL_END_AT, trialEnd)
                .apply()
        }

        return getSubscriptionInfo().copy(
            userId = userId,
            trialStartAt = trialStart,
            trialEndAt = trialEnd
        )
    }

    fun getSubscriptionInfo(): SubscriptionInfo {
        val userId = prefs.getString(KEY_USER_ID, "") ?: ""
        val trialStart = prefs.getLong(KEY_TRIAL_START_AT, 0L)
        val trialEnd = prefs.getLong(KEY_TRIAL_END_AT, 0L)
        val subStart = prefs.getLong(KEY_SUB_START_AT, -1L).let { if (it == -1L) null else it }
        val subEnd = prefs.getLong(KEY_SUB_END_AT, -1L).let { if (it == -1L) null else it }
        val statusName = prefs.getString(KEY_SUB_STATUS, SubscriptionStatus.CHECKING.name) ?: SubscriptionStatus.CHECKING.name
        val status = try { SubscriptionStatus.valueOf(statusName) } catch (e: Exception) { SubscriptionStatus.CHECKING }
        val productId = prefs.getString(KEY_PRODUCT_ID, MarketConfig.YEARLY_PRODUCT_ID) ?: MarketConfig.YEARLY_PRODUCT_ID
        val token = prefs.getString(KEY_PURCHASE_TOKEN, null)
        val marketAccountState = prefs.getString(KEY_MARKET_ACCOUNT_STATE, "ACTIVE") ?: "ACTIVE"
        val lastSyncAt = prefs.getLong(KEY_LAST_SYNC_AT, System.currentTimeMillis())

        return SubscriptionInfo(
            userId = userId,
            trialStartAt = trialStart,
            trialEndAt = trialEnd,
            subscriptionStartAt = subStart,
            subscriptionEndAt = subEnd,
            subscriptionStatus = status,
            productId = productId,
            purchaseTokenReference = token,
            marketAccountState = marketAccountState,
            lastSyncAt = lastSyncAt
        )
    }

    fun saveSubscriptionInfo(info: SubscriptionInfo) {
        prefs.edit()
            .putString(KEY_USER_ID, info.userId)
            .putLong(KEY_TRIAL_START_AT, info.trialStartAt)
            .putLong(KEY_TRIAL_END_AT, info.trialEndAt)
            .putLong(KEY_SUB_START_AT, info.subscriptionStartAt ?: -1L)
            .putLong(KEY_SUB_END_AT, info.subscriptionEndAt ?: -1L)
            .putString(KEY_SUB_STATUS, info.subscriptionStatus.name)
            .putString(KEY_PRODUCT_ID, info.productId)
            .putString(KEY_PURCHASE_TOKEN, info.purchaseTokenReference)
            .putString(KEY_MARKET_ACCOUNT_STATE, info.marketAccountState)
            .putLong(KEY_LAST_SYNC_AT, info.lastSyncAt)
            .apply()
    }
}
