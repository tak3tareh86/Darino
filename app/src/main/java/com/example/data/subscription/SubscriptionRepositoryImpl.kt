package com.example.data.subscription

import android.content.Context
import androidx.activity.result.ActivityResultRegistry
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
    private val poolakeyDataSource: PoolakeySubscriptionDataSource = PoolakeySubscriptionDataSource(context)
) : SubscriptionRepository {

    private val _subscriptionFlow = MutableStateFlow(localDataSource.getSubscriptionInfo())
    override fun observeSubscriptionInfo(): Flow<SubscriptionInfo> = _subscriptionFlow.asStateFlow()

    override suspend fun getSubscriptionInfo(): SubscriptionInfo = withContext(Dispatchers.IO) {
        val info = localDataSource.getSubscriptionInfo()
        val updated = info.copy(subscriptionStatus = SubscriptionValidator.evaluateStatus(info))
        if (updated.subscriptionStatus != info.subscriptionStatus) {
            localDataSource.saveSubscriptionInfo(updated)
            _subscriptionFlow.value = updated
        }
        updated
    }

    override suspend fun ensureTrialStarted(userId: String): SubscriptionInfo = withContext(Dispatchers.IO) {
        val info = localDataSource.ensureTrialInitialized(userId)
        val updated = info.copy(subscriptionStatus = SubscriptionValidator.evaluateStatus(info))
        localDataSource.saveSubscriptionInfo(updated)
        _subscriptionFlow.value = updated
        updated
    }

    /** Restores the subscription directly from Bazaar/Poolakey without a Darino backend. */
    override suspend fun syncWithMarket(): Result<SubscriptionInfo> = withContext(Dispatchers.IO) {
        val currentLocal = localDataSource.getSubscriptionInfo()
        when (val marketResult = poolakeyDataSource.getSubscribedProducts()) {
            is PoolakeyResult.Success -> {
                val darinoPurchases = marketResult.data.filter { it.productId == MarketConfig.YEARLY_PRODUCT_ID }
                val purchase = darinoPurchases.firstOrNull {
                    it.purchaseState == PurchaseState.PURCHASED && it.purchaseToken.isNotBlank()
                } ?: darinoPurchases.firstOrNull { it.purchaseState == PurchaseState.REFUNDED }

                val now = System.currentTimeMillis()
                val updated = when (purchase?.purchaseState) {
                    PurchaseState.PURCHASED -> {
                        val purchaseAt = purchase.purchaseTime.takeIf { it > 0L }
                            ?: currentLocal.subscriptionPurchaseAt
                        currentLocal.copy(
                            subscriptionStartAt = purchaseAt ?: currentLocal.subscriptionStartAt,
                            subscriptionPurchaseAt = purchaseAt,
                            // Poolakey confirms active state but does not expose a reliable expiry.
                            subscriptionEndAt = null,
                            productId = purchase.productId,
                            purchaseTokenReference = purchase.purchaseToken,
                            marketAccountState = "MARKET_ACTIVE",
                            lastSyncAt = now,
                            subscriptionStatus = SubscriptionStatus.ACTIVE
                        )
                    }
                    PurchaseState.REFUNDED -> currentLocal.copy(
                        subscriptionStartAt = null,
                        subscriptionPurchaseAt = null,
                        subscriptionEndAt = null,
                        productId = "",
                        purchaseTokenReference = null,
                        marketAccountState = "REFUNDED",
                        lastSyncAt = now,
                        subscriptionStatus = SubscriptionStatus.EXPIRED
                    )
                    else -> {
                        val cleared = currentLocal.copy(
                            subscriptionStartAt = null,
                            subscriptionPurchaseAt = null,
                            subscriptionEndAt = null,
                            productId = "",
                            purchaseTokenReference = null,
                            marketAccountState = "NO_PURCHASE_FOUND",
                            lastSyncAt = now
                        )
                        cleared.copy(subscriptionStatus = SubscriptionValidator.evaluateStatus(cleared, now))
                    }
                }
                localDataSource.saveSubscriptionInfo(updated)
                _subscriptionFlow.value = updated
                Result.success(updated)
            }
            is PoolakeyResult.Error -> {
                // Retain the last Market-confirmed local state while the Market app is unreachable.
                val cached = currentLocal.copy(
                    subscriptionStatus = SubscriptionValidator.evaluateStatus(currentLocal)
                )
                localDataSource.saveSubscriptionInfo(cached)
                _subscriptionFlow.value = cached
                Result.failure(Exception(marketResult.message, marketResult.throwable))
            }
        }
    }

    override suspend fun getAnnualSubscriptionPrice(): Result<String> = withContext(Dispatchers.IO) {
        when (val result = poolakeyDataSource.getSubscriptionSkuDetails()) {
            is PoolakeyResult.Success -> {
                val price = result.data
                    .firstOrNull { it.sku == MarketConfig.YEARLY_PRODUCT_ID }
                    ?.price
                    ?.takeIf { it.isNotBlank() }
                if (price != null) Result.success(price)
                else Result.failure(IllegalStateException("قیمت اشتراک سالیانه در بازار ثبت نشده است."))
            }
            is PoolakeyResult.Error -> Result.failure(Exception(result.message, result.throwable))
        }
    }

    override suspend fun recordSuccessfulPurchase(purchaseInfo: PurchaseInfo): SubscriptionInfo = withContext(Dispatchers.IO) {
        require(purchaseInfo.productId == MarketConfig.YEARLY_PRODUCT_ID) {
            "شناسه محصول خرید با اشتراک سالیانه دارینو مطابقت ندارد."
        }
        require(purchaseInfo.purchaseState == PurchaseState.PURCHASED && purchaseInfo.purchaseToken.isNotBlank()) {
            "اطلاعات خرید معتبر نیست."
        }

        val now = System.currentTimeMillis()
        val purchaseAt = purchaseInfo.purchaseTime.takeIf { it > 0L }
        val currentLocal = localDataSource.getSubscriptionInfo()
        val candidate = currentLocal.copy(
            subscriptionStartAt = purchaseAt ?: currentLocal.subscriptionStartAt,
            subscriptionPurchaseAt = purchaseAt ?: currentLocal.subscriptionPurchaseAt,
            subscriptionEndAt = null,
            productId = purchaseInfo.productId,
            purchaseTokenReference = purchaseInfo.purchaseToken,
            marketAccountState = "MARKET_ACTIVE",
            lastSyncAt = now
        )
        val updated = candidate.copy(
            subscriptionStatus = SubscriptionValidator.evaluateStatus(candidate, now)
        )
        localDataSource.saveSubscriptionInfo(updated)
        _subscriptionFlow.value = updated
        updated
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
            onSucceed = onSucceed,
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
