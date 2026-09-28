package com.example.ui.screens.installments.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.ui.screens.installments.components.InstallmentProgressBar
import com.example.ui.screens.installments.components.InstallmentStatusBadge
import com.example.ui.screens.installments.model.InstallmentItem
import com.example.ui.screens.installments.model.InstallmentStatus
import com.example.ui.screens.installments.model.PaymentHistoryItem
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD

@Composable
fun InstallmentDetailScreen(
    item: InstallmentItem,
    onBackClick: () -> Unit,
    onViewScheduleClick: (InstallmentItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 3.dp,
                        ambientColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFF0F172A).copy(alpha = 0.05f),
                        spotColor = if (isDark) Color.Black.copy(alpha = 0.6f) else Color(0xFF0F172A).copy(alpha = 0.08f)
                    ),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.testTag("installment_detail_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "بازگشت",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "جزئیات قسط",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    InstallmentStatusBadge(status = item.status)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Title Card
            Layered3DCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(RadiusLG),
                backgroundColor = MaterialTheme.colorScheme.surface,
                elevation = 3.dp,
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Soft3DIcon(
                        imageRes = item.category.iconRes,
                        contentDescription = item.title,
                        size = 52.dp,
                        accentColor = item.category.accentColor,
                        containerShape = RoundedCornerShape(14.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${item.category.title} • ${item.providerOrPerson}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Financial Summary Card with Progress Bar
            Layered3DCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(RadiusLG),
                backgroundColor = MaterialTheme.colorScheme.surface,
                elevation = 2.dp,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "وضعیت مالی و پیشرفت پرداخت",
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.5.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DetailMetric(
                            label = "مبلغ کل",
                            value = item.totalAmountFormatted,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        DetailMetric(
                            label = "پرداخت‌شده",
                            value = item.paidAmountFormatted,
                            color = EmeraldPrimaryLight,
                            modifier = Modifier.weight(1f)
                        )
                        DetailMetric(
                            label = "باقیمانده",
                            value = item.remainingAmountFormatted,
                            color = item.category.accentColor,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Progress Bar
                    InstallmentProgressBar(
                        progress = item.progressPercentage,
                        activeColor = item.category.accentColor,
                        label = "درصد تسویه شده"
                    )
                }
            }

            // Schedule & Installment Info Card
            Layered3DCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(RadiusLG),
                backgroundColor = MaterialTheme.colorScheme.surface,
                elevation = 2.dp,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "مشخصات اقساط و سررسید",
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.5.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    InfoRow(
                        icon = Icons.Rounded.Payments,
                        label = "مبلغ هر قسط:",
                        value = item.monthlyPaymentFormatted
                    )
                    InfoRow(
                        icon = Icons.Rounded.ReceiptLong,
                        label = "تعداد کل اقساط:",
                        value = "${item.totalInstallments} قسط (${item.remainingInstallments} قسط باقی‌مانده)"
                    )
                    InfoRow(
                        icon = Icons.Rounded.CalendarMonth,
                        label = "قسط بعدی:",
                        value = "${item.nextPaymentDate} (${item.nextDueDaysText})"
                    )
                    InfoRow(
                        icon = Icons.Rounded.CalendarMonth,
                        label = "دوره قرارداد:",
                        value = "از ${item.startDate} تا ${item.endDate}"
                    )

                    if (!item.vehicleName.isNullOrEmpty()) {
                        InfoRow(
                            icon = Icons.Rounded.DirectionsCar,
                            label = "خودرو مربوطه:",
                            value = item.vehicleName
                        )
                    }

                    if (!item.insuranceType.isNullOrEmpty()) {
                        InfoRow(
                            icon = Icons.Rounded.AccountBalance,
                            label = "نوع پوشش:",
                            value = item.insuranceType
                        )
                    }
                }
            }

            // Payment History ("سوابق پرداخت")
            Layered3DCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(RadiusLG),
                backgroundColor = MaterialTheme.colorScheme.surface,
                elevation = 2.dp,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    var selectedTabIndex by remember { mutableStateOf(0) }
                    val tabs = listOf("همه", "پرداخت شده", "پرداخت نشده")
                    val filteredHistory = remember(item.paymentHistory, selectedTabIndex) {
                        when (selectedTabIndex) {
                            1 -> item.paymentHistory.filter { it.status == InstallmentStatus.PAID || it.status == InstallmentStatus.COMPLETED }
                            2 -> item.paymentHistory.filter { it.status != InstallmentStatus.PAID && it.status != InstallmentStatus.COMPLETED }
                            else -> item.paymentHistory
                        }
                    }
                    
                    Text(
                        text = "سوابق پرداخت",
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.5.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    TabRow(selectedTabIndex = selectedTabIndex) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(title, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    if (filteredHistory.isEmpty()) {
                        Text(
                            text = "موردی یافت نشد.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            filteredHistory.forEach { historyItem ->
                                PaymentHistoryRow(item = historyItem)
                            }
                        }
                    }
                }
            }

            // Notes Section
            if (item.notes.isNotEmpty()) {
                Layered3DCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(RadiusLG),
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    elevation = 2.dp,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Notes,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "توضیحات و یادداشت‌ها",
                                style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = item.notes,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Schedule Navigation Button
            Button(
                onClick = { onViewScheduleClick(item) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("view_full_schedule_btn"),
                shape = RoundedCornerShape(RadiusMD),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "مشاهده جدول زمان‌بندی کامل اقساط",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.5.sp),
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun DetailMetric(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.5.sp),
            color = color,
            maxLines = 1
        )
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PaymentHistoryRow(item: PaymentHistoryItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusMD),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = item.status.color.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "قسط ${item.installmentNumber}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = item.status.color,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = item.amountFormatted,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.5.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "سررسید: ${item.dueDate}" + (item.paidDate?.let { " • پرداخت: $it" } ?: ""),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            InstallmentStatusBadge(status = item.status, compact = true)
        }
    }
}
