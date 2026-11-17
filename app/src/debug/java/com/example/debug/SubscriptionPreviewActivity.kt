package com.example.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.ui.screens.subscription.SubscriptionLockedScreen
import com.example.ui.theme.FinanceManagerTheme

/**
 * Debug-only launcher for testing the real expired-subscription screen and Bazaar actions.
 * This activity is excluded from release builds by the debug source set.
 */
class SubscriptionPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FinanceManagerTheme {
                // Keep the expired-screen presentation for testing, but delegate all
                // actions and subscription state to the production ViewModel.
                SubscriptionLockedScreen(forceLockedForDebug = true)
            }
        }
    }
}
