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
        verticalArrangement = Arrangement.spacedBy(16.dp)
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

        // Quick Preset Comparison Scenario Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(RadiusMD))
                    .clickable {
                        onUpdateLoan1(
                            ComparisonLoanInput(
                                name = "طرح کوتاه‌مدت (۱۸ ماه)",
                                amount = 100_000_000L,
                                interestRate = 18.0,
                                durationMonths = 18
                            )
                        )
                        onUpdateLoan2(
                            ComparisonLoanInput(
                                name = "طرح بلندمدت (۳۶ ماه)",
                                amount = 100_000_000L,
                                interestRate = 23.0,
                                durationMonths = 36
                            )
                        )
                    },
                shape = RoundedCornerShape(RadiusMD),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "سناریوی پیشنهادی: ۱۸ ماه ۱۸٪ در مقابل ۳۶ ماه ۲۳٪",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
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
                    result = res1,
                    isEditing = isEditingLoan1,
                    onToggleEdit = { isEditingLoan1 = !isEditingLoan1 },
                    onSaveEdit = { name, amt, rate, dur ->
                        onUpdateLoan1(ComparisonLoanInput(name, amt, rate, dur))
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
                    result = res2,
                    isEditing = isEditingLoan2,
                    onToggleEdit = { isEditingLoan2 = !isEditingLoan2 },
                    onSaveEdit = { name, amt, rate, dur ->
                        onUpdateLoan2(ComparisonLoanInput(name, amt, rate, dur))
                        isEditingLoan2 = false
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Summary Differences & Smart Financial Advice
            val interestDiff = comparisonResult.totalInterestDiff
            val monthlyDiff = comparisonResult.monthlyPaymentDiff

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(RadiusLG),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "تحلیل تفاوت دو وام",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Difference Item 1: Interest
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (interestDiff > 0) Icons.Rounded.Savings else Icons.Rounded.TrendingUp,
                                contentDescription = null,
                                tint = if (interestDiff > 0) EmeraldPrimaryLight else ExpenseRoseLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "تفاوت سود پرداختی:",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val absInterest = abs(interestDiff)
                        val interestText = if (interestDiff > 0) {
                            "طرح ۱: ${CalculationResult.formatMoney(absInterest)} سود کمتر"
                        } else {
                            "طرح ۲: ${CalculationResult.formatMoney(absInterest)} سود کمتر"
                        }
                        Text(
                            text = interestText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = if (interestDiff > 0) EmeraldPrimaryLight else ExpenseRoseLight
                        )
                    }

                    // Difference Item 2: Monthly Payment
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CompareArrows,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "تفاوت قسط ماهانه:",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val absMonthly = abs(monthlyDiff)
                        val monthlyText = if (monthlyDiff < 0) {
                            "طرح ۲: ${CalculationResult.formatMoney(absMonthly)} قسط سبک‌تر"
                        } else {
                            "طرح ۱: ${CalculationResult.formatMoney(absMonthly)} قسط سبک‌تر"
                        }
                        Text(
                            text = monthlyText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Recommendation Pill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RadiusMD))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = if (loan1Input.durationMonths < loan2Input.durationMonths) {
                                "💡 نتیجه‌گیری: اگر توانایی پرداخت ماهانه بیشتری دارید، طرح اول (${loan1Input.durationMonths} ماهه) به دلیل صرفه‌جویی چشمگیر در کل سود بانکی بسیار به‌صرفه‌تر است."
                            } else {
                                "💡 نتیجه‌گیری: اگر به دنبال اقساط سبک‌تر ماهیانه برای مدیریت جریان نقدی روزمره هستید، وام با مدت زمان بالاتر فشار کمتری به بودجه شما وارد می‌کند."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                lineHeight = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
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
    result: CalculationResult,
    isEditing: Boolean,
    onToggleEdit: () -> Unit,
    onSaveEdit: (String, Long, Double, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var editAmount by remember(amount) { mutableStateOf(amount.toString()) }
    var editInterest by remember(interestRate) { mutableStateOf(interestRate.toString()) }
    var editDuration by remember(durationMonths) { mutableStateOf(durationMonths.toString()) }

    Surface(
        modifier = modifier
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(RadiusLG)),
        shape = RoundedCornerShape(RadiusLG),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = accentColor
                )

                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.clickable { onToggleEdit() }
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "ویرایش",
                        modifier = Modifier
                            .padding(4.dp)
                            .size(16.dp),
                        tint = accentColor
                    )
                }
            }

            AnimatedVisibility(visible = isEditing) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PersianAmountInputField(
                        value = editAmount,
                        onValueChange = { editAmount = it },
                        label = "مبلغ وام",
                        unitLabel = "تومان",
                        placeholder = "مثال: ۱۰۰,۰۰۰,۰۰۰",
                        showWordsPreview = false
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = editInterest,
                            onValueChange = { editInterest = it },
                            label = { Text("نرخ سود٪", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editDuration,
                            onValueChange = { editDuration = it },
                            label = { Text("ماه", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Button(
                        onClick = {
                            val a = IranianAmountUtils.parseAmountToLong(editAmount)
                            val r = IranianPhoneUtils.convertDigitsToEnglish(editInterest).replace(',', '.').toDoubleOrNull() ?: interestRate
                            val d = IranianPhoneUtils.convertDigitsToEnglish(editDuration).filter { ch -> ch.isDigit() }.toIntOrNull() ?: durationMonths
                            onSaveEdit(title, if (a > 0L) a else amount, r, d)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(RadiusMD)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Done,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("اعمال تغییرات", fontSize = 11.sp)
                    }
                }
            }

            // Specs
            Surface(
                shape = RoundedCornerShape(RadiusMD),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "مبلغ: ${CalculationResult.formatMoney(amount)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "مدت: ${IranianPhoneUtils.convertDigitsToPersian(durationMonths.toString())} ماه | سود: ${IranianPhoneUtils.convertDigitsToPersian(interestRate.toString())}٪",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Monthly Payment
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "قسط ماهانه",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = result.monthlyPaymentFormatted,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = accentColor
                )
            }

            // Total Interest
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "سود کل",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = result.totalInterestFormatted,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp
                    ),
                    color = ExpenseRoseLight
                )
            }

            // Total Payment
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "کل بازپرداخت",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = result.totalPaymentFormatted,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
