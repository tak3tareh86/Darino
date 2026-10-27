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

        try {
            val insertedId = db?.checkDao()?.insertCheck(check.toEntity(userId)) ?: 0L
            if (insertedId > 0 && (check.status == CheckStatus.REGISTERED || check.status == CheckStatus.PENDING)) {
                try {
                    reminderManager?.syncCheckReminder(
                        checkId = insertedId.toString(),
                        title = "${check.bankName} - ${check.checkNumber}",
                        amount = check.amount,
                        dueDatePersian = check.formattedDueDate,
                        type = check.type.name
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

        try {
            db?.checkDao()?.updateCheck(check.toEntity(userId))
            
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
                        type = check.type.name
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
            db?.checkDao()?.softDeleteCheck(userId, id, System.currentTimeMillis(), System.currentTimeMillis())
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
            createdAt = createdAt,
            updatedAt = System.currentTimeMillis()
        )
    }

    companion object {
        val instance: LocalCheckRepository by lazy { LocalCheckRepository() }
    }
}
