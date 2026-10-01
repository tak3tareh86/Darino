package com.financemanager.backend.service

import com.financemanager.backend.domain.Transaction
import com.financemanager.backend.repository.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

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
        val t = repository.findByUserIdAndClientId(userId, c.clientId).orElseGet {
            Transaction(userId = userId, clientId = c.clientId, amount = c.amount, type = c.type, category = c.category, accountName = c.accountName, occurredAt = c.occurredAt)
        }
        t.amount=c.amount; t.type=c.type; t.category=c.category; t.accountName=c.accountName
        t.description=c.description; t.occurredAt=c.occurredAt; t.timeFormatted=c.timeFormatted
        t.title=c.title; t.subCategory=c.subCategory; t.datePersian=c.datePersian
        t.paymentMethod=c.paymentMethod; t.sourceType=c.sourceType; t.sourceId=c.sourceId
        t.isRecurring=c.isRecurring; t.deletedAt=null; t.updatedAt=Instant.now()
        return repository.save(t)
    }

    @Transactional
    fun tombstone(id: String, userId: String): Boolean {
        val t=getById(id,userId) ?: return false
        t.deletedAt=Instant.now(); t.updatedAt=t.deletedAt!!
        repository.save(t)
        return true
    }
}
