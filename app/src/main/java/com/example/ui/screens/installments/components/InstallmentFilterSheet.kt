package com.example.ui.screens.installments.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.installments.model.InstallmentCategory
import com.example.ui.screens.installments.model.InstallmentStatus
import com.example.ui.theme.RadiusMD

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentFilterSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onApplyFilter: (InstallmentCategory?, InstallmentStatus?, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<InstallmentCategory?>(null) }
    var selectedStatus by remember { mutableStateOf<InstallmentStatus?>(null) }
    var selectedPeriod by remember { mutableStateOf("همه") }

    val periods = listOf("همه", "ماه جاری", "۳ ماه آینده", "سال جاری")
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "فیلتر اقساط و تعهدات",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "بستن",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Category Filter
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "نوع تعهد",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    InstallmentCategory.entries.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedCategory = if (isSelected) null else cat
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) cat.accentColor.copy(alpha = if (isDark) 0.3f else 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, cat.accentColor) else null
                        ) {
                            Text(
                                text = cat.title,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = if (isSelected) cat.accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Status Filter
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "وضعیت پرداخت",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        InstallmentStatus.PENDING,
                        InstallmentStatus.DUE_SOON,
                        InstallmentStatus.OVERDUE,
                        InstallmentStatus.COMPLETED
                    ).forEach { status ->
                        val isSelected = selectedStatus == status
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedStatus = if (isSelected) null else status
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) status.color.copy(alpha = if (isDark) 0.3f else 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, status.color) else null
                        ) {
                            Text(
                                text = status.title,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = if (isSelected) status.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Period Filter
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "بازه زمانی سررسید",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    periods.forEach { period ->
                        val isSelected = selectedPeriod == period
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedPeriod = period },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.3f else 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Text(
                                text = period,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Action Buttons (Reset, Apply)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        selectedCategory = null
                        selectedStatus = null
                        selectedPeriod = "همه"
                        onApplyFilter(null, null, null)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(RadiusMD)
                ) {
                    Text("حذف فیلترها", style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = {
                        onApplyFilter(selectedCategory, selectedStatus, if (selectedPeriod == "همه") null else selectedPeriod)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("apply_installment_filter_btn"),
                    shape = RoundedCornerShape(RadiusMD),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Rounded.Done, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("اعمال فیلتر", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
