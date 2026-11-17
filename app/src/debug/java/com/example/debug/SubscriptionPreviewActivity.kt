package com.example.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.ui.screens.subscription.SubscriptionLockedScreenContent
import com.example.ui.screens.subscription.SubscriptionState
import com.example.ui.theme.FinanceManagerTheme

/**
 * Debug-only launcher for visually checking the real subscription-expired screen.
 * It uses an isolated UI state and deliberately has no purchase or restore actions.
 */
class SubscriptionPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FinanceManagerTheme {
                SubscriptionLockedScreenContent(
                    uiState = SubscriptionState(),
                    requiresMarketReverification = false,
                    onPurchaseClick = {},
                    onRestoreClick = {},
                    actionsEnabled = false
                )
            }
        }
    }
}
