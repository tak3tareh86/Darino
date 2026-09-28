package com.financemanager.backend.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "vehicles")
data class Vehicle(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(name = "user_id", nullable = false)
    val userId: String,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(nullable = false, length = 100)
    var brand: String,

    @Column(name = "model_year", length = 10)
    var modelYear: String? = null,

    @Column(name = "plate_number", length = 50)
    var plateNumber: String? = null,

    @Column(name = "odometer_km")
    var odometerKm: Long = 0,

    @Column(length = 50)
    var color: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)

@Entity
@Table(name = "installments")
data class Installment(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(name = "user_id", nullable = false)
    val userId: String,

    @Column(nullable = false, length = 150)
    var title: String,

    @Column(name = "provider_name", nullable = false, length = 100)
    var providerName: String,

    @Column(nullable = false, length = 50)
    var category: String,

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    var totalAmount: BigDecimal,

    @Column(name = "monthly_payment", nullable = false, precision = 18, scale = 2)
    var monthlyPayment: BigDecimal,

    @Column(name = "total_installments", nullable = false)
    var totalInstallments: Int,

    @Column(name = "paid_installments", nullable = false)
    var paidInstallments: Int = 0,

    @Column(name = "due_day", nullable = false)
    var dueDay: Int,

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate,

    @Column(name = "end_date")
    var endDate: LocalDate? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)

@Entity
@Table(name = "audit_logs")
data class AuditLog(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(name = "user_id")
    val userId: String?,

    @Column(nullable = false, length = 100)
    val action: String,

    @Column(name = "entity_name", nullable = false, length = 100)
    val entityName: String,

    @Column(name = "entity_id", length = 36)
    val entityId: String?,

    @Column(name = "ip_address", length = 45)
    val ipAddress: String? = null,

    @Column(name = "user_agent", length = 255)
    val userAgent: String? = null,

    @Column(columnDefinition = "TEXT")
    val details: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
