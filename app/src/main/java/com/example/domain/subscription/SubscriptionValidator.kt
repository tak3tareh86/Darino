package com.example.domain.subscription

import com.example.util.PersianCalendarHelper

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

        // Only a recent Poolakey-verified purchase or subscription query can confirm paid access.
        // Never infer an annual subscription's expiry from purchaseTime: it remains the
        // original signup timestamp after renewals.
        if (info.marketAccountState == "MARKET_ACTIVE") {
            val verificationAge = currentTimeMillis - info.lastSyncAt
            return if (info.lastSyncAt > 0L &&
                verificationAge in 0..MARKET_VERIFICATION_MAX_AGE_MILLIS
            ) {
                SubscriptionStatus.ACTIVE
            } else {
                SubscriptionStatus.LOCKED
            }
        }

        // Trial Check
        if (info.trialEndAt > currentTimeMillis) {
            return SubscriptionStatus.TRIAL
        }

        // 3. Expired / Locked
        return SubscriptionStatus.EXPIRED
    }
}
