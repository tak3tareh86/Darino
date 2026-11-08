package com.example.domain.subscription

import androidx.activity.result.ActivityResultRegistry
import com.example.data.subscription.SubscriptionRepository
import ir.cafebazaar.poolakey.entity.PurchaseInfo
import kotlinx.coroutines.flow.Flow

class SubscriptionManager(private val repository: SubscriptionRepository) {

    fun observeSubscriptionInfo(): Flow<SubscriptionInfo> = repository.observeSubscriptionInfo()

    suspend fun getSubscriptionInfo(): SubscriptionInfo = repository.getSubscriptionInfo()

    suspend fun ensureTrialStarted(userId: String): SubscriptionInfo = repository.ensureTrialStarted(userId)

    suspend fun syncWithMarket(): Result<SubscriptionInfo> = repository.syncWithMarket()

    suspend fun recordSuccessfulPurchase(purchaseInfo: PurchaseInfo): SubscriptionInfo =
        repository.recordSuccessfulPurchase(purchaseInfo)

    fun launchPurchaseFlow(
        registry: ActivityResultRegistry,
        onFlowBegan: () -> Unit,
        onFailedToBegin: (String) -> Unit,
        onSucceed: (PurchaseInfo) -> Unit,
        onCanceled: () -> Unit,
        onFailed: (String) -> Unit
    ) {
        repository.launchPurchaseFlow(
            registry = registry,
            onFlowBegan = onFlowBegan,
            onFailedToBegin = onFailedToBegin,
            onSucceed = onSucceed,
            onCanceled = onCanceled,
            onFailed = onFailed
        )
    }

    fun disconnect() = repository.disconnect()
}
