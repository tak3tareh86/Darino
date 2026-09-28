package com.example.loan.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loan.domain.CalculationResult
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.WarningAmberLight

@Composable
fun LoanResultCard(
    result: CalculationResult?,
    onSaveClick: () -> Unit,
    onCreateInstallmentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (result == null) return

    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val gradientColors = if (isDark) {
        listOf(
            Color(0xFF1E293B),
            Color(0xFF0F172A)
        )
    } else {
        listOf(
            Color(0xFFF8FAFC),
            Color(0xFFF1F5F9)
        )
    }

    val animatedInterestRatio by animateFloatAsState(
        targetValue = result.interestRatio,
        animationSpec = tween(durationMillis = 600),
        label = "InterestRatioAnim"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(RadiusLG),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            )
            .testTag("loan_result_card"),
        shape = RoundedCornerShape(RadiusLG),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.5.dp,
            Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .background(Brush.verticalGradient(gradientColors))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with badge: "نتیجه محاسبه تسهیلات" and Real Cost
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
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Payments,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "نتیجه محاسبه هوشمند",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Real Cost Pill (هزینه واقعی وام)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (result.realCostPercentage > 30.0) ExpenseRoseLight.copy(alpha = 0.15f)
                    else WarningAmberLight.copy(alpha = 0.15f),
                    border = BorderStroke(
                        1.dp,
                        if (result.realCostPercentage > 30.0) ExpenseRoseLight.copy(alpha = 0.3f)
                        else WarningAmberLight.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Percent,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (result.realCostPercentage > 30.0) ExpenseRoseLight else WarningAmberLight
                        )
                        Text(
                            text = "هزینه واقعی: ${result.realCostFormatted}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = if (result.realCostPercentage > 30.0) ExpenseRoseLight else WarningAmberLight
                        )
                    }
                }
            }

            // Hero Monthly Payment Box (مبلغ قسط ماهانه)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RadiusMD))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.25f else 0.15f),
                                MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.12f else 0.05f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        RoundedCornerShape(RadiusMD)
                    )
                    .padding(vertical = 14.dp, horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "مبلغ قسط ماهانه",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    AnimatedContent(
                        targetState = result.monthlyPaymentFormatted,
                        transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                        label = "MonthlyPaymentAnim"
                    ) { formatted ->
                        Text(
                            text = formatted,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Two Key Metrics: Total Repayment & Total Interest
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Total Repayment (کل بازپرداخت)
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(RadiusMD),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "کل بازپرداخت",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        AnimatedContent(
                            targetState = result.totalPaymentFormatted,
                            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                            label = "TotalPaymentAnim"
                        ) { formatted ->
                            Text(
                                text = formatted,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Total Interest (سود پرداختی)
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(RadiusMD),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "سود پرداختی",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.Rounded.TrendingUp,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = ExpenseRoseLight
                            )
                        }
                        AnimatedContent(
                            targetState = result.totalInterestFormatted,
                            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                            label = "TotalInterestAnim"
                        ) { formatted ->
                            Text(
                                text = formatted,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                ),
                                color = ExpenseRoseLight
                            )
                        }
                    }
                }
            }

            // Visual Share Bar (اصل وام در برابر سود پرداختی)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "نسبت اصل به سود",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "سود: ${(animatedInterestRatio * 100).toInt()}٪ | اصل: ${(100 - (animatedInterestRatio * 100).toInt())}٪",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.matchParentSize()) {
                        // Principal share (Emerald/Primary)
                        Box(
                            modifier = Modifier
                                .weight((1f - animatedInterestRatio).coerceAtLeast(0.05f))
                                .background(EmeraldPrimaryLight)
                        )
                        // Interest share (Rose/Amber)
                        if (animatedInterestRatio > 0.001f) {
                            Box(
                                modifier = Modifier
                                    .weight(animatedInterestRatio.coerceAtLeast(0.05f))
                                    .background(ExpenseRoseLight)
                            )
                        }
                    }
                }
            }

            // Action Buttons (Save Calculation & Convert to Installment)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Save calculation
                OutlinedButton(
                    onClick = onSaveClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("loan_save_calculation_button"),
                    shape = RoundedCornerShape(RadiusMD),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ذخیره محاسبه",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // 2. Convert to real installment
                Button(
                    onClick = onCreateInstallmentClick,
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("loan_create_installment_button"),
                    shape = RoundedCornerShape(RadiusMD),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ایجاد قسط از این محاسبه",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}
