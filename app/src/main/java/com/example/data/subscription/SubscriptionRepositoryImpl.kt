package com.example.data.subscription

import android.content.Context
import androidx.activity.result.ActivityResultRegistry
import com.example.data.api.ApiClient
import com.example.data.api.NetworkBazaarSubscriptionDto
import com.example.data.api.NetworkVerifyBazaarSubscriptionRequest
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
                val darinoPurchases = purchases.filter { it.productId == MarketConfig.YEARLY_PRODUCT_ID }
                val darinoPurchase = darinoPurchases.firstOrNull {
                    it.purchaseState == PurchaseState.PURCHASED && it.purchaseToken.isNotBlank()
                } ?: darinoPurchases.firstOrNull { it.purchaseState == PurchaseState.REFUNDED }

                try {
                    val snapshot = when (darinoPurchase?.purchaseState) {
                        PurchaseState.PURCHASED -> verifyWithBackend(darinoPurchase.productId, darinoPurchase.purchaseToken)
                        PurchaseState.REFUNDED -> null
                        else -> getCurrentFromBackend()
                    }

                    if (snapshot == null) {
                        val now = System.currentTimeMillis()
                        val cleared = currentLocal.copy(
                            subscriptionStartAt = null,
                            subscriptionPurchaseAt = null,
                            subscriptionEndAt = null,
                            productId = "",
                            purchaseTokenReference = null,
                            marketAccountState = if (darinoPurchase?.purchaseState == PurchaseState.REFUNDED) "REFUNDED" else "NO_PURCHASE_FOUND",
                            lastSyncAt = now,
                            subscriptionStatus = if (darinoPurchase?.purchaseState == PurchaseState.REFUNDED) {
                                SubscriptionStatus.EXPIRED
                            } else {
                                SubscriptionValidator.evaluateStatus(
                                    currentLocal.copy(
                                        subscriptionStartAt = null,
                                        subscriptionEndAt = null,
                                        productId = "",
                                        purchaseTokenReference = null,
                                        marketAccountState = "NO_PURCHASE_FOUND",
                                        lastSyncAt = now
                                    ), now
                                )
                            }
                        )
                        localDataSource.saveSubscriptionInfo(cleared)
                        _subscriptionFlow.value = cleared
                        Result.success(cleared)
                    } else {
                        val updated = currentLocal.copy(
                            subscriptionStartAt = snapshot.initiationTimestampMillis,
                            subscriptionPurchaseAt = darinoPurchase?.purchaseTime ?: currentLocal.subscriptionPurchaseAt,
                            subscriptionEndAt = snapshot.validUntilTimestampMillis,
                            productId = snapshot.productId,
                            purchaseTokenReference = darinoPurchase?.purchaseToken ?: currentLocal.purchaseTokenReference,
                            marketAccountState = if (snapshot.active) "MARKET_ACTIVE" else "MARKET_EXPIRED",
                            lastSyncAt = snapshot.verifiedAtMillis
                        ).let { candidate ->
                            candidate.copy(subscriptionStatus = SubscriptionValidator.evaluateStatus(candidate, snapshot.serverTimeMillis))
                        }
                        localDataSource.saveSubscriptionInfo(updated)
                        _subscriptionFlow.value = updated
                        Result.success(updated)
                    }
                } catch (error: Exception) {
                    val evaluated = currentLocal.copy(
                        subscriptionStatus = SubscriptionValidator.evaluateStatus(currentLocal)
                    )
                    localDataSource.saveSubscriptionInfo(evaluated)
                    _subscriptionFlow.value = evaluated
                    Result.failure(error)
                }
            }
            is PoolakeyResult.Error -> {
                try {
                    val snapshot = getCurrentFromBackend()
                    if (snapshot != null) {
                        val updated = currentLocal.copy(
                            subscriptionStartAt = snapshot.initiationTimestampMillis,
                            subscriptionEndAt = snapshot.validUntilTimestampMillis,
                            productId = snapshot.productId,
                            marketAccountState = if (snapshot.active) "MARKET_ACTIVE" else "MARKET_EXPIRED",
                            lastSyncAt = snapshot.verifiedAtMillis
                        ).let { candidate ->
                            candidate.copy(subscriptionStatus = SubscriptionValidator.evaluateStatus(candidate, snapshot.serverTimeMillis))
                        }
                        localDataSource.saveSubscriptionInfo(updated)
                        _subscriptionFlow.value = updated
                        Result.success(updated)
                    } else {
                        val evaluated = currentLocal.copy(
                            subscriptionStartAt = null,
                            subscriptionEndAt = null,
                            productId = "",
                            purchaseTokenReference = null,
                            marketAccountState = "NO_PURCHASE_FOUND",
                            lastSyncAt = System.currentTimeMillis(),
                            subscriptionStatus = SubscriptionStatus.EXPIRED
                        )
                        localDataSource.saveSubscriptionInfo(evaluated)
                        _subscriptionFlow.value = evaluated
                        Result.success(evaluated)
                    }
                } catch (backendError: Exception) {
                    val evaluated = currentLocal.copy(subscriptionStatus = SubscriptionValidator.evaluateStatus(currentLocal))
                    localDataSource.saveSubscriptionInfo(evaluated)
                    _subscriptionFlow.value = evaluated
                    Result.failure(Exception("${poolakeyResult.message}; ${backendError.message}", backendError))
                }
            }
        }
    }

    private suspend fun verifyWithBackend(productId: String, purchaseToken: String): NetworkBazaarSubscriptionDto {
        val response = ApiClient.bazaarSubscriptionApi.verifySubscription(
            NetworkVerifyBazaarSubscriptionRequest(productId, purchaseToken)
        )
        if (!response.isSuccessful) throw IllegalStateException("اعتبارسنجی اشتراک در سرور دارینو ناموفق بود (HTTP ${response.code()}).")
        val envelope = response.body() ?: throw IllegalStateException("پاسخ سرور دارینو خالی است.")
        if (!envelope.success) throw IllegalStateException("سرور دارینو نتوانست اشتراک را تأیید کند.")
        return envelope.data ?: throw IllegalStateException("اطلاعات تاریخ اعتبار از سرور دریافت نشد.")
    }

    private suspend fun getCurrentFromBackend(): NetworkBazaarSubscriptionDto? {
        val response = ApiClient.bazaarSubscriptionApi.getCurrentSubscription()
        if (!response.isSuccessful) throw IllegalStateException("دریافت وضعیت اشتراک از سرور دارینو ناموفق بود (HTTP ${response.code()}).")
        val envelope = response.body() ?: throw IllegalStateException("پاسخ سرور دارینو خالی است.")
        if (!envelope.success) throw IllegalStateException("سرور دارینو نتوانست وضعیت اشتراک را دریافت کند.")
        return envelope.data
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
        val currentLocal = localDataSource.getSubscriptionInfo()
        val snapshot = verifyWithBackend(purchaseInfo.productId, purchaseInfo.purchaseToken)
        val candidate = currentLocal.copy(
            subscriptionStartAt = snapshot.initiationTimestampMillis,
            subscriptionPurchaseAt = purchaseInfo.purchaseTime,
            subscriptionEndAt = snapshot.validUntilTimestampMillis,
            productId = snapshot.productId,
            purchaseTokenReference = purchaseInfo.purchaseToken,
            marketAccountState = if (snapshot.active) "MARKET_ACTIVE" else "MARKET_EXPIRED",
            lastSyncAt = snapshot.verifiedAtMillis
        )
        val updated = candidate.copy(
            subscriptionStatus = SubscriptionValidator.evaluateStatus(candidate, snapshot.serverTimeMillis)
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
