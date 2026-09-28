package com.example.ui.screens.subscription

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.subscription.SubscriptionStatus

@Composable
fun SubscriptionGate(
    subscriptionViewModel: SubscriptionViewModel = viewModel(),
    content: @Composable () -> Unit
) {
    val uiState by subscriptionViewModel.uiState.collectAsState()
    val status = uiState.info.subscriptionStatus

    if (status == SubscriptionStatus.EXPIRED || status == SubscriptionStatus.LOCKED) {
        SubscriptionLockedScreen(viewModel = subscriptionViewModel)
    } else {
        content()
    }
}
