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
        val database = db ?: return@withContext CheckOperationResult.PERSISTENCE_ERROR
        
        if (check.amount <= 0 || check.checkNumber.isBlank() || check.bankName.isBlank()) {
            return@withContext CheckOperationResult.VALIDATION_ERROR
        }

        if (check.dueDate < check.issueDate ||
            (check.sayadId != null && check.sayadId.isNotBlank() &&
                (check.sayadId.length != 16 || !check.sayadId.all { it.isDigit() })) ||
            (check.reminderDateTime != null && check.reminderDateTime > check.dueDate) ||
            check.status == CheckStatus.CLEARED
        ) {
            return@withContext CheckOperationResult.VALIDATION_ERROR
        }

        val normalizedSayadId = check.sayadId?.normalizeCheckDigits()?.trim().orEmpty()
        val duplicateSayad = try {
            if (normalizedSayadId.isBlank()) null
            else database.checkDao().getCheckBySayadId(userId, normalizedSayadId)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext CheckOperationResult.PERSISTENCE_ERROR
        }
        if (duplicateSayad != null) return@withContext CheckOperationResult.VALIDATION_ERROR

        try {
            val insertedId = database.checkDao().insertCheck(check.toEntity(userId))
            if (insertedId <= 0L) return@withContext CheckOperationResult.PERSISTENCE_ERROR
            if (insertedId > 0 && (check.status == CheckStatus.REGISTERED || check.status == CheckStatus.PENDING) && check.reminderDateTime != null) {
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
        val database = db ?: return@withContext CheckOperationResult.PERSISTENCE_ERROR
        
        if (check.amount <= 0 || check.checkNumber.isBlank() || check.bankName.isBlank()) {
            return@withContext CheckOperationResult.VALIDATION_ERROR
        }

        if (check.dueDate < check.issueDate ||
            (check.sayadId != null && check.sayadId.isNotBlank() &&
                (check.sayadId.length != 16 || !check.sayadId.all { it.isDigit() })) ||
            (check.reminderDateTime != null && check.reminderDateTime > check.dueDate)
        ) {
            return@withContext CheckOperationResult.VALIDATION_ERROR
        }

        val currentEntity = try {
            database.checkDao().getCheckById(userId, check.id)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext CheckOperationResult.PERSISTENCE_ERROR
        } ?: return@withContext CheckOperationResult.NOT_FOUND
        if (currentEntity.deletedAt != null) return@withContext CheckOperationResult.NOT_FOUND
        val normalizedSayadId = check.sayadId?.normalizeCheckDigits()?.trim().orEmpty()
        if (normalizedSayadId.isNotBlank()) {
            val duplicate = try {
                database.checkDao().getCheckBySayadId(userId, normalizedSayadId)
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext CheckOperationResult.PERSISTENCE_ERROR
            }
            if (duplicate != null && duplicate.id != check.id) {
                return@withContext CheckOperationResult.VALIDATION_ERROR
            }
        }
        // A cleared check is part of the financial ledger. Editing it without updating the
        // linked transaction would leave the check and ledger inconsistent.
        if (currentEntity.status == CheckStatus.CLEARED.name) {
            return@withContext CheckOperationResult.VALIDATION_ERROR
        }
        // Settlement must go through settleCheck(), which creates the matching ledger transaction.
        if (currentEntity.status != CheckStatus.CLEARED.name && check.status == CheckStatus.CLEARED) {
            return@withContext CheckOperationResult.VALIDATION_ERROR
        }

        try {
            val entity = check.toEntity(userId)
            val rowsAffected = database.checkDao().updateCheckWithOwner(
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
                reminderDateTime = entity.reminderDateTime,
                updatedAt = entity.updatedAt
            )
            
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
        val database = db ?: return@withContext CheckOperationResult.PERSISTENCE_ERROR
        try {
            val existing = database.checkDao().getCheckById(userId, id)
                ?: return@withContext CheckOperationResult.NOT_FOUND
            if (existing.deletedAt != null) return@withContext CheckOperationResult.NOT_FOUND
            // Do not orphan a financial transaction by deleting its settled check.
            if (existing.status == CheckStatus.CLEARED.name) {
                return@withContext CheckOperationResult.VALIDATION_ERROR
            }
            val rowsAffected = database.checkDao().softDeleteCheck(userId, id, System.currentTimeMillis(), System.currentTimeMillis())
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
        val database = db ?: return@withContext CheckOperationResult.PERSISTENCE_ERROR
        
        // 1. Verify account ownership and validity
        val accountEntity = database.accountDao().getAccountByStringId(userId, account.id)
        if (accountEntity == null || accountEntity.userId != userId || !accountEntity.isActive || accountEntity.deletedAt != null) {
            return@withContext CheckOperationResult.VALIDATION_ERROR
        }

        // 2. Verify check ownership and current status from Database
        val existingCheck = database.checkDao().getCheckById(userId, check.id)
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

            val success = database.checkDao().settleCheckWithTransaction(
                check = updatedCheckEntity,
                transaction = transaction,
                transactionDao = database.transactionDao()
            )

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
            sayadId = sayadId?.normalizeCheckDigits(),
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
            sayadId = sayadId?.normalizeCheckDigits(),
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


private fun String.normalizeCheckDigits(): String = map { char ->
    when (char) {
        in '۰'..'۹' -> ('0'.code + (char.code - '۰'.code)).toChar()
        in '٠'..'٩' -> ('0'.code + (char.code - '٠'.code)).toChar()
        else -> char
    }
}.joinToString("")
