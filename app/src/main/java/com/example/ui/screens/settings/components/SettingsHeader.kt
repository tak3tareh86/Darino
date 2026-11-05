package com.example.ui.screens.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.AppSectionHeader

@Composable
fun SettingsHeader(
    title: String = "تنظیمات",
    subtitle: String = "شخصی‌سازی و مدیریت برنامه",
    showBack: Boolean = false,
    showSearch: Boolean = true,
    isSearchActive: Boolean = false,
    onBackClick: () -> Unit = {},
    onSearchToggle: () -> Unit = {},
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val actionBackground = if (isDark) {
        Color.White.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    }

    AppSectionHeader(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        leadingIcon = {
            if (showBack) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(actionBackground)
                        .clickable(onClick = onBackClick)
                        .testTag("settings_header_back_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "بازگشت",
                        tint = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.img_3d_settings_gear),
                    contentDescription = "تنظیمات",
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
        },
        actions = {
            if (showSearch) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSearchActive) MaterialTheme.colorScheme.primary
                            else actionBackground
                        )
                        .clickable(onClick = onSearchToggle)
                        .testTag("settings_search_toggle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "جستجو در تنظیمات",
                        tint = if (isSearchActive) MaterialTheme.colorScheme.onPrimary
                        else if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    )
}
