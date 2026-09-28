package com.example.ui.screens.finance.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.screens.finance.model.BudgetStatus
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.util.MoneyFormatter

@Composable
fun MonthlyBudgetCard(
    totalBudgetFormatted: String,
    spentFormatted: String,
    remainingFormatted: String,
    usagePercentage: Int,
    status: BudgetStatus,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val progressAnimated by animateFloatAsState(
        targetValue = (usagePercentage.coerceIn(0, 100) / 100f),
        animationSpec = tween(600),
        label = "budget_progress"
    )

    val progressGradient = when (status) {
        BudgetStatus.SAFE -> Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF34D399)))
        BudgetStatus.WARNING -> Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFFBBF24)))
        BudgetStatus.EXCEEDED -> Brush.horizontalGradient(listOf(Color(0xFFEF4444), Color(0xFFF87171)))
    }

    val cardBg = if (isDark) Color(0xFF131A29) else Color.White
    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_budget_card")
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(RadiusLG),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .clip(RoundedCornerShape(RadiusLG))
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(RadiusLG),
        color = cardBg,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Title + Status Badge + Arrow
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
                            .background(Color(0xFF6366F1).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📊", fontSize = 14.sp)
                    }

                    Text(
                        text = "بودجه این ماه",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = status.color.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, status.color.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = status.title,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            ),
                            color = status.color
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "مشاهده جزئیات بودجه",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مصرف‌شده: $spentFormatted",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${MoneyFormatter.toPersianDigits(usagePercentage.toString())}٪",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = status.color
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressAnimated)
                            .clip(RoundedCornerShape(4.dp))
                            .background(progressGradient)
                    )
                }
            }

            // Row with Total Budget & Remaining Amount
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "بودجه کل",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = totalBudgetFormatted,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "مبلغ باقیمانده",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = remainingFormatted,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = if (status == BudgetStatus.EXCEEDED) Color(0xFFEF4444) else Color(0xFF10B981)
                    )
                }
            }
        }
    }
}
