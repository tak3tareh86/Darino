package com.example.reports.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
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
import com.example.reports.domain.ReportSummary
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.util.IranianPhoneUtils
import java.text.NumberFormat
import java.util.Locale

/**
 * Bottom Sheet for Exporting and Sharing Financial Reports.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportExportSheet(
    summary: ReportSummary?,
    onExportPdf: () -> Unit,
    onExportImage: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("report_export_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Soft3DIcon(
                        imageRes = R.drawable.img_3d_analytics,
                        contentDescription = "خروجی و اشتراک‌گذاری گزارش",
                        size = 32.dp,
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "خروجی و اشتراک‌گذاری گزارش",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "بستن",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

            // Report Preview Summary Card
            if (summary != null) {
                Layered3DCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 2.dp,
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "پیش‌نمایش خروجی ${summary.periodLabel}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "مجموع درآمد: ${IranianPhoneUtils.convertDigitsToPersian(NumberFormat.getNumberInstance(Locale.US).format(summary.totalIncome))} تومان",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF10B981)
                            )
                            Text(
                                text = "مجموع هزینه: ${IranianPhoneUtils.convertDigitsToPersian(NumberFormat.getNumberInstance(Locale.US).format(summary.totalExpense))} تومان",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFEF4444)
                            )
                        }
                        Text(
                            text = "تحلیل شامل: نمودارهای دایره‌ای، میله‌ای، وضعیت اقساط و خودرو",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Export Options
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // PDF Option
                Button(
                    onClick = onExportPdf,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("export_pdf_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PictureAsPdf,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Text(
                            text = "دریافت فایل PDF تحلیلی (Mock)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // Image Option
                OutlinedButton(
                    onClick = onExportImage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("export_image_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "ذخیره تصویر گرافیکی خلاصه (Mock)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
