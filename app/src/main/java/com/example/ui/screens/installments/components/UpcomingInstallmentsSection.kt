package com.example.ui.screens.installments.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
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
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.ui.screens.installments.model.InstallmentItem
import com.example.ui.theme.RadiusMD

@Composable
fun UpcomingInstallmentsSection(
    items: List<InstallmentItem>,
    onItemClick: (InstallmentItem) -> Unit,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        text = "اقساط نزدیک (${items.size} مورد)",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                TextButton(
                    onClick = onSeeAllClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("upcoming_installments_see_all")
                ) {
                    Text(
                        text = "مشاهده همه",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (isExpanded) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.take(3).forEach { item ->
                    Layered3DCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(RadiusMD),
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        elevation = 2.dp,
                        contentPadding = PaddingValues(12.dp),
                        testTag = "upcoming_installment_${item.id}",
                        onClick = { onItemClick(item) }
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
                                    imageRes = item.category.iconRes,
                                    contentDescription = item.title,
                                    size = 38.dp,
                                    accentColor = item.category.accentColor,
                                    containerShape = RoundedCornerShape(10.dp)
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${item.nextPaymentDate} • ${item.nextDueDaysText}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = item.monthlyPaymentFormatted,
                                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                InstallmentStatusBadge(
                                    status = item.status,
                                    compact = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
