package com.example.data.subscription

import android.content.Context
import androidx.activity.result.ActivityResultRegistry
import com.example.domain.subscription.MarketPurchaseVerifier
import com.example.domain.subscription.ProductionMarketPurchaseVerifier
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
    private val verifier: MarketPurchaseVerifier = ProductionMarketPurchaseVerifier()
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
                val now = System.currentTimeMillis()
                val darinoPurchases = purchases.filter {
                    it.productId == MarketConfig.YEARLY_PRODUCT_ID
                }
                // Prefer a purchased item; a refunded item must never grant access.
                val darinoPurchase = darinoPurchases.firstOrNull {
                    it.purchaseState == PurchaseState.PURCHASED
                } ?: darinoPurchases.firstOrNull {
                    it.purchaseState == PurchaseState.REFUNDED
                }

                if (darinoPurchase?.purchaseState == PurchaseState.PURCHASED) {
                    // Poolakey's getSubscribedProducts() returns the user's currently active
                    // subscriptions. Its presence is the authoritative current entitlement signal.
                    // purchaseTime is not an expiry timestamp and may refer to the original
                    // subscription even after renewals, so never use it to override Market's
                    // active-subscription result.
                    val purchaseTime = darinoPurchase.purchaseTime.takeIf { it > 0L }
                    val subEnd = purchaseTime?.let {
                        it + MarketConfig.SUBSCRIPTION_DURATION_DAYS * 86_400_000L
                    } ?: currentLocal.subscriptionEndAt
                    val candidate = currentLocal.copy(
                        subscriptionStartAt = purchaseTime ?: currentLocal.subscriptionStartAt,
                        subscriptionEndAt = subEnd,
                        productId = MarketConfig.YEARLY_PRODUCT_ID,
                        purchaseTokenReference = darinoPurchase.purchaseToken,
                        marketAccountState = "MARKET_ACTIVE",
                        lastSyncAt = maxOf(currentLocal.lastSyncAt, now)
                    )
                    val updatedInfo = candidate.copy(
                        subscriptionStatus = SubscriptionValidator.evaluateStatus(candidate, now)
                    )
                    localDataSource.saveSubscriptionInfo(updatedInfo)
                    _subscriptionFlow.value = updatedInfo
                    Result.success(updatedInfo)
                } else {
                    // A successful Market query is authoritative: revoke the cached entitlement.
                    val withoutSubscription = currentLocal.copy(
                        subscriptionStartAt = null,
                        subscriptionEndAt = null,
                        productId = "",
                        purchaseTokenReference = null,
                        marketAccountState = if (darinoPurchase?.purchaseState == PurchaseState.REFUNDED) {
                            "REFUNDED"
                        } else {
                            "NO_PURCHASE_FOUND"
                        },
                        lastSyncAt = maxOf(currentLocal.lastSyncAt, now)
                    )
                    val revoked = withoutSubscription.copy(
                        subscriptionStatus = SubscriptionValidator.evaluateStatus(withoutSubscription, now)
                    )
                    localDataSource.saveSubscriptionInfo(revoked)
                    _subscriptionFlow.value = revoked
                    Result.success(revoked)
                }
            }
            is PoolakeyResult.Error -> {
                // Connection failed or market offline, evaluate using safe local cache
                val evaluated = SubscriptionValidator.evaluateStatus(currentLocal)
                val updatedInfo = currentLocal.copy(
                    subscriptionStatus = evaluated,
                    // A failed query must not refresh the age of the last successful Market verification.
                    lastSyncAt = currentLocal.lastSyncAt
                )
                localDataSource.saveSubscriptionInfo(updatedInfo)
                _subscriptionFlow.value = updatedInfo
                Result.failure(Exception(poolakeyResult.message))
            }
        }
    }

    override suspend fun recordSuccessfulPurchase(purchaseInfo: PurchaseInfo): SubscriptionInfo = withContext(Dispatchers.IO) {
        require(purchaseInfo.productId == MarketConfig.YEARLY_PRODUCT_ID) {
            "شناسه محصول خرید با اشتراک سالیانه دارینو مطابقت ندارد."
        }
        require(purchaseInfo.purchaseState == PurchaseState.PURCHASED && purchaseInfo.purchaseToken.isNotBlank()) {
            "اطلاعات خرید معتبر نیست."
        }
        val currentLocal = localDataSource.getSubscriptionInfo()
        val now = System.currentTimeMillis()
        val purchaseStart = purchaseInfo.purchaseTime.takeIf { it > 0L } ?: now
        val subEnd = purchaseStart + MarketConfig.SUBSCRIPTION_DURATION_DAYS * 86_400_000L

        val updatedInfo = currentLocal.copy(
            subscriptionStartAt = purchaseStart,
            subscriptionEndAt = subEnd,
            subscriptionStatus = SubscriptionValidator.evaluateStatus(
                currentLocal.copy(subscriptionStartAt = purchaseStart, subscriptionEndAt = subEnd),
                now
            ),
            productId = purchaseInfo.productId,
            purchaseTokenReference = purchaseInfo.purchaseToken,
            marketAccountState = "MARKET_ACTIVE",
            lastSyncAt = maxOf(currentLocal.lastSyncAt, now)
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
