package com.example.ui.screens.settings.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import com.example.ui.theme.HeaderShape
import com.example.ui.theme.RadiusMD

@Composable
fun SettingsHeader(
    title: String = "تنظیمات",
    subtitle: String = "شخصی‌سازی و مدیریت برنامه",
    showBack: Boolean = false,
    showSearch: Boolean = true,
    isSearchActive: Boolean = false,
    onBackClick: () -> Unit = {},
    onSearchToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val headerGradient = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F172A),
                Color(0xFF131B2E),
                Color(0xFF090D16)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F172A),
                Color(0xFF134E4A),
                Color(0xFF0D9488)
            )
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = HeaderShape,
                ambientColor = Color.Black.copy(alpha = 0.3f),
                spotColor = Color.Black.copy(alpha = 0.4f)
            ),
        shape = HeaderShape,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerGradient)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Leading: Back button OR 3D Illustration
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (showBack) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(RadiusMD))
                                .background(Color.White.copy(alpha = 0.12f))
                                .clickable(onClick = onBackClick)
                                .testTag("settings_header_back_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = "بازگشت",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        // 3D Settings Gear Icon with soft glow
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .shadow(8.dp, RoundedCornerShape(RadiusMD), spotColor = Color(0xFF14B8A6))
                                .clip(RoundedCornerShape(RadiusMD))
                                .background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_3d_settings_gear),
                                contentDescription = "تنظیمات",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(RadiusMD)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp
                            ),
                            color = Color.White
                        )

                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp
                            ),
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                }

                // Trailing: Search Action Button (on main settings)
                if (showSearch) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSearchActive) Color(0xFF14B8A6) else Color.White.copy(alpha = 0.12f)
                            )
                            .clickable(onClick = onSearchToggle)
                            .testTag("settings_search_toggle_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "جستجو در تنظیمات",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
