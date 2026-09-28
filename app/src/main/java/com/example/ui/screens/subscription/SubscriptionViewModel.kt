package com.example.ui.screens.subscription

import android.app.Application
import androidx.activity.result.ActivityResultRegistry
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.subscription.SubscriptionRepositoryImpl
import com.example.domain.subscription.SubscriptionInfo
import com.example.domain.subscription.SubscriptionManager
import com.example.domain.subscription.SubscriptionStatus
import com.example.domain.subscription.SubscriptionValidator
import ir.cafebazaar.poolakey.entity.PurchaseInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SubscriptionViewModel(application: Application) : AndroidViewModel(application) {

    private val manager: SubscriptionManager = SubscriptionManager(
        SubscriptionRepositoryImpl.getInstance(application.applicationContext)
    )

    private val _uiState = MutableStateFlow(SubscriptionState())
    val uiState: StateFlow<SubscriptionState> = _uiState.asStateFlow()

    init {
        observeSubscription()
        syncWithMarketOnStartup()
    }

    fun initializeUserTrial(userId: String) {
        viewModelScope.launch {
            val info = manager.ensureTrialStarted(userId)
            updateStateWithInfo(info)
        }
    }

    private fun observeSubscription() {
        viewModelScope.launch {
            manager.observeSubscriptionInfo().collect { info ->
                updateStateWithInfo(info)
            }
        }
    }

    private fun updateStateWithInfo(info: SubscriptionInfo) {
        val now = System.currentTimeMillis()
        val trialDays = SubscriptionValidator.calculateTrialDaysRemaining(info, now)
        val trialJalali = SubscriptionValidator.formatEndJalaliDate(info.trialEndAt)
        val subDays = SubscriptionValidator.calculateSubscriptionDaysRemaining(info, now)
        val subJalali = SubscriptionValidator.formatEndJalaliDate(info.subscriptionEndAt ?: 0L)

        _uiState.update { current ->
            current.copy(
                info = info,
                trialDaysRemaining = trialDays,
                trialEndJalaliDate = trialJalali,
                subscriptionDaysRemaining = subDays,
                subscriptionEndJalaliDate = subJalali
            )
        }
    }

    fun syncWithMarketOnStartup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, userMessage = null) }
            val result = manager.syncWithMarket()
            result.onSuccess { info ->
                _uiState.update { it.copy(isSyncing = false) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        userMessage = error.localizedMessage ?: "امکان بازیابی خودکار از بازار وجود ندارد."
                    )
                }
            }
        }
    }

    fun manualRestorePurchase() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, userMessage = "در حال ارتباط با بازار...") }
            val result = manager.syncWithMarket()
            result.onSuccess { info ->
                val msg = if (info.subscriptionStatus == SubscriptionStatus.ACTIVE) {
                    "اشتراک شما با موفقیت بازیابی شد."
                } else {
                    "هیچ اشتراک فعال دارینو در حساب بازار فعلی یافت نشد."
                }
                _uiState.update { it.copy(isSyncing = false, userMessage = msg, isError = info.subscriptionStatus != SubscriptionStatus.ACTIVE) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        userMessage = "خطا در ارتباط با بازار: ${error.message}",
                        isError = true
                    )
                }
            }
        }
    }

    fun startPurchaseFlow(registry: ActivityResultRegistry, onSuccessNavigation: () -> Unit) {
        _uiState.update { it.copy(isLoading = true, userMessage = null) }

        manager.launchPurchaseFlow(
            registry = registry,
            onFlowBegan = {
                _uiState.update { it.copy(userMessage = "در حال انتقال به درگاه بازار...") }
            },
            onFailedToBegin = { errorMsg ->
                _uiState.update { it.copy(isLoading = false, userMessage = errorMsg, isError = true) }
            },
            onSucceed = { purchaseInfo ->
                viewModelScope.launch {
                    val updatedInfo = manager.recordSuccessfulPurchase(purchaseInfo)
                    updateStateWithInfo(updatedInfo)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userMessage = "خرید اشتراک سالیانه دارینو با موفقیت انجام شد.",
                            isError = false
                        )
                    }
                    onSuccessNavigation()
                }
            },
            onCanceled = {
                _uiState.update { it.copy(isLoading = false, userMessage = "عملیات خرید توسط کاربر لغو شد.", isError = false) }
            },
            onFailed = { errorMsg ->
                _uiState.update { it.copy(isLoading = false, userMessage = errorMsg, isError = true) }
            }
        )
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        manager.disconnect()
    }
}
