package com.financemanager.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "transactions")
data class Transaction(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(name = "user_id", nullable = false, length = 36)
    val userId: String,

    @Column(name = "client_id", nullable = false, length = 100)
    val clientId: String,

    @Column(nullable = false)
    var amount: Long,

    @Column(nullable = false, length = 30)
    var type: String,

    @Column(nullable = false, length = 150)
    var category: String,

    @Column(name = "account_name", nullable = false, length = 150)
    var accountName: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String = "",

    @Column(name = "occurred_at", nullable = false)
    var occurredAt: Instant,

    @Column(name = "time_formatted", nullable = false, length = 50)
    var timeFormatted: String = "",

    @Column(nullable = false, length = 255)
    var title: String = "",

    @Column(name = "sub_category", length = 150)
    var subCategory: String? = null,

    @Column(name = "date_persian", nullable = false, length = 50)
    var datePersian: String = "",

    @Column(name = "payment_method", nullable = false, length = 50)
    var paymentMethod: String = "BANK_CARD",

    @Column(name = "source_type", nullable = false, length = 50)
    var sourceType: String = "MANUAL",

    @Column(name = "source_id", length = 100)
    var sourceId: String? = null,

    @Column(name = "is_recurring", nullable = false)
    var isRecurring: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null
)
