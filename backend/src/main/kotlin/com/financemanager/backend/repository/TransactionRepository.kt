package com.financemanager.backend.repository

import com.financemanager.backend.domain.Transaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.Optional

interface TransactionRepository : JpaRepository<Transaction, String> {
    fun findAllByUserIdAndDeletedAtIsNullOrderByOccurredAtDesc(userId: String): List<Transaction>
    fun findByIdAndUserIdAndDeletedAtIsNull(id: String, userId: String): Optional<Transaction>
    fun findByUserIdAndClientId(userId: String, clientId: String): Optional<Transaction>
    fun findByUserIdAndClientIdAndDeletedAtIsNull(userId: String, clientId: String): Optional<Transaction>

    @Modifying
    @Query(
        value = """
            INSERT INTO transactions (
                id, user_id, client_id, amount, type, category, account_name,
                description, occurred_at, time_formatted, title, sub_category,
                date_persian, payment_method, source_type, source_id, is_recurring,
                created_at, updated_at, deleted_at
            ) VALUES (
                :id, :userId, :clientId, :amount, :type, :category, :accountName,
                :description, :occurredAt, :timeFormatted, :title, :subCategory,
                :datePersian, :paymentMethod, :sourceType, :sourceId, :isRecurring,
                CURRENT_TIMESTAMP, :updatedAt, NULL
            )
            ON CONFLICT (user_id, client_id) DO UPDATE SET
                amount = EXCLUDED.amount,
                type = EXCLUDED.type,
                category = EXCLUDED.category,
                account_name = EXCLUDED.account_name,
                description = EXCLUDED.description,
                occurred_at = EXCLUDED.occurred_at,
                time_formatted = EXCLUDED.time_formatted,
                title = EXCLUDED.title,
                sub_category = EXCLUDED.sub_category,
                date_persian = EXCLUDED.date_persian,
                payment_method = EXCLUDED.payment_method,
                source_type = EXCLUDED.source_type,
                source_id = EXCLUDED.source_id,
                is_recurring = EXCLUDED.is_recurring,
                updated_at = EXCLUDED.updated_at,
                deleted_at = NULL
        """,
        nativeQuery = true
    )
    fun upsertAtomically(
        @Param("id") id: String,
        @Param("userId") userId: String,
        @Param("clientId") clientId: String,
        @Param("amount") amount: Long,
        @Param("type") type: String,
        @Param("category") category: String,
        @Param("accountName") accountName: String,
        @Param("description") description: String,
        @Param("occurredAt") occurredAt: Instant,
        @Param("timeFormatted") timeFormatted: String,
        @Param("title") title: String,
        @Param("subCategory") subCategory: String?,
        @Param("datePersian") datePersian: String,
        @Param("paymentMethod") paymentMethod: String,
        @Param("sourceType") sourceType: String,
        @Param("sourceId") sourceId: String?,
        @Param("isRecurring") isRecurring: Boolean,
        @Param("updatedAt") updatedAt: Instant
    ): Int
}
