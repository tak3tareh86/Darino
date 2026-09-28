package com.example.ui.screens.finance.components

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.R
import com.example.ui.components.Layered3DCard
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.SummaryCardGradientDark
import com.example.ui.theme.SummaryCardGradientLight
import com.example.util.MoneyFormatter

@Composable
fun FinancialSummaryCard(
    formattedIncome: String,
    formattedExpense: String,
    formattedBalance: String,
    savingsRate: Int,
    modifier: Modifier = Modifier,
    onCardClick: () -> Unit = {}
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val cardGradient = if (isDark) SummaryCardGradientDark else SummaryCardGradientLight

    Layered3DCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLG),
        backgroundColor = Color.Transparent,
        elevation = 4.dp,
        contentPadding = PaddingValues(0.dp),
        testTag = "monthly_financial_status_card",
        onClick = onCardClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardGradient)
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card Header: "وضعیت مالی این ماه" + Savings Rate Pill
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
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_3d_wallet),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Text(
                            text = "وضعیت مالی این ماه",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = Color.White
                        )
                    }

                    // Savings Rate Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.18f),
                        contentColor = Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "نرخ پس‌انداز: ${MoneyFormatter.toPersianDigits(savingsRate.toString())}٪",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = Color.White
                            )
                        }
                    }
                }

                // Balance Big Number
                Column(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "مانده پس‌انداز و نقدینگی",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFFE2E8F0).copy(alpha = 0.85f)
                    )
                    Text(
                        text = formattedBalance,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        ),
                        color = Color.White
                    )
                }

                // 2-Column Split: Income vs Expense
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Income Block
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(RadiusMD),
                        color = Color.White.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981).copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDownward,
                                    contentDescription = "درآمد",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "درآمد",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color(0xFFE2E8F0).copy(alpha = 0.8f)
                                )
                                Text(
                                    text = formattedIncome,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Expense Block
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(RadiusMD),
                        color = Color.White.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444).copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowUpward,
                                    contentDescription = "هزینه",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "هزینه",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color(0xFFE2E8F0).copy(alpha = 0.8f)
                                )
                                Text(
                                    text = formattedExpense,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
