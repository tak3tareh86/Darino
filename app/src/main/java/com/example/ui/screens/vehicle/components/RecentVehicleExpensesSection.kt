package com.example.ui.screens.vehicle.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Layered3DCard
import com.example.ui.screens.vehicle.model.VehicleExpenseItemData
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusMD

@Composable
fun RecentVehicleExpensesSection(
    expenses: List<VehicleExpenseItemData>,
    modifier: Modifier = Modifier,
    onExpenseClick: (VehicleExpenseItemData) -> Unit = {},
    onSeeAllClick: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(RadiusMD))
                .clickable { isExpanded = !isExpanded },
            shape = RoundedCornerShape(RadiusMD),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "تغییر وضعیت نمایش",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "هزینه‌های اخیر خودرو (${expenses.size} مورد)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = "مشاهده همه",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onSeeAllClick)
                        .testTag("see_all_vehicle_expenses")
                )
            }
        }

        if (isExpanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                expenses.forEach { item ->
                    VehicleExpenseItem(
                        expense = item,
                        onClick = { onExpenseClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun VehicleExpenseItem(
    expense: VehicleExpenseItemData,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Layered3DCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusMD),
        backgroundColor = MaterialTheme.colorScheme.surface,
        elevation = 2.dp,
        contentPadding = PaddingValues(12.dp),
        testTag = "vehicle_expense_${expense.id}",
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Right Side: 3D Category Icon + Title + Meta Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Soft 3D Icon
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            expense.accentColor.copy(alpha = if (isDark) 0.25f else 0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = expense.iconRes),
                        contentDescription = expense.categoryTitle,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = expense.datePersian,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = expense.odometerKmFormatted,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Left Side: Cost & Category Tag
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = expense.amountFormatted,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp),
                    color = ExpenseRoseLight
                )

                Surface(
                    shape = CircleShape,
                    color = expense.accentColor.copy(alpha = if (isDark) 0.18f else 0.1f),
                    border = BorderStroke(0.6.dp, expense.accentColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = expense.categoryTitle,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = expense.accentColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
