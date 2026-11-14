package com.example.loan.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loan.domain.CalculationResult
import com.example.loan.domain.ComparisonLoanInput
import com.example.loan.domain.ComparisonResult
import com.example.ui.components.PersianAmountInputField
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.WarningAmberLight
import com.example.util.IranianAmountUtils
import com.example.util.MoneyAmount
import com.example.util.MoneyFormatter
import com.example.ui.screens.settings.model.AppCurrency
import com.example.util.IranianPhoneUtils
import kotlin.math.abs

@Composable
fun LoanComparisonScreen(
    loan1Input: ComparisonLoanInput,
    loan2Input: ComparisonLoanInput,
    comparisonResult: ComparisonResult?,
    onUpdateLoan1: (ComparisonLoanInput) -> Unit,
    onUpdateLoan2: (ComparisonLoanInput) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditingLoan1 by remember { mutableStateOf(false) }
    var isEditingLoan2 by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hero Comparison Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(RadiusLG),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CompareArrows,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "مقایسه هوشمند دو وام",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "بررسی تفاوت اقساط ماهانه و سود پرداختی برای تصمیم‌گیری بهینه مالی",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Show empty editable inputs before any comparison has been entered.
        if (comparisonResult == null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LoanComparisonCard(
                    title = "وام اول",
                    accentColor = InfoIndigoLight,
                    durationMonths = loan1Input.durationMonths,
                    interestRate = loan1Input.interestRate,
                    amount = loan1Input.amount,
                    amountUnit = loan1Input.amountUnit,
                    result = null,
                    isEditing = true,
                    onToggleEdit = {},
                    onSaveEdit = { name, amt, unit, rate, dur ->
                        onUpdateLoan1(ComparisonLoanInput(name, amt, unit, rate, dur))
                    },
                    modifier = Modifier.weight(1f)
                )
                LoanComparisonCard(
                    title = "وام دوم",
                    accentColor = WarningAmberLight,
                    durationMonths = loan2Input.durationMonths,
                    interestRate = loan2Input.interestRate,
                    amount = loan2Input.amount,
                    amountUnit = loan2Input.amountUnit,
                    result = null,
                    isEditing = true,
                    onToggleEdit = {},
                    onSaveEdit = { name, amt, unit, rate, dur ->
                        onUpdateLoan2(ComparisonLoanInput(name, amt, unit, rate, dur))
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        // Side-by-Side Comparison Cards
        if (comparisonResult != null) {
            val res1 = comparisonResult.loan1
            val res2 = comparisonResult.loan2

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1
                LoanComparisonCard(
                    title = loan1Input.name,
                    accentColor = InfoIndigoLight,
                    durationMonths = loan1Input.durationMonths,
                    interestRate = loan1Input.interestRate,
                    amount = loan1Input.amount,
                    amountUnit = loan1Input.amountUnit,
                    result = res1,
                    isEditing = isEditingLoan1,
                    onToggleEdit = { isEditingLoan1 = !isEditingLoan1 },
                    onSaveEdit = { name, amt, unit, rate, dur ->
                        onUpdateLoan1(ComparisonLoanInput(name, amt, unit, rate, dur))
                        isEditingLoan1 = false
                    },
                    modifier = Modifier.weight(1f)
                )

                // Card 2
                LoanComparisonCard(
                    title = loan2Input.name,
                    accentColor = WarningAmberLight,
                    durationMonths = loan2Input.durationMonths,
                    interestRate = loan2Input.interestRate,
                    amount = loan2Input.amount,
                    amountUnit = loan2Input.amountUnit,
                    result = res2,
                    isEditing = isEditingLoan2,
                    onToggleEdit = { isEditingLoan2 = !isEditingLoan2 },
                    onSaveEdit = { name, amt, unit, rate, dur ->
                        onUpdateLoan2(ComparisonLoanInput(name, amt, unit, rate, dur))
                        isEditingLoan2 = false
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Compact comparison result panel
            val interestDiff = comparisonResult.totalInterestDiff
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(RadiusLG),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "نتایج مقایسه",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(RadiusMD))
                                .background(InfoIndigoLight.copy(alpha = 0.08f)).padding(9.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("وام اول", style = MaterialTheme.typography.labelSmall, color = InfoIndigoLight)
                            Text("مبلغ قسط ماهانه", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(res1.monthlyPaymentFormatted, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = InfoIndigoLight)
                        }
                        Column(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(RadiusMD))
                                .background(WarningAmberLight.copy(alpha = 0.08f)).padding(9.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("وام دوم", style = MaterialTheme.typography.labelSmall, color = WarningAmberLight)
                            Text("مبلغ قسط ماهانه", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(res2.monthlyPaymentFormatted, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = WarningAmberLight)
                        }
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(RadiusMD))
                            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = when {
                                interestDiff < 0 -> "وام دوم سود کل کمتری دارد و از نظر هزینه سود مقرون‌به‌صرفه‌تر است."
                                interestDiff > 0 -> "وام اول سود کل کمتری دارد و از نظر هزینه سود مقرون‌به‌صرفه‌تر است."
                                else -> "سود کل دو وام برابر است؛ مبلغ قسط ماهانه را جداگانه مقایسه کنید."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Text(
                        text = "کل بازپرداخت وام اول: ${res1.totalPaymentFormatted} | وام دوم: ${res2.totalPaymentFormatted}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val interestText = when {
                        interestDiff > 0 -> "وام اول: ${CalculationResult.formatMoney(abs(interestDiff), AppCurrency.RIAL)} سود کمتر"
                        interestDiff < 0 -> "وام دوم: ${CalculationResult.formatMoney(abs(interestDiff), AppCurrency.RIAL)} سود کمتر"
                        else -> "سود کل دو وام برابر است"
                    }
                    Text(
                        text = "تفاوت سود کل: $interestText",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LoanComparisonCard(
    title: String,
    accentColor: Color,
    durationMonths: Int,
    interestRate: Double,
    amount: Long,
    amountUnit: AppCurrency,
    result: CalculationResult?,
    isEditing: Boolean,
    onToggleEdit: () -> Unit,
    onSaveEdit: (String, Long, AppCurrency, Double, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeCurrency = MoneyFormatter.activeCurrency
    var editAmount by remember(amount, amountUnit, activeCurrency) {
        mutableStateOf(if (amount > 0L) IranianAmountUtils.formatDecimalWithCommas(
            MoneyAmount.of(amount, amountUnit).inUnit(activeCurrency).value.stripTrailingZeros().toPlainString()
        ) else "")
    }
    var editInterest by remember(interestRate) { mutableStateOf(if (interestRate > 0.0) interestRate.toString() else "") }
    val parsedInterestRate = IranianPhoneUtils.convertDigitsToEnglish(editInterest)
        .replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it.isFinite() && it >= 0.0 && it <= 1000.0 }
    var editDuration by remember(durationMonths) { mutableStateOf(if (durationMonths > 0) durationMonths.toString() else "") }

    Surface(
        modifier = modifier.shadow(2.dp, RoundedCornerShape(RadiusLG)),
        shape = RoundedCornerShape(RadiusLG),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp), color = accentColor)
                Surface(shape = CircleShape, color = accentColor.copy(alpha = 0.12f), modifier = Modifier.clickable { onToggleEdit() }) {
                    Icon(
                        imageVector = if (isEditing) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "ویرایش",
                        modifier = Modifier.padding(4.dp).size(18.dp),
                        tint = accentColor
                    )
                }
            }

            AnimatedVisibility(visible = isEditing) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(
                        text = "مبلغ وام (${MoneyFormatter.getUnitLabel(activeCurrency)})",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PersianAmountInputField(
                        value = editAmount,
                        onValueChange = { editAmount = it },
                        allowDecimalInput = true,
                        onRawDecimalAmountChange = { rawValue, _ -> editAmount = rawValue },
                        label = null,
                        unitLabel = "",
                        placeholder = "مبلغ را وارد کنید",
                        showWordsPreview = false
                    )
                    OutlinedTextField(
                        value = editInterest,
                        onValueChange = { editInterest = it },
                        label = { Text("نرخ سود سالانه") },
                        placeholder = { Text("درصد") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(RadiusMD)
                    )
                    if (parsedInterestRate == null) {
                        Text(
                            text = "نرخ سود معتبر بین صفر تا ۱۰۰۰ درصد وارد کنید.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    OutlinedTextField(
                        value = editDuration,
                        onValueChange = { editDuration = it },
                        label = { Text("مدت بازپرداخت") },
                        placeholder = { Text("ماه") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(RadiusMD)
                    )
                    Button(
                        onClick = {
                            val parsedAmount = IranianAmountUtils.parseAmountToBigDecimal(editAmount)
                                ?.let { runCatching { MoneyAmount(it, activeCurrency).asRialsLongExact() }.getOrNull() } ?: 0L
                            val rate = parsedInterestRate ?: return@Button
                            val duration = IranianPhoneUtils.convertDigitsToEnglish(editDuration).filter { it.isDigit() }.toIntOrNull() ?: durationMonths
                            onSaveEdit(
                                title,
                                if (parsedAmount > 0L) parsedAmount else MoneyAmount.of(amount, amountUnit).asRialsLongExact(),
                                AppCurrency.RIAL,
                                rate,
                                duration
                            )
                        },
                        enabled = parsedInterestRate != null,
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(RadiusMD)
                    ) {
                        Icon(Icons.Rounded.Done, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("اعمال تغییرات", fontSize = 11.sp)
                    }
                }
            }

            AnimatedVisibility(visible = !isEditing) {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(RadiusMD))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
                        .padding(horizontal = 9.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CompactLoanValue("مبلغ وام", if (amount > 0L) CalculationResult.formatMoney(amount, amountUnit) else "—")
                    CompactLoanValue("مدت بازپرداخت", if (durationMonths > 0) "${IranianPhoneUtils.convertDigitsToPersian(durationMonths.toString())} ماه" else "—")
                    CompactLoanValue("نرخ سود سالانه", if (interestRate > 0.0) "${IranianPhoneUtils.convertDigitsToPersian(interestRate.toString())}٪" else "—")
                }
            }
        }
    }
}

@Composable
private fun CompactLoanValue(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp), color = MaterialTheme.colorScheme.onSurface)
    }
}
