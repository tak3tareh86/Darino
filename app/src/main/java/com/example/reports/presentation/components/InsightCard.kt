package com.example.reports.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reports.domain.FinancialInsight
import com.example.reports.domain.InsightSeverity
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.util.IranianPhoneUtils

/**
 * Premium 3D Insight Card for Darino Insights.
 */
@Composable
fun InsightCard(
    insight: FinancialInsight,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val (accentColor, bgColor, badgeText) = when (insight.severity) {
        InsightSeverity.POSITIVE -> Triple(
            Color(0xFF10B981),
            Color(0xFF10B981).copy(alpha = 0.08f),
            "عملکرد عالی"
        )
        InsightSeverity.WARNING -> Triple(
            Color(0xFFF59E0B),
            Color(0xFFF59E0B).copy(alpha = 0.09f),
            "توجه بودجه"
        )
        InsightSeverity.CRITICAL -> Triple(
            Color(0xFFEF4444),
            Color(0xFFEF4444).copy(alpha = 0.10f),
            "هشدار فوری"
        )
        InsightSeverity.INFO -> Triple(
            Color(0xFF3B82F6),
            Color(0xFF3B82F6).copy(alpha = 0.08f),
            "نکته تحلیلی"
        )
    }

    Layered3DCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("insight_card_${insight.id}"),
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = accentColor.copy(alpha = 0.35f),
        elevation = 3.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Icon + Title + Severity Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Soft3DIcon(
                        imageRes = insight.iconRes,
                        contentDescription = insight.title,
                        size = 38.dp,
                        accentColor = accentColor
                    )
                    Column {
                        Text(
                            text = insight.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (insight.changePercent != null) {
                            Text(
                                text = "تغییر: ${IranianPhoneUtils.convertDigitsToPersian(insight.changePercent.toString())}٪",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor
                            )
                        }
                    }
                }

                // Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = bgColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Description
            Text(
                text = insight.description,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 20.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Recommendation Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when (insight.severity) {
                        InsightSeverity.POSITIVE -> Icons.Rounded.CheckCircle
                        InsightSeverity.WARNING -> Icons.Rounded.Warning
                        InsightSeverity.CRITICAL -> Icons.Rounded.Warning
                        InsightSeverity.INFO -> Icons.Rounded.Info
                    },
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
                Text(
                    text = insight.recommendation,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
