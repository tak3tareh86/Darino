package com.example.ui.screens.reports.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.ui.screens.reports.model.InstallmentReportData
import com.example.ui.screens.reports.model.TrendPoint
import com.example.ui.screens.reports.model.VehicleExpenseReportData
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM

enum class ChartViewType(val title: String) {
    INCOME_EXPENSE("درآمد و هزینه"),
    INSTALLMENTS("روند اقساط"),
    VEHICLES("هزینه‌های خودرو")
}

@Composable
fun PrimaryReportChart(
    trendPoints: List<TrendPoint>,
    installmentData: InstallmentReportData,
    vehicleData: VehicleExpenseReportData,
    onVehicleClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedView by remember { mutableIntStateOf(0) }
    val views = ChartViewType.values()

    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Segmented Control
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(RadiusMD),
            color = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color(0xFFE2E8F0).copy(alpha = 0.7f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                views.forEachIndexed { index, viewType ->
                    val isSelected = selectedView == index
                    val tabBgColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                        label = "chartTabBg"
                    )
                    val tabTextColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "chartTabText"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(RadiusSM))
                            .background(tabBgColor)
                            .clickable { selectedView = index }
                            .padding(vertical = 8.dp)
                            .testTag("chart_tab_${viewType.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = viewType.title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            ),
                            color = tabTextColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Active Chart content with Crossfade
        Crossfade(
            targetState = selectedView,
            label = "chartViewCrossfade"
        ) { tabIndex ->
            when (tabIndex) {
                0 -> IncomeExpenseTrendChart(points = trendPoints)
                1 -> InstallmentAnalysisCard(data = installmentData)
                2 -> VehicleExpenseAnalysisCard(
                    data = vehicleData,
                    onVehicleClick = { onVehicleClick(it.vehicleName) }
                )
            }
        }
    }
}
