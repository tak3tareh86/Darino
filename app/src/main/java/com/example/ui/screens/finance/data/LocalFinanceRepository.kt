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
import com.example.ui.screens.finance.model.FinanceMockDataSource
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

    private var appContext: Context? = null
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val appCtx = context.applicationContext
        appContext = appCtx

        loadMetadataFromDisk(appCtx)

        val db = AppDatabase.getDatabase(appCtx)
        repositoryScope.launch {
            try {
                db.transactionDao().getAllTransactions().collect { entities ->
                    // Room is the production source of truth; an empty database stays empty.
                    val mapped = entities.map { toItemData(it, _categories.value) }
                    _transactions.value = mapped
                    recalculateBudgets()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun getTransactions(): Flow<List<TransactionItemData>> = _transactions.asStateFlow()
    override fun getCategories(): Flow<List<TransactionCategory>> = _categories.asStateFlow()
    override fun getBudgets(): Flow<List<Budget>> = _budgets.asStateFlow()
    override fun getSavingsGoals(): Flow<List<SavingsGoal>> = _savingsGoals.asStateFlow()
    override fun getRecurringTransactions(): Flow<List<RecurringTransaction>> = _recurringTransactions.asStateFlow()

    override fun addTransaction(transaction: TransactionItemData) {
        val current = _transactions.value.toMutableList()
        current.add(0, transaction)
        _transactions.value = current
        recalculateBudgets()

        val ctx = appContext ?: return
        repositoryScope.launch {
            try {
                AppDatabase.getDatabase(ctx).transactionDao().insertTransaction(toEntity(transaction))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun updateTransaction(transaction: TransactionItemData) {
        val current = _transactions.value.toMutableList()
        val index = current.indexOfFirst { it.id == transaction.id }
        if (index != -1) {
            current[index] = transaction
            _transactions.value = current
            recalculateBudgets()
        }

        val ctx = appContext ?: return
        repositoryScope.launch {
            try {
                AppDatabase.getDatabase(ctx).transactionDao().insertTransaction(toEntity(transaction))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun deleteTransaction(id: String) {
        val current = _transactions.value.toMutableList()
        current.removeAll { it.id == id }
        _transactions.value = current
        recalculateBudgets()

        val ctx = appContext ?: return
        repositoryScope.launch {
            try {
                AppDatabase.getDatabase(ctx).transactionDao().deleteByStringId(id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun duplicateTransaction(id: String): TransactionItemData? {
        val original = _transactions.value.find { it.id == id } ?: return null
        val duplicate = original.copy(
            id = UUID.randomUUID().toString(),
            title = "${original.title} (کپی)",
            datePersian = "امروز",
            dateMillis = System.currentTimeMillis()
        )
        addTransaction(duplicate)
        return duplicate
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
        current.add(recalculated)
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
        _transactions.value = emptyList()
        _recurringTransactions.value = emptyList()
        _budgets.value = emptyList()
        _savingsGoals.value = emptyList()
        saveMetadataToDisk()

        val ctx = appContext ?: return
        repositoryScope.launch {
            try {
                AppDatabase.getDatabase(ctx).transactionDao().clearAllTransactions()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun restoreSampleTransactions() {
        _recurringTransactions.value = FinanceMockDataSource.initialRecurring
        _budgets.value = FinanceMockDataSource.initialBudgets
        _savingsGoals.value = FinanceMockDataSource.initialSavingsGoals
        saveMetadataToDisk()

        val ctx = appContext ?: return
        repositoryScope.launch {
            try {
                val db = AppDatabase.getDatabase(ctx)
                db.transactionDao().clearAllTransactions()
                val initialEntities = FinanceMockDataSource.initialTransactions.map { toEntity(it) }
                db.transactionDao().insertTransactions(initialEntities)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
        repositoryScope.launch {
            try {
                val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val editor = prefs.edit()

                // Budgets
                val bArr = JSONArray()
                _budgets.value.forEach { b ->
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
                editor.putString(KEY_BUDGETS, bArr.toString())

                // Savings Goals
                val gArr = JSONArray()
                _savingsGoals.value.forEach { g ->
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
                editor.putString(KEY_SAVINGS, gArr.toString())

                // Recurring
                val rArr = JSONArray()
                _recurringTransactions.value.forEach { r ->
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
                editor.putString(KEY_RECURRING, rArr.toString())

                editor.apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadMetadataFromDisk(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isCleanSlate = prefs.getBoolean("pref_is_clean_slate", false)

            val budgetsJson = prefs.getString(KEY_BUDGETS, null)
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
            } else if (!isCleanSlate) {
                _budgets.value = FinanceMockDataSource.initialBudgets
            }

            val savingsJson = prefs.getString(KEY_SAVINGS, null)
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
            } else if (!isCleanSlate) {
                _savingsGoals.value = FinanceMockDataSource.initialSavingsGoals
            }

            val recurringJson = prefs.getString(KEY_RECURRING, null)
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
            } else if (!isCleanSlate) {
                _recurringTransactions.value = FinanceMockDataSource.initialRecurring
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toEntity(item: TransactionItemData): TransactionEntity {
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
            isRecurring = item.isRecurring
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
            isRecurring = entity.isRecurring
        )
    }

    companion object {
        private const val PREFS_NAME = "darino_general_preferences"
        private const val KEY_BUDGETS = "pref_persisted_budgets"
        private const val KEY_SAVINGS = "pref_persisted_savings_goals"
        private const val KEY_RECURRING = "pref_persisted_recurring_txs"

        val instance: LocalFinanceRepository by lazy { LocalFinanceRepository() }
    }
}
