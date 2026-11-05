package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RadiusXL

/**
 * Shared section header shell. Dimensions, gradient, shadow and typography
 * intentionally mirror HomeHeader; screen-specific actions remain caller-owned.
 */
@Composable
fun AppSectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable BoxScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val headerGradient = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF131D31),
                Color(0xFF0F172A),
                Color(0xFF0B2926)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.primaryContainer
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(bottomStart = RadiusXL, bottomEnd = RadiusXL),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            )
            .clip(RoundedCornerShape(bottomStart = RadiusXL, bottomEnd = RadiusXL))
            .background(headerGradient)
            .statusBarsPadding()
            .padding(start = 14.dp, end = 14.dp, top = 0.dp, bottom = 2.dp)
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
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(8.dp),
                            ambientColor = Color(0xFF14B8A6).copy(alpha = 0.15f),
                            spotColor = Color(0xFF14B8A6).copy(alpha = 0.3f)
                        )
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                        .padding(1.dp),
                    contentAlignment = Alignment.Center,
                    content = leadingIcon
                )

                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = actions
            )
        }
    }
}
