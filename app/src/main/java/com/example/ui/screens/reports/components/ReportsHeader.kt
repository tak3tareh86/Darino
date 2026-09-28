package com.example.ui.screens.reports.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.RadiusMD

@Composable
fun ReportsHeader(
    onFilterClick: () -> Unit,
    onExportClick: () -> Unit,
    hasActiveFilters: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                ambientColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFF0F172A).copy(alpha = 0.04f),
                spotColor = if (isDark) Color.Black.copy(alpha = 0.7f) else Color(0xFF0F172A).copy(alpha = 0.08f)
            ),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Title & 3D Icon Branding
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 3D Analytics Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(RadiusMD),
                            ambientColor = EmeraldPrimaryLight.copy(alpha = 0.3f),
                            spotColor = EmeraldPrimaryLight.copy(alpha = 0.5f)
                        )
                        .clip(RoundedCornerShape(RadiusMD))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    EmeraldPrimaryLight.copy(alpha = if (isDark) 0.3f else 0.15f),
                                    EmeraldPrimaryLight.copy(alpha = if (isDark) 0.15f else 0.05f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = if (isDark) 0.3f else 0.8f),
                                    EmeraldPrimaryLight.copy(alpha = 0.2f)
                                )
                            ),
                            shape = RoundedCornerShape(RadiusMD)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_3d_analytics),
                        contentDescription = "گزارشات",
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(RadiusMD)),
                        contentScale = ContentScale.Crop
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "گزارشات و تحلیل مالی",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "تصویر واضحتری از وضعیت مالی شما",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action Buttons (Export + Filter)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Export Button Placeholder
                Surface(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(RadiusMD))
                        .clickable(onClick = onExportClick)
                        .testTag("reports_export_btn"),
                    shape = RoundedCornerShape(RadiusMD),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.FileDownload,
                            contentDescription = "خروجی گزارش",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // Filter Button with active badge
                Surface(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(RadiusMD))
                        .clickable(onClick = onFilterClick)
                        .testTag("reports_filter_btn"),
                    shape = RoundedCornerShape(RadiusMD),
                    color = if (hasActiveFilters) EmeraldPrimaryLight.copy(alpha = if (isDark) 0.3f else 0.15f)
                    else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (hasActiveFilters) 1.5.dp else 1.dp,
                        color = if (hasActiveFilters) EmeraldPrimaryLight else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.FilterList,
                            contentDescription = "فیلتر گزارش",
                            tint = if (hasActiveFilters) EmeraldPrimaryLight else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )

                        if (hasActiveFilters) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(5.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPrimaryLight)
                            )
                        }
                    }
                }
            }
        }
    }
}
