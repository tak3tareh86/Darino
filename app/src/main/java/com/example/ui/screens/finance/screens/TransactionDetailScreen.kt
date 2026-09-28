package com.example.ui.screens.finance.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.finance.model.TransactionItemData
import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionDetailScreen(
    transaction: TransactionItemData,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onDuplicateClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    var showDeleteDialog by remember { mutableStateOf(false) }

    val amountColor = when (transaction.type) {
        TransactionType.EXPENSE -> ExpenseRoseLight
        TransactionType.INCOME -> if (isDark) Color(0xFF34D399) else EmeraldPrimaryLight
        TransactionType.TRANSFER -> InfoIndigoLight
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("transaction_detail_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "جزئیات تراکنش",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "بازگشت",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onDuplicateClick) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "کپی / تکرار",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "حذف",
                            tint = Color(0xFFEF4444)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Amount Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(RadiusLG),
                        ambientColor = transaction.category.accentColor.copy(alpha = 0.2f),
                        spotColor = transaction.category.accentColor.copy(alpha = 0.3f)
                    ),
                shape = RoundedCornerShape(RadiusLG),
                color = if (isDark) Color(0xFF131A29) else Color.White,
                border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(transaction.category.accentColor.copy(alpha = if (isDark) 0.25f else 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (transaction.category.iconRes != null) {
                            Image(
                                painter = painterResource(id = transaction.category.iconRes),
                                contentDescription = transaction.category.title,
                                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(text = transaction.category.iconEmoji, fontSize = 28.sp)
                        }
                    }

                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = transaction.amountFormatted,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp
                        ),
                        color = amountColor
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (transaction.type) {
                            TransactionType.EXPENSE -> ExpenseRoseLight.copy(alpha = 0.12f)
                            TransactionType.INCOME -> EmeraldPrimaryLight.copy(alpha = 0.12f)
                            TransactionType.TRANSFER -> InfoIndigoLight.copy(alpha = 0.12f)
                        }
                    ) {
                        Text(
                            text = transaction.type.title,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = amountColor
                        )
                    }
                }
            }

            // Transaction Details Breakdown List
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(RadiusMD),
                color = if (isDark) Color(0xFF131A29) else Color.White,
                border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailRow(label = "دسته‌بندی", value = "${transaction.category.iconEmoji} ${transaction.category.title}")

                    transaction.subCategory?.let { sub ->
                        DetailRow(label = "زیردسته‌بندی", value = sub)
                    }

                    DetailRow(label = "تاریخ", value = transaction.datePersian)
                    DetailRow(label = "ساعت", value = transaction.timePersian)
                    DetailRow(label = "روش پرداخت / حساب", value = "${transaction.paymentMethod.iconEmoji} ${transaction.paymentMethod.title}")

                    if (transaction.isRecurring) {
                        DetailRow(
                            label = "تراکنش تکرارشونده",
                            value = "بله (${transaction.recurringFrequency?.title ?: "دوره‌ای"})"
                        )
                    }

                    if (transaction.sourceType != com.example.ui.screens.finance.model.TransactionSourceType.MANUAL) {
                        DetailRow(label = "منبع ثبت", value = transaction.sourceType.title)
                    }

                    if (transaction.description.isNotBlank()) {
                        DetailRow(label = "توضیحات", value = transaction.description)
                    }

                    if (transaction.tags.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "برچسب‌ها",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                transaction.tags.forEach { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "#$tag",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Action Buttons (ویرایش + کپی)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onEditClick,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(RadiusMD),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("ویرایش تراکنش", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }

                OutlinedButton(
                    onClick = onDuplicateClick,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(RadiusMD)
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("تکرار سریع", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("حذف تراکنش") },
            text = { Text("آیا از حذف این تراکنش به مبلغ ${transaction.amountFormatted} اطمینان دارید؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteClick()
                    }
                ) {
                    Text("حذف", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
