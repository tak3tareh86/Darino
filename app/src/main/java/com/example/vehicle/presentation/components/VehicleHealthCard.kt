package com.example.vehicle.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.util.IranianPhoneUtils
import com.example.vehicle.domain.VehicleHealthReport

/**
 * Vehicle Health Card (کارت وضعیت خودرو)
 * Shows status: خوب / نیاز به سرویس / هشدار based on oil, insurance, brakes, mileage, and repairs.
 */
@Composable
fun VehicleHealthCard(
    report: VehicleHealthReport,
    modifier: Modifier = Modifier
) {
    val statusColor = Color(report.level.colorHex)
    val scorePersian = IranianPhoneUtils.convertDigitsToPersian(report.healthScorePercent.toString())

    Layered3DCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("vehicle_health_card"),
        elevation = 4.dp,
        borderColor = statusColor.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Title + Health Badge & Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Soft3DIcon(
                        imageRes = R.drawable.img_3d_analytics,
                        contentDescription = "وضعیت فنی و سلامت خودرو",
                        size = 32.dp,
                        accentColor = statusColor
                    )
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "وضعیت سلامت خودرو (Vehicle Health)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = report.level.summaryText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Health Status Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = report.level.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = statusColor
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))

            // Dynamic Recommendation Alert
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = statusColor.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (report.healthScorePercent >= 80) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = report.recommendation,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Vehicle health checklist: fixed right-side labels and left-side status badges.
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                report.checklist.forEach { item ->
                    val itemColor = when {
                        item.isOk -> Color(0xFF16A34A)
                        item.isWarning -> Color(0xFFDC2626)
                        else -> Color(0xFFD97706)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(9.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.weight(1.25f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Soft3DIcon(
                                imageRes = item.iconRes,
                                contentDescription = item.title,
                                size = 20.dp,
                                accentColor = itemColor
                            )
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                softWrap = true,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Clip
                            )
                        }

                        Surface(
                            modifier = Modifier.weight(0.85f),
                            shape = RoundedCornerShape(8.dp),
                            color = itemColor.copy(alpha = 0.10f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                itemColor.copy(alpha = 0.22f)
                            )
                        ) {
                            Text(
                                text = IranianPhoneUtils.convertDigitsToPersian(item.statusText),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = itemColor,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                softWrap = true,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Clip
                            )
                        }
                    }
                }
            }
        }
    }
}
