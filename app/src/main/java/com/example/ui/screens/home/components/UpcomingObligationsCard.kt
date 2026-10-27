package com.example.ui.screens.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.style.TextOverflow
import com.example.util.IranianPhoneUtils
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.ui.screens.home.domain.UpcomingObligationItem
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.WarningAmberLight

@Composable
fun UpcomingObligationsCard(
    obligations: List<UpcomingObligationItem>,
    onViewAllClick: () -> Unit,
    onObligationClick: (UpcomingObligationItem) -> Unit,
    onMarkDoneClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    var isExpanded by remember { mutableStateOf(true) }
    val nearestItem = obligations.firstOrNull()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "تعهد نزدیک",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            TextButton(
                onClick = onViewAllClick,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "مشاهده همه",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack, // Persian forward arrow
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (nearestItem == null) {
                    Layered3DCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 2.dp,
                        contentPadding = PaddingValues(16.dp),
                        testTag = "empty_obligations_card"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(EmeraldPrimaryLight.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.EventAvailable,
                                    contentDescription = null,
                                    tint = EmeraldPrimaryLight,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "در حال حاضر هیچ قسط بانکی پرداخت‌نشده‌ای برای نمایش وجود ندارد.",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "همه وام‌ها و اقساط بانکی در وضعیت پرداخت‌شده و منظم هستند.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    NearestObligationItem(
                        item = nearestItem,
                        onClick = { onObligationClick(nearestItem) },
                        onMarkDone = { onMarkDoneClick(nearestItem.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NearestObligationItem(
    item: UpcomingObligationItem,
    onClick: () -> Unit,
    onMarkDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Layered3DCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("obligation_row_${item.id}"),
        elevation = 2.dp,
        contentPadding = PaddingValues(12.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Soft3DIcon(
                    imageRes = item.iconRes,
                    contentDescription = item.title,
                    size = 40.dp,
                    accentColor = item.accentColor,
                    containerShape = RoundedCornerShape(RadiusMD)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        // Relative Days Badge (e.g. فردا / امروز / معوق)
                        Surface(
                            shape = RoundedCornerShape(RadiusMD),
                            color = when {
                                item.isOverdue || item.relativeDaysText.contains("گذشته") || item.relativeDaysText.contains("معوق") ->
                                    ExpenseRoseLight.copy(alpha = 0.12f)
                                item.relativeDaysText == "فردا" || item.relativeDaysText == "امروز" ->
                                    ExpenseRoseLight.copy(alpha = 0.12f)
                                else ->
                                    WarningAmberLight.copy(alpha = 0.14f)
                            }
                        ) {
                            Text(
                                text = item.relativeDaysText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        item.isOverdue || item.relativeDaysText.contains("گذشته") || item.relativeDaysText.contains("معوق") ->
                                            ExpenseRoseLight
                                        item.relativeDaysText == "فردا" || item.relativeDaysText == "امروز" ->
                                            ExpenseRoseLight
                                        else ->
                                            WarningAmberLight
                                    }
                                )
                            )
                        }
                    }

                    // Bank name & Persian due date
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (item.bankName.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = item.bankName,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "سررسید: ${IranianPhoneUtils.convertDigitsToPersian(item.dueDatePersian)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Formatted Amount
                    Text(
                        text = item.formattedAmount,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Quick Mark-Done Button
            IconButton(
                onClick = onMarkDone,
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                        shape = CircleShape
                    )
                    .testTag("pay_obligation_button_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "ثبت انجام شد",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
