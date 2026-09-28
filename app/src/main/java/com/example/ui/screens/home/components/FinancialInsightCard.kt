package com.example.ui.screens.home.components

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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.ui.screens.home.domain.HomeFinancialInsight
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD

@Composable
fun FinancialInsightCard(
    insight: HomeFinancialInsight,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val gradientBg = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF064E3B).copy(alpha = 0.35f),
                Color(0xFF0F172A),
                Color(0xFF1E1B4B).copy(alpha = 0.25f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFECFDF5),
                Color(0xFFF8FAFC),
                Color(0xFFEFF6FF)
            )
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = EmeraldPrimaryLight,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Surface(
                shape = RoundedCornerShape(RadiusMD),
                color = EmeraldPrimaryLight.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "هوشمند",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = EmeraldPrimaryLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )
            }
        }

        Layered3DCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = 3.dp,
            contentPadding = PaddingValues(14.dp),
            onClick = onClick,
            testTag = "home_financial_insight_card"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Soft3DIcon(
                    imageRes = insight.iconRes,
                    contentDescription = insight.title,
                    size = 42.dp,
                    accentColor = insight.accentColor,
                    containerShape = RoundedCornerShape(RadiusMD)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = insight.message,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (insight.subMessage.isNotBlank()) {
                        Text(
                            text = insight.subMessage,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}
