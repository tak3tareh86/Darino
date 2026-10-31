package com.example.ui.screens.subscription

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.subscription.SubscriptionStatus

/**
 * Applies subscription access to the whole authenticated application.
 * No destination or bottom-navigation route is composed while access is locked.
 */
@Composable
fun SubscriptionGate(
    subscriptionViewModel: SubscriptionViewModel = viewModel(),
    content: @Composable () -> Unit
) {
    val uiState by subscriptionViewModel.uiState.collectAsState()
    val status = uiState.info.subscriptionStatus

    when (status) {
        SubscriptionStatus.EXPIRED,
        SubscriptionStatus.LOCKED -> {
            // This is intentionally non-dismissible. Only a verified purchase or restore
            // updates the observed subscription state and releases the application.
            BackHandler(enabled = true) {}
            SubscriptionLockedScreen(viewModel = subscriptionViewModel)
        }

        SubscriptionStatus.CHECKING,
        SubscriptionStatus.ERROR -> {
            // Fail closed while the persisted entitlement / Market state is being resolved.
            BackHandler(enabled = true) {}
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "در حال بررسی وضعیت اشتراک...",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        SubscriptionStatus.TRIAL,
        SubscriptionStatus.ACTIVE -> content()
    }
}
