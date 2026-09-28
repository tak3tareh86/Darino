package com.example.ui.screens.installments.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.ui.screens.installments.model.InstallmentItem
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM

@Composable
fun OverdueInstallmentsSection(
    items: List<InstallmentItem>,
    onItemClick: (InstallmentItem) -> Unit,
    onSeeAllOverdueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val cardBgGradient = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF2A1215),
                Color(0xFF1F1517),
                Color(0xFF181012)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFF1F2),
                Color(0xFFFFE4E6),
                Color(0xFFFAE8FF)
            )
        )
    }

    val alertBorderColor = ExpenseRoseLight.copy(alpha = if (isDark) 0.45f else 0.35f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(RadiusLG),
                ambientColor = ExpenseRoseLight.copy(alpha = 0.3f),
                spotColor = ExpenseRoseLight.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(RadiusLG),
        color = Color.Transparent,
        border = BorderStroke(1.dp, alertBorderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBgGradient)
                .padding(16.dp)
                .testTag("overdue_installments_section")
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(ExpenseRoseLight.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = ExpenseRoseLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "اقساط سررسید گذشته",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isDark) Color(0xFFFECDD3) else Color(0xFF9F1239)
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = ExpenseRoseLight
                                ) {
                                    Text(
                                        text = "${items.size} قسط",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "لطفاً جهت جلوگیری از جریمه سریع‌تر اقدام کنید",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = if (isDark) Color(0xFFFDA4AF) else Color(0xFFBE123C)
                            )
                        }
                    }

                    TextButton(
                        onClick = onSeeAllOverdueClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("overdue_see_all_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "همه",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = ExpenseRoseLight
                            )
                            Icon(
                                imageVector = Icons.Rounded.ChevronLeft,
                                contentDescription = null,
                                tint = ExpenseRoseLight,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Overdue items
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items.take(2).forEach { item ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(RadiusMD))
                                .clickable { onItemClick(item) },
                            shape = RoundedCornerShape(RadiusMD),
                            color = if (isDark) Color(0xFF3B181B).copy(alpha = 0.7f) else Color.White,
                            border = BorderStroke(
                                width = 1.dp,
                                color = ExpenseRoseLight.copy(alpha = if (isDark) 0.3f else 0.2f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(RadiusSM),
                                            color = ExpenseRoseLight.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = item.nextDueDaysText,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = ExpenseRoseLight,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = item.nextPaymentDate,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${item.monthlyPaymentFormatted} تومان",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = ExpenseRoseLight
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(RadiusSM),
                                        color = ExpenseRoseLight,
                                        modifier = Modifier.clickable { onItemClick(item) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Payment,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "پرداخت",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold
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
        }
    }
}

