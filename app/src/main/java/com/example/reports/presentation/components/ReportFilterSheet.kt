package com.example.reports.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reports.domain.ReportFilter
import com.example.reports.domain.ReportPeriod

/**
 * Filter Bottom Sheet for Financial Reports
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportFilterSheet(
    currentFilter: ReportFilter,
    onApplyFilter: (ReportFilter) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPeriod by remember { mutableStateOf(currentFilter.period) }
    var selectedCategory by remember { mutableStateOf(currentFilter.category) }
    var selectedTransactionType by remember { mutableStateOf(currentFilter.transactionType) }
    var selectedCostType by remember { mutableStateOf(currentFilter.costType) }

    val categories = listOf("همه", "خودرو", "خوراک", "خرید", "حمل‌ونقل", "قبوض", "سایر")
    val transactionTypes = listOf("همه", "واریز", "برداشت", "اقساط", "چک")
    val costTypes = listOf("همه", "ضروری", "اقساط", "تعمیرات خودرو", "متفرقه")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("report_filter_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FilterList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "فیلتر گزارش‌های مالی",
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

            // 1. Time Period Selector
            FilterSection(title = "بازه زمانی") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportPeriod.values().forEach { period ->
                        val isSelected = selectedPeriod == period
                        FilterChipItem(
                            title = period.title,
                            isSelected = isSelected,
                            onClick = { selectedPeriod = period },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 2. Category Selector
            FilterSection(title = "دسته‌بندی مخارج") {
                FlowRowLayout(
                    items = categories,
                    selectedItem = selectedCategory,
                    onSelect = { selectedCategory = it }
                )
            }

            // 3. Transaction Type Selector
            FilterSection(title = "نوع تراکنش") {
                FlowRowLayout(
                    items = transactionTypes,
                    selectedItem = selectedTransactionType,
                    onSelect = { selectedTransactionType = it }
                )
            }

            // 4. Cost Type Selector
            FilterSection(title = "نوع هزینه") {
                FlowRowLayout(
                    items = costTypes,
                    selectedItem = selectedCostType,
                    onSelect = { selectedCostType = it }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions: Reset and Apply
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        selectedPeriod = ReportPeriod.MONTH
                        selectedCategory = "همه"
                        selectedTransactionType = "همه"
                        selectedCostType = "همه"
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("تنظیم مجدد")
                }

                Button(
                    onClick = {
                        onApplyFilter(
                            ReportFilter(
                                period = selectedPeriod,
                                category = selectedCategory,
                                transactionType = selectedTransactionType,
                                costType = selectedCostType
                            )
                        )
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("apply_filter_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "اعمال فیلتر",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        content()
    }
}

@Composable
private fun FilterChipItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp
                ),
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun FlowRowLayout(
    items: List<String>,
    selectedItem: String,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowItems.forEach { item ->
                    val isSelected = item == selectedItem
                    FilterChipItem(
                        title = item,
                        isSelected = isSelected,
                        onClick = { onSelect(item) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size < 3) {
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
