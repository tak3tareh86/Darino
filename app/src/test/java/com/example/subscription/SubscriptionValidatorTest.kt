package com.example.subscription

import com.example.domain.subscription.SubscriptionInfo
import com.example.domain.subscription.SubscriptionStatus
import com.example.domain.subscription.SubscriptionValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionValidatorTest {

    @Test
    fun testCalculateTrialDaysRemaining_validFutureEnd() {
        val now = 1000000000000L
        val end = now + (10 * 86400000L) // 10 days later
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
            lastSyncAt = now
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
        val tamperedCurrentTime = syncTime - (60 * 60 * 1000L) // Clock turned back 1 hour
        val info = SubscriptionInfo(trialEndAt = syncTime + 86400000L, lastSyncAt = syncTime)

        val status = SubscriptionValidator.evaluateStatus(info, tamperedCurrentTime)
        assertEquals(SubscriptionStatus.LOCKED, status)
    }
}
