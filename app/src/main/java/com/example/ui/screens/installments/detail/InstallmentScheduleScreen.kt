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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Layered3DCard
import com.example.ui.screens.installments.components.InstallmentStatusBadge
import com.example.ui.screens.installments.model.InstallmentItem
import com.example.ui.screens.installments.model.PaymentHistoryItem
import com.example.ui.theme.RadiusMD

@Composable
fun InstallmentScheduleScreen(
    installment: InstallmentItem,
    onBackClick: () -> Unit,
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
                            modifier = Modifier.testTag("schedule_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "بازگشت",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "برنامه پرداخت اقساط",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = installment.title,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "جدول اقساط و موعد سررسیدها (${installment.totalInstallments} قسط)",
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.5.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(installment.paymentHistory, key = { it.id }) { scheduleItem ->
                ScheduleTimelineItem(item = scheduleItem)
            }

            item {
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun ScheduleTimelineItem(item: PaymentHistoryItem) {
    Layered3DCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusMD),
        backgroundColor = MaterialTheme.colorScheme.surface,
        elevation = 1.5.dp,
        contentPadding = PaddingValues(12.dp)
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
                // Circle timeline indicator
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(item.status.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${item.installmentNumber}",
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                        color = item.status.color
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "قسط شماره ${item.installmentNumber}",
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "سررسید: ${item.dueDate}" + (item.paidDate?.let { " • پرداخت: $it" } ?: ""),
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
                    text = item.amountFormatted,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                InstallmentStatusBadge(status = item.status, compact = true)
            }
        }
    }
}
