package com.example.financial_health.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.financial_health.domain.FinancialHealthStatus
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.ui.theme.RadiusMD
import com.example.util.IranianPhoneUtils

@Composable
fun FinancialHealthCard(
    pressurePercentage: Float,
    healthStatus: FinancialHealthStatus,
    monthlyIncome: Long,
    totalCommitments: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    // Animated progress ratio between 0.0 and 1.0
    val animatedProgress by animateFloatAsState(
        targetValue = (pressurePercentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "FinancialPressureProgress"
    )

    val statusColor = healthStatus.color
    val progressBrush = Brush.horizontalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            statusColor
        )
    )

    Layered3DCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_financial_health_card"),
        backgroundColor = if (isDark) Color(0xFF141F32) else Color(0xFFFFFFFF),
        borderColor = statusColor.copy(alpha = 0.28f),
        elevation = 3.dp,
        onClick = onClick,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Title & 3D Icon & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Soft3DIcon(
                        imageRes = R.drawable.img_3d_analytics,
                        contentDescription = "سلامت تعهدات مالی",
                        size = 40.dp,
                        accentColor = statusColor
                    )
                    Column {
                        Text(
                            text = "سلامت تعهدات مالی",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "سنجش نسبت تعهدات به درآمد",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status Badge (🟢 مناسب / 🟡 متوسط / 🔴 بالا)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) healthStatus.darkContainerColor.copy(alpha = 0.6f) else healthStatus.lightContainerColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                    modifier = Modifier.testTag("financial_health_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = healthStatus.badgeText,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = if (isDark) Color(0xFFF3F4F6) else statusColor
                        )
                    }
                }
            }

            // Percentage & Visual Meter
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${IranianPhoneUtils.convertDigitsToPersian(pressurePercentage.toInt().toString())}٪",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 28.sp
                            ),
                            color = statusColor
                        )
                        Text(
                            text = "فشار مالی ماهانه",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    // Ratio numbers (تعهدات / درآمد)
                    val incomeInM = (monthlyIncome / 1_000_000.0).let {
                        if (it % 1.0 == 0.0) it.toInt().toString() else "%.1f".format(it)
                    }
                    val commitmentsInM = (totalCommitments / 1_000_000.0).let {
                        if (it % 1.0 == 0.0) it.toInt().toString() else "%.1f".format(it)
                    }

                    Text(
                        text = "${IranianPhoneUtils.convertDigitsToPersian(commitmentsInM)} از ${IranianPhoneUtils.convertDigitsToPersian(incomeInM)} م.ت",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Custom Animated Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDark) Color(0xFF1E2B42) else Color(0xFFE2E8F0)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(progressBrush)
                    )
                }
            }

            // Message & Drilldown CTA
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isDark) Color(0xFF182338) else Color(0xFFF8FAFC)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = healthStatus.defaultMessage,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "جزئیات",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
