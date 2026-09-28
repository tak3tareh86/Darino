package com.example.ui.screens.reports.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.reports.model.ReportPeriod
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD

@Composable
fun ReportPeriodSelector(
    selectedPeriod: ReportPeriod,
    onPeriodSelected: (ReportPeriod) -> Unit,
    onCustomRangeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val periods = ReportPeriod.values()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Pill Bar Container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(RadiusLG),
                    ambientColor = if (isDark) Color.Black.copy(alpha = 0.4f) else Color(0xFF0F172A).copy(alpha = 0.04f),
                    spotColor = if (isDark) Color.Black.copy(alpha = 0.6f) else Color(0xFF0F172A).copy(alpha = 0.08f)
                ),
            shape = RoundedCornerShape(RadiusLG),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            )
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(periods) { period ->
                    val isSelected = period == selectedPeriod

                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            EmeraldPrimaryLight
                        } else {
                            if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                        },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "PeriodBgColor"
                    )

                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            Color.White
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "PeriodTextColor"
                    )

                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.0f else 0.98f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "PeriodScale"
                    )

                    Surface(
                        modifier = Modifier
                            .scale(scale)
                            .shadow(
                                elevation = if (isSelected) 4.dp else 0.dp,
                                shape = RoundedCornerShape(RadiusMD),
                                ambientColor = EmeraldPrimaryLight.copy(alpha = 0.3f),
                                spotColor = EmeraldPrimaryLight.copy(alpha = 0.5f)
                            )
                            .clip(RoundedCornerShape(RadiusMD))
                            .clickable {
                                onPeriodSelected(period)
                                if (period == ReportPeriod.CUSTOM) {
                                    onCustomRangeClick()
                                }
                            }
                            .testTag("period_tab_${period.name}"),
                        shape = RoundedCornerShape(RadiusMD),
                        color = bgColor,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) EmeraldPrimaryLight else Color.Transparent
                        )
                    ) {
                        Text(
                            text = period.title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            ),
                            color = textColor,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Active Period Subtitle & Range Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarToday,
                    contentDescription = null,
                    tint = EmeraldPrimaryLight,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = selectedPeriod.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (selectedPeriod == ReportPeriod.CUSTOM) {
                Surface(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onCustomRangeClick)
                        .testTag("edit_custom_range_badge"),
                    shape = CircleShape,
                    color = EmeraldPrimaryLight.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "تغییر تاریخ",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = EmeraldPrimaryLight,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
