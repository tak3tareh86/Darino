package com.example.domain.subscription

data class SubscriptionInfo(
    val userId: String = "",
    val trialStartAt: Long = 0L,
    val trialEndAt: Long = 0L,
    val subscriptionStartAt: Long? = null,
    val subscriptionEndAt: Long? = null,
    val subscriptionStatus: SubscriptionStatus = SubscriptionStatus.CHECKING,
    val productId: String = "",
    val purchaseTokenReference: String? = null,
    val marketAccountState: String = "ACTIVE_ACCOUNT",
    val lastSyncAt: Long = System.currentTimeMillis()
)
