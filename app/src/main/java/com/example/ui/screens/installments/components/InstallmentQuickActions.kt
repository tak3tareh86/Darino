package com.example.ui.screens.installments.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Layered3DCard
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.RadiusMD

@Composable
fun InstallmentQuickActions(
    onAddInstallmentClick: () -> Unit,
    onLoanCalculatorClick: () -> Unit,
    onFinancialHealthClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickActionButton(
            title = "افزودن قسط",
            icon = Icons.Rounded.Add,
            accentColor = EmeraldPrimaryLight,
            onClick = onAddInstallmentClick,
            testTag = "quick_action_add_installment",
            modifier = Modifier.weight(1f)
        )

        QuickActionButton(
            title = "محاسبه‌گر اقساط",
            icon = Icons.Rounded.Calculate,
            accentColor = Color(0xFF0EA5E9),
            onClick = onLoanCalculatorClick,
            testTag = "quick_action_loan_calculator",
            modifier = Modifier.weight(1f)
        )

        QuickActionButton(
            title = "وضعیت مالی من",
            icon = Icons.Rounded.Assessment,
            accentColor = Color(0xFF8B5CF6),
            onClick = onFinancialHealthClick,
            testTag = "quick_action_financial_health",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionButton(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Layered3DCard(
        modifier = modifier,
        shape = RoundedCornerShape(RadiusMD),
        backgroundColor = MaterialTheme.colorScheme.surface,
        elevation = 2.dp,
        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp),
        testTag = testTag,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.padding(end = 4.dp).size(16.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
