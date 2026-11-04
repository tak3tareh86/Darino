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
import com.example.ui.components.PersianAmountInputField
import com.example.util.IranianAmountUtils
import com.example.util.IranianPhoneUtils
import com.example.util.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditIncomeBottomSheet(
    currentIncome: Long,
    currentFixedExpenses: Long,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSave: (income: Long, fixedExpenses: Long, onResult: (Boolean) -> Unit) -> Unit
) {
    val activeCurrency = MoneyFormatter.activeCurrency
    var rawIncomeText by remember(currentIncome) {
        mutableStateOf(if (currentIncome > 0L) MoneyFormatter.convertFromToman(currentIncome).toString() else "")
    }
    var rawFixedText by remember(currentFixedExpenses) {
        mutableStateOf(if (currentFixedExpenses > 0L) MoneyFormatter.convertFromToman(currentFixedExpenses).toString() else "")
    }
    var previousCurrency by remember { mutableStateOf(activeCurrency) }

    // Preserve in-progress values when the selected currency changes while this sheet is open.
    LaunchedEffect(activeCurrency) {
        if (previousCurrency != activeCurrency) {
            val incomeInToman = MoneyFormatter.convertToToman(IranianAmountUtils.parseAmountToLong(rawIncomeText), previousCurrency)
            val fixedInToman = MoneyFormatter.convertToToman(IranianAmountUtils.parseAmountToLong(rawFixedText), previousCurrency)
            rawIncomeText = if (rawIncomeText.isBlank()) "" else MoneyFormatter.convertFromToman(incomeInToman, activeCurrency).toString()
            rawFixedText = if (rawFixedText.isBlank()) "" else MoneyFormatter.convertFromToman(fixedInToman, activeCurrency).toString()
            previousCurrency = activeCurrency
        }
    }

    var validationError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val unitLabel = MoneyFormatter.getUnitLabel()
    val formattedIncomePreview = MoneyFormatter.formatRawAmount(IranianAmountUtils.parseAmountToLong(rawIncomeText))
    val formattedFixedPreview = MoneyFormatter.formatRawAmount(IranianAmountUtils.parseAmountToLong(rawFixedText))

    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
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
                IconButton(onClick = onDismiss, enabled = !isSaving) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "بستن",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Description
            Text(
                text = "برای محاسبه دقیق شاخص سلامت مالی و فشار ماهانه، درآمد خالص ماهانه خود را وارد کنید. این اطلاعات در پایگاه داده محلی برنامه ذخیره می‌شود. برای جلوگیری از نمایش اعداد نادرست، منبع درآمد و هزینه‌ها را مطابق وضعیت واقعی خود وارد کنید.",
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

                PersianAmountInputField(
                    value = rawIncomeText,
                    onValueChange = { rawIncomeText = it },
                    label = null,
                    unitLabel = unitLabel,
                    placeholder = "مثال: ۳۰,۰۰۰,۰۰۰ $unitLabel",
                    showWordsPreview = false,
                    testTag = "input_monthly_income"
                )

                // Quick presets for Income
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(20_000_000L, 30_000_000L, 40_000_000L, 50_000_000L)
                        .forEach { baseTomanValue ->
                            val value = MoneyFormatter.convertFromToman(baseTomanValue, activeCurrency)
                            val label = IranianPhoneUtils.convertDigitsToPersian("${value / 1_000_000L} م")
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
                                    text = label,
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

                PersianAmountInputField(
                    value = rawFixedText,
                    onValueChange = { rawFixedText = it },
                    label = null,
                    unitLabel = unitLabel,
                    placeholder = "مثال: ۴,۰۰۰,۰۰۰ $unitLabel",
                    showWordsPreview = false,
                    testTag = "input_fixed_expenses"
                )
            }

            validationError?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Save Action Button
            Button(
                onClick = {
                    val incomeVal = IranianAmountUtils.parseAmountToLong(rawIncomeText)
                    val fixedVal = IranianAmountUtils.parseAmountToLong(rawFixedText)
                    if (incomeVal <= 0L) {
                        validationError = "درآمد ماهانه باید بیشتر از صفر باشد."
                    } else if (rawFixedText.isBlank()) {
                        validationError = "هزینه‌های ثابت را وارد کنید؛ اگر هزینه ثابتی ندارید، صفر بنویسید."
                    } else if (fixedVal < 0L) {
                        validationError = "هزینه ثابت نمی‌تواند منفی باشد."
                    } else {
                        validationError = null
                        isSaving = true
                        val incomeInToman = MoneyFormatter.convertToToman(incomeVal, activeCurrency)
                        val fixedInToman = MoneyFormatter.convertToToman(fixedVal, activeCurrency)
                        onSave(incomeInToman, fixedInToman) { saved ->
                            isSaving = false
                            if (saved) {
                                onDismiss()
                            } else {
                                validationError = "ذخیره اطلاعات انجام نشد؛ لطفاً دوباره تلاش کنید."
                            }
                        }
                    }
                },
                enabled = !isSaving,
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
                        text = if (isSaving) "در حال ذخیره..." else "ذخیره و محاسبه مجدد شاخص",
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
