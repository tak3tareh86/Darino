package com.example.domain.subscription

import com.example.data.subscription.MarketConfig
import com.example.util.PersianCalendarHelper
import java.util.Date

object SubscriptionValidator {

    private const val MARKET_VERIFICATION_MAX_AGE_MILLIS = 24 * 60 * 60 * 1000L

    fun calculateTrialDaysRemaining(info: SubscriptionInfo, currentTimeMillis: Long = System.currentTimeMillis()): Long {
        if (info.trialEndAt <= 0L) return 0L
        val diffMillis = info.trialEndAt - currentTimeMillis
        return if (diffMillis <= 0L) 0L else ((diffMillis + 86_400_000L - 1L) / 86_400_000L)
    }

    fun calculateSubscriptionDaysRemaining(info: SubscriptionInfo, currentTimeMillis: Long = System.currentTimeMillis()): Long {
        val endAt = info.subscriptionEndAt ?: return 0L
        val diffMillis = endAt - currentTimeMillis
        return if (diffMillis <= 0L) 0L else (diffMillis / (1000 * 60 * 60 * 24))
    }

    fun formatEndJalaliDate(timestampMillis: Long): String {
        if (timestampMillis <= 0L) return "-"
        return PersianCalendarHelper.fromEpochMillis(timestampMillis).toFormattedDate()
    }

    /**
     * Determines current subscription status based on Trial time, Active subscription time,
     * and clock manipulation safeguards.
     */
    fun evaluateStatus(info: SubscriptionInfo, currentTimeMillis: Long = System.currentTimeMillis()): SubscriptionStatus {
        // An active-looking cached timestamp is not sufficient when Market could not provide
        // the purchase timestamp needed to establish the entitlement period.
        if (info.marketAccountState == "PURCHASE_TIME_UNAVAILABLE") {
            return SubscriptionStatus.LOCKED
        }

        // A device clock moved materially backwards since the last trusted Market sync.
        // Do not grant access based on the now-manipulated local clock.
        if (info.lastSyncAt > 0L && currentTimeMillis < info.lastSyncAt - (5 * 60 * 1000L)) {
            return SubscriptionStatus.LOCKED
        }

        // Poolakey getSubscribedProducts() reports currently active subscriptions.
        // Trust that Market confirmation for a bounded cache window, even if purchaseTime
        // still points to the original cycle and its locally estimated 365-day end has passed.
        val marketVerificationFresh =
            info.marketAccountState == "MARKET_ACTIVE" &&
                currentTimeMillis - info.lastSyncAt <= MARKET_VERIFICATION_MAX_AGE_MILLIS
        if (marketVerificationFresh) {
            return SubscriptionStatus.ACTIVE
        }

        // 1. Active Paid Subscription Check (local fallback when Market is unavailable)
        if (info.subscriptionEndAt != null && info.subscriptionEndAt > currentTimeMillis) {
            return SubscriptionStatus.ACTIVE
        }

        // 2. Trial Check
        if (info.trialEndAt > currentTimeMillis) {
            return SubscriptionStatus.TRIAL
        }

        // 3. Expired / Locked
        return SubscriptionStatus.EXPIRED
    }
}
