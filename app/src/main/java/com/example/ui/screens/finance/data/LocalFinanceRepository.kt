package com.example.ui.screens.finance.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.example.data.database.AppDatabase
import com.example.data.database.TransactionEntity
import com.example.data.security.SessionManager
import com.example.ui.screens.finance.domain.BudgetEngine
import com.example.ui.screens.finance.domain.FinanceEngine
import com.example.ui.screens.finance.model.Budget
import com.example.ui.screens.finance.model.FinanceDefaultCategories
import com.example.ui.screens.finance.model.PaymentMethod
import com.example.ui.screens.finance.model.RecurringFrequency
import com.example.ui.screens.finance.model.RecurringTransaction
import com.example.ui.screens.finance.model.SavingsGoal
import com.example.ui.screens.finance.model.SavingsGoalStatus
import com.example.ui.screens.finance.model.TransactionCategory
import com.example.ui.screens.finance.model.TransactionItemData
import com.example.ui.screens.finance.model.TransactionSourceType
import com.example.ui.screens.finance.model.TransactionType
import com.example.util.PersianCalendarHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class LocalFinanceRepository private constructor() : FinanceRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _transactions = MutableStateFlow<List<TransactionItemData>>(emptyList())
    private val _categories = MutableStateFlow<List<TransactionCategory>>(FinanceDefaultCategories.allDefaultCategories)
    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    private val _savingsGoals = MutableStateFlow<List<SavingsGoal>>(emptyList())
    private val _recurringTransactions = MutableStateFlow<List<RecurringTransaction>>(emptyList())
    private val _accounts = MutableStateFlow<List<com.example.ui.screens.finance.model.Account>>(emptyList())

    private var appContext: Context? = null
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val appCtx = context.applicationContext
        appContext = appCtx

        val db = AppDatabase.getDatabase(appCtx)
        repositoryScope.launch {
            SessionManager.sessionState.collectLatest { state ->
                val authenticatedUserId = when (state) {
                    is com.example.data.security.SessionState.Authenticated -> state.user.id
                    is com.example.data.security.SessionState.PhoneVerificationRequired -> state.user.id
                    else -> null
                }

                if (authenticatedUserId == null) {
                    _transactions.value = emptyList()
                    _budgets.value = emptyList()
                    _savingsGoals.value = emptyList()
                    _recurringTransactions.value = emptyList()
                    _accounts.value = emptyList()
                    recalculateBudgets()
                    return@collectLatest
                }

                loadMetadataFromDisk(appCtx, authenticatedUserId)

                try {
                    // Collect accounts
                    launch {
                        db.accountDao().getAllAccountsFlow(authenticatedUserId).collectLatest { entities ->
                            val mapped = entities.map { toAccount(it) }
                            _accounts.value = mapped
                        }
                    }

                    // Collect transactions
                    launch {
                        db.transactionDao().getAllTransactions(authenticatedUserId).collectLatest { entities ->
                            // Room is the production source of truth; an empty database stays empty.
                            val mapped = entities.map { toItemData(it, _categories.value) }
                            _transactions.value = mapped
                            recalculateBudgets()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun getTransactions(): Flow<List<TransactionItemData>> = _transactions.asStateFlow()
    override fun getCategories(): Flow<List<TransactionCategory>> = _categories.asStateFlow()
    override fun getBudgets(): Flow<List<Budget>> = _budgets.asStateFlow()
    override fun getSavingsGoals(): Flow<List<SavingsGoal>> = _savingsGoals.asStateFlow()
    override fun getRecurringTransactions(): Flow<List<RecurringTransaction>> = _recurringTransactions.asStateFlow()
    override fun getAccounts(): Flow<List<com.example.ui.screens.finance.model.Account>> = _accounts.asStateFlow()

    private fun toAccount(entity: com.example.data.database.AccountEntity): com.example.ui.screens.finance.model.Account {
        val typeEnum = try {
            com.example.ui.screens.finance.model.AccountType.valueOf(entity.type)
        } catch (e: Exception) {
            com.example.ui.screens.finance.model.AccountType.OTHER
        }
        return com.example.ui.screens.finance.model.Account(
            id = entity.stringId,
            userId = entity.userId,
            name = entity.name,
            type = typeEnum,
            bankName = entity.bankName,
            accountNumberMasked = entity.accountNumberMasked,
            initialBalance = entity.initialBalance,
            isActive = entity.isActive,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    private fun toAccountEntity(account: com.example.ui.screens.finance.model.Account): com.example.data.database.AccountEntity {
        return com.example.data.database.AccountEntity(
            stringId = account.id,
            userId = account.userId,
            name = account.name,
            type = account.type.name,
            bankName = account.bankName,
            accountNumberMasked = account.accountNumberMasked,
            initialBalance = account.initialBalance,
            isActive = account.isActive,
            createdAt = account.createdAt,
            updatedAt = account.updatedAt,
            deletedAt = null
        )
    }

    private fun validateTransaction(transaction: TransactionItemData): Boolean {
        if (transaction.amount <= 0L) return false
        if (transaction.title.isBlank()) return false
        if (transaction.type != TransactionType.EXPENSE && transaction.type != TransactionType.INCOME && transaction.type != TransactionType.TRANSFER) return false
        if (transaction.type == TransactionType.INCOME || transaction.type == TransactionType.EXPENSE) {
            if (transaction.accountId.isNullOrBlank()) return false
        } else if (transaction.type == TransactionType.TRANSFER) {
            val src = transaction.transferSourceAccountId
            val dest = transaction.transferDestinationAccountId
            if (src.isNullOrBlank() || dest.isNullOrBlank()) return false
            if (src == dest) return false
        }
        return true
    }

    private suspend fun validateTransactionWithDatabase(tx: TransactionItemData, userId: String, db: AppDatabase): Boolean {
        if (!validateTransaction(tx)) return false

        if (tx.type == TransactionType.TRANSFER) {
            val srcId = tx.transferSourceAccountId ?: return false
            val destId = tx.transferDestinationAccountId ?: return false
            if (srcId.isBlank() || destId.isBlank()) return false
            if (srcId == destId) return false

            val srcAcc = db.accountDao().getAccountByStringId(userId, srcId) ?: return false
            val destAcc = db.accountDao().getAccountByStringId(userId, destId) ?: return false

            if (srcAcc.userId != userId || destAcc.userId != userId) return false
            if (!srcAcc.isActive || !destAcc.isActive) return false
            if (srcAcc.deletedAt != null || destAcc.deletedAt != null) return false
        } else {
            // INCOME or EXPENSE: accountId is strictly required and must be valid, active, non-deleted, and belong to current user
            val accId = tx.accountId
            if (accId.isNullOrBlank()) return false
            
            val acc = db.accountDao().getAccountByStringId(userId, accId) ?: return false
            if (acc.userId != userId) return false
            if (!acc.isActive || acc.deletedAt != null) return false
        }
        return true
    }

    override fun addTransaction(transaction: TransactionItemData) {
        kotlinx.coroutines.runBlocking {
            addTransactionResult(transaction)
        }
    }

    override fun updateTransaction(transaction: TransactionItemData) {
        kotlinx.coroutines.runBlocking {
            updateTransactionResult(transaction)
        }
    }

    override fun deleteTransaction(id: String) {
        kotlinx.coroutines.runBlocking {
            deleteTransactionResult(id)
        }
    }

    override fun duplicateTransaction(id: String): TransactionItemData? {
        return kotlinx.coroutines.runBlocking {
            duplicateTransactionResult(id)
        }
    }

    fun refreshMetadataForCurrentUser() {
        val appCtx = appContext ?: return
        val currentUserId = SessionManager.userId
        if (currentUserId == null) {
            _transactions.value = emptyList()
            _budgets.value = emptyList()
            _savingsGoals.value = emptyList()
            _recurringTransactions.value = emptyList()
        } else {
            loadMetadataFromDisk(appCtx, currentUserId)
        }
    }

    override suspend fun addTransactionResult(transaction: TransactionItemData): TransactionOperationResult {
        val userId = SessionManager.userId ?: return TransactionOperationResult.NO_AUTHENTICATED_USER
        
        val validatedTx = if (transaction.id.isBlank()) {
            transaction.copy(id = UUID.randomUUID().toString())
        } else {
            transaction
        }

        val ctx = appContext ?: return TransactionOperationResult.PERSISTENCE_ERROR
        return try {
            val db = AppDatabase.getDatabase(ctx)
            if (!validateTransactionWithDatabase(validatedTx, userId, db)) {
                return TransactionOperationResult.VALIDATION_ERROR
            }
            val entity = toEntity(validatedTx)
            db.transactionDao().insertTransaction(entity)
            val current = _transactions.value.filter { it.id != validatedTx.id }.toMutableList()
            current.add(0, validatedTx)
            _transactions.value = current
            recalculateBudgets()
            TransactionOperationResult.SUCCESS
        } catch (e: Exception) {
            e.printStackTrace()
            TransactionOperationResult.PERSISTENCE_ERROR
        }
    }

    override suspend fun updateTransactionResult(transaction: TransactionItemData): TransactionOperationResult {
        val userId = SessionManager.userId ?: return TransactionOperationResult.NO_AUTHENTICATED_USER

        val ctx = appContext ?: return TransactionOperationResult.PERSISTENCE_ERROR
        return try {
            val db = AppDatabase.getDatabase(ctx)
            val existing = db.transactionDao().getTransactionByStringId(userId, transaction.id)
                ?: db.transactionDao().getTransactionIncludingDeleted(userId, transaction.id)
            if (existing == null) {
                return TransactionOperationResult.NOT_FOUND
            }
            if (existing.deletedAt != null) {
                return TransactionOperationResult.VALIDATION_ERROR
            }
            if (!validateTransactionWithDatabase(transaction, userId, db)) {
                return TransactionOperationResult.VALIDATION_ERROR
            }
            val entity = toEntity(transaction).copy(id = existing.id, userId = userId)
            db.transactionDao().updateTransaction(entity)
            val current = _transactions.value.toMutableList()
            val index = current.indexOfFirst { it.id == transaction.id }
            if (index != -1) {
                current[index] = transaction
            }
            _transactions.value = current
            recalculateBudgets()
            TransactionOperationResult.SUCCESS
        } catch (e: Exception) {
            e.printStackTrace()
            TransactionOperationResult.PERSISTENCE_ERROR
        }
    }

    override suspend fun deleteTransactionResult(id: String): TransactionOperationResult {
        val userId = SessionManager.userId ?: return TransactionOperationResult.NO_AUTHENTICATED_USER
        val ctx = appContext ?: return TransactionOperationResult.PERSISTENCE_ERROR
        return try {
            val db = AppDatabase.getDatabase(ctx)
            val existing = db.transactionDao().getTransactionByStringId(userId, id)
                ?: db.transactionDao().getTransactionIncludingDeleted(userId, id)
            if (existing == null) {
                return TransactionOperationResult.NOT_FOUND
            }
            db.transactionDao().softDeleteByStringId(userId, id, System.currentTimeMillis(), System.currentTimeMillis())
            _transactions.value = _transactions.value.filter { it.id != id }
            recalculateBudgets()
            TransactionOperationResult.SUCCESS
        } catch (e: Exception) {
            e.printStackTrace()
            TransactionOperationResult.PERSISTENCE_ERROR
        }
    }

    override suspend fun duplicateTransactionResult(id: String): TransactionItemData? {
        val userId = SessionManager.userId ?: return null
        val ctx = appContext ?: return null
        
        // Always check DB directly to ensure we have the latest state (especially deleted status)
        val entity = AppDatabase.getDatabase(ctx).transactionDao().getTransactionByStringId(userId, id)
        val original = entity?.let { toItemData(it, _categories.value) } ?: return null

        val duplicate = original.copy(
            id = UUID.randomUUID().toString(),
            title = "${original.title} (کپی)",
            datePersian = "امروز",
            dateMillis = System.currentTimeMillis()
        )
        val result = addTransactionResult(duplicate)
        return if (result == TransactionOperationResult.SUCCESS) duplicate else null
    }

    override fun addCategory(category: TransactionCategory) {
        val current = _categories.value.toMutableList()
        current.add(category)
        _categories.value = current
        saveMetadataToDisk()
    }

    override fun updateCategory(category: TransactionCategory) {
        val current = _categories.value.toMutableList()
        val index = current.indexOfFirst { it.id == category.id }
        if (index != -1) {
            current[index] = category
        } else {
            current.add(category)
        }
        _categories.value = current
        saveMetadataToDisk()
    }

    override fun deleteCategory(id: String) {
        val current = _categories.value.toMutableList()
        current.removeAll { it.id == id && !it.isDefault }
        _categories.value = current
        saveMetadataToDisk()
    }

    override fun toggleCategoryActive(id: String) {
        val current = _categories.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            val item = current[index]
            current[index] = item.copy(isActive = !item.isActive)
            _categories.value = current
            saveMetadataToDisk()
        }
    }

    override fun addBudget(budget: Budget) {
        val recalculated = BudgetEngine.calculateBudgetUsage(budget, _transactions.value)
        val current = _budgets.value.toMutableList()
        val index = current.indexOfFirst { it.id == budget.id }
        if (index != -1) {
            current[index] = recalculated
        } else {
            current.add(recalculated)
        }
        _budgets.value = current
        saveMetadataToDisk()
    }

    override fun updateBudget(budget: Budget) {
        val recalculated = BudgetEngine.calculateBudgetUsage(budget, _transactions.value)
        val current = _budgets.value.toMutableList()
        val index = current.indexOfFirst { it.id == budget.id }
        if (index != -1) {
            current[index] = recalculated
            _budgets.value = current
            saveMetadataToDisk()
        }
    }

    override fun deleteBudget(id: String) {
        val current = _budgets.value.toMutableList()
        current.removeAll { it.id == id }
        _budgets.value = current
        saveMetadataToDisk()
    }
    
    fun clearInMemoryBudgetsForTesting() {
        _budgets.value = emptyList()
    }

    override fun toggleBudget(id: String, enabled: Boolean): Boolean {
        val currentUserId = SessionManager.userId ?: run {
            android.util.Log.e("LocalFinanceRepository", "toggleBudget: No authenticated user")
            return false
        }
        val current = _budgets.value
        val index = current.indexOfFirst { it.id == id }
        if (index == -1) {
            android.util.Log.e("LocalFinanceRepository", "toggleBudget: Budget with id $id not found")
            return false
        }

        // Prepare updated list WITHOUT modifying in-memory state yet
        val updatedList = current.toMutableList()
        val item = current[index]
        updatedList[index] = item.copy(isEnabled = enabled, enabled = enabled)

        // 1. Persist to disk FIRST
        val persisted = persistBudgetsToDisk(updatedList, currentUserId)
        if (!persisted) {
            android.util.Log.e("LocalFinanceRepository", "toggleBudget: Persistence failed for budget $id")
            return false
        }

        // 2. Only after persistence succeeds, update in-memory state
        _budgets.value = updatedList
        return true
    }

    private fun persistBudgetsToDisk(budgets: List<Budget>, userId: String?): Boolean {
        val ctx = appContext ?: run {
            android.util.Log.e("LocalFinanceRepository", "persistBudgetsToDisk: appContext is null")
            return false
        }
        return try {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val bArr = JSONArray()
            budgets.forEach { b ->
                val obj = JSONObject()
                obj.put("id", b.id)
                obj.put("title", b.title)
                obj.put("categoryId", b.categoryId ?: "")
                obj.put("categoryTitle", b.categoryTitle ?: "")
                obj.put("amount", b.amount)
                obj.put("period", b.period)
                obj.put("startDate", b.startDate)
                obj.put("endDate", b.endDate)
                obj.put("isEnabled", b.isEnabled)
                bArr.put(obj)
            }
            val committed = prefs.edit()
                .putString(userScopedKey(KEY_BUDGETS, userId), bArr.toString())
                .commit()
            if (!committed) {
                android.util.Log.e("LocalFinanceRepository", "persistBudgetsToDisk: commit() returned false")
            }
            committed
        } catch (e: Exception) {
            android.util.Log.e("LocalFinanceRepository", "Failed to persist budgets to disk", e)
            false
        }
    }

    override fun addSavingsGoal(goal: SavingsGoal) {
        val progress = FinanceEngine.calculateSavingsGoalProgress(goal.currentAmount, goal.targetAmount)
        val current = _savingsGoals.value.toMutableList()
        current.add(goal.copy(progressPercentage = progress))
        _savingsGoals.value = current
        saveMetadataToDisk()
    }

    override fun updateSavingsGoal(goal: SavingsGoal) {
        val progress = FinanceEngine.calculateSavingsGoalProgress(goal.currentAmount, goal.targetAmount)
        val current = _savingsGoals.value.toMutableList()
        val index = current.indexOfFirst { it.id == goal.id }
        if (index != -1) {
            current[index] = goal.copy(progressPercentage = progress)
            _savingsGoals.value = current
            saveMetadataToDisk()
        }
    }

    override fun deleteSavingsGoal(id: String) {
        val current = _savingsGoals.value.toMutableList()
        current.removeAll { it.id == id }
        _savingsGoals.value = current
        saveMetadataToDisk()
    }

    override fun depositToSavingsGoal(goalId: String, amount: Long) {
        val current = _savingsGoals.value.toMutableList()
        val index = current.indexOfFirst { it.id == goalId }
        if (index != -1) {
            val goal = current[index]
            val newAmount = goal.currentAmount + amount
            val progress = FinanceEngine.calculateSavingsGoalProgress(newAmount, goal.targetAmount)
            val status = if (newAmount >= goal.targetAmount) SavingsGoalStatus.COMPLETED else goal.status
            current[index] = goal.copy(
                currentAmount = newAmount,
                progressPercentage = progress,
                status = status
            )
            _savingsGoals.value = current
            saveMetadataToDisk()
        }
    }

    override fun completeSavingsGoal(goalId: String) {
        val current = _savingsGoals.value.toMutableList()
        val index = current.indexOfFirst { it.id == goalId }
        if (index != -1) {
            val goal = current[index]
            current[index] = goal.copy(status = SavingsGoalStatus.COMPLETED)
            _savingsGoals.value = current
            saveMetadataToDisk()
        }
    }

    override fun addRecurringTransaction(recurring: RecurringTransaction) {
        val current = _recurringTransactions.value.toMutableList()
        current.add(recurring)
        _recurringTransactions.value = current
        saveMetadataToDisk()
    }

    override fun updateRecurringTransaction(recurring: RecurringTransaction) {
        val current = _recurringTransactions.value.toMutableList()
        val index = current.indexOfFirst { it.id == recurring.id }
        if (index != -1) {
            current[index] = recurring
            _recurringTransactions.value = current
            saveMetadataToDisk()
        }
    }

    override fun deleteRecurringTransaction(id: String) {
        val current = _recurringTransactions.value.toMutableList()
        current.removeAll { it.id == id }
        _recurringTransactions.value = current
        saveMetadataToDisk()
    }

    override fun toggleRecurringEnabled(id: String) {
        val current = _recurringTransactions.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            val item = current[index]
            current[index] = item.copy(enabled = !item.enabled)
            _recurringTransactions.value = current
            saveMetadataToDisk()
        }
    }

    override fun clearAllTransactionsData() {
        val userId = SessionManager.userId ?: return
        _transactions.value = emptyList()
        _recurringTransactions.value = emptyList()
        _budgets.value = emptyList()
        _savingsGoals.value = emptyList()
        saveMetadataToDisk()

        val ctx = appContext ?: return
        repositoryScope.launch {
            try {
                AppDatabase.getDatabase(ctx).transactionDao().clearAllTransactions(userId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun restoreSampleTransactions() {
        // Mock data is prohibited in production. This method is now a no-op to ensure clean state.
    }

    private fun recalculateBudgets() {
        val txs = _transactions.value
        val updatedBudgets = _budgets.value.map { budget ->
            BudgetEngine.calculateBudgetUsage(budget, txs)
        }
        _budgets.value = updatedBudgets
    }

    private fun saveMetadataToDisk() {
        val ctx = appContext ?: return
        val currentUserId = SessionManager.userId
        val currentBudgets = _budgets.value
        val currentSavings = _savingsGoals.value
        val currentRecurring = _recurringTransactions.value
        try {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()

            // Budgets
            val bArr = JSONArray()
            currentBudgets.forEach { b ->
                val obj = JSONObject()
                obj.put("id", b.id)
                obj.put("title", b.title)
                obj.put("categoryId", b.categoryId ?: "")
                obj.put("categoryTitle", b.categoryTitle ?: "")
                obj.put("amount", b.amount)
                obj.put("period", b.period)
                obj.put("startDate", b.startDate)
                obj.put("endDate", b.endDate)
                obj.put("isEnabled", b.isEnabled)
                bArr.put(obj)
            }
            editor.putString(userScopedKey(KEY_BUDGETS, currentUserId), bArr.toString())

            // Savings Goals
            val gArr = JSONArray()
            currentSavings.forEach { g ->
                val obj = JSONObject()
                obj.put("id", g.id)
                obj.put("title", g.title)
                obj.put("targetAmount", g.targetAmount)
                obj.put("currentAmount", g.currentAmount)
                obj.put("targetDate", g.targetDate)
                obj.put("description", g.description)
                obj.put("status", g.status.name)
                obj.put("iconEmoji", g.iconEmoji)
                gArr.put(obj)
            }
            editor.putString(userScopedKey(KEY_SAVINGS, currentUserId), gArr.toString())

            // Recurring
            val rArr = JSONArray()
            currentRecurring.forEach { r ->
                val obj = JSONObject()
                obj.put("id", r.id)
                obj.put("title", r.title)
                obj.put("amount", r.amount)
                obj.put("type", r.type.name)
                obj.put("categoryId", r.categoryId)
                obj.put("categoryTitle", r.categoryTitle)
                obj.put("frequency", r.frequency.name)
                obj.put("startDate", r.startDate)
                obj.put("endDate", r.endDate ?: "")
                obj.put("nextExecutionDate", r.nextExecutionDate)
                obj.put("enabled", r.enabled)
                rArr.put(obj)
            }
            editor.putString(userScopedKey(KEY_RECURRING, currentUserId), rArr.toString())

            // Categories
            val cArr = JSONArray()
            _categories.value.forEach { c ->
                val obj = JSONObject()
                obj.put("id", c.id)
                obj.put("title", c.title)
                obj.put("iconEmoji", c.iconEmoji)
                obj.put("accentColor", c.accentColor.value.toLong())
                obj.put("type", c.type.name)
                obj.put("isDefault", c.isDefault)
                obj.put("isActive", c.isActive)
                val subArr = JSONArray()
                c.subCategories.forEach { subArr.put(it) }
                obj.put("subCategories", subArr)
                cArr.put(obj)
            }
            editor.putString(userScopedKey(KEY_CATEGORIES, currentUserId), cArr.toString())

            editor.commit()
        } catch (e: Exception) {
            android.util.Log.e("LocalFinanceRepository", "saveMetadataToDisk failed", e)
        }
    }

    private fun loadMetadataFromDisk(context: Context, userId: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val budgetsJson = prefs.getString(userScopedKey(KEY_BUDGETS, userId), null)
            if (!budgetsJson.isNullOrBlank()) {
                val arr = JSONArray(budgetsJson)
                val list = mutableListOf<Budget>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        Budget(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            categoryId = obj.optString("categoryId").ifBlank { null },
                            categoryTitle = obj.optString("categoryTitle").ifBlank { null },
                            amount = obj.getLong("amount"),
                            period = obj.optString("period", "این ماه"),
                            startDate = obj.optString("startDate", ""),
                            endDate = obj.optString("endDate", ""),
                            isEnabled = obj.optBoolean("isEnabled", true)
                        )
                    )
                }
                _budgets.value = list
            } else {
                _budgets.value = emptyList()
            }

            val savingsJson = prefs.getString(userScopedKey(KEY_SAVINGS, userId), null)
            if (!savingsJson.isNullOrBlank()) {
                val arr = JSONArray(savingsJson)
                val list = mutableListOf<SavingsGoal>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val st = try {
                        SavingsGoalStatus.valueOf(obj.getString("status"))
                    } catch (e: Exception) {
                        SavingsGoalStatus.IN_PROGRESS
                    }
                    val target = obj.getLong("targetAmount")
                    val curr = obj.getLong("currentAmount")
                    list.add(
                        SavingsGoal(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            targetAmount = target,
                            currentAmount = curr,
                            targetDate = obj.optString("targetDate", ""),
                            description = obj.optString("description", ""),
                            status = st,
                            progressPercentage = FinanceEngine.calculateSavingsGoalProgress(curr, target),
                            iconEmoji = obj.optString("iconEmoji", "🎯")
                        )
                    )
                }
                _savingsGoals.value = list
            } else {
                _savingsGoals.value = emptyList()
            }

            val recurringJson = prefs.getString(userScopedKey(KEY_RECURRING, userId), null)
            if (!recurringJson.isNullOrBlank()) {
                val arr = JSONArray(recurringJson)
                val list = mutableListOf<RecurringTransaction>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        RecurringTransaction(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            amount = obj.getLong("amount"),
                            type = try { TransactionType.valueOf(obj.getString("type")) } catch (e: Exception) { TransactionType.EXPENSE },
                            categoryId = obj.getString("categoryId"),
                            categoryTitle = obj.getString("categoryTitle"),
                            frequency = try { RecurringFrequency.valueOf(obj.getString("frequency")) } catch (e: Exception) { RecurringFrequency.MONTHLY },
                            startDate = obj.getString("startDate"),
                            endDate = obj.optString("endDate").ifBlank { null },
                            nextExecutionDate = obj.getString("nextExecutionDate"),
                            enabled = obj.optBoolean("enabled", true)
                        )
                    )
                }
                _recurringTransactions.value = list
            } else {
                _recurringTransactions.value = emptyList()
            }

            val categoriesJson = prefs.getString(userScopedKey(KEY_CATEGORIES, userId), null)
            if (!categoriesJson.isNullOrBlank()) {
                val arr = JSONArray(categoriesJson)
                val list = mutableListOf<TransactionCategory>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val subArr = obj.optJSONArray("subCategories")
                    val subs = mutableListOf<String>()
                    if (subArr != null) {
                        for (j in 0 until subArr.length()) {
                            subs.add(subArr.getString(j))
                        }
                    }
                    list.add(
                        TransactionCategory(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            iconEmoji = obj.getString("iconEmoji"),
                            accentColor = Color(obj.getLong("accentColor").toULong()),
                            type = try { TransactionType.valueOf(obj.getString("type")) } catch (e: Exception) { TransactionType.EXPENSE },
                            isDefault = obj.optBoolean("isDefault", true),
                            isActive = obj.optBoolean("isActive", true),
                            subCategories = subs
                        )
                    )
                }
                _categories.value = list
            } else {
                _categories.value = FinanceDefaultCategories.allDefaultCategories
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toEntity(item: TransactionItemData): TransactionEntity {
        val userId = SessionManager.userId ?: throw IllegalStateException("No authenticated user")
        return TransactionEntity(
            stringId = item.id,
            title = item.title,
            amount = item.amount,
            type = item.type.name,
            category = item.category.title,
            accountName = item.accountName,
            description = item.description,
            timestamp = item.dateMillis,
            timeFormatted = item.timePersian,
            subCategory = item.subCategory,
            datePersian = item.datePersian,
            paymentMethod = item.paymentMethod.name,
            sourceType = item.sourceType.name,
            sourceId = item.sourceId,
            isRecurring = item.isRecurring,
            userId = userId,
            accountId = item.accountId,
            transferSourceAccountId = item.transferSourceAccountId,
            transferDestinationAccountId = item.transferDestinationAccountId
        )
    }

    private fun toItemData(entity: TransactionEntity, allCats: List<TransactionCategory>): TransactionItemData {
        val typeEnum = try {
            TransactionType.valueOf(entity.type)
        } catch (e: Exception) {
            TransactionType.EXPENSE
        }
        val cat = allCats.find {
            it.title.equals(entity.category, ignoreCase = true) || it.id.equals(entity.category, ignoreCase = true)
        } ?: TransactionCategory(
            id = "cat_${entity.category.hashCode()}",
            title = entity.category.ifBlank { "عمومی" },
            iconEmoji = if (typeEnum == TransactionType.INCOME) "💰" else "🛍️",
            accentColor = if (typeEnum == TransactionType.INCOME) Color(0xFF10B981) else Color(0xFFEF4444),
            type = typeEnum
        )
        val payMethod = try {
            PaymentMethod.valueOf(entity.paymentMethod)
        } catch (e: Exception) {
            PaymentMethod.BANK_CARD
        }
        val srcType = try {
            TransactionSourceType.valueOf(entity.sourceType)
        } catch (e: Exception) {
            TransactionSourceType.MANUAL
        }
        return TransactionItemData(
            id = if (entity.stringId.isNotBlank()) entity.stringId else entity.id.toString(),
            title = if (entity.title.isNotBlank()) entity.title else entity.description.ifBlank { entity.category },
            amount = entity.amount,
            type = typeEnum,
            category = cat,
            categoryId = cat.id,
            subCategory = entity.subCategory,
            datePersian = if (entity.datePersian.isNotBlank()) entity.datePersian else PersianCalendarHelper.fromEpochMillis(entity.timestamp).toFormattedDate(),
            timePersian = entity.timeFormatted.ifBlank { "۱۲:۰۰" },
            dateMillis = entity.timestamp,
            description = entity.description,
            paymentMethod = payMethod,
            accountName = entity.accountName.ifBlank { "کارت بانکی" },
            sourceType = srcType,
            sourceId = entity.sourceId,
            tags = emptyList(),
            isRecurring = entity.isRecurring,
            accountId = entity.accountId,
            transferSourceAccountId = entity.transferSourceAccountId,
            transferDestinationAccountId = entity.transferDestinationAccountId
        )
    }

    override suspend fun addAccountResult(account: com.example.ui.screens.finance.model.Account): TransactionOperationResult {
        val userId = SessionManager.userId ?: return TransactionOperationResult.NO_AUTHENTICATED_USER
        if (account.name.isBlank()) {
            return TransactionOperationResult.VALIDATION_ERROR
        }
        val ctx = appContext ?: return TransactionOperationResult.PERSISTENCE_ERROR
        val db = AppDatabase.getDatabase(ctx)
        
        // Ensure isolation: use current logged-in user id
        val boundAccount = account.copy(userId = userId)
        
        return try {
            val entity = toAccountEntity(boundAccount)
            db.accountDao().insertAccount(entity)
            TransactionOperationResult.SUCCESS
        } catch (e: Exception) {
            e.printStackTrace()
            TransactionOperationResult.PERSISTENCE_ERROR
        }
    }

    override suspend fun updateAccountResult(account: com.example.ui.screens.finance.model.Account): TransactionOperationResult {
        val userId = SessionManager.userId ?: return TransactionOperationResult.NO_AUTHENTICATED_USER
        if (account.name.isBlank()) {
            return TransactionOperationResult.VALIDATION_ERROR
        }
        val ctx = appContext ?: return TransactionOperationResult.PERSISTENCE_ERROR
        val db = AppDatabase.getDatabase(ctx)
        
        return try {
            val existing = db.accountDao().getAccountByStringId(userId, account.id)
                ?: return TransactionOperationResult.NOT_FOUND
                
            val entity = toAccountEntity(account).copy(id = existing.id, userId = userId)
            db.accountDao().updateAccount(entity)
            TransactionOperationResult.SUCCESS
        } catch (e: Exception) {
            e.printStackTrace()
            TransactionOperationResult.PERSISTENCE_ERROR
        }
    }

    override suspend fun deleteAccountResult(id: String): TransactionOperationResult {
        val userId = SessionManager.userId ?: return TransactionOperationResult.NO_AUTHENTICATED_USER
        val ctx = appContext ?: return TransactionOperationResult.PERSISTENCE_ERROR
        val db = AppDatabase.getDatabase(ctx)
        
        return try {
            val existing = db.accountDao().getAccountByStringId(userId, id)
                ?: return TransactionOperationResult.NOT_FOUND
                
            db.accountDao().softDeleteAccount(userId, id, System.currentTimeMillis(), System.currentTimeMillis())
            TransactionOperationResult.SUCCESS
        } catch (e: Exception) {
            e.printStackTrace()
            TransactionOperationResult.PERSISTENCE_ERROR
        }
    }

    override suspend fun calculateAccountBalance(accountId: String): Long {
        val userId = SessionManager.userId ?: return 0L
        val ctx = appContext ?: return 0L
        val db = AppDatabase.getDatabase(ctx)
        val account = db.accountDao().getAccountByStringId(userId, accountId) ?: return 0L
        
        val txs = db.transactionDao().getAllTransactionsList(userId)
        
        var balance = account.initialBalance
        for (tx in txs) {
            if (tx.deletedAt != null) continue
            
            when (tx.type) {
                "INCOME" -> {
                    if (tx.accountId == accountId) {
                        balance += tx.amount
                    }
                }
                "EXPENSE" -> {
                    if (tx.accountId == accountId) {
                        balance -= tx.amount
                    }
                }
                "TRANSFER" -> {
                    if (tx.transferSourceAccountId == accountId) {
                        balance -= tx.amount
                    }
                    if (tx.transferDestinationAccountId == accountId) {
                        balance += tx.amount
                    }
                }
            }
        }
        return balance
    }

    companion object {
        private const val PREFS_NAME = "darino_general_preferences"
        private const val KEY_BUDGETS = "pref_persisted_budgets"
        private const val KEY_SAVINGS = "pref_persisted_savings_goals"
        private const val KEY_RECURRING = "pref_persisted_recurring_txs"
        private const val KEY_CATEGORIES = "pref_persisted_categories"

        private fun userScopedKey(base: String, userId: String?): String =
            if (userId.isNullOrBlank()) "${base}_anonymous" else "${base}_$userId"

        val instance: LocalFinanceRepository by lazy { LocalFinanceRepository() }
    }
}
