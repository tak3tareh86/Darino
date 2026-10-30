package com.example.ui.screens.installments.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.screens.installments.model.InstallmentStatus

@Composable
fun InstallmentStatusBadge(
    status: InstallmentStatus,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    singleLine: Boolean = false
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val bgAlpha = if (isDark) 0.22f else 0.12f
    val borderAlpha = if (isDark) 0.35f else 0.25f

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(if (compact) 6.dp else 8.dp),
        color = status.color.copy(alpha = bgAlpha),
        border = BorderStroke(0.8.dp, status.color.copy(alpha = borderAlpha))
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (singleLine) 5.dp else if (compact) 8.dp else 10.dp,
                vertical = if (singleLine) 3.dp else if (compact) 3.dp else 5.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (singleLine) 3.dp else 5.dp)
        ) {
            Icon(
                imageVector = status.icon,
                contentDescription = null,
                tint = status.color,
                modifier = Modifier.size(if (singleLine) 10.dp else if (compact) 12.dp else 15.dp)
            )
            Text(
                text = status.title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = if (singleLine) 9.sp else if (compact) 10.5.sp else 11.5.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = if (singleLine) 1 else Int.MAX_VALUE,
                softWrap = !singleLine,
                color = status.color
            )
        }
    }
}
