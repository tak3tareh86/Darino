package com.example.ui.screens.settings.subviews

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.screens.settings.components.SettingsHeader

@Composable
fun SmsPatternManagementScreen(onBackClick: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    var requestAddBank by remember { mutableStateOf(0) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { requestAddBank++ }) {
                    Icon(Icons.Rounded.Add, contentDescription = "افزودن بانک")
                }
            }
        },
        topBar = {
            SettingsHeader(
                title = "مدیریت الگوهای پیامکی",
                subtitle = "قواعد تشخیص پیامک بانکی را مدیریت کنید",
                showBack = true,
                showSearch = false,
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp)
        ) {
            item {
                UserBankSmsRulesSection(selectedTab, { selectedTab = it }, requestAddBank)
            }
        }
    }
}
