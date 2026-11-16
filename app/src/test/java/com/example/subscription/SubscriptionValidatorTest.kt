package com.example.subscription

import com.example.domain.subscription.SubscriptionInfo
import com.example.domain.subscription.SubscriptionStatus
import com.example.domain.subscription.SubscriptionValidator
import org.junit.Assert.assertEquals
import org.junit.Test

class SubscriptionValidatorTest {

    @Test
    fun testEvaluateStatus_marketPurchaseExpiresAtOneYearEvenWhenCachedAsActive() {
        val now = 2_000_000_000_000L
        val purchaseAt = now - 375 * 86_400_000L
        val info = SubscriptionInfo(
            subscriptionPurchaseAt = purchaseAt,
            subscriptionEndAt = SubscriptionValidator.calculateSubscriptionEndAt(purchaseAt),
            subscriptionStatus = SubscriptionStatus.ACTIVE,
            marketAccountState = "MARKET_ACTIVE",
            lastSyncAt = now - 60_000L,
            lastMarketVerificationAt = now - 60_000L
        )

        assertEquals(SubscriptionStatus.EXPIRED, SubscriptionValidator.evaluateStatus(info, now))
    }

    @Test
    fun testEvaluateStatus_marketPurchaseRemainsActiveBeforeOneYearEnd() {
        val now = 2_000_000_000_000L
        val purchaseAt = now - 200 * 86_400_000L
        val info = SubscriptionInfo(
            subscriptionPurchaseAt = purchaseAt,
            subscriptionEndAt = SubscriptionValidator.calculateSubscriptionEndAt(purchaseAt),
            marketAccountState = "MARKET_ACTIVE",
            lastSyncAt = now - 8 * 86_400_000L
        )

        assertEquals(SubscriptionStatus.ACTIVE, SubscriptionValidator.evaluateStatus(info, now))
    }

    @Test
    fun testEvaluateStatus_marketActiveWithoutPurchaseTimestampIsLocked() {
        val info = SubscriptionInfo(
            marketAccountState = "MARKET_ACTIVE",
            subscriptionPurchaseAt = null,
            subscriptionEndAt = null
        )

        assertEquals(SubscriptionStatus.LOCKED, SubscriptionValidator.evaluateStatus(info, 2_000_000_000_000L))
    }

    @Test
    fun testCalculateSubscriptionEndAt_addsOneCalendarYear() {
        val purchaseAt = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2024, java.util.Calendar.FEBRUARY, 29, 12, 0, 0)
        }.timeInMillis

        val expected = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2025, java.util.Calendar.FEBRUARY, 28, 12, 0, 0)
        }.timeInMillis

        assertEquals(expected, SubscriptionValidator.calculateSubscriptionEndAt(purchaseAt))
    }

    @Test
    fun testEvaluateStatus_marketConfirmationOlderThanSevenDaysRemainsActive() {
        val now = 2_000_000_000_000L
        val info = SubscriptionInfo(
            subscriptionStartAt = now - 400 * 86_400_000L,
            subscriptionEndAt = now - 35 * 86_400_000L,
            subscriptionStatus = SubscriptionStatus.ACTIVE,
            marketAccountState = "MARKET_ACTIVE",
            lastSyncAt = now - 8 * 86_400_000L,
            lastMarketVerificationAt = now - 8 * 86_400_000L
        )

        assertEquals(SubscriptionStatus.ACTIVE, SubscriptionValidator.evaluateStatus(info, now))
    }

    @Test
    fun testEvaluateStatus_marketConfirmationAtSevenDayBoundaryRemainsActive() {
        val now = 2_000_000_000_000L
        val info = SubscriptionInfo(
            marketAccountState = "MARKET_ACTIVE",
            lastSyncAt = now - 7 * 86_400_000L,
            lastMarketVerificationAt = now - 7 * 86_400_000L
        )

        assertEquals(SubscriptionStatus.ACTIVE, SubscriptionValidator.evaluateStatus(info, now))
    }

    @Test
    fun testEvaluateStatus_marketActiveWithoutVerificationTimestampRemainsActive() {
        val now = 2_000_000_000_000L
        val info = SubscriptionInfo(
            marketAccountState = "MARKET_ACTIVE",
            lastSyncAt = now - 8 * 86_400_000L,
            lastMarketVerificationAt = 0L
        )

        assertEquals(SubscriptionStatus.ACTIVE, SubscriptionValidator.evaluateStatus(info, now))
    }

    @Test
    fun testCalculateTrialDaysRemaining_validFutureEnd() {
        val now = 1000000000000L
        val end = now + (10 * 86400000L)
        val info = SubscriptionInfo(trialEndAt = end)

        val remaining = SubscriptionValidator.calculateTrialDaysRemaining(info, now)
        assertEquals(10L, remaining)
    }

    @Test
    fun testCalculateTrialDaysRemaining_expired() {
        val now = 1000000000000L
        val end = now - 1000L
        val info = SubscriptionInfo(trialEndAt = end)

        val remaining = SubscriptionValidator.calculateTrialDaysRemaining(info, now)
        assertEquals(0L, remaining)
    }

    @Test
    fun testEvaluateStatus_activeTrial() {
        val now = 1000000000000L
        val end = now + (15 * 86400000L)
        val info = SubscriptionInfo(trialEndAt = end, lastSyncAt = now)

        val status = SubscriptionValidator.evaluateStatus(info, now)
        assertEquals(SubscriptionStatus.TRIAL, status)
    }

    @Test
    fun testEvaluateStatus_activeSubscriptionOverridesTrial() {
        val now = 1000000000000L
        val trialEnd = now - 1000L
        val subEnd = now + (300 * 86400000L)
        val info = SubscriptionInfo(
            trialEndAt = trialEnd,
            subscriptionStartAt = now,
            subscriptionEndAt = subEnd,
            marketAccountState = "MARKET_ACTIVE",
            lastSyncAt = now,
            lastMarketVerificationAt = now
        )

        val status = SubscriptionValidator.evaluateStatus(info, now)
        assertEquals(SubscriptionStatus.ACTIVE, status)
    }

    @Test
    fun testEvaluateStatus_expiredTrialNoSubscription() {
        val now = 1000000000000L
        val trialEnd = now - 1000L
        val info = SubscriptionInfo(trialEndAt = trialEnd, lastSyncAt = now)

        val status = SubscriptionValidator.evaluateStatus(info, now)
        assertEquals(SubscriptionStatus.EXPIRED, status)
    }

    @Test
    fun testEvaluateStatus_clockRollbackProtection() {
        val syncTime = 1000000000000L
        val tamperedCurrentTime = syncTime - (60 * 60 * 1000L)
        val info = SubscriptionInfo(trialEndAt = syncTime + 86400000L, lastSyncAt = syncTime)

        val status = SubscriptionValidator.evaluateStatus(info, tamperedCurrentTime)
        assertEquals(SubscriptionStatus.LOCKED, status)
    }
}
