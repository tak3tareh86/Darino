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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import com.example.data.security.SessionManager
import com.example.data.security.SessionState

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

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CheckUiState> = SessionManager.sessionState.flatMapLatest { state ->
        if (state is SessionState.Authenticated || state is SessionState.PhoneVerificationRequired) {
            combine(
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
            }
        } else {
            MutableStateFlow(CheckUiState(isLoading = false))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CheckUiState(isLoading = true)
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val accounts: StateFlow<List<com.example.ui.screens.finance.model.Account>> = SessionManager.sessionState.flatMapLatest { state ->
        if (state is SessionState.Authenticated || state is SessionState.PhoneVerificationRequired) {
            financeRepository.getAccounts()
        } else {
            MutableStateFlow(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            val result = repository.settleCheck(check, account)
            onResult(result)
        }
    }
}
