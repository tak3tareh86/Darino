package com.example.data.subscription

import androidx.activity.result.ActivityResultRegistry
import com.example.domain.subscription.SubscriptionInfo
import ir.cafebazaar.poolakey.entity.PurchaseInfo
import kotlinx.coroutines.flow.Flow

/** A Bazaar-provided display price; cached values are informational only. */
data class AnnualSubscriptionPrice(
    val display: String,
    val isCached: Boolean,
    val fetchedAtMillis: Long
)

interface SubscriptionRepository {
    fun observeSubscriptionInfo(): Flow<SubscriptionInfo>
    suspend fun getSubscriptionInfo(): SubscriptionInfo
    suspend fun ensureTrialStarted(userId: String): SubscriptionInfo
    suspend fun syncWithMarket(): Result<SubscriptionInfo>
    suspend fun getAnnualSubscriptionPrice(): Result<AnnualSubscriptionPrice> =
        Result.failure(UnsupportedOperationException("دریافت قیمت اشتراک پشتیبانی نمی‌شود."))
    suspend fun recordSuccessfulPurchase(purchaseInfo: PurchaseInfo): SubscriptionInfo
    fun launchPurchaseFlow(
        registry: ActivityResultRegistry,
        onFlowBegan: () -> Unit,
        onFailedToBegin: (String) -> Unit,
        onSucceed: (PurchaseInfo) -> Unit,
        onCanceled: () -> Unit,
        onFailed: (String) -> Unit
    )
    fun disconnect()
}
