package com.example.ui.screens.installments.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.installments.model.InstallmentItem
import com.example.ui.theme.RadiusMD

@Composable
fun ExpandableInstallmentCard(
    item: InstallmentItem,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        InstallmentCard(
            item = item,
            onClick = onToggle
        )
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                shape = RoundedCornerShape(RadiusMD),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "تاریخچه پرداخت‌ها",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    item.paymentHistory.forEach { history ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "قسط ${history.installmentNumber}: ${history.dueDate}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = history.amountFormatted,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (history.status == com.example.ui.screens.installments.model.InstallmentStatus.PAID) 
                                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
