package com.example.ui.screens.home.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.TransactionEntity
import com.example.ui.screens.home.data.HomeDashboardRepository
import com.example.ui.screens.home.domain.BankSmsSuggestion
import com.example.ui.screens.home.domain.HomeDashboardAggregator
import com.example.ui.screens.home.domain.HomeDashboardState
import com.example.util.IranianPhoneUtils
import com.example.util.MoneyFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class HomeDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HomeDashboardRepository(application)
    private val aggregator = HomeDashboardAggregator(repository)

    private val _uiState = MutableStateFlow(HomeDashboardState(isLoading = true))
    val uiState: StateFlow<HomeDashboardState> = _uiState.asStateFlow()

    // SMS suggestions queue for preventing home screen cluttering
    private val _pendingSmsQueue = MutableStateFlow<List<BankSmsSuggestion>>(emptyList())
    val pendingSmsQueue: StateFlow<List<BankSmsSuggestion>> = _pendingSmsQueue.asStateFlow()

    init {
        viewModelScope.launch {
            com.example.data.security.SessionManager.sessionState.collect {
                loadDashboardData()
            }
        }
        viewModelScope.launch {
            com.example.ui.screens.finance.data.LocalFinanceRepository.instance.getTransactions().collect {
                loadDashboardData()
            }
        }
        viewModelScope.launch {
            com.example.vehicle.data.VehicleRepository.instance.vehicles.collect {
                loadDashboardData()
            }
        }
        viewModelScope.launch {
            com.example.data.preferences.AppPreferencesRepository.getInstance(application).currency.collect {
                loadDashboardData()
            }
        }
        // Pre-populate with typical bank SMS messages in Iran to show the queue pattern
        prepopulateSmsQueue()
    }

    private fun prepopulateSmsQueue() {
        _pendingSmsQueue.value = listOf(
            BankSmsSuggestion(
                id = "sms_1",
                bankName = "بانک ملت",
                amount = 180_000L,
                formattedAmount = MoneyFormatter.formatSignedToman(180_000L, isExpense = true),
                isExpense = true,
                smsText = "بانک ملت\nبرداشت از حساب: ${MoneyFormatter.formatToman(180_000L)}\nخرید فروشگاهی افق کوروش\nمانده: ${MoneyFormatter.formatToman(2_450_000L)}",
                dateText = "۰۵ مهر ۱۴۰۳",
                timeText = "۱۶:۲۰",
                category = "سوپرمارکت"
            ),
            BankSmsSuggestion(
                id = "sms_2",
                bankName = "بانک ملی",
                amount = 2_400_000L,
                formattedAmount = MoneyFormatter.formatSignedToman(2_400_000L, isExpense = false),
                isExpense = false,
                smsText = "بانک ملی ایران\nواریز به حساب: ${MoneyFormatter.formatToman(2_400_000L)}\nکارت به کارت علی احمدی\nمانده: ${MoneyFormatter.formatToman(4_850_000L)}",
                dateText = "۰۶ مهر ۱۴۰۳",
                timeText = "۱۱:۴۵",
                category = "درآمد"
            ),
            BankSmsSuggestion(
                id = "sms_3",
                bankName = "بانک سامان",
                amount = 450_000L,
                formattedAmount = MoneyFormatter.formatSignedToman(450_000L, isExpense = true),
                isExpense = true,
                smsText = "بانک سامان\nبرداشت: ${MoneyFormatter.formatToman(450_000L)}\nخرید بنزین جایگاه کاج\nمانده: ${MoneyFormatter.formatToman(1_200_000L)}",
                dateText = "۰۶ مهر ۱۴۰۳",
                timeText = "۲۰:۱۰",
                category = "خودرو و بنزین"
            )
        )
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                val aggregated = aggregator.aggregate()
                _uiState.value = aggregated
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "خطا در بارگذاری اطلاعات داشبورد: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun acceptSmsSuggestion(id: String, customCategory: String? = null, customAccount: String? = null) {
        viewModelScope.launch {
            val item = _pendingSmsQueue.value.find { it.id == id } ?: return@launch
            try {
                val db = AppDatabase.getDatabase(getApplication())
                val userId = com.example.data.security.SessionManager.currentUser?.id ?: "default_user"
                
                val finalCategory = customCategory?.trim()?.ifEmpty { null } ?: item.category
                val finalAccount = customAccount?.trim()?.ifEmpty { null } ?: item.bankName

                // Add to Room transactions database
                db.transactionDao().insertTransaction(
                    TransactionEntity(
                        userId = userId,
                        amount = item.amount,
                        type = if (item.isExpense) "EXPENSE" else "INCOME",
                        category = finalCategory,
                        accountName = finalAccount,
                        description = "ثبت هوشمند از پیامک $finalAccount",
                        timestamp = System.currentTimeMillis(),
                        timeFormatted = item.timeText
                    )
                )

                // Add to LocalFinanceRepository so FinancialScreen and TransactionsScreen update immediately!
                val catObj = com.example.ui.screens.finance.model.FinanceDefaultCategories.allDefaultCategories.find { 
                    it.title.contains(finalCategory, ignoreCase = true) 
                } ?: com.example.ui.screens.finance.model.TransactionCategory(
                    id = "cat_gen_${System.currentTimeMillis()}",
                    title = finalCategory,
                    iconEmoji = if (item.isExpense) "🛍️" else "💰",
                    accentColor = if (item.isExpense) androidx.compose.ui.graphics.Color(0xFFEF4444) else androidx.compose.ui.graphics.Color(0xFF10B981),
                    type = if (item.isExpense) com.example.ui.screens.finance.model.TransactionType.EXPENSE else com.example.ui.screens.finance.model.TransactionType.INCOME
                )

                val finTx = com.example.ui.screens.finance.model.TransactionItemData(
                    id = "tx_sms_${System.currentTimeMillis()}",
                    title = if (item.isExpense) "برداشت: $finalCategory" else "واریز: $finalCategory",
                    amount = item.amount,
                    type = if (item.isExpense) com.example.ui.screens.finance.model.TransactionType.EXPENSE else com.example.ui.screens.finance.model.TransactionType.INCOME,
                    category = catObj,
                    datePersian = item.dateText,
                    timePersian = item.timeText,
                    accountName = finalAccount,
                    description = "ثبت هوشمند پیامک $finalAccount"
                )

                com.example.ui.screens.finance.data.LocalFinanceRepository.instance.addTransaction(finTx)

                // Remove from local queue
                _pendingSmsQueue.update { list -> list.filter { it.id != id } }
                
                // Refresh dashboard to reflect new balance & totals!
                loadDashboardData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun dismissSmsSuggestion(id: String) {
        _pendingSmsQueue.update { list -> list.filter { it.id != id } }
    }

    fun simulateIncomingSms() {
        val banks = listOf("پاسارگاد", "تجارت", "صادرات", "رسالت", "بلوبانک")
        val categories = listOf("غذا و رستوران", "پوشاک", "تفریح", "قسط", "قبوض")
        val randomBank = banks.random()
        val randomCategory = categories.random()
        val amount = Random.nextLong(20_000, 950_000)
        val isExpense = Random.nextBoolean()
        val formattedAmount = MoneyFormatter.formatSignedToman(amount, isExpense = isExpense)
        
        val typeText = if (isExpense) "برداشت (خرید)" else "واریز (کارت به کارت)"
        val randomId = "sim_${System.currentTimeMillis()}"

        val newSms = BankSmsSuggestion(
            id = randomId,
            bankName = "بانک $randomBank",
            amount = amount,
            formattedAmount = formattedAmount,
            isExpense = isExpense,
            smsText = "بانک $randomBank\n$typeText: $formattedAmount\nتراکنش شبیه‌سازی‌شده\nمانده: ${MoneyFormatter.formatToman(3_500_000L)}",
            dateText = "امروز",
            timeText = "۱۴:۳۰",
            category = if (isExpense) randomCategory else "درآمد"
        )

        _pendingSmsQueue.update { it + newSms }
    }

    fun markObligationDone(id: String) {
        _uiState.update { current ->
            val updatedList = current.upcomingObligations.filter { it.id != id }
            current.copy(
                upcomingObligations = updatedList,
                isAllClear = current.overdueItems.isEmpty() && updatedList.none { it.relativeDaysText == "امروز" }
            )
        }
    }

    fun dismissOverdueAlert(id: String) {
        _uiState.update { current ->
            val updated = current.overdueItems.filter { it.id != id }
            current.copy(
                overdueItems = updated,
                isAllClear = updated.isEmpty() && current.upcomingObligations.none { it.relativeDaysText == "امروز" }
            )
        }
    }

    fun markReminderCompleted(id: String) {
        _uiState.update { current ->
            val updated = current.upcomingReminders.filter { it.id != id }
            current.copy(upcomingReminders = updated)
        }
    }
}
