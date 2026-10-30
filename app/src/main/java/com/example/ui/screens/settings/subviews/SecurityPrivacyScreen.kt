package com.example.ui.screens.settings.subviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.screens.settings.components.SettingsHeader
import com.example.ui.screens.settings.components.SettingsItem
import com.example.ui.screens.settings.components.SettingsSectionCard
import com.example.ui.theme.EmeraldPrimaryLight

@Composable
fun SecurityPrivacyScreen(
    onNavigateToLockAndAuth: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SettingsHeader(
                title = "امنیت و حریم خصوصی",
                subtitle = "مدیریت دسترسی و تنظیمات امنیتی برنامه",
                showBack = true,
                showSearch = false,
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSectionCard(title = "مدیریت امنیت برنامه") {
                    SettingsItem(
                        title = "قفل و احراز هویت",
                        subtitle = "تنظیم PIN، الگو، اثر انگشت، روش پیش‌فرض ورود و حالت حریم خصوصی",
                        value = "مدیریت تنظیمات",
                        iconRes = R.drawable.img_3d_shield_security,
                        iconAccentColor = EmeraldPrimaryLight,
                        onClick = onNavigateToLockAndAuth
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
