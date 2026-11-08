package com.example.vehicle.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.Layered3DCard
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
    val checklistByTitle = report.checklist.associateBy { it.title }

    // Each analyzer checklist entry gets its own independent, actionable card.
    // Only presentation order and layout are defined here; source items and their states remain untouched.
    val displayGroups = listOfNotNull(
        checklistByTitle["روغن موتور"]?.let {
            VehicleHealthDisplayGroup(title = it.title, items = listOf(it))
        },
        checklistByTitle["فیلترها"]?.let {
            VehicleHealthDisplayGroup(title = it.title, items = listOf(it))
        },
        checklistByTitle["روغن گیربکس"]?.let {
            VehicleHealthDisplayGroup(title = it.title, items = listOf(it))
        },
        checklistByTitle["تسمه دینام"]?.let {
            VehicleHealthDisplayGroup(title = it.title, items = listOf(it))
        },
        checklistByTitle["تسمه تایم"]?.let {
            VehicleHealthDisplayGroup(title = it.title, items = listOf(it))
        },
        checklistByTitle["بیمه‌نامه شخص ثالث"]?.let {
            VehicleHealthDisplayGroup(title = it.title, items = listOf(it))
        },
        checklistByTitle["بیمه‌نامه بدنه"]?.let {
            VehicleHealthDisplayGroup(title = it.title, items = listOf(it))
        },
        checklistByTitle["معاینه فنی"]?.let {
            VehicleHealthDisplayGroup(title = it.title, items = listOf(it))
        }
    )

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("vehicle_health_details_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = bottomBar
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.align(Alignment.CenterEnd)
                                .size(44.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
                        ) {
                            Icon(
                                Icons.Rounded.ArrowBack,
                                contentDescription = "بازگشت",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "وضعیت سلامت و استهلاک خودرو",
                            modifier = Modifier.align(Alignment.Center).padding(horizontal = 48.dp),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                        Icon(
                            Icons.Rounded.DirectionsCar,
                            contentDescription = "سلامت خودرو",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.CenterStart).size(34.dp)
                        )
                    }
                }
            }

            item {
                Layered3DCard(
                    modifier = Modifier.fillMaxWidth().height(244.dp)
                        .testTag("vehicle_health_score_card"),
                    elevation = 4.dp,
                    borderColor = statusColor.copy(alpha = 0.34f)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f),
                                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.48f)
                                    ),
                                    start = Offset.Zero,
                                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                )
                            )
                    ) {
                        // Layered theme-colored glows create a refined dashboard-style backdrop.
                        Box(
                            modifier = Modifier.align(Alignment.CenterEnd)
                                .offset(x = 34.dp, y = 4.dp)
                                .size(230.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier.align(Alignment.BottomStart)
                                .offset(x = (-54).dp, y = 58.dp)
                                .size(176.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).padding(13.dp).size(40.dp),
                            shape = CircleShape,
                            color = Color(0xFF16A34A).copy(alpha = 0.13f)
                        ) {
                            Icon(
                                Icons.Rounded.Eco,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.padding(7.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.align(Alignment.Center)
                                .padding(horizontal = 12.dp)
                                .widthIn(max = 178.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = "امتیاز سلامت خودرو",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Box(
                                modifier = Modifier.width(168.dp).height(98.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                SemiCircularHealthGauge(
                                    score = report.healthScorePercent,
                                    color = statusColor,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Text(
                                    text = IranianPhoneUtils.convertDigitsToPersian(
                                        report.healthScorePercent.toString()
                                    ) + "٪",
                                    modifier = Modifier.align(Alignment.Center).padding(top = 16.dp),
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 31.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(30.dp),
                                color = statusColor.copy(alpha = 0.14f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, statusColor.copy(alpha = 0.27f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = if (report.healthScorePercent >= 80)
                                            Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Text(
                                        text = report.level.label,
                                        color = statusColor,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }

                        Text(
                            text = report.level.summaryText,
                            modifier = Modifier.align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
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

            items(displayGroups.chunked(3).size) { rowIndex ->
                val rowGroups = displayGroups.chunked(3)[rowIndex]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    rowGroups.forEach { group ->
                        VehicleHealthGroupCard(
                            group = group,
                            modifier = Modifier.weight(1f),
                            onItemClick = onItemClick
                        )
                    }
                    repeat(3 - rowGroups.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            item {
                MaintenanceGuidancePanel()
            }
        }
    }
}

private data class VehicleHealthDisplayGroup(
    val title: String,
    val items: List<VehicleHealthCheckItem>
)

@Composable
private fun SemiCircularHealthGauge(
    score: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stroke = 11.dp.toPx()
        val diameter = size.width - stroke
        drawArc(
            color = color.copy(alpha = 0.18f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(stroke / 2f, size.height - diameter / 2f),
            size = androidx.compose.ui.geometry.Size(diameter, diameter),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f * (score.coerceIn(0, 100) / 100f),
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(stroke / 2f, size.height - diameter / 2f),
            size = androidx.compose.ui.geometry.Size(diameter, diameter),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun VehicleHealthGroupCard(
    group: VehicleHealthDisplayGroup,
    modifier: Modifier = Modifier,
    onItemClick: (String) -> Unit
) {
    Surface(
        modifier = modifier.heightIn(min = 104.dp).clip(RoundedCornerShape(18.dp))
            .then(
                if (group.items.size == 1) Modifier.clickable { onItemClick(group.items.first().title) }
                else Modifier
            )
            .testTag("vehicle_health_group_${group.title}"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        ),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Image(
                painter = painterResource(id = group.items.first().iconRes),
                contentDescription = group.title,
                modifier = Modifier.size(56.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                text = group.title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (group.items.size == 1) {
                val item = group.items.first()
                HealthStatusChip(item = item)
            } else {
                group.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp))
                            .clickable { onItemClick(item.title) }
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        HealthStatusChip(item = item, compact = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthStatusChip(
    item: VehicleHealthCheckItem,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val itemColor = healthItemColor(item)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = itemColor.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, itemColor.copy(alpha = 0.22f)
        )
    ) {
        Text(
            text = IranianPhoneUtils.convertDigitsToPersian(item.statusText),
            modifier = Modifier.padding(
                horizontal = if (compact) 4.dp else 7.dp,
                vertical = if (compact) 3.dp else 5.dp
            ),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 8.sp else 10.sp
            ),
            color = itemColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun healthItemColor(item: VehicleHealthCheckItem): Color = when {
    item.isOk -> Color(0xFF16A34A)
    item.isWarning -> Color(0xFFD97706)
    else -> Color(0xFF2563EB)
}

@Composable
private fun MaintenanceGuidancePanel() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        )
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
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
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
            Icon(
                Icons.Rounded.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
