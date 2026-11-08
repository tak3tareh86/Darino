package com.financemanager.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "user_subscriptions")
data class BazaarSubscription(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(name = "user_id", nullable = false, unique = true, length = 36)
    val userId: String,

    @Column(name = "product_id", nullable = false, length = 150)
    var productId: String,

    @Column(name = "purchase_token_encrypted", nullable = false, columnDefinition = "TEXT")
    var purchaseTokenEncrypted: String,

    @Column(name = "initiation_timestamp_msec", nullable = false)
    var initiationTimestampMillis: Long,

    @Column(name = "valid_until_timestamp_msec", nullable = false)
    var validUntilTimestampMillis: Long,

    @Column(name = "auto_renewing", nullable = false)
    var autoRenewing: Boolean,

    @Column(nullable = false, length = 20)
    var status: String,

    @Column(name = "verified_at", nullable = false)
    var verifiedAt: Instant = Instant.now()
)
