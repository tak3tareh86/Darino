package com.example.ui.screens.finance.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.Layered3DCard
import com.example.ui.screens.finance.model.FinanceFilterPeriod
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.util.MoneyFormatter

@Composable
fun FinancialSummaryCard(
    title: String,
    formattedIncome: String,
    formattedExpense: String,
    formattedBalance: String,
    savingsRate: Int,
    hasIncome: Boolean,
    savingsStatus: String?,
    selectedSummaryPeriod: FinanceFilterPeriod,
    onSummaryPeriodChange: (FinanceFilterPeriod) -> Unit,
    yearLabel: String,
    modifier: Modifier = Modifier,
    balanceLabel: String,
    cardTestTag: String = "financial_status_card",
    onCardClick: () -> Unit = {}
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val isYearSelected = selectedSummaryPeriod == FinanceFilterPeriod.THIS_YEAR
    val gradientStart by animateColorAsState(
        targetValue = when {
            isDark && isYearSelected -> Color(0xFF211A38)
            isDark -> Color(0xFF111F38)
            isYearSelected -> Color(0xFFF2EAFE)
            else -> Color(0xFFEAF4FF)
        },
        animationSpec = tween(durationMillis = 450),
        label = "summary_gradient_start"
    )
    val gradientMiddle by animateColorAsState(
        targetValue = when {
            isDark && isYearSelected -> Color(0xFF3A285C)
            isDark -> Color(0xFF193B63)
            isYearSelected -> Color(0xFFE9DDFB)
            else -> Color(0xFFD9EBFF)
        },
        animationSpec = tween(durationMillis = 450),
        label = "summary_gradient_middle"
    )
    val gradientEnd by animateColorAsState(
        targetValue = when {
            isDark && isYearSelected -> Color(0xFF593B70)
            isDark -> Color(0xFF164B66)
            isYearSelected -> Color(0xFFDCCAF4)
            else -> Color(0xFFC7E3FA)
        },
        animationSpec = tween(durationMillis = 450),
        label = "summary_gradient_end"
    )
    val periodAccent by animateColorAsState(
        targetValue = if (isYearSelected) Color(0xFF8B5CC7) else Color(0xFF2879B8),
        animationSpec = tween(durationMillis = 450),
        label = "summary_period_accent"
    )
    val cardGradient = Brush.linearGradient(
        colors = listOf(gradientStart, gradientMiddle, gradientEnd)
    )
    val primaryTextColor = if (isDark) Color.White else Color(0xFF0F172A)
    val secondaryTextColor = if (isDark) Color(0xFFE2E8F0).copy(alpha = 0.85f) else Color(0xFF475569)
    val mutedTextColor = if (isDark) Color(0xFFE2E8F0).copy(alpha = 0.8f) else Color(0xFF64748B)
    val translucentSurfaceColor = if (isDark) Color.White.copy(alpha = 0.12f) else periodAccent.copy(alpha = 0.08f)
    val iconBackgroundColor = if (isDark) Color.White.copy(alpha = 0.15f) else periodAccent.copy(alpha = 0.12f)
    val selectedTabColor = if (isDark) periodAccent.copy(alpha = 0.82f) else periodAccent.copy(alpha = 0.16f)
    val unselectedTabColor = if (isDark) Color.White.copy(alpha = 0.07f) else Color.Transparent

    Layered3DCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLG),
        backgroundColor = Color.Transparent,
        elevation = 4.dp,
        contentPadding = PaddingValues(0.dp),
        testTag = cardTestTag,
        onClick = onCardClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardGradient)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = primaryTextColor
                        )
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(iconBackgroundColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_3d_wallet),
                                contentDescription = null,
                                modifier = Modifier.size(23.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(translucentSurfaceColor)
                            .padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SummaryPeriodOption(
                            text = "این ماه",
                            selected = selectedSummaryPeriod == FinanceFilterPeriod.THIS_MONTH,
                            selectedColor = selectedTabColor,
                            unselectedColor = unselectedTabColor,
                            textColor = primaryTextColor,
                            onClick = { onSummaryPeriodChange(FinanceFilterPeriod.THIS_MONTH) }
                        )
                        SummaryPeriodOption(
                            text = "سال $yearLabel",
                            selected = selectedSummaryPeriod == FinanceFilterPeriod.THIS_YEAR,
                            selectedColor = selectedTabColor,
                            unselectedColor = unselectedTabColor,
                            textColor = primaryTextColor,
                            onClick = { onSummaryPeriodChange(FinanceFilterPeriod.THIS_YEAR) }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = balanceLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = secondaryTextColor
                    )
                    Text(
                        text = formattedBalance,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        ),
                        color = primaryTextColor
                    )
                    Text(
                        text = MoneyFormatter.getUnitLabel(),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = mutedTextColor
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryAmountBlock(
                        modifier = Modifier.weight(1f),
                        label = "درآمد (${MoneyFormatter.getUnitLabel()})",
                        amount = formattedIncome,
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.ArrowDownward,
                                contentDescription = "درآمد",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        iconColor = Color(0xFF0D9B83),
                        surfaceColor = translucentSurfaceColor,
                        textColor = primaryTextColor,
                        labelColor = mutedTextColor
                    )
                    SummaryAmountBlock(
                        modifier = Modifier.weight(1f),
                        label = "هزینه (${MoneyFormatter.getUnitLabel()})",
                        amount = formattedExpense,
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.ArrowUpward,
                                contentDescription = "هزینه",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        iconColor = Color(0xFFBE315C),
                        surfaceColor = translucentSurfaceColor,
                        textColor = primaryTextColor,
                        labelColor = mutedTextColor
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(RadiusMD))
                        .background(translucentSurfaceColor)
                        .padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "نرخ پس‌انداز",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        color = secondaryTextColor
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color.White.copy(alpha = 0.13f) else Color(0xFF94A3B8).copy(alpha = 0.25f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((savingsRate.coerceIn(0, 100) / 100f))
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(periodAccent)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFF59E0B),
                        contentColor = Color(0xFF111827)
                    ) {
                        Text(
                            text = if (hasIncome) "${MoneyFormatter.toPersianDigits(savingsRate.toString())}٪" else "—",
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                if (savingsStatus != null) {
                    Text(
                        text = savingsStatus,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (savingsStatus.contains("بیشتر")) Color(0xFFEF4444) else mutedTextColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryPeriodOption(
    text: String,
    selected: Boolean,
    selectedColor: Color,
    unselectedColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = if (selected) selectedColor else unselectedColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            color = textColor,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
private fun SummaryAmountBlock(
    modifier: Modifier,
    label: String,
    amount: String,
    icon: @Composable () -> Unit,
    iconColor: Color,
    surfaceColor: Color,
    textColor: Color,
    labelColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(RadiusMD),
        color = surfaceColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(25.dp)
                    .clip(CircleShape)
                    .background(iconColor),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = labelColor
                )
                Text(
                    text = amount,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = textColor
                )
            }
        }
    }
}
