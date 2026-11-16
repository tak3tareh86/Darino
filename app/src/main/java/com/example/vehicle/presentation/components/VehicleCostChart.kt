package com.example.vehicle.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.MoneyAmount
import com.example.util.MoneyFormatter
import com.example.ui.screens.settings.model.AppCurrency
import com.example.vehicle.domain.VehicleMonthlyExpenseBar

/**
 * Modern Stacked Monthly Vehicle Cost Chart (نمودار هزینه خودرو)
 * Segments: Fuel (Blue), Repairs (Red), Service (Green), Insurance (Amber)
 */
@Composable
fun VehicleCostChart(
    bars: List<VehicleMonthlyExpenseBar>,
    modifier: Modifier = Modifier
) {
    val maxTotal = (bars.maxOfOrNull { it.totalAmount } ?: 5_000_000L).coerceAtLeast(1_000_000L).toFloat()
    val progress = remember { Animatable(0f) }
    var selectedBarIndex by remember(bars) { mutableIntStateOf(-1) }

    LaunchedEffect(bars) {
        progress.animateTo(1f, animationSpec = tween(700))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("vehicle_cost_chart")
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Chart Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            bars.forEachIndexed { barIndex, bar ->
                val fuelRatio = (bar.fuelAmount / maxTotal) * progress.value
                val repairRatio = (bar.repairAmount / maxTotal) * progress.value
                val serviceRatio = (bar.serviceAmount / maxTotal) * progress.value
                val insuranceRatio = (bar.insuranceAmount / maxTotal) * progress.value
                val otherRatio = (bar.otherAmount / maxTotal) * progress.value

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .weight(1f)
                        .pointerInput(barIndex) {
                            detectTapGestures {
                                selectedBarIndex = if (selectedBarIndex == barIndex) -1 else barIndex
                            }
                        }
                ) {
                    // Keep the original label space so the chart geometry remains unchanged.
                    Spacer(modifier = Modifier.height(18.dp))

                    // Stacked Bar.
                    Column(
                        modifier = Modifier
                            .width(14.dp)
                            .height(118.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        if (insuranceRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((118 * insuranceRatio).dp)
                                    .background(Color(0xFFFBBF24))
                            )
                        }
                        if (repairRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((118 * repairRatio).dp)
                                    .background(Color(0xFFFB7185))
                            )
                        }
                        if (serviceRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((118 * serviceRatio).dp)
                                    .background(Color(0xFF34D399))
                            )
                        }
                        if (fuelRatio > 0f) {
                            Box(Modifier.fillMaxWidth().height((118 * fuelRatio).dp).background(Color(0xFF38BDF8)))
                        }
                        if (otherRatio > 0f) {
                            Box(Modifier.fillMaxWidth().height((118 * otherRatio).dp).background(Color(0xFFA78BFA)))
                        }
                    }

                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Full month names are rotated vertically to remain readable
                    // beneath their own bars without colliding with the legend.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = bar.monthName,
                            modifier = Modifier
                                .requiredWidth(54.dp)
                                .graphicsLayer(rotationZ = -90f),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = if (bar.isCurrentMonth) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (bar.isCurrentMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Legend Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            ChartLegendItem(title = "سوخت", color = Color(0xFF38BDF8))
            ChartLegendItem(title = "سرویس", color = Color(0xFF34D399))
            ChartLegendItem(title = "تعمیرات و قطعات", color = Color(0xFFFB7185))
            ChartLegendItem(title = "بیمه", color = Color(0xFFFBBF24))
            ChartLegendItem(title = "سایر", color = Color(0xFFA78BFA))
        }
      }

      // A single tooltip is anchored to this chart's top-center, not to an individual bar.
      val selectedBar = bars.getOrNull(selectedBarIndex)
      Box(
          modifier = Modifier
              .align(Alignment.TopCenter)
              .wrapContentSize()
      ) {
          DropdownMenu(
              expanded = selectedBar != null,
              onDismissRequest = { selectedBarIndex = -1 },
              offset = DpOffset(x = 0.dp, y = 0.dp)
          ) {
              selectedBar?.let { bar ->
                  Surface(
                      shape = RoundedCornerShape(10.dp),
                      color = MaterialTheme.colorScheme.surface,
                      tonalElevation = 6.dp,
                      shadowElevation = 6.dp,
                      modifier = Modifier.widthIn(min = 132.dp, max = 220.dp)
                  ) {
                      Column(
                          modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                          horizontalAlignment = Alignment.CenterHorizontally,
                          verticalArrangement = Arrangement.spacedBy(4.dp)
                      ) {
                          Text(
                              text = bar.monthName,
                              style = MaterialTheme.typography.labelMedium,
                              color = MaterialTheme.colorScheme.onSurface,
                              textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                              modifier = Modifier.fillMaxWidth()
                          )
                          Text(
                              text = MoneyFormatter.formatExact(
                                  MoneyAmount.of(bar.totalAmount, AppCurrency.RIAL)
                              ),
                              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                              color = MaterialTheme.colorScheme.primary,
                              textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                              modifier = Modifier.fillMaxWidth()
                          )
                      }
                  }
              }
          }
      }
    }
}

@Composable
private fun ChartLegendItem(
    title: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
