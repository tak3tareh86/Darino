package com.example.ui.screens.check.data

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.data.database.CheckEntity
import com.example.data.security.SessionManager
import com.example.ui.screens.check.model.Check
import com.example.ui.screens.check.model.CheckStatus
import com.example.ui.screens.check.model.CheckType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LocalCheckRepository private constructor() : CheckRepository {

    private var db: AppDatabase? = null
    private var reminderManager: com.example.reminder.domain.ReminderManager? = null

    fun init(context: Context) {
        if (db == null) {
            db = AppDatabase.getDatabase(context)
            reminderManager = com.example.reminder.domain.ReminderManager(context)
        }
    }

    override fun getChecks(): Flow<List<Check>> {
        val userId = SessionManager.userId ?: return flowOf(emptyList())
        return db?.checkDao()?.getAllChecks(userId)?.map { entities ->
            entities.map { it.toCheck() }
        } ?: flowOf(emptyList())
    }

    override suspend fun getCheckById(id: Int): Check? = withContext(Dispatchers.IO) {
        val userId = SessionManager.userId ?: return@withContext null
        db?.checkDao()?.getCheckById(userId, id)?.toCheck()
    }

    override suspend fun addCheck(check: Check): CheckOperationResult = withContext(Dispatchers.IO) {
        val userId = SessionManager.userId ?: return@withContext CheckOperationResult.NO_AUTHENTICATED_USER
        
        if (check.amount <= 0 || check.checkNumber.isBlank() || check.bankName.isBlank()) {
            return@withContext CheckOperationResult.VALIDATION_ERROR
        }

        if (check.dueDate < check.issueDate) {
            return@withContext CheckOperationResult.VALIDATION_ERROR
        }

        try {
            val insertedId = db?.checkDao()?.insertCheck(check.toEntity(userId)) ?: 0L
            if (insertedId > 0 && (check.status == CheckStatus.REGISTERED || check.status == CheckStatus.PENDING)) {
                try {
                    reminderManager?.syncCheckReminder(
                        checkId = insertedId.toString(),
                        title = "${check.bankName} - ${check.checkNumber}",
                        amount = check.amount,
                        dueDatePersian = check.formattedDueDate,
                        type = check.type.name,
                        reminderDateTime = check.reminderDateTime
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            CheckOperationResult.SUCCESS
        } catch (e: Exception) {
            e.printStackTrace()
            CheckOperationResult.PERSISTENCE_ERROR
        }
    }

    override suspend fun updateCheck(check: Check): CheckOperationResult = withContext(Dispatchers.IO) {
        val userId = SessionManager.userId ?: return@withContext CheckOperationResult.NO_AUTHENTICATED_USER
        
        if (check.amount <= 0 || check.checkNumber.isBlank() || check.bankName.isBlank()) {
            return@withContext CheckOperationResult.VALIDATION_ERROR
        }

        if (check.dueDate < check.issueDate) {
            return@withContext CheckOperationResult.VALIDATION_ERROR
        }

        try {
            val entity = check.toEntity(userId)
            val rowsAffected = db?.checkDao()?.updateCheckWithOwner(
                id = entity.id,
                userId = userId,
                type = entity.type,
                amount = entity.amount,
                checkNumber = entity.checkNumber,
                sayadId = entity.sayadId,
                bankName = entity.bankName,
                branch = entity.branch,
                holderName = entity.holderName,
                receiverName = entity.receiverName,
                issueDate = entity.issueDate,
                dueDate = entity.dueDate,
                status = entity.status,
                notes = entity.notes,
                accountId = entity.accountId,
                updatedAt = entity.updatedAt
            ) ?: 0
            
            if (rowsAffected == 0) return@withContext CheckOperationResult.NOT_FOUND

            if (check.status == CheckStatus.CLEARED || check.status == CheckStatus.CANCELED || check.status == CheckStatus.BOUNCED) {
                try {
                    reminderManager?.onCheckSettled(check.id.toString())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                try {
                    reminderManager?.syncCheckReminder(
                        checkId = check.id.toString(),
                        title = "${check.bankName} - ${check.checkNumber}",
                        amount = check.amount,
                        dueDatePersian = check.formattedDueDate,
                        type = check.type.name,
                        reminderDateTime = check.reminderDateTime
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            CheckOperationResult.SUCCESS
        } catch (e: Exception) {
            e.printStackTrace()
            CheckOperationResult.PERSISTENCE_ERROR
        }
    }

    override suspend fun deleteCheck(id: Int): CheckOperationResult = withContext(Dispatchers.IO) {
        val userId = SessionManager.userId ?: return@withContext CheckOperationResult.NO_AUTHENTICATED_USER
        try {
            val rowsAffected = db?.checkDao()?.softDeleteCheck(userId, id, System.currentTimeMillis(), System.currentTimeMillis()) ?: 0
            if (rowsAffected == 0) return@withContext CheckOperationResult.NOT_FOUND
            
            try {
                reminderManager?.onCheckSettled(id.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
            CheckOperationResult.SUCCESS
        } catch (e: Exception) {
            e.printStackTrace()
            CheckOperationResult.PERSISTENCE_ERROR
        }
    }

    override suspend fun settleCheck(check: Check, account: com.example.ui.screens.finance.model.Account): CheckOperationResult = withContext(Dispatchers.IO) {
        val userId = SessionManager.userId ?: return@withContext CheckOperationResult.NO_AUTHENTICATED_USER
        
        // 1. Verify account ownership and validity
        val accountEntity = db?.accountDao()?.getAccountByStringId(userId, account.id)
        if (accountEntity == null || accountEntity.userId != userId || !accountEntity.isActive || accountEntity.deletedAt != null) {
            return@withContext CheckOperationResult.VALIDATION_ERROR
        }

        // 2. Verify check ownership and current status from Database
        val existingCheck = db?.checkDao()?.getCheckById(userId, check.id)
        if (existingCheck == null || existingCheck.userId != userId) {
            return@withContext CheckOperationResult.NOT_FOUND
        }
        
        // 3. Prevent settling already canceled, bounced or deleted checks
        if (existingCheck.status == CheckStatus.CANCELED.name || existingCheck.status == CheckStatus.BOUNCED.name) {
            return@withContext CheckOperationResult.VALIDATION_ERROR // Cannot settle canceled or bounced check
        }
        if (existingCheck.deletedAt != null) {
            return@withContext CheckOperationResult.NOT_FOUND
        }

        // 4. Date validation (using DB record)
        if (existingCheck.dueDate < existingCheck.issueDate) {
            return@withContext CheckOperationResult.VALIDATION_ERROR
        }

        try {
            // CRITICAL: Use data from DB record (existingCheck) for all financial information
            // to prevent tampering from UI parameters.
            val updatedCheckEntity = existingCheck.copy(
                status = CheckStatus.CLEARED.name,
                accountId = account.id,
                updatedAt = System.currentTimeMillis()
            )

            // Create a stable, deterministic transaction ID for idempotency
            val settleTransactionId = "check_settle_${existingCheck.id}"
            
            val transaction = com.example.data.database.TransactionEntity(
                userId = userId,
                amount = existingCheck.amount,
                type = if (existingCheck.type == CheckType.RECEIVED.name) "INCOME" else "EXPENSE",
                category = "انتقال", 
                accountName = accountEntity.name,
                accountId = accountEntity.stringId,
                description = "تسویه خودکار چک شماره ${existingCheck.checkNumber}",
                timestamp = System.currentTimeMillis(),
                stringId = settleTransactionId,
                title = "وصول چک: ${existingCheck.bankName} (${existingCheck.checkNumber})",
                sourceType = "CHECK",
                sourceId = existingCheck.id.toString(),
                datePersian = com.example.util.PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate(),
                timeFormatted = com.example.util.PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedTime()
            )

            val success = db?.checkDao()?.settleCheckWithTransaction(
                check = updatedCheckEntity,
                transaction = transaction,
                transactionDao = db!!.transactionDao()
            ) ?: false

            if (success) {
                try {
                    reminderManager?.onCheckSettled(existingCheck.id.toString())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                CheckOperationResult.SUCCESS
            } else {
                CheckOperationResult.PERSISTENCE_ERROR
            }
        } catch (e: Exception) {
            e.printStackTrace()
            CheckOperationResult.PERSISTENCE_ERROR
        }
    }

    suspend fun clearAllChecks() = withContext(Dispatchers.IO) {
        val userId = SessionManager.userId ?: return@withContext
        db?.checkDao()?.clearAllChecks(userId)
    }

    private fun CheckEntity.toCheck(): Check {
        return Check(
            id = id,
            userId = userId,
            type = CheckType.valueOf(type),
            amount = amount,
            checkNumber = checkNumber,
            sayadId = sayadId,
            bankName = bankName,
            branch = branch,
            holderName = holderName,
            receiverName = receiverName,
            issueDate = issueDate,
            dueDate = dueDate,
            status = CheckStatus.valueOf(status),
            notes = notes,
            accountId = accountId,
            reminderDateTime = reminderDateTime,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun Check.toEntity(userId: String): CheckEntity {
        return CheckEntity(
            id = id,
            userId = userId,
            type = type.name,
            amount = amount,
            checkNumber = checkNumber,
            sayadId = sayadId,
            bankName = bankName,
            branch = branch,
            holderName = holderName,
            receiverName = receiverName,
            issueDate = issueDate,
            dueDate = dueDate,
            status = status.name,
            notes = notes,
            accountId = accountId,
            reminderDateTime = reminderDateTime,
            createdAt = createdAt,
            updatedAt = System.currentTimeMillis()
        )
    }

    companion object {
        val instance: LocalCheckRepository by lazy { LocalCheckRepository() }
    }
}
