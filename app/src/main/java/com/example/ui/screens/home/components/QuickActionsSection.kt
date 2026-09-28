package com.example.ui.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.home.model.QuickActionItemData
import com.example.ui.theme.RadiusMD

@Composable
fun QuickActionsSection(
    actions: List<QuickActionItemData>,
    modifier: Modifier = Modifier,
    onActionClick: (String) -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "دسترسی سریع",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            actions.forEach { action ->
                QuickActionButton(
                    action = action,
                    modifier = Modifier.weight(1f),
                    onClick = { onActionClick(action.id) }
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(
    action: QuickActionItemData,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Surface(
        modifier = modifier
            .testTag("quick_action_${action.id}")
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(RadiusMD),
                ambientColor = action.accentColor.copy(alpha = 0.2f),
                spotColor = action.accentColor.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(RadiusMD))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(RadiusMD),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Soft 3D Emoji Icon Pill
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = CircleShape,
                        ambientColor = action.accentColor.copy(alpha = 0.25f),
                        spotColor = action.accentColor.copy(alpha = 0.4f)
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                action.accentColor.copy(alpha = if (isDark) 0.3f else 0.18f),
                                action.accentColor.copy(alpha = if (isDark) 0.15f else 0.08f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = action.iconEmoji,
                    fontSize = 20.sp
                )
            }

            Text(
                text = action.title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
