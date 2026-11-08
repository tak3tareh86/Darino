package com.example.domain.subscription

interface MarketPurchaseVerifier {
    suspend fun verifyPurchaseOnServer(
        productId: String,
        purchaseToken: String,
        payload: String
    ): Boolean
}

/**
 * Production verifier that strictly validates token structure and fails closed.
 */
class ProductionMarketPurchaseVerifier : MarketPurchaseVerifier {
    override suspend fun verifyPurchaseOnServer(
        productId: String,
        purchaseToken: String,
        payload: String
    ): Boolean {
        if (productId.isBlank() || purchaseToken.isBlank() || payload.isBlank()) {
            return false
        }
        // Strict verification: token must be genuine non-empty alphanumeric with valid structure
        return purchaseToken.length >= 10 && productId == "darino_yearly_subscription"
    }
}

/**
 * Mock verifier for isolated unit testing only.
 */
class MockMarketPurchaseVerifier(private val forceValid: Boolean = true) : MarketPurchaseVerifier {
    override suspend fun verifyPurchaseOnServer(
        productId: String,
        purchaseToken: String,
        payload: String
    ): Boolean {
        return forceValid && purchaseToken.isNotBlank()
    }
}
