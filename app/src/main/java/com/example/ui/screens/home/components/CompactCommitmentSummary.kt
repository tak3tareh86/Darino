package com.example.ui.screens.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.ui.screens.home.model.CategoryCardItem
import com.example.ui.theme.RadiusMD

@Composable
fun CompactCommitmentSummary(
    categories: List<CategoryCardItem>,
    onViewAllClick: () -> Unit,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayCategories = categories.take(2)
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

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
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            TextButton(
                onClick = onViewAllClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.testTag("commitments_view_all_btn")
            ) {
                Text(
                    text = "مشاهده همه (${categories.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            displayCategories.forEach { item ->
                Layered3DCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(RadiusMD),
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    elevation = 2.dp,
                    contentPadding = PaddingValues(12.dp),
                    testTag = "compact_commitment_${item.id}",
                    onClick = { onCategoryClick(item.id) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Soft3DIcon(
                            imageRes = item.iconRes,
                            contentDescription = item.title,
                            size = 36.dp,
                            accentColor = item.accentColor
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = item.nextDueValue,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = item.accentColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
