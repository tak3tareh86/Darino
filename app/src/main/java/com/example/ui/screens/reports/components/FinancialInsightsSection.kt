package com.example.ui.screens.reports.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.screens.reports.model.FinancialInsightItem
import com.example.ui.screens.reports.model.InsightType
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.SuccessGreenLight
import com.example.ui.theme.WarningAmberLight

@Composable
fun FinancialInsightsSection(
    insights: List<FinancialInsightItem>,
    onInsightClick: (FinancialInsightItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Layered3DCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("financial_insights_card"),
        shape = RoundedCornerShape(RadiusLG),
        backgroundColor = MaterialTheme.colorScheme.surface,
        elevation = 3.dp,
        contentPadding = PaddingValues(16.dp)
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
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = EmeraldPrimaryLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "بینش‌های مالی هوشمند",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "تحلیل الگوهای مصرف و فرصت‌های بهینه‌سازی",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Insights list
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                insights.forEach { insight ->
                    FinancialInsightRow(
                        insight = insight,
                        onClick = { onInsightClick(insight) }
                    )
                }
            }
        }
    }
}

@Composable
fun FinancialInsightRow(
    insight: FinancialInsightItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val accentColor = when (insight.type) {
        InsightType.POSITIVE -> SuccessGreenLight
        InsightType.WARNING -> WarningAmberLight
        InsightType.INFO -> InfoIndigoLight
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMD))
            .clickable(onClick = onClick)
            .testTag("insight_row_${insight.id}"),
        shape = RoundedCornerShape(RadiusMD),
        color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Soft3DIcon(
                imageRes = insight.iconRes,
                contentDescription = insight.title,
                size = 38.dp,
                accentColor = accentColor,
                containerShape = RoundedCornerShape(10.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = insight.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (insight.percentageBadge != null) {
                        Surface(
                            shape = CircleShape,
                            color = accentColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = insight.percentageBadge,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                ),
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = insight.description,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.5f) else Color(0xFFE2E8F0),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = insight.tagText,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
