package com.example.ui.screens.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Layered3DCard
import com.example.ui.screens.home.model.MonthlySummary
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.SummaryCardGradientDark
import com.example.ui.theme.SummaryCardGradientLight

@Composable
fun MonthlySummaryCard(
    summary: MonthlySummary,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    var progressTarget by remember { mutableStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 1000),
        label = "ProgressAnimation"
    )

    LaunchedEffect(Unit) {
        progressTarget = summary.progressPercentage
    }

    val cardGradient = if (isDark) SummaryCardGradientDark else SummaryCardGradientLight

    Layered3DCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLG),
        backgroundColor = Color.Transparent,
        elevation = 8.dp,
        contentPadding = PaddingValues(0.dp),
        testTag = "monthly_summary_card",
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardGradient)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Header Row of the Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "وضعیت مالی",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF94A3B8)
                    )

                    // Pill Badge for Active Dues
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF14B8A6).copy(alpha = 0.2f),
                        border = BorderStroke(0.8.dp, Color(0xFF14B8A6).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2DD4BF))
                            )
                            Text(
                                text = "${summary.activeDuesCount} قسط فعال",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF5EEAD4)
                            )
                        }
                    }
                }

                // Main Amount (Prominent Hero Display: موجودی کل)
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "موجودی کل در دسترس",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "۲۸,۵۰۰,۰۰۰",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 32.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "تومان",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFCBD5E1),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }

                // Sub-data Row (درآمد این ماه vs هزینه این ماه)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(RadiusMD))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "درآمد این ماه",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "۱۸,۰۰۰,۰۰۰ ت",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                            color = Color(0xFF34D399)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .padding(horizontal = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .height(20.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(0.5.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "هزینه این ماه",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "۹,۵۰۰,۰۰۰ ت",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                            color = Color(0xFFF87171)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .padding(horizontal = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .height(20.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(0.5.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "اقساط مانده",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "${summary.remainingAmountFormatted} ت",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                            color = Color(0xFFFBBF24)
                        )
                    }
                }

                // Progress Bar Container with 3D Depth
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "پیشرفت تسویه تعهدات ماه جاری",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "${(summary.progressPercentage * 100).toInt()}٪ تسویه شده",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2DD4BF)
                        )
                    }

                    // 3D Capsule Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            EmeraldPrimaryLight,
                                            Color(0xFF2DD4BF),
                                            Color(0xFF5EEAD4)
                                        )
                                    )
                                )
                                .shadow(4.dp, CircleShape, spotColor = Color(0xFF2DD4BF))
                        )
                    }
                }
            }
        }
    }
}
