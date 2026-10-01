package com.financemanager.backend.service

import com.financemanager.backend.domain.Transaction
import com.financemanager.backend.repository.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

data class UpsertTransactionCommand(
    val clientId: String,
    val amount: Long,
    val type: String,
    val category: String,
    val accountName: String,
    val description: String,
    val occurredAt: Instant,
    val timeFormatted: String,
    val title: String,
    val subCategory: String?,
    val datePersian: String,
    val paymentMethod: String,
    val sourceType: String,
    val sourceId: String?,
    val isRecurring: Boolean
)

@Service
class TransactionService(private val repository: TransactionRepository) {
    @Transactional(readOnly = true)
    fun getAll(userId: String): List<Transaction> =
        repository.findAllByUserIdAndDeletedAtIsNullOrderByOccurredAtDesc(userId)

    @Transactional(readOnly = true)
    fun getById(id: String, userId: String): Transaction? =
        repository.findByIdAndUserIdAndDeletedAtIsNull(id, userId).orElse(null)

    @Transactional
    fun save(userId: String, c: UpsertTransactionCommand): Transaction {
        val now = Instant.now()
        repository.upsertAtomically(
            id = UUID.randomUUID().toString(),
            userId = userId,
            clientId = c.clientId,
            amount = c.amount,
            type = c.type,
            category = c.category,
            accountName = c.accountName,
            description = c.description,
            occurredAt = c.occurredAt,
            timeFormatted = c.timeFormatted,
            title = c.title,
            subCategory = c.subCategory,
            datePersian = c.datePersian,
            paymentMethod = c.paymentMethod,
            sourceType = c.sourceType,
            sourceId = c.sourceId,
            isRecurring = c.isRecurring,
            updatedAt = now
        )
        return repository.findByUserIdAndClientIdAndDeletedAtIsNull(userId, c.clientId)
            .orElseThrow { IllegalStateException("Transaction upsert did not produce an active transaction") }
    }

    @Transactional
    fun tombstone(id: String, userId: String): Boolean {
        val t = getById(id, userId) ?: return false
        t.deletedAt = Instant.now()
        t.updatedAt = t.deletedAt!!
        repository.save(t)
        return true
    }
}
