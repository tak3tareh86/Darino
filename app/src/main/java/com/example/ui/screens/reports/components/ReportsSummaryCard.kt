package com.example.ui.screens.reports.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.example.ui.screens.reports.model.ReportsSummaryData
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.SuccessGreenLight

@Composable
fun ReportsSummaryCard(
    summary: ReportsSummaryData,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Layered3DCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("reports_summary_card"),
        shape = RoundedCornerShape(RadiusLG),
        backgroundColor = MaterialTheme.colorScheme.surface,
        elevation = 4.dp,
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Row: Header Badge & 3D Visual Asset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldPrimaryLight.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldPrimaryLight)
                                )
                                Text(
                                    text = summary.periodLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = EmeraldPrimaryLight
                                )
                            }
                        }

                        // Growth Badge
                        Surface(
                            shape = CircleShape,
                            color = if (summary.isIncomeGrowth) SuccessGreenLight.copy(alpha = 0.15f)
                            else ExpenseRoseLight.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = if (summary.isIncomeGrowth) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (summary.isIncomeGrowth) SuccessGreenLight else ExpenseRoseLight,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = summary.incomeChangePercent,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = if (summary.isIncomeGrowth) SuccessGreenLight else ExpenseRoseLight
                                )
                            }
                        }
                    }

                    Text(
                        text = "مانده و پس‌انداز این دوره",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 3D Wallet / Analytics Visual Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(RadiusMD),
                            ambientColor = EmeraldPrimaryLight.copy(alpha = 0.35f),
                            spotColor = EmeraldPrimaryLight.copy(alpha = 0.5f)
                        )
                        .clip(RoundedCornerShape(RadiusMD))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    EmeraldPrimaryLight.copy(alpha = if (isDark) 0.35f else 0.18f),
                                    EmeraldPrimaryLight.copy(alpha = if (isDark) 0.15f else 0.06f)
                                )
                            )
                        )
                        .border(
                            width = 1.2.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = if (isDark) 0.4f else 0.85f),
                                    EmeraldPrimaryLight.copy(alpha = 0.3f)
                                )
                            ),
                            shape = RoundedCornerShape(RadiusMD)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_3d_wallet),
                        contentDescription = "کیف پول",
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(RadiusMD)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Dominant Remaining Balance
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = summary.remainingBalanceFormatted,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = EmeraldPrimaryLight
                )
                Text(
                    text = "تومان",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Savings rate pill
                Surface(
                    shape = RoundedCornerShape(RadiusMD),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Savings,
                            contentDescription = null,
                            tint = EmeraldPrimaryLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "نرخ پس‌انداز: ${summary.savingsRatePercentage}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Divider line with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Bottom Dual Breakdown: Total Income vs Total Expense
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Income
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SuccessGreenLight)
                        )
                        Text(
                            text = "مجموع درآمد",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = summary.totalIncomeFormatted,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "تومان",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .size(width = 1.dp, height = 40.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                )

                // Expense
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ExpenseRoseLight)
                        )
                        Text(
                            text = "مجموع هزینه",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = summary.totalExpenseFormatted,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "تومان",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
