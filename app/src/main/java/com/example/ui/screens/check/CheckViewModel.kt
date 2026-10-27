package com.example.ui.screens.check

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ui.screens.check.data.CheckOperationResult
import com.example.ui.screens.check.data.LocalCheckRepository
import com.example.ui.screens.check.model.Check
import com.example.ui.screens.check.model.CheckStatus
import com.example.ui.screens.check.model.CheckType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CheckUiState(
    val checks: List<Check> = emptyList(),
    val filteredChecks: List<Check> = emptyList(),
    val searchQuery: String = "",
    val selectedType: CheckType? = null,
    val selectedStatus: CheckStatus? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class CheckViewModel : ViewModel() {
    private val repository = LocalCheckRepository.instance
    private val financeRepository = com.example.ui.screens.finance.data.LocalFinanceRepository.instance

    private val _searchQuery = MutableStateFlow("")
    private val _selectedType = MutableStateFlow<CheckType?>(null)
    private val _selectedStatus = MutableStateFlow<CheckStatus?>(null)

    val uiState: StateFlow<CheckUiState> = combine(
        repository.getChecks(),
        financeRepository.getAccounts(),
        _searchQuery,
        _selectedType,
        _selectedStatus
    ) { checks, accounts, query, type, status ->
        val filtered = checks.filter { check ->
            val matchesQuery = check.checkNumber.contains(query, ignoreCase = true) ||
                    check.holderName.contains(query, ignoreCase = true) ||
                    check.receiverName.contains(query, ignoreCase = true) ||
                    check.bankName.contains(query, ignoreCase = true) ||
                    (check.notes?.contains(query, ignoreCase = true) ?: false)

            val matchesType = type == null || check.type == type
            val matchesStatus = status == null || check.status == status

            matchesQuery && matchesType && matchesStatus
        }

        CheckUiState(
            checks = checks,
            filteredChecks = filtered,
            searchQuery = query,
            selectedType = type,
            selectedStatus = status,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CheckUiState(isLoading = true)
    )

    val accounts: StateFlow<List<com.example.ui.screens.finance.model.Account>> = financeRepository.getAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onTypeSelected(type: CheckType?) {
        _selectedType.value = type
    }

    fun onStatusSelected(status: CheckStatus?) {
        _selectedStatus.value = status
    }

    fun addCheck(check: Check, onResult: (CheckOperationResult) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.addCheck(check)
            onResult(result)
        }
    }

    fun updateCheck(check: Check, onResult: (CheckOperationResult) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.updateCheck(check)
            onResult(result)
        }
    }

    fun deleteCheck(id: Int, onResult: (CheckOperationResult) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.deleteCheck(id)
            onResult(result)
        }
    }

    fun updateCheckStatus(check: Check, newStatus: CheckStatus, onResult: (CheckOperationResult) -> Unit = {}) {
        viewModelScope.launch {
            val updatedCheck = check.copy(status = newStatus, updatedAt = System.currentTimeMillis())
            val result = repository.updateCheck(updatedCheck)
            onResult(result)
        }
    }

    fun settleCheck(check: Check, account: com.example.ui.screens.finance.model.Account, onResult: (CheckOperationResult) -> Unit = {}) {
        viewModelScope.launch {
            val newStatus = if (check.type == CheckType.RECEIVED) CheckStatus.CLEARED else CheckStatus.CLEARED // Or PAID for paid checks
            // Wait, I defined CLEARED for both in CheckStatus title? 
            // CLEARED("وصول شده", 0xFF10B981)
            // Let's use PAID for paid checks if I want different titles, but CLEARED is fine for both in sense of "settled".
            
            val updatedCheck = check.copy(
                status = newStatus, 
                accountId = account.id,
                updatedAt = System.currentTimeMillis()
            )
            
            val result = repository.updateCheck(updatedCheck)
            
            if (result == CheckOperationResult.SUCCESS) {
                // Create financial transaction
                val transaction = com.example.ui.screens.finance.model.TransactionItemData(
                    id = java.util.UUID.randomUUID().toString(),
                    title = "وصول چک: ${check.bankName} (${check.checkNumber})",
                    amount = check.amount,
                    type = if (check.type == CheckType.RECEIVED) 
                        com.example.ui.screens.finance.model.TransactionType.INCOME 
                    else 
                        com.example.ui.screens.finance.model.TransactionType.EXPENSE,
                    category = com.example.ui.screens.finance.model.FinanceDefaultCategories.transferCategory, // Or a better category
                    datePersian = com.example.util.PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate(),
                    timePersian = com.example.util.PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedTime(),
                    dateMillis = System.currentTimeMillis(),
                    description = "تسویه خودکار چک شماره ${check.checkNumber}",
                    paymentMethod = com.example.ui.screens.finance.model.PaymentMethod.OTHER,
                    accountName = account.name,
                    accountId = account.id,
                    sourceType = com.example.ui.screens.finance.model.TransactionSourceType.OTHER,
                    sourceId = check.id.toString()
                )
                
                financeRepository.addTransactionResult(transaction)
            }
            
            onResult(result)
        }
    }
}
