package com.example.domain.subscription

import com.example.util.PersianCalendarHelper
import java.util.Calendar
import java.util.TimeZone

object SubscriptionValidator {

    fun calculateTrialDaysRemaining(info: SubscriptionInfo, currentTimeMillis: Long = System.currentTimeMillis()): Long {
        if (info.trialEndAt <= 0L) return 0L
        val diffMillis = info.trialEndAt - currentTimeMillis
        return if (diffMillis <= 0L) 0L else ((diffMillis + 86_400_000L - 1L) / 86_400_000L)
    }

    /**
     * Calculates the end of a one-year manual subscription from its purchase timestamp.
     * UTC is used so the result does not depend on the device's current time zone.
     */
    fun calculateSubscriptionEndAt(purchaseAtMillis: Long): Long? {
        if (purchaseAtMillis <= 0L) return null
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = purchaseAtMillis
            add(Calendar.YEAR, 1)
        }.timeInMillis
    }

    fun calculateSubscriptionDaysRemaining(info: SubscriptionInfo, currentTimeMillis: Long = System.currentTimeMillis()): Long {
        val endAt = info.subscriptionEndAt
            ?: info.subscriptionPurchaseAt?.let(::calculateSubscriptionEndAt)
            ?: return 0L
        val diffMillis = endAt - currentTimeMillis
        return if (diffMillis <= 0L) 0L else (diffMillis + 86_400_000L - 1L) / 86_400_000L
    }

    fun formatEndJalaliDate(timestampMillis: Long): String {
        if (timestampMillis <= 0L) return "-"
        return PersianCalendarHelper.fromEpochMillis(timestampMillis).toFormattedDate()
    }

    /**
     * Determines current subscription status based on the seven-day trial and the
     * one-year manual Market subscription period. Cached purchases remain usable offline,
     * but never beyond their locally calculated annual end date.
     */
    fun evaluateStatus(info: SubscriptionInfo, currentTimeMillis: Long = System.currentTimeMillis()): SubscriptionStatus {
        if (info.marketAccountState == "PURCHASE_TIME_UNAVAILABLE") {
            return SubscriptionStatus.LOCKED
        }

        // A device clock moved materially backwards since the last trusted Market sync.
        if (info.lastSyncAt > 0L && currentTimeMillis < info.lastSyncAt - (5 * 60 * 1000L)) {
            return SubscriptionStatus.LOCKED
        }

        if (info.marketAccountState == "MARKET_ACTIVE") {
            val endAt = info.subscriptionEndAt
                ?: info.subscriptionPurchaseAt?.let(::calculateSubscriptionEndAt)
                ?: return SubscriptionStatus.LOCKED
            return if (currentTimeMillis < endAt) {
                SubscriptionStatus.ACTIVE
            } else {
                SubscriptionStatus.EXPIRED
            }
        }

        if (info.trialEndAt > currentTimeMillis) {
            return SubscriptionStatus.TRIAL
        }

        return SubscriptionStatus.EXPIRED
    }
}
