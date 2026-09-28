package com.example.data.subscription

import androidx.activity.result.ActivityResultRegistry
import com.example.domain.subscription.SubscriptionInfo
import ir.cafebazaar.poolakey.entity.PurchaseInfo
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    fun observeSubscriptionInfo(): Flow<SubscriptionInfo>
    suspend fun getSubscriptionInfo(): SubscriptionInfo
    suspend fun ensureTrialStarted(userId: String): SubscriptionInfo
    suspend fun syncWithMarket(): Result<SubscriptionInfo>
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
