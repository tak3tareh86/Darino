package ir.cafebazaar.poolakey

import android.content.Context
import androidx.activity.result.ActivityResultRegistry
import ir.cafebazaar.poolakey.config.PaymentConfiguration
import ir.cafebazaar.poolakey.entity.PurchaseInfo
import ir.cafebazaar.poolakey.request.PurchaseRequest

class ConnectionCallbackBuilder {
    private var onConnected: (() -> Unit)? = null
    private var onFailed: ((Throwable) -> Unit)? = null
    private var onDisconnected: (() -> Unit)? = null

    fun connectionSucceed(block: () -> Unit) {
        onConnected = block
    }

    fun connectionFailed(block: (Throwable) -> Unit) {
        onFailed = block
    }

    fun disconnected(block: () -> Unit) {
        onDisconnected = block
    }

    fun execute() {
        onConnected?.invoke()
    }
}

class QueryCallbackBuilder {
    private var onSucceed: ((List<PurchaseInfo>) -> Unit)? = null
    private var onFailed: ((Throwable) -> Unit)? = null

    fun querySucceed(block: (List<PurchaseInfo>) -> Unit) {
        onSucceed = block
    }

    fun queryFailed(block: (Throwable) -> Unit) {
        onFailed = block
    }

    fun execute(purchases: List<PurchaseInfo> = emptyList()) {
        onSucceed?.invoke(purchases)
    }
}

class PurchaseCallbackBuilder {
    private var onFlowBegan: (() -> Unit)? = null
    private var onFailedToBegin: ((Throwable) -> Unit)? = null
    private var onSucceed: ((PurchaseInfo) -> Unit)? = null
    private var onCanceled: (() -> Unit)? = null
    private var onFailed: ((Throwable) -> Unit)? = null

    fun purchaseFlowBegan(block: () -> Unit) {
        onFlowBegan = block
    }

    fun failedToBeginFlow(block: (Throwable) -> Unit) {
        onFailedToBegin = block
    }

    fun purchaseSucceed(block: (PurchaseInfo) -> Unit) {
        onSucceed = block
    }

    fun purchaseCanceled(block: () -> Unit) {
        onCanceled = block
    }

    fun purchaseFailed(block: (Throwable) -> Unit) {
        onFailed = block
    }

    fun execute(purchaseInfo: PurchaseInfo) {
        onFlowBegan?.invoke()
        onSucceed?.invoke(purchaseInfo)
    }
}

class Payment(context: Context, config: PaymentConfiguration) {

    fun connect(block: ConnectionCallbackBuilder.() -> Unit): Connection {
        val builder = ConnectionCallbackBuilder().apply(block)
        builder.execute()
        return Connection()
    }

    fun getSubscribedProducts(block: QueryCallbackBuilder.() -> Unit) {
        val builder = QueryCallbackBuilder().apply(block)
        builder.execute(emptyList())
    }

    fun subscribeProduct(
        registry: ActivityResultRegistry,
        request: PurchaseRequest,
        block: PurchaseCallbackBuilder.() -> Unit
    ) {
        val builder = PurchaseCallbackBuilder().apply(block)
        builder.execute(
            PurchaseInfo(
                productId = request.productId,
                purchaseToken = "dummy_token_${System.currentTimeMillis()}",
                purchaseState = ir.cafebazaar.poolakey.entity.PurchaseState.PURCHASED,
                purchaseTime = System.currentTimeMillis(),
                payload = request.payload
            )
        )
    }
}
