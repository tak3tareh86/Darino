package com.example.ui.screens.installments.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimaryLight

@Composable
fun InstallmentProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    activeColor: Color = EmeraldPrimaryLight,
    label: String = "پرداخت شده",
    showPercentage: Boolean = true
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "InstallmentProgressAnimation"
    )
    val percentageInt = (animatedProgress * 100).toInt()
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (showPercentage) {
                Text(
                    text = "$percentageInt٪",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = activeColor
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                activeColor.copy(alpha = 0.8f),
                                activeColor
                            )
                        )
                    )
            )
        }
    }
}
