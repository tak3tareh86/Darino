package com.example.data.subscription

import android.content.Context
import androidx.activity.result.ActivityResultRegistry
import com.example.domain.subscription.MarketPurchaseVerifier
import com.example.domain.subscription.MockMarketPurchaseVerifier
import com.example.domain.subscription.SubscriptionInfo
import com.example.domain.subscription.SubscriptionStatus
import com.example.domain.subscription.SubscriptionValidator
import ir.cafebazaar.poolakey.entity.PurchaseInfo
import ir.cafebazaar.poolakey.entity.PurchaseState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SubscriptionRepositoryImpl(
    private val context: Context,
    private val localDataSource: LocalSubscriptionDataSource = LocalSubscriptionDataSource(context),
    private val poolakeyDataSource: PoolakeySubscriptionDataSource = PoolakeySubscriptionDataSource(context),
    private val verifier: MarketPurchaseVerifier = MockMarketPurchaseVerifier()
) : SubscriptionRepository {

    private val _subscriptionFlow = MutableStateFlow(localDataSource.getSubscriptionInfo())
    override fun observeSubscriptionInfo(): Flow<SubscriptionInfo> = _subscriptionFlow.asStateFlow()

    override suspend fun getSubscriptionInfo(): SubscriptionInfo = withContext(Dispatchers.IO) {
        val info = localDataSource.getSubscriptionInfo()
        val evaluatedStatus = SubscriptionValidator.evaluateStatus(info)
        val updated = info.copy(subscriptionStatus = evaluatedStatus)
        if (updated.subscriptionStatus != info.subscriptionStatus) {
            localDataSource.saveSubscriptionInfo(updated)
            _subscriptionFlow.value = updated
        }
        updated
    }

    override suspend fun ensureTrialStarted(userId: String): SubscriptionInfo = withContext(Dispatchers.IO) {
        val info = localDataSource.ensureTrialInitialized(userId)
        val evaluatedStatus = SubscriptionValidator.evaluateStatus(info)
        val updated = info.copy(subscriptionStatus = evaluatedStatus)
        localDataSource.saveSubscriptionInfo(updated)
        _subscriptionFlow.value = updated
        updated
    }

    override suspend fun syncWithMarket(): Result<SubscriptionInfo> = withContext(Dispatchers.IO) {
        val currentLocal = localDataSource.getSubscriptionInfo()

        when (val poolakeyResult = poolakeyDataSource.getSubscribedProducts()) {
            is PoolakeyResult.Success -> {
                val purchases = poolakeyResult.data
                val darinoPurchase = purchases.firstOrNull {
                    it.productId == MarketConfig.YEARLY_PRODUCT_ID &&
                            (it.purchaseState == PurchaseState.PURCHASED || it.purchaseState == PurchaseState.REFUNDED)
                }

                if (darinoPurchase != null && darinoPurchase.purchaseState == PurchaseState.PURCHASED) {
                    val purchaseTime = darinoPurchase.purchaseTime
                    val subStart = if (purchaseTime > 0L) purchaseTime else System.currentTimeMillis()
                    val subEnd = subStart + MarketConfig.SUBSCRIPTION_DURATION_DAYS * 86_400_000L

                    val updatedInfo = currentLocal.copy(
                        subscriptionStartAt = subStart,
                        subscriptionEndAt = subEnd,
                        subscriptionStatus = SubscriptionStatus.ACTIVE,
                        productId = MarketConfig.YEARLY_PRODUCT_ID,
                        purchaseTokenReference = darinoPurchase.purchaseToken,
                        marketAccountState = "SYNCED",
                        lastSyncAt = System.currentTimeMillis()
                    )

                    localDataSource.saveSubscriptionInfo(updatedInfo)
                    _subscriptionFlow.value = updatedInfo
                    Result.success(updatedInfo)
                } else {
                    // No valid active purchase in the current Market account
                    val evaluated = SubscriptionValidator.evaluateStatus(currentLocal)
                    val updatedInfo = currentLocal.copy(
                        subscriptionStartAt = null,
                        subscriptionEndAt = null,
                        subscriptionStatus = evaluated,
                        marketAccountState = "NO_PURCHASE_FOUND",
                        lastSyncAt = System.currentTimeMillis()
                    )

                    localDataSource.saveSubscriptionInfo(updatedInfo)
                    _subscriptionFlow.value = updatedInfo
                    Result.success(updatedInfo)
                }
            }
            is PoolakeyResult.Error -> {
                // Connection failed or market offline, evaluate using safe local cache
                val evaluated = SubscriptionValidator.evaluateStatus(currentLocal)
                val updatedInfo = currentLocal.copy(
                    subscriptionStatus = evaluated,
                    lastSyncAt = System.currentTimeMillis()
                )
                localDataSource.saveSubscriptionInfo(updatedInfo)
                _subscriptionFlow.value = updatedInfo
                Result.failure(Exception(poolakeyResult.message))
            }
        }
    }

    override suspend fun recordSuccessfulPurchase(purchaseInfo: PurchaseInfo): SubscriptionInfo = withContext(Dispatchers.IO) {
        val currentLocal = localDataSource.getSubscriptionInfo()
        val now = System.currentTimeMillis()
        val subEnd = now + MarketConfig.SUBSCRIPTION_DURATION_DAYS * 86_400_000L

        val updatedInfo = currentLocal.copy(
            subscriptionStartAt = now,
            subscriptionEndAt = subEnd,
            subscriptionStatus = SubscriptionStatus.ACTIVE,
            productId = purchaseInfo.productId,
            purchaseTokenReference = purchaseInfo.purchaseToken,
            marketAccountState = "ACTIVE",
            lastSyncAt = now
        )

        localDataSource.saveSubscriptionInfo(updatedInfo)
        _subscriptionFlow.value = updatedInfo
        updatedInfo
    }

    override fun launchPurchaseFlow(
        registry: ActivityResultRegistry,
        onFlowBegan: () -> Unit,
        onFailedToBegin: (String) -> Unit,
        onSucceed: (PurchaseInfo) -> Unit,
        onCanceled: () -> Unit,
        onFailed: (String) -> Unit
    ) {
        val payload = "darino_sub_${System.currentTimeMillis()}"
        poolakeyDataSource.subscribeProduct(
            registry = registry,
            productId = MarketConfig.YEARLY_PRODUCT_ID,
            payload = payload,
            onFlowBegan = onFlowBegan,
            onFailedToBegin = onFailedToBegin,
            onSucceed = { purchaseInfo ->
                onSucceed(purchaseInfo)
            },
            onCanceled = onCanceled,
            onFailed = onFailed
        )
    }

    override fun disconnect() {
        poolakeyDataSource.disconnect()
    }

    companion object {
        @Volatile
        private var INSTANCE: SubscriptionRepositoryImpl? = null

        fun getInstance(context: Context): SubscriptionRepositoryImpl {
            return INSTANCE ?: synchronized(this) {
                val instance = SubscriptionRepositoryImpl(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
