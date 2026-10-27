package com.example.ui.screens.check.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.check.model.Check
import com.example.ui.screens.check.model.CheckStatus
import com.example.ui.screens.check.model.CheckType
import com.example.util.MoneyFormatter

@Composable
fun CheckStats(
    checks: List<Check>,
    modifier: Modifier = Modifier
) {
    val receivedChecks = checks.filter { it.type == CheckType.RECEIVED && (it.status == CheckStatus.REGISTERED || it.status == CheckStatus.PENDING || it.status == CheckStatus.BOUNCED) }
    val paidChecks = checks.filter { it.type == CheckType.PAID && (it.status == CheckStatus.REGISTERED || it.status == CheckStatus.PENDING || it.status == CheckStatus.BOUNCED) }
    
    val totalReceivedAmount = receivedChecks.sumOf { it.amount }
    val totalPaidAmount = paidChecks.sumOf { it.amount }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatItem(
                    title = "دریافتی (باز)",
                    count = receivedChecks.size,
                    amount = totalReceivedAmount,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                
                StatItem(
                    title = "پرداختی (باز)",
                    count = paidChecks.size,
                    amount = totalPaidAmount,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }

            val overdueChecks = checks.filter { it.isOverdue }
            val nearDueChecks = checks.filter { it.daysToDueDate in 0..3 && (it.status == CheckStatus.REGISTERED || it.status == CheckStatus.PENDING || it.status == CheckStatus.BOUNCED) && !it.isOverdue }

            if (overdueChecks.isNotEmpty() || nearDueChecks.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (overdueChecks.isNotEmpty()) {
                        SmallStatItem(
                            title = "سررسید گذشته",
                            count = overdueChecks.size,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (nearDueChecks.isNotEmpty()) {
                        SmallStatItem(
                            title = "نزدیک به سررسید",
                            count = nearDueChecks.size,
                            color = androidx.compose.ui.graphics.Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallStatItem(
    title: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
            Text("$count", style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun StatItem(
    title: String,
    count: Int,
    amount: Long,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = MoneyFormatter.format(amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Text(
                text = "$count عدد",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
