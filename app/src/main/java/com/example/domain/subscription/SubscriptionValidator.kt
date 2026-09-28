package com.example.domain.subscription

import com.example.data.subscription.MarketConfig
import com.example.util.PersianCalendarHelper
import java.util.Date

object SubscriptionValidator {

    fun calculateTrialDaysRemaining(info: SubscriptionInfo, currentTimeMillis: Long = System.currentTimeMillis()): Long {
        if (info.trialEndAt <= 0L) return 0L
        val diffMillis = info.trialEndAt - currentTimeMillis
        return if (diffMillis <= 0L) 0L else (diffMillis / (1000 * 60 * 60 * 24))
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
        // Clock tampering check: if current time is significantly before last sync time, lock or require sync
        if (info.lastSyncAt > 0L && currentTimeMillis < info.lastSyncAt - (5 * 60 * 1000L)) {
            // Clock was set backwards
            if (info.subscriptionStartAt != null && info.subscriptionEndAt != null) {
                if (currentTimeMillis <= info.subscriptionEndAt) {
                    return SubscriptionStatus.ACTIVE
                }
            }
            return SubscriptionStatus.LOCKED
        }

        // 1. Active Paid Subscription Check
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
