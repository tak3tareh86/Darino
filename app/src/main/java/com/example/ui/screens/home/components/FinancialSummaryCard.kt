package com.example.ui.screens.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
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
import com.example.R
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusXL
import com.example.util.IranianPhoneUtils

@Composable
fun FinancialSummaryCard(
    formattedIncome: String,
    formattedExpense: String,
    formattedBalance: String,
    savingsRate: Int,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("darino_security_prefs", android.content.Context.MODE_PRIVATE) }
    val showBalanceOnHome = prefs.getBoolean("show_balance_on_home", true)
    val isPrivacyModeEnabled = prefs.getBoolean("privacy_mode_enabled", false)

    val finalBalanceText = if (!showBalanceOnHome) {
        "••••••••"
    } else if (isPrivacyModeEnabled) {
        "•••••••• تومان"
    } else {
        formattedBalance
    }

    val finalIncomeText = if (isPrivacyModeEnabled) {
        "••••••••"
    } else {
        formattedIncome
    }

    val finalExpenseText = if (isPrivacyModeEnabled) {
        "••••••••"
    } else {
        formattedExpense
    }

    val cardBg = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF131D31),
                Color(0xFF1E293B),
                Color(0xFF0F172A)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFF8FAFC),
                Color(0xFFF1F5F9)
            )
        )
    }

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
                text = "وضعیت مالی این ماه",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            // Savings Rate Badge
            Surface(
                shape = RoundedCornerShape(RadiusMD),
                color = EmeraldPrimaryLight.copy(alpha = 0.14f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Savings,
                        contentDescription = null,
                        tint = EmeraldPrimaryLight,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "نرخ پس‌انداز: ${IranianPhoneUtils.convertDigitsToPersian(savingsRate.toString())}٪",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimaryLight,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        Layered3DCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = 5.dp,
            contentPadding = PaddingValues(16.dp),
            onClick = onClick,
            testTag = "monthly_financial_summary_card"
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Main Balance Block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Soft3DIcon(
                            imageRes = R.drawable.img_3d_wallet,
                            contentDescription = "مانده موجودی",
                            size = 46.dp,
                            accentColor = EmeraldPrimaryLight
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "مانده کل پس‌انداز ماه",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            AnimatedContent(
                                targetState = finalBalanceText,
                                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
                                label = "balance_anim"
                            ) { balanceText ->
                                Text(
                                    text = balanceText,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 19.sp
                                    ),
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                }

                // Divider line with depth
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            if (isDark) Color.White.copy(alpha = 0.08f)
                            else Color.Black.copy(alpha = 0.06f)
                        )
                )

                // Bottom Row: Income & Expense in 2 balanced columns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Income Metric
                    FinancialMetricPill(
                        title = "درآمد ماه",
                        amountText = finalIncomeText,
                        icon = Icons.Rounded.ArrowUpward,
                        accentColor = EmeraldPrimaryLight,
                        modifier = Modifier.weight(1f)
                    )

                    // Expense Metric
                    FinancialMetricPill(
                        title = "هزینه ماه",
                        amountText = finalExpenseText,
                        icon = Icons.Rounded.ArrowDownward,
                        accentColor = ExpenseRoseLight,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FinancialMetricPill(
    title: String,
    amountText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(RadiusMD),
        color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFFF1F5F9).copy(alpha = 0.8f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(accentColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
    }
}
