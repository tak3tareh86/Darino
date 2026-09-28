package com.example.ui.screens.installments.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.ui.screens.installments.model.InstallmentItem
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD

@Composable
fun InstallmentCard(
    item: InstallmentItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Layered3DCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLG),
        backgroundColor = MaterialTheme.colorScheme.surface,
        elevation = 2.5.dp,
        contentPadding = PaddingValues(14.dp),
        testTag = "installment_card_${item.id}",
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Icon + Title/Provider + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Soft3DIcon(
                        imageRes = item.category.iconRes,
                        contentDescription = item.title,
                        size = 40.dp,
                        accentColor = item.category.accentColor,
                        containerShape = RoundedCornerShape(12.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = buildString {
                                append(item.providerOrPerson)
                                if (!item.vehicleName.isNullOrEmpty()) {
                                    append(" • ${item.vehicleName}")
                                }
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                InstallmentStatusBadge(status = item.status)
            }

            // Key Financial Metrics Grid (Total, Paid, Remaining)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(
                    title = "کل:",
                    value = item.totalAmountFormatted,
                    modifier = Modifier.weight(1f)
                )
                MetricColumn(
                    title = "پرداخت‌شده:",
                    value = item.paidAmountFormatted,
                    modifier = Modifier.weight(1f)
                )
                MetricColumn(
                    title = "باقیمانده:",
                    value = item.remainingAmountFormatted,
                    highlight = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Modern Horizontal Progress Bar
            InstallmentProgressBar(
                progress = item.progressPercentage,
                activeColor = item.category.accentColor,
                label = "پیشرفت پرداخت"
            )

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            )

            // Detail Button
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = RoundedCornerShape(RadiusMD),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = "جزئیات اقساط",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Footer Details: Monthly payment, Next payment date, Remaining count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "قسط ماهانه:",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item.monthlyPaymentFormatted,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "سررسید: ${item.nextPaymentDate}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "${item.remainingInstallments} قسط باقی",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricColumn(
    title: String,
    value: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp),
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}
