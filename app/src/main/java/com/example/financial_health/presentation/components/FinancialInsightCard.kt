package com.example.financial_health.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.financial_health.domain.FinancialHealthInsight
import com.example.financial_health.domain.InsightType

@Composable
fun FinancialInsightCard(
    insight: FinancialHealthInsight,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val (tintColor, containerColor, iconVector) = when (insight.type) {
        InsightType.POSITIVE -> Triple(
            Color(0xFF10B981),
            if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFECFDF5),
            Icons.Rounded.CheckCircle
        )
        InsightType.WARNING -> Triple(
            Color(0xFFF59E0B),
            if (isDark) Color(0xFF78350F).copy(alpha = 0.35f) else Color(0xFFFFFBEB),
            Icons.Rounded.Warning
        )
        InsightType.ALERT -> Triple(
            Color(0xFFEF4444),
            if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.35f) else Color(0xFFFEF2F2),
            Icons.Rounded.NotificationsActive
        )
        InsightType.INFO -> Triple(
            Color(0xFF3B82F6),
            if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.35f) else Color(0xFFEFF6FF),
            Icons.Rounded.Info
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, tintColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = insight.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!insight.recommendation.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            tintColor.copy(alpha = 0.12f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "💡 راهکار پیشنهادی: ${insight.recommendation}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
