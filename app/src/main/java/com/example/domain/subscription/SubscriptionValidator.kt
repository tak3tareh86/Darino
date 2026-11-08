package com.example.domain.subscription

import com.example.util.PersianCalendarHelper

object SubscriptionValidator {

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

        // The current PURCHASED state returned by Bazaar/Poolakey is authoritative.
        // No remote verification server or synthetic expiry timestamp is used.
        if (info.marketAccountState == "MARKET_ACTIVE") {
            val verificationAge = currentTimeMillis - info.lastSyncAt
            val verifiedExpiry = info.subscriptionEndAt
            return when {
                verifiedExpiry != null && verifiedExpiry > 0L && currentTimeMillis >= verifiedExpiry -> SubscriptionStatus.EXPIRED
                else -> SubscriptionStatus.ACTIVE
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
