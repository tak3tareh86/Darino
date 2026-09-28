package com.example.ui.screens.home.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.WarningAmberLight

data class QuickActionItemData(
    val id: String,
    val title: String,
    @DrawableRes val iconRes: Int,
    val accentColor: Color,
    val testTag: String
)

@Composable
fun QuickActions(
    onAddExpenseClick: () -> Unit,
    onAddIncomeClick: () -> Unit,
    onAddInstallmentClick: () -> Unit,
    onAddReminderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        QuickActionItemData(
            id = "add_expense",
            title = "ثبت هزینه",
            iconRes = R.drawable.img_3d_card,
            accentColor = ExpenseRoseLight,
            testTag = "quick_action_add_expense"
        ),
        QuickActionItemData(
            id = "add_income",
            title = "ثبت درآمد",
            iconRes = R.drawable.img_3d_wallet,
            accentColor = EmeraldPrimaryLight,
            testTag = "quick_action_add_income"
        ),
        QuickActionItemData(
            id = "add_installment",
            title = "ثبت قسط",
            iconRes = R.drawable.img_3d_bank,
            accentColor = InfoIndigoLight,
            testTag = "quick_action_add_installment"
        ),
        QuickActionItemData(
            id = "add_reminder",
            title = "افزودن یادآور",
            iconRes = R.drawable.img_3d_bell_notification,
            accentColor = WarningAmberLight,
            testTag = "quick_action_add_reminder"
        )
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "دسترسی سریع",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                QuickActionPill(
                    item = item,
                    onClick = {
                        when (item.id) {
                            "add_expense" -> onAddExpenseClick()
                            "add_income" -> onAddIncomeClick()
                            "add_installment" -> onAddInstallmentClick()
                            "add_reminder" -> onAddReminderClick()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuickActionPill(
    item: QuickActionItemData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Layered3DCard(
        modifier = modifier.testTag(item.testTag),
        elevation = 3.dp,
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 6.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Soft3DIcon(
                imageRes = item.iconRes,
                contentDescription = item.title,
                size = 36.dp,
                accentColor = item.accentColor,
                containerShape = RoundedCornerShape(RadiusMD)
            )

            Text(
                text = item.title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
