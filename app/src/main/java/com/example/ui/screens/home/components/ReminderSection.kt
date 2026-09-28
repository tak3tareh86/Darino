package com.example.ui.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.components.Layered3DCard
import com.example.ui.screens.home.model.ReminderBannerData
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD

@Composable
fun ReminderSection(
    reminder: ReminderBannerData,
    modifier: Modifier = Modifier,
    onViewRemindersClick: () -> Unit = {}
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val bannerBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF1E293B),
                Color(0xFF131B2E),
                Color(0xFF1E1B4B)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFEFF6FF),
                Color(0xFFF0FDF4),
                Color(0xFFF8FAFC)
            )
        )
    }

    Layered3DCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLG),
        backgroundColor = MaterialTheme.colorScheme.surface,
        elevation = 3.dp,
        contentPadding = PaddingValues(16.dp),
        testTag = "home_reminder_card",
        onClick = onViewRemindersClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Right Side: 3D Bell with Glowing Halo + Texts
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Soft 3D Bell Container
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(RadiusMD),
                            ambientColor = Color(0xFF3B82F6).copy(alpha = 0.3f),
                            spotColor = Color(0xFF3B82F6).copy(alpha = 0.45f)
                        )
                        .clip(RoundedCornerShape(RadiusMD))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF3B82F6).copy(alpha = if (isDark) 0.35f else 0.2f),
                                    Color(0xFF60A5FA).copy(alpha = if (isDark) 0.15f else 0.08f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = "زنگوله یادآوری",
                        tint = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = reminder.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action Button
            Button(
                onClick = onViewRemindersClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(RadiusMD),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("view_reminders_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "مشاهده",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
