package com.example.ui.screens.settings.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM

@Composable
fun SettingsItem(
    title: String,
    subtitle: String? = null,
    value: String? = null,
    @DrawableRes iconRes: Int? = null,
    iconEmoji: String? = null,
    vectorIcon: ImageVector? = null,
    iconAccentColor: Color? = null,
    badgeText: String? = null,
    badgeColor: Color? = null,
    isDanger: Boolean = false,
    showChevron: Boolean = true,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    testTag: String = "settings_item_${title.hashCode()}"
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val interactionSource = remember { MutableInteractionSource() }

    val primaryTextColor = if (isDanger) {
        ExpenseRoseLight
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val subtitleTextColor = if (isDanger) {
        ExpenseRoseLight.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMD))
            .clickable(
                enabled = onClick != null || onCheckedChange != null,
                interactionSource = interactionSource,
                indication = null
            ) {
                if (checked != null && onCheckedChange != null) {
                    onCheckedChange(!checked)
                } else {
                    onClick?.invoke()
                }
            }
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 3D / Styled Icon container
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .shadow(
                        elevation = if (isDark) 4.dp else 2.dp,
                        shape = RoundedCornerShape(RadiusSM),
                        ambientColor = if (isDanger) ExpenseRoseLight.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.1f),
                        spotColor = if (isDanger) ExpenseRoseLight.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.15f)
                    )
                    .clip(RoundedCornerShape(RadiusSM))
                    .background(
                        if (isDanger) {
                            ExpenseRoseLight.copy(alpha = 0.12f)
                        } else if (iconAccentColor != null) {
                            iconAccentColor.copy(alpha = if (isDark) 0.18f else 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.6f else 0.7f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    iconRes != null -> {
                        Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = title,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(RadiusSM)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    vectorIcon != null -> {
                        Icon(
                            imageVector = vectorIcon,
                            contentDescription = title,
                            tint = if (isDanger) ExpenseRoseLight else (iconAccentColor ?: MaterialTheme.colorScheme.primary),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    iconEmoji != null -> {
                        Text(
                            text = iconEmoji,
                            fontSize = 20.sp
                        )
                    }
                }
            }

            // Text Content
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp
                        ),
                        color = primaryTextColor
                    )

                    if (badgeText != null) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    badgeColor ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = badgeColor ?: MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        color = subtitleTextColor,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // End Action / Value / Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!value.isNullOrBlank()) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (checked != null && onCheckedChange != null) {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.scale(0.85f)
                )
            } else if (showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
