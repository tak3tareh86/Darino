package com.example.ui.screens.finance.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RadiusMD

@Composable
fun FinanceQuickLinks(
    onNavigateToBudgets: () -> Unit,
    onNavigateToSavingsGoals: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToCategories: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val cardBg = if (isDark) Color(0xFF131A29) else Color.White
    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickLinkItem(
            title = "بودجه‌ها",
            emoji = "📊",
            color = Color(0xFF6366F1),
            modifier = Modifier.weight(1f),
            onClick = onNavigateToBudgets,
            testTag = "finance_link_budgets"
        )

        QuickLinkItem(
            title = "اهداف پس‌انداز",
            emoji = "🎯",
            color = Color(0xFF10B981),
            modifier = Modifier.weight(1f),
            onClick = onNavigateToSavingsGoals,
            testTag = "finance_link_savings"
        )

        QuickLinkItem(
            title = "تکرارشونده",
            emoji = "🔁",
            color = Color(0xFFF59E0B),
            modifier = Modifier.weight(1f),
            onClick = onNavigateToRecurring,
            testTag = "finance_link_recurring"
        )

        QuickLinkItem(
            title = "دسته‌ها",
            emoji = "🏷️",
            color = Color(0xFFEC4899),
            modifier = Modifier.weight(1f),
            onClick = onNavigateToCategories,
            testTag = "finance_link_categories"
        )
    }
}

@Composable
private fun QuickLinkItem(
    title: String,
    emoji: String,
    color: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val cardBg = if (isDark) Color(0xFF131A29) else Color.White
    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)

    Surface(
        modifier = modifier
            .testTag(testTag)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(RadiusMD),
                ambientColor = color.copy(alpha = 0.1f),
                spotColor = color.copy(alpha = 0.2f)
            )
            .clip(RoundedCornerShape(RadiusMD))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(RadiusMD),
        color = cardBg,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = emoji, fontSize = 13.sp)
            Text(
                text = " $title",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
