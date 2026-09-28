package com.example.ui.screens.reports.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Layered3DCard
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.WarningAmberLight

data class DrillDownItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun ReportDrillDownLinks(
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val links = listOf(
        DrillDownItem(
            id = "expense_analysis",
            title = "تحلیل دسته‌بندی هزینه‌ها",
            subtitle = "سهم خوراک، خرید، مسکن و قبوض",
            icon = Icons.Rounded.PieChart,
            accentColor = ExpenseRoseLight
        ),
        DrillDownItem(
            id = "installment_report",
            title = "گزارش اقساط و تعهدات مالی",
            subtitle = "روند پرداخت و مانده وام‌های بانکی و خانگی",
            icon = Icons.Rounded.AccountBalance,
            accentColor = WarningAmberLight
        ),
        DrillDownItem(
            id = "vehicle_report",
            title = "هزینه‌ها و کارکرد خودرو",
            subtitle = "سوخت، بیمه و سرویس‌های دوره‌ای",
            icon = Icons.Rounded.DirectionsCar,
            accentColor = InfoIndigoLight
        ),
        DrillDownItem(
            id = "annual_report",
            title = "گزارش سالانه و مقایسه‌ای",
            subtitle = "روند رشد پس‌انداز و مقایسه ماهانه",
            icon = Icons.Rounded.CalendarMonth,
            accentColor = EmeraldPrimaryLight
        )
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "گزارش‌های تخصصی",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            links.forEach { link ->
                Layered3DCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(RadiusMD),
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    elevation = 2.dp,
                    contentPadding = PaddingValues(12.dp),
                    testTag = "drilldown_${link.id}",
                    onClick = { onLinkClick(link.id) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(link.accentColor.copy(alpha = if (isDark) 0.2f else 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = link.icon,
                                    contentDescription = null,
                                    tint = link.accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = link.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = link.subtitle,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "مشاهده",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
