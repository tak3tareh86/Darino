package com.example.data.subscription

import android.content.Context
import com.example.domain.subscription.SubscriptionStatus
import com.example.domain.subscription.SubscriptionValidator

/**
 * Single source of truth for the paid-only backup entitlement.
 *
 * Trial access is deliberately excluded. A cached Market purchase is accepted only when
 * its yearly product, purchase token, purchase timestamp and Market verification evidence
 * are present and the annual entitlement has not expired.
 */
object BackupSubscriptionAccess {

    fun hasActiveYearlySubscription(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        val info = LocalSubscriptionDataSource(context).getSubscriptionInfo()
        return info.productId == MarketConfig.YEARLY_PRODUCT_ID &&
            info.marketAccountState == "MARKET_ACTIVE" &&
            !info.purchaseTokenReference.isNullOrBlank() &&
            (info.subscriptionPurchaseAt ?: 0L) > 0L &&
            info.lastMarketVerificationAt > 0L &&
            SubscriptionValidator.evaluateStatus(info, now) == SubscriptionStatus.ACTIVE
    }

    fun requireActiveYearlySubscription(context: Context) {
        check(hasActiveYearlySubscription(context)) {
            "این قابلیت فقط برای خریداران دارای اشتراک سالیانه معتبر و تأییدشده کافه‌بازار فعال است."
        }
    }
}
