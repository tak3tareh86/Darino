package com.example.ui.screens.settings.subviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.screens.settings.components.SettingsHeader
import com.example.ui.screens.settings.components.SettingsItem
import com.example.ui.screens.settings.components.SettingsSectionCard
import com.example.ui.screens.settings.model.AppLockType
import com.example.ui.theme.EmeraldPrimaryLight

@Composable
fun SecurityPrivacyScreen(
    currentLockType: AppLockType,
    onNavigateToAppLock: () -> Unit,
    onNavigateToLockAndAuth: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isBiometricEnabled by remember { mutableStateOf(currentLockType == AppLockType.BIOMETRIC) }
    var showBalanceOnHome by remember { mutableStateOf(true) }
    var isPrivacyModeEnabled by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SettingsHeader(
                title = "امنیت و حریم خصوصی",
                subtitle = "قفل برنامه، احراز هویت و پنهان‌سازی اطلاعات",
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Lock Section
            item {
                SettingsSectionCard(title = "قفل و احراز هویت ورود") {
                    SettingsItem(
                        title = "قفل و احراز هویت",
                        subtitle = "تنظیم PIN، الگو، اثر انگشت و انتخاب روش پیش‌فرض",
                        value = "مدیریت قفل",
                        iconRes = R.drawable.img_3d_shield_security,
                        iconAccentColor = EmeraldPrimaryLight,
                        onClick = onNavigateToLockAndAuth
                    )

                    SettingsItem(
                        title = "نوع سریع قفل",
                        subtitle = "تغییر سریع روش قفل‌گذاری",
                        value = currentLockType.title,
                        vectorIcon = Icons.Rounded.Lock,
                        iconAccentColor = EmeraldPrimaryLight,
                        onClick = onNavigateToAppLock
                    )
                }
            }

            // Privacy Display Section
            item {
                SettingsSectionCard(title = "حریم خصوصی و نمایش مبالغ") {
                    SettingsItem(
                        title = "حالت حریم خصوصی (Privacy Mode)",
                        subtitle = "اطلاعات مالی و مبالغ حساس را در صفحه‌های برنامه با ستاره (••••) مخفی می‌کند.",
                        vectorIcon = Icons.Rounded.Visibility,
                        iconAccentColor = Color(0xFF8B5CF6),
                        checked = isPrivacyModeEnabled,
                        onCheckedChange = { isPrivacyModeEnabled = it }
                    )

                    SettingsItem(
                        title = "نمایش موجودی کل در صفحه اصلی",
                        subtitle = "نمایش مانده حساب در کارت خلاصه داشبورد",
                        vectorIcon = Icons.Rounded.Lock,
                        iconAccentColor = EmeraldPrimaryLight,
                        checked = showBalanceOnHome,
                        onCheckedChange = { showBalanceOnHome = it }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
