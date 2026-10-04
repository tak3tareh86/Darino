package ir.cafebazaar.poolakey.entity

enum class PurchaseState {
    PURCHASED,
    REFUNDED,
    CANCELLED,
    PENDING
}

data class PurchaseInfo(
    val productId: String,
    val purchaseToken: String,
    val purchaseState: PurchaseState,
    val purchaseTime: Long = System.currentTimeMillis(),
    val orderId: String? = null,
    val payload: String? = null
)
