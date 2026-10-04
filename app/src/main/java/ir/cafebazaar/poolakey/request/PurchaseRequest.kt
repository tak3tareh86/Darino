package ir.cafebazaar.poolakey.request

data class PurchaseRequest(
    val productId: String,
    val payload: String = "",
    val dynamicPriceToken: String? = null
)
