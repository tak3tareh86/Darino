package com.example.ui.screens.settings.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusXL

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

    val headerTextColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(bottomStart = RadiusXL, bottomEnd = RadiusXL),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ),
        shape = RoundedCornerShape(bottomStart = RadiusXL, bottomEnd = RadiusXL),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerGradient)
                .padding(start = 14.dp, end = 14.dp, top = 0.dp, bottom = 2.dp)
        ) {
            // The title/subtitle follow FinancialHeader's centered composition.
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.72f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = headerTextColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Keep the existing back/search actions and their callbacks unchanged.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (showBack) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isDark) Color.White.copy(alpha = 0.15f)
                                else Color(0xFF0D9488).copy(alpha = 0.10f)
                            )
                            .clickable(onClick = onBackClick)
                            .testTag("settings_header_back_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "بازگشت",
                            tint = headerTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .shadow(
                                elevation = 2.dp,
                                shape = RoundedCornerShape(8.dp),
                                ambientColor = Color.Black.copy(alpha = 0.15f),
                                spotColor = Color(0xFF14B8A6).copy(alpha = 0.2f)
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isDark) Color.White.copy(alpha = 0.15f)
                                else Color(0xFF0D9488).copy(alpha = 0.10f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_3d_settings_gear),
                            contentDescription = "تنظیمات",
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(RadiusMD)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                if (showSearch) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSearchActive) MaterialTheme.colorScheme.primary
                                else if (isDark) Color.White.copy(alpha = 0.15f)
                                else Color(0xFF0D9488).copy(alpha = 0.10f)
                            )
                            .clickable(onClick = onSearchToggle)
                            .testTag("settings_search_toggle_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "جستجو در تنظیمات",
                            tint = if (isSearchActive) MaterialTheme.colorScheme.onPrimary
                            else headerTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Box(modifier = Modifier.size(34.dp))
                }
            }
        }
    }
}
