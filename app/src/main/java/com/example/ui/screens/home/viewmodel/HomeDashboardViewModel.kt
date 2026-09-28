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
        // Pre-populate with typical bank SMS messages in Iran to show the queue pattern
        prepopulateSmsQueue()
    }

    private fun prepopulateSmsQueue() {
        _pendingSmsQueue.value = listOf(
            BankSmsSuggestion(
                id = "sms_1",
                bankName = "بانک ملت",
                amount = 180_000L,
                formattedAmount = IranianPhoneUtils.convertDigitsToPersian("۱۸۰,۰۰۰") + " تومان",
                isExpense = true,
                smsText = "بانک ملت\nبرداشت از حساب: ۱۸۰,۰۰۰ تومان\nخرید فروشگاهی افق کوروش\nمانده: ۲,۴۵۰,۰۰۰ تومان",
                dateText = "۰۵ مهر",
                category = "سوپرمارکت"
            ),
            BankSmsSuggestion(
                id = "sms_2",
                bankName = "بانک ملی",
                amount = 2_400_000L,
                formattedAmount = IranianPhoneUtils.convertDigitsToPersian("۲,۴۰۰,۰۰۰") + " تومان",
                isExpense = false,
                smsText = "بانک ملی ایران\nواریز به حساب: ۲,۴۰۰,۰۰۰ تومان\nکارت به کارت علی احمدی\nمانده: ۴,۸۵۰,۰۰۰ تومان",
                dateText = "۰۶ مهر",
                category = "درآمد"
            ),
            BankSmsSuggestion(
                id = "sms_3",
                bankName = "بانک سامان",
                amount = 450_000L,
                formattedAmount = IranianPhoneUtils.convertDigitsToPersian("۴۵۰,۰۰۰") + " تومان",
                isExpense = true,
                smsText = "بانک سامان\nبرداشت: ۴۵۰,۰۰۰ تومان\nخرید بنزین جایگاه کاج\nمانده: ۱,۲۰۰,۰۰۰ تومان",
                dateText = "۰۶ مهر",
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

    fun acceptSmsSuggestion(id: String) {
        viewModelScope.launch {
            val item = _pendingSmsQueue.value.find { it.id == id } ?: return@launch
            try {
                val db = AppDatabase.getDatabase(getApplication())
                val userId = com.example.data.security.SessionManager.currentUser?.id ?: "default_user"
                
                // Add to Room transactions database
                db.transactionDao().insertTransaction(
                    TransactionEntity(
                        userId = userId,
                        amount = item.amount,
                        type = if (item.isExpense) "EXPENSE" else "INCOME",
                        category = item.category,
                        accountName = item.bankName,
                        description = "ثبت هوشمند از پیامک " + item.bankName,
                        timestamp = System.currentTimeMillis(),
                        timeFormatted = "۱۰:۴۵"
                    )
                )

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
        val formattedAmount = IranianPhoneUtils.convertDigitsToPersian(String.format("%,d", amount)) + " تومان"
        val isExpense = Random.nextBoolean()
        
        val typeText = if (isExpense) "برداشت (خرید)" else "واریز (کارت به کارت)"
        val randomId = "sim_${System.currentTimeMillis()}"

        val newSms = BankSmsSuggestion(
            id = randomId,
            bankName = "بانک $randomBank",
            amount = amount,
            formattedAmount = formattedAmount,
            isExpense = isExpense,
            smsText = "بانک $randomBank\n$typeText: $formattedAmount\nتراکنش شبیه‌سازی‌شده\nمانده: ۳,۵۰۰,۰۰۰ تومان",
            dateText = "امروز",
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
