package com.example.ui.screens.finance.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.components.AppSectionHeader

@Composable
fun FinancialHeader(
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    AppSectionHeader(
        title = "مالی",
        subtitle = "مدیریت درآمد، هزینه و پس‌انداز",
        modifier = modifier,
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.AccountBalanceWallet,
                contentDescription = "مدیریت مالی",
                tint = if (isDark) Color.White else MaterialTheme.colorScheme.primary,
                modifier = Modifier
            )
        }
    )
}
