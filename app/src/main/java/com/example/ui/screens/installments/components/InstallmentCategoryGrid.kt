package com.example.ui.screens.installments.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.ui.screens.installments.model.CategorySummaryStat
import com.example.ui.screens.installments.model.InstallmentCategory
import com.example.ui.theme.RadiusMD

@Composable
fun InstallmentCategoryGrid(
    categories: List<CategorySummaryStat>,
    onCategoryClick: (InstallmentCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "دسته‌بندی تعهدات و اقساط",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "۴ دسته اصلی",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 2x2 Grid using Rows
        val firstRow = categories.take(2)
        val secondRow = categories.drop(2).take(2)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            firstRow.forEach { item ->
                CategoryGridCard(
                    item = item,
                    onClick = { onCategoryClick(item.category) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            secondRow.forEach { item ->
                CategoryGridCard(
                    item = item,
                    onClick = { onCategoryClick(item.category) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CategoryGridCard(
    item: CategorySummaryStat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Layered3DCard(
        modifier = modifier,
        shape = RoundedCornerShape(RadiusMD),
        backgroundColor = MaterialTheme.colorScheme.surface,
        elevation = 2.dp,
        contentPadding = PaddingValues(10.dp),
        testTag = "category_card_${item.category.id}",
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Soft3DIcon(
                imageRes = item.category.iconRes,
                contentDescription = item.category.title,
                size = 38.dp,
                accentColor = item.category.accentColor,
                containerShape = RoundedCornerShape(10.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.category.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 12.5.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.countText,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item.remainingFormatted,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = item.category.accentColor
                    )
                }
            }
        }
    }
}
