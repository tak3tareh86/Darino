package com.example.ui.screens.reports.components

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.ui.screens.reports.model.InstallmentCategoryItem
import com.example.ui.screens.reports.model.InstallmentReportData
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.SuccessGreenLight
import com.example.ui.theme.WarningAmberLight

@Composable
fun InstallmentAnalysisCard(
    data: InstallmentReportData,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    var startAnim by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { startAnim = true }

    val animatedOverallProgress by animateFloatAsState(
        targetValue = if (startAnim) data.overallProgress else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "InstallmentOverallProgress"
    )

    Layered3DCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("installment_analysis_card"),
        shape = RoundedCornerShape(RadiusLG),
        backgroundColor = MaterialTheme.colorScheme.surface,
        elevation = 3.dp,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalance,
                            contentDescription = null,
                            tint = InfoIndigoLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "تحلیل اقساط و تعهدات مالی",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "وضعیت پرداخت ${data.activeInstallmentsCount} قسط فعال دوره",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Active Badges
                Surface(
                    shape = CircleShape,
                    color = InfoIndigoLight.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${data.activeInstallmentsCount} قسط فعال",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = InfoIndigoLight,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Countdown & Health Rating Row (ADDED)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Countdown card
                Surface(
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(RadiusMD),
                    color = WarningAmberLight.copy(alpha = 0.12f),
                    border = BorderStroke(0.8.dp, WarningAmberLight.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.HourglassTop,
                            contentDescription = null,
                            tint = WarningAmberLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = "نزدیک‌ترین سررسید قسط",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "۴ روز دیگر (وام مسکن)",
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                                color = WarningAmberLight
                            )
                        }
                    }
                }

                // Health rating card
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(RadiusMD),
                    color = SuccessGreenLight.copy(alpha = 0.12f),
                    border = BorderStroke(0.8.dp, SuccessGreenLight.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = null,
                            tint = SuccessGreenLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = "سلامت پرداخت",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "عالی (۱۰۰٪ منظم)",
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                                color = SuccessGreenLight
                            )
                        }
                    }
                }
            }

            // Summary Stats Row (Total, Paid, Remaining)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(RadiusMD),
                color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Total
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "مجموع اقساط",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = data.totalAmountFormatted,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Paid
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "پرداخت‌شده",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = data.paidAmountFormatted,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreenLight
                            )
                        )
                    }

                    // Remaining
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "باقیمانده",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = data.remainingAmountFormatted,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = WarningAmberLight
                            )
                        )
                    }
                }
            }

            // Overall Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "پیشرفت تسویه اقساط این دوره",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${(data.overallProgress * 100).toInt()}٪ تسویه شده",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = InfoIndigoLight
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedOverallProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(InfoIndigoLight)
                    )
                }
            }

            // Commitment Proportion Share Horizontal Bar (ADDED)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "سهم انواع تعهدات و اقساط",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    // Stack proportions sequentially
                    Box(
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight()
                            .background(Color(0xFF38BDF8))
                    )
                    Box(
                        modifier = Modifier
                            .weight(0.25f)
                            .fillMaxHeight()
                            .background(Color(0xFFFBBF24))
                    )
                    Box(
                        modifier = Modifier
                            .weight(0.2f)
                            .fillMaxHeight()
                            .background(Color(0xFFEF4444))
                    )
                }

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    InstallmentLegendItem(label = "وام مسکن (۵۵٪)", color = Color(0xFF38BDF8))
                    InstallmentLegendItem(label = "وام خودرو (۲۵٪)", color = Color(0xFFFBBF24))
                    InstallmentLegendItem(label = "متفرقه (۲۰٪)", color = Color(0xFFEF4444))
                }
            }

            // Category Breakdown Items
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                data.categories.forEach { cat ->
                    InstallmentCategoryRow(cat = cat)
                }
            }
        }
    }
}

@Composable
fun InstallmentLegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun InstallmentCategoryRow(
    cat: InstallmentCategoryItem,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    var startAnim by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { startAnim = true }

    val animatedProgress by animateFloatAsState(
        targetValue = if (startAnim) cat.progress else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "InstCatProgress"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Soft3DIcon(
                    imageRes = cat.iconRes,
                    contentDescription = cat.title,
                    size = 32.dp,
                    accentColor = cat.accentColor,
                    containerShape = RoundedCornerShape(8.dp)
                )

                Text(
                    text = cat.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${cat.paidAmountFormatted} از ${cat.totalAmountFormatted}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "(${(cat.progress * 100).toInt()}٪)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = cat.accentColor
                )
            }
        }

        // Mini progress
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(cat.accentColor)
            )
        }
    }
}
