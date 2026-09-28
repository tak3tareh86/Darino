package com.example.ui.screens.finance.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Layered3DCard
import com.example.ui.screens.finance.model.TransactionItemData
import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusMD

@Composable
fun RecentTransactionsSection(
    transactions: List<TransactionItemData>,
    modifier: Modifier = Modifier,
    onTransactionClick: (TransactionItemData) -> Unit = {},
    onEditClick: (TransactionItemData) -> Unit = {},
    onDuplicateClick: (TransactionItemData) -> Unit = {},
    onDeleteClick: (TransactionItemData) -> Unit = {},
    onSeeAllClick: () -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "آخرین تراکنش‌ها",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onSeeAllClick)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .testTag("finance_see_all_transactions_btn"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "همه",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "مشاهده همه تراکنش‌ها",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "تراکنشی در این ماه ثبت نشده است",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                transactions.take(4).forEach { tx ->
                    TransactionRowItem(
                        transaction = tx,
                        onClick = { onTransactionClick(tx) },
                        onEditClick = { onEditClick(tx) },
                        onDuplicateClick = { onDuplicateClick(tx) },
                        onDeleteClick = { onDeleteClick(tx) }
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionRowItem(
    transaction: TransactionItemData,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onEditClick: () -> Unit = {},
    onDuplicateClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    val isExpense = transaction.type == TransactionType.EXPENSE
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    var menuExpanded by remember { mutableStateOf(false) }

    val amountColor = when (transaction.type) {
        TransactionType.EXPENSE -> ExpenseRoseLight
        TransactionType.INCOME -> if (isDark) Color(0xFF34D399) else EmeraldPrimaryLight
        TransactionType.TRANSFER -> InfoIndigoLight
    }

    Layered3DCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusMD),
        backgroundColor = MaterialTheme.colorScheme.surface,
        elevation = 2.dp,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        testTag = "transaction_item_${transaction.id}",
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Right Side: 3D Category Icon + Title + Meta Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Soft 3D Icon Container
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            transaction.category.accentColor.copy(alpha = if (isDark) 0.25f else 0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (transaction.category.iconRes != null) {
                        Image(
                            painter = painterResource(id = transaction.category.iconRes),
                            contentDescription = transaction.category.title,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = transaction.category.iconEmoji,
                            fontSize = 18.sp
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = transaction.category.title,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )

                        Text(
                            text = transaction.datePersian,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Left Side: Amount + Payment Method Icon + Options Dropdown Menu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = transaction.amountFormatted,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        ),
                        color = amountColor
                    )

                    Text(
                        text = transaction.paymentMethod.title,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "گزینه‌های تراکنش",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("👁️ مشاهده جزئیات", style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                menuExpanded = false
                                onClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("✏️ ویرایش تراکنش", style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                menuExpanded = false
                                onEditClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("📋 کپی و تکرار", style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                menuExpanded = false
                                onDuplicateClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("❌ حذف تراکنش", style = MaterialTheme.typography.bodyMedium, color = ExpenseRoseLight) },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }
        }
    }
}
