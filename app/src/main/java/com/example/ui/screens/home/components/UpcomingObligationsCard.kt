package com.example.ui.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Layered3DCard
import com.example.ui.screens.home.domain.UpcomingObligationItem
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.WarningAmberLight
import com.example.util.IranianPhoneUtils

@Composable
fun UpcomingObligationsCard(
    obligations: List<UpcomingObligationItem>,
    onViewAllClick: () -> Unit,
    onObligationClick: (UpcomingObligationItem) -> Unit,
    onMarkDoneClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val nearestItem = obligations.firstOrNull()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Layered3DCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
            elevation = 2.dp,
            contentPadding = PaddingValues(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "تعهد نزدیک",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    if (nearestItem == null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                                .testTag("empty_obligations_card"),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(EmeraldPrimaryLight.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.EventAvailable,
                                    contentDescription = null,
                                    tint = EmeraldPrimaryLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "تعهد بانکی فعالی نیست",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "همه اقساط بانکی پرداخت شده‌اند.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    } else {
                        NearestObligationFlatItem(
                            item = nearestItem,
                            onClick = { onObligationClick(nearestItem) },
                            onMarkDone = { onMarkDoneClick(nearestItem.id) }
                        )
                    }
                }
            }
        }

        // View All button centered strictly below the card, outside card container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { onViewAllClick() },
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "مشاهده همه تعهدات",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun NearestObligationFlatItem(
    item: UpcomingObligationItem,
    onClick: () -> Unit,
    onMarkDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("obligation_row_${item.id}"),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // 1. Title + Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            )

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
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
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

        // 2. Bank / Provider Name
        if (item.bankName.isNotBlank()) {
            Text(
                text = "بانک: ${item.bankName}",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 3. Due Date
        Text(
            text = "سررسید: ${IranianPhoneUtils.convertDigitsToPersian(item.dueDatePersian)}",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )

        // 4. Amount
        Text(
            text = "مبلغ قسط: ${item.formattedAmount}",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )

        // 5. Progress percentage & bar
        val totalCount = item.totalInstallments.coerceAtLeast(1)
        val paidCount = (item.totalInstallments - item.remainingInstallments).coerceIn(0, totalCount)
        val progressFraction = (paidCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)
        val percentInt = (progressFraction * 100).toInt()
        val percentPersian = IranianPhoneUtils.convertDigitsToPersian(percentInt.toString())
        val paidPersian = IranianPhoneUtils.convertDigitsToPersian(paidCount.toString())
        val totalPersian = IranianPhoneUtils.convertDigitsToPersian(totalCount.toString())

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "پیشرفت: $percentPersian% ($paidPersian از $totalPersian قسط)",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
        }

        // 6. Action Button below progress bar
        Button(
            onClick = onMarkDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .testTag("pay_obligation_button_${item.id}"),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "ثبت پرداخت",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp
                )
            )
        }
    }
}
