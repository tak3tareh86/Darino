package com.example.vehicle.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.util.IranianPhoneUtils
import com.example.vehicle.domain.VehicleHealthCheckItem
import com.example.vehicle.domain.VehicleHealthReport

/**
 * Dedicated presentation for the existing vehicle health report.
 * This screen only renders the supplied report; it does not calculate or persist data.
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
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "بازگشت",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "وضعیت سلامت و استهلاک خودرو",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "گزارش وضعیت قطعات و مدارک خودرو",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.HealthAndSafety,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            item {
                Layered3DCard(
                    modifier = Modifier.fillMaxWidth().testTag("vehicle_health_score_card"),
                    elevation = 4.dp,
                    borderColor = statusColor.copy(alpha = 0.42f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        statusColor.copy(alpha = 0.13f),
                                        MaterialTheme.colorScheme.surface,
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(108.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = report.healthScorePercent.coerceIn(0, 100) / 100f,
                                    modifier = Modifier.fillMaxSize(),
                                    color = statusColor,
                                    trackColor = statusColor.copy(alpha = 0.15f),
                                    strokeWidth = 9.dp
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = IranianPhoneUtils.convertDigitsToPersian(report.healthScorePercent.toString()) + "٪",
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 23.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "سلامت خودرو",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.DirectionsCar,
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.size(23.dp)
                                    )
                                    Text(
                                        text = "شاخص سلامت خودرو",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = statusColor.copy(alpha = 0.14f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = report.level.label,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = statusColor
                                    )
                                }
                                Text(
                                    text = report.level.summaryText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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

            items(report.checklist.chunked(2).size) { rowIndex ->
                val rowItems = report.checklist.chunked(2)[rowIndex]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { item ->
                        VehicleHealthItemCard(
                            item = item,
                            modifier = Modifier.weight(1f),
                            onClick = { onItemClick(item.title) }
                        )
                    }
                    if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
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
        modifier = modifier
            .heightIn(min = 132.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("vehicle_health_item_${item.title}"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        ),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Soft3DIcon(
                imageRes = item.iconRes,
                contentDescription = item.title,
                size = 38.dp,
                accentColor = itemColor
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                minLines = 2,
                maxLines = 2
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = itemColor.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, itemColor.copy(alpha = 0.28f))
            ) {
                Text(
                    text = IranianPhoneUtils.convertDigitsToPersian(item.statusText),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = itemColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}
