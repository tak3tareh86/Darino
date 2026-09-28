package com.example.ui.screens.vehicle.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.vehicle.model.VehicleCategoryData
import com.example.ui.theme.RadiusMD

@Composable
fun VehicleCategoryGrid(
    categories: List<VehicleCategoryData>,
    modifier: Modifier = Modifier,
    onCategoryClick: (VehicleCategoryData) -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "دسته‌بندی‌های خودرو",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        // 3 rows of 3 items
        val chunked = categories.chunked(3)
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            chunked.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { cat ->
                        CategoryGridItem(
                            category = cat,
                            modifier = Modifier.weight(1f),
                            onClick = { onCategoryClick(cat) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryGridItem(
    category: VehicleCategoryData,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Surface(
        modifier = modifier
            .testTag("veh_cat_${category.id}")
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(RadiusMD),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .clip(RoundedCornerShape(RadiusMD))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(RadiusMD),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        category.accentColor.copy(alpha = if (isDark) 0.25f else 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (category.iconRes != null) {
                    Image(
                        painter = painterResource(id = category.iconRes),
                        contentDescription = category.title,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = category.iconEmoji,
                        fontSize = 18.sp
                    )
                }
            }

            Text(
                text = category.title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
