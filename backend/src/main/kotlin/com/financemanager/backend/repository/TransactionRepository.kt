package com.financemanager.backend.repository

import com.financemanager.backend.domain.Transaction
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface TransactionRepository : JpaRepository<Transaction, String> {
    fun findAllByUserIdAndDeletedAtIsNullOrderByOccurredAtDesc(userId: String): List<Transaction>
    fun findByIdAndUserIdAndDeletedAtIsNull(id: String, userId: String): Optional<Transaction>
    fun findByUserIdAndClientId(userId: String, clientId: String): Optional<Transaction>
    fun findByUserIdAndClientIdAndDeletedAtIsNull(userId: String, clientId: String): Optional<Transaction>
}
