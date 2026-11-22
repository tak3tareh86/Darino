package com.example.ui.screens.installments.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.AppNumberFormatter
import com.example.ui.screens.installments.model.InstallmentSummaryData

@Composable
fun InstallmentsSummaryCard(
    summary: InstallmentSummaryData,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val numberMode by AppNumberFormatter.modeFlow.collectAsState()

    // Keep the active-loan card on the exact active-subscription color theme.
    val activeGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0B132B),
            Color(0xFF112240),
            Color(0xFF0A2B28)
        )
    )
    val cardTextColor = Color(0xFFF8FAFC)
    val cardMutedTextColor = Color(0xFFCBD5E1)
    val accentGreen = Color(0xFF22C55E)
    val accentTeal = Color(0xFF2DD4BF)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("installments_summary_card")
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0xFF0D9488).copy(alpha = 0.2f),
                spotColor = Color(0xFF0F172A).copy(alpha = 0.35f)
            ),
            .border(
                width = 1.dp,
                color = accentTeal.copy(alpha = 0.28f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(activeGradient)
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(accentGreen)
                        )
                        Text(
                            text = "وام‌های فعال",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                            color = cardTextColor
                        )
                        Surface(
                            shape = CircleShape,
                            color = accentTeal.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "${summary.activeCount} مورد",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = accentTeal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = accentTeal.copy(alpha = 0.13f),
                        contentColor = cardMutedTextColor
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccessTime,
                                contentDescription = null,
                                tint = accentTeal,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "قسط بعدی: ${AppNumberFormatter.formatDigits(summary.nextDueText, numberMode)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = cardMutedTextColor
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "مجموع کل اقساط وام‌های فعال",
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                        color = cardMutedTextColor
                    )
                    Text(
                        text = summary.displayTotal,
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp),
                        color = cardTextColor
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.16f))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryMetricItem(
                        title = "مبلغ اقساط پرداخت‌شده",
                        amount = summary.displayPaid,
                        color = accentGreen,
                        textColor = cardTextColor,
                        mutedTextColor = cardMutedTextColor,
                        icon = Icons.Rounded.CheckCircle,
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .padding(horizontal = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 1.dp, height = 38.dp)
                                .background(Color.White.copy(alpha = 0.16f))
                        )
                    }

                    SummaryMetricItem(
                        title = "مبلغ اقساط باقی‌مانده",
                        amount = summary.displayRemaining,
                        color = accentTeal,
                        textColor = cardTextColor,
                        mutedTextColor = cardMutedTextColor,
                        icon = Icons.Rounded.Payments,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricItem(
    title: String,
    amount: String,
    color: Color,
    textColor: Color,
    mutedTextColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    unit: String = ""
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                color = mutedTextColor
            )
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                color = textColor
            )
            if (unit.isNotBlank()) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = mutedTextColor
                )
            }
        }
    }
}
