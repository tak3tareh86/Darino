package com.example.ui.screens.subscription

import com.example.data.subscription.MarketConfig
import com.example.domain.subscription.SubscriptionInfo
import com.example.domain.subscription.SubscriptionStatus

data class SubscriptionState(
    val info: SubscriptionInfo = SubscriptionInfo(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val trialDaysRemaining: Long = 0L,
    val trialEndJalaliDate: String = "",
    val subscriptionDaysRemaining: Long = 0L,
    val subscriptionEndJalaliDate: String = "",
    val priceDisplayToman: Long = MarketConfig.DISPLAY_PRICE_TOMAN,
    val userMessage: String? = null,
    val isError: Boolean = false
) {
    val isLocked: Boolean
        get() = info.subscriptionStatus == SubscriptionStatus.EXPIRED || info.subscriptionStatus == SubscriptionStatus.LOCKED

    val isWarningTrial: Boolean
        get() = info.subscriptionStatus == SubscriptionStatus.TRIAL && trialDaysRemaining in 1..7
}
