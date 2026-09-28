package com.example.domain.subscription

interface MarketPurchaseVerifier {
    suspend fun verifyPurchaseOnServer(
        productId: String,
        purchaseToken: String,
        payload: String
    ): Boolean
}

class MockMarketPurchaseVerifier : MarketPurchaseVerifier {
    override suspend fun verifyPurchaseOnServer(
        productId: String,
        purchaseToken: String,
        payload: String
    ): Boolean {
        // Backend verification placeholder.
        // Returns true locally until a dedicated Darino backend server API is attached.
        return true
    }
}
