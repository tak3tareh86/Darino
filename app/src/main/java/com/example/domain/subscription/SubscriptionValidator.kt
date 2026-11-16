package com.example.domain.subscription

import com.example.util.PersianCalendarHelper

object SubscriptionValidator {

    private const val MARKET_OFFLINE_GRACE_PERIOD_MILLIS = 7L * 24L * 60L * 60L * 1000L

    fun calculateTrialDaysRemaining(info: SubscriptionInfo, currentTimeMillis: Long = System.currentTimeMillis()): Long {
        if (info.trialEndAt <= 0L) return 0L
        val diffMillis = info.trialEndAt - currentTimeMillis
        return if (diffMillis <= 0L) 0L else ((diffMillis + 86_400_000L - 1L) / 86_400_000L)
    }

    fun calculateSubscriptionDaysRemaining(info: SubscriptionInfo, currentTimeMillis: Long = System.currentTimeMillis()): Long {
        val endAt = info.subscriptionEndAt ?: return 0L
        val diffMillis = endAt - currentTimeMillis
        return if (diffMillis <= 0L) 0L else (diffMillis + 86_400_000L - 1L) / 86_400_000L
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

        // A successful Market confirmation grants a bounded offline grace period.
        // A missing timestamp is not proof of a recent confirmation and must fail closed.
        if (info.marketAccountState == "MARKET_ACTIVE") {
            if (info.lastSyncAt <= 0L) return SubscriptionStatus.LOCKED
            if (currentTimeMillis - info.lastSyncAt >= MARKET_OFFLINE_GRACE_PERIOD_MILLIS) {
                return SubscriptionStatus.LOCKED
            }

            // Market's current PURCHASED result is authoritative; old locally cached expiry
            // values must not override a more recent successful Market confirmation.
            return SubscriptionStatus.ACTIVE
        }

        // Trial Check
        if (info.trialEndAt > currentTimeMillis) {
            return SubscriptionStatus.TRIAL
        }

        // 3. Expired / Locked
        return SubscriptionStatus.EXPIRED
    }
}
