package com.example.ui.screens.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.ui.screens.home.model.UpcomingPaymentItemData
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusMD

@Composable
fun ImportantUpcomingSection(
    items: List<UpcomingPaymentItemData>,
    onViewAllClick: () -> Unit,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayItems = items.take(3)
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
                text = "موارد مهم پیش رو",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            TextButton(
                onClick = onViewAllClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.testTag("upcoming_view_all_btn")
            ) {
                Text(
                    text = "مشاهده همه",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            displayItems.forEach { item ->
                Layered3DCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(RadiusMD),
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    elevation = 2.dp,
                    contentPadding = PaddingValues(10.dp),
                    testTag = "important_item_${item.id}",
                    onClick = { onItemClick(item.id) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Soft3DIcon(
                                imageRes = item.iconRes,
                                contentDescription = item.title,
                                size = 36.dp,
                                accentColor = item.accentColor
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${item.daysRemainingText} • ${item.dueDatePersian}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = if (item.isUrgent) ExpenseRoseLight else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = item.amountFormatted,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
