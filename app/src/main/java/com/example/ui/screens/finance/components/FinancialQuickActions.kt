package com.example.ui.screens.finance.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusMD

data class FinanceActionItem(
    val id: String,
    val title: String,
    val emoji: String,
    val accentColor: Color,
    val isPrimary: Boolean = false
)

@Composable
fun FinancialQuickActions(
    modifier: Modifier = Modifier,
    onActionClick: (String) -> Unit = {}
) {
    val actions = listOf(
        FinanceActionItem("add_expense", "ثبت هزینه", "💸", ExpenseRoseLight, isPrimary = true),
        FinanceActionItem("add_income", "ثبت درآمد", "💰", EmeraldPrimaryLight),
        FinanceActionItem("transfer", "ثبت انتقال وجه", "🔄", InfoIndigoLight)
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        actions.forEach { action ->
            FinanceActionButton(
                action = action,
                modifier = Modifier.weight(1f),
                onClick = { onActionClick(action.id) }
            )
        }
    }
}

@Composable
fun FinanceActionButton(
    action: FinanceActionItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val surfaceBg = if (action.isPrimary) {
        if (isDark) Color(0xFF2E1017) else Color(0xFFFFF1F2)
    } else {
        if (isDark) Color(0xFF131A29) else Color.White
    }

    val borderColor = if (action.isPrimary) {
        action.accentColor.copy(alpha = 0.45f)
    } else {
        if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
    }

    Surface(
        modifier = modifier
            .testTag("finance_action_${action.id}")
            .shadow(
                elevation = if (action.isPrimary) 6.dp else 2.dp,
                shape = RoundedCornerShape(RadiusMD),
                ambientColor = action.accentColor.copy(alpha = if (action.isPrimary) 0.35f else 0.15f),
                spotColor = action.accentColor.copy(alpha = if (action.isPrimary) 0.45f else 0.2f)
            )
            .clip(RoundedCornerShape(RadiusMD))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(RadiusMD),
        color = surfaceBg,
        border = BorderStroke(1.2.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .shadow(
                        elevation = 3.dp,
                        shape = CircleShape,
                        ambientColor = action.accentColor.copy(alpha = 0.25f),
                        spotColor = action.accentColor.copy(alpha = 0.4f)
                    )
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = action.accentColor.copy(alpha = if (isDark) 0.22f else 0.14f),
                    shape = CircleShape,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = action.emoji,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            Text(
                text = action.title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (action.isPrimary) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = if (action.id == "transfer") 10.5.sp else 12.sp
                ),
                maxLines = 1,
                softWrap = false,
                color = if (action.isPrimary) action.accentColor else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
