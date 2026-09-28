package com.example.financial_health.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.IranianPhoneUtils
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditIncomeBottomSheet(
    currentIncome: Long,
    currentFixedExpenses: Long,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSave: (income: Long, fixedExpenses: Long) -> Unit
) {
    var rawIncomeText by remember(currentIncome) {
        mutableStateOf(if (currentIncome > 0) currentIncome.toString() else "")
    }
    var rawFixedText by remember(currentFixedExpenses) {
        mutableStateOf(if (currentFixedExpenses > 0) currentFixedExpenses.toString() else "")
    }

    val decimalFormat = remember { DecimalFormat("#,###") }

    val formattedIncomePreview = remember(rawIncomeText) {
        val num = rawIncomeText.toLongOrNull() ?: 0L
        if (num > 0) {
            IranianPhoneUtils.convertDigitsToPersian(decimalFormat.format(num)) + " تومان"
        } else {
            "۰ تومان"
        }
    }

    val formattedFixedPreview = remember(rawFixedText) {
        val num = rawFixedText.toLongOrNull() ?: 0L
        if (num > 0) {
            IranianPhoneUtils.convertDigitsToPersian(decimalFormat.format(num)) + " تومان"
        } else {
            "۰ تومان"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("edit_income_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "تنظیم درآمد و تعهدات پایه",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
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

            // Description
            Text(
                text = "برای محاسبه دقیق شاخص سلامت مالی و فشار ماهانه، درآمد خالص ماهانه خود را وارد کنید. این اطلاعات کاملاً محلی و امن ذخیره می‌شود.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Monthly Income Field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "درآمد ماهانه من",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formattedIncomePreview,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = rawIncomeText,
                    onValueChange = { input ->
                        val digits = IranianPhoneUtils.convertDigitsToEnglish(input).filter { it.isDigit() }
                        if (digits.length <= 12) {
                            rawIncomeText = digits
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_monthly_income"),
                    placeholder = { Text("مثال: ۳۰,۰۰۰,۰۰۰ تومان") },
                    trailingIcon = {
                        Text(
                            text = "تومان",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Quick presets for Income
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "۲۰ م" to 20_000_000L,
                        "۳۰ م" to 30_000_000L,
                        "۴۰ م" to 40_000_000L,
                        "۵۰ م" to 50_000_000L
                    ).forEach { (label, value) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { rawIncomeText = value.toString() }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = IranianPhoneUtils.convertDigitsToPersian(label),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Fixed Expenses Field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "هزینه‌های ثابت ماهانه (قبوض، اجاره، شارژ)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formattedFixedPreview,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                OutlinedTextField(
                    value = rawFixedText,
                    onValueChange = { input ->
                        val digits = IranianPhoneUtils.convertDigitsToEnglish(input).filter { it.isDigit() }
                        if (digits.length <= 12) {
                            rawFixedText = digits
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_fixed_expenses"),
                    placeholder = { Text("مثال: ۴,۰۰۰,۰۰۰ تومان") },
                    trailingIcon = {
                        Text(
                            text = "تومان",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Save Action Button
            Button(
                onClick = {
                    val incomeVal = rawIncomeText.toLongOrNull() ?: 30_000_000L
                    val fixedVal = rawFixedText.toLongOrNull() ?: 4_000_000L
                    onSave(incomeVal, fixedVal)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_save_financial_health_profile"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "ذخیره و محاسبه مجدد شاخص",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }
    }
}
