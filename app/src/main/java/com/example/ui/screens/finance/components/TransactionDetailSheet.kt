package com.example.ui.screens.finance.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.finance.model.TransactionItemData
import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusMD

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailSheet(
    transaction: TransactionItemData,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onDeleteTransaction: (TransactionItemData) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isExpense = transaction.type == TransactionType.EXPENSE
    val amountColor = if (isExpense) ExpenseRoseLight else EmeraldPrimaryLight

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("transaction_detail_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(transaction.category.accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ReceiptLong,
                            contentDescription = null,
                            tint = transaction.category.accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "جزئیات تراکنش",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "بستن",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Amount Display
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = transaction.category.accentColor.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(RadiusMD)
                    )
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = transaction.amountFormatted,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = amountColor
                )
            }

            // Details List
            DetailRowItem(label = "نوع تراکنش", value = if (isExpense) "هزینه" else "درآمد")
            DetailRowItem(label = "دسته‌بندی", value = "${transaction.category.iconEmoji} ${transaction.category.title}")
            DetailRowItem(label = "تاریخ و زمان", value = "${transaction.datePersian} ساعت ${transaction.timePersian}")
            DetailRowItem(label = "حساب / روش پرداخت", value = transaction.accountName)

            // Action Buttons: Edit and Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onDeleteTransaction(transaction)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("delete_tx_btn"),
                    shape = RoundedCornerShape(RadiusMD),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRoseLight)
                ) {
                    Icon(imageVector = Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("حذف")
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("edit_tx_btn"),
                    shape = RoundedCornerShape(RadiusMD)
                ) {
                    Icon(imageVector = Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("ویرایش")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailRowItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
