package com.example.vehicle.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.util.IranianPhoneUtils
import com.example.vehicle.domain.VehicleHealthCheckItem
import com.example.vehicle.domain.VehicleHealthReport

/**
 * Dedicated visual presentation of the existing vehicle health report.
 * All scores and checklist states are supplied by VehicleHealthReport.
 * This screen intentionally contains no health calculations or persistence logic.
 */
@Composable
fun VehicleHealthDetailsScreen(
    report: VehicleHealthReport,
    onBack: () -> Unit,
    onItemClick: (String) -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    val statusColor = Color(report.level.colorHex)

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("vehicle_health_details_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = bottomBar
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterLeft).size(44.dp)) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "بازگشت",
                                tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(
                            text = "وضعیت سلامت و استهلاک خودرو",
                            modifier = Modifier.align(Alignment.Center).padding(horizontal = 46.dp),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                        Icon(Icons.Rounded.HealthAndSafety, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.CenterRight).size(30.dp))
                    }
                }
            }

            item {
                Layered3DCard(
                    modifier = Modifier.fillMaxWidth().height(226.dp).testTag("vehicle_health_score_card"),
                    elevation = 4.dp,
                    borderColor = statusColor.copy(alpha = 0.38f)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                                        MaterialTheme.colorScheme.surface,
                                        statusColor.copy(alpha = 0.10f)
                                    )
                                )
                            )
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_3d_car),
                            contentDescription = "تصویر خودرو",
                            modifier = Modifier.align(Alignment.CenterLeft).fillMaxHeight().fillMaxWidth(0.58f),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier.fillMaxSize().background(
                                Brush.horizontalGradient(
                                    0f to Color.Transparent,
                                    0.42f to MaterialTheme.colorScheme.surface.copy(alpha = 0.10f),
                                    1f to MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                                )
                            )
                        )
                        Column(
                            modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 14.dp)
                                .widthIn(max = 180.dp),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                "امتیاز سلامت خودرو",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            SemiCircularHealthGauge(
                                score = report.healthScorePercent,
                                color = statusColor,
                                modifier = Modifier.width(145.dp).height(72.dp)
                            )
                            Text(
                                text = IranianPhoneUtils.convertDigitsToPersian(report.healthScorePercent.toString()) + "٪",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold, fontSize = 27.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(30.dp),
                                color = statusColor.copy(alpha = 0.14f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.28f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = if (report.healthScorePercent >= 80) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                                        contentDescription = null, tint = statusColor, modifier = Modifier.size(17.dp)
                                    )
                                    Text(report.level.label, color = statusColor,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
                                .padding(horizontal = 13.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = report.level.summaryText,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2
                            )
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null,
                                tint = statusColor, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }

            item {
                Text(
                    text = "وضعیت قطعات و مدارک",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            items(report.checklist.chunked(3).size) { rowIndex ->
                val rowItems = report.checklist.chunked(3)[rowIndex]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    rowItems.forEach { checkItem ->
                        VehicleHealthItemCard(
                            item = checkItem,
                            modifier = Modifier.weight(1f),
                            onClick = { onItemClick(checkItem.title) }
                        )
                    }
                    repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            item {
                MaintenanceGuidancePanel()
            }
        }
    }
}


@Composable
private fun SemiCircularHealthGauge(
    score: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stroke = 10.dp.toPx()
        val diameter = size.width - stroke
        drawArc(
            color = color.copy(alpha = 0.18f),
            startAngle = 180f,
            sweepAngle = -180f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(stroke / 2f, size.height - diameter / 2f),
            size = androidx.compose.ui.geometry.Size(diameter, diameter),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = -180f * (score.coerceIn(0, 100) / 100f),
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(stroke / 2f, size.height - diameter / 2f),
            size = androidx.compose.ui.geometry.Size(diameter, diameter),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun VehicleHealthItemCard(
    item: VehicleHealthCheckItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val itemColor = when {
        item.isOk -> Color(0xFF16A34A)
        item.isWarning -> Color(0xFFDC2626)
        else -> Color(0xFFD97706)
    }

    Surface(
        modifier = modifier.heightIn(min = 142.dp).clip(RoundedCornerShape(17.dp))
            .clickable(onClick = onClick).testTag("vehicle_health_item_${item.title}"),
        shape = RoundedCornerShape(17.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f)
        ),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Soft3DIcon(
                imageRes = item.iconRes,
                contentDescription = item.title,
                size = 35.dp,
                accentColor = itemColor
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                minLines = 2,
                maxLines = 2,
                lineHeight = 16.sp
            )
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = itemColor.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, itemColor.copy(alpha = 0.24f))
            ) {
                Text(
                    text = IranianPhoneUtils.convertDigitsToPersian(item.statusText),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                    color = itemColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

@Composable
private fun MaintenanceGuidancePanel() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Build,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "توصیه‌های نگهداری",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "برای حفظ سلامت خودرو، سرویس‌های دوره‌ای را به‌موقع انجام دهید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Rounded.ArrowBack, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
    }
}
