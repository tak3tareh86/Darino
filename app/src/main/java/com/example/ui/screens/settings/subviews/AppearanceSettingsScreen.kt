package com.example.ui.screens.settings.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.settings.components.SettingsHeader
import com.example.ui.screens.settings.components.SettingsSectionCard
import com.example.ui.screens.settings.model.AccentColorOption
import com.example.ui.screens.settings.model.AppThemeMode
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM

@Composable
fun AppearanceSettingsScreen(
    currentTheme: AppThemeMode,
    onThemeChanged: (AppThemeMode) -> Unit,
    currentAccent: AccentColorOption,
    onAccentChanged: (AccentColorOption) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SettingsHeader(
                title = "حالت تم و شخصی‌سازی",
                subtitle = "تنظیم حالت تاریک، روشن و رنگ پوسته برنامه",
                showBack = true,
                showSearch = false,
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Mode with 3 Visual Cards
            item {
                SettingsSectionCard(
                    title = "حالت تم (رنگ قالب)",
                    badge = currentTheme.title
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "نحوه نمایش رنگ‌های پس‌زمینه و کارت‌های برنامه را مشخص کنید:",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ThemeVisualCard(
                                title = "روشن",
                                icon = Icons.Rounded.LightMode,
                                isDarkDemo = false,
                                isSelected = currentTheme == AppThemeMode.LIGHT,
                                onClick = { onThemeChanged(AppThemeMode.LIGHT) },
                                modifier = Modifier.weight(1f)
                            )

                            ThemeVisualCard(
                                title = "تاریک",
                                icon = Icons.Rounded.DarkMode,
                                isDarkDemo = true,
                                isSelected = currentTheme == AppThemeMode.DARK,
                                onClick = { onThemeChanged(AppThemeMode.DARK) },
                                modifier = Modifier.weight(1f)
                            )

                            ThemeVisualCard(
                                title = "سیستم",
                                icon = Icons.Rounded.SettingsBrightness,
                                isDarkDemo = null,
                                isSelected = currentTheme == AppThemeMode.SYSTEM,
                                onClick = { onThemeChanged(AppThemeMode.SYSTEM) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Accent Colors
            item {
                SettingsSectionCard(
                    title = "رنگ شاخص و برجسته‌سازی (Accent Color)",
                    badge = currentAccent.title
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "انتخاب رنگ برای دکمه‌ها، نمودارها و آیکون‌های اصلی برنامه:",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AccentColorOption.values().forEach { accent ->
                                val isSelected = accent == currentAccent
                                AccentColorRow(
                                    option = accent,
                                    isSelected = isSelected,
                                    onClick = { onAccentChanged(accent) }
                                )
                            }
                        }
                    }
                }
            }

            // Typography & Visual Tips
            item {
                SettingsSectionCard(title = "تنظیمات فونت و خوانایی") {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "فونت پیش‌فرض: قلم استاندارد و زیبای وزیرمتن (Vazirmatn)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "تمامی ارقام و اعداد به صورت فارسی و بهینه برای سیستم‌های مالی نمایش داده می‌شوند. اندازه متون به صورت خودکار با تنظیمات بزرگنمایی سیستم شما هماهنگ خواهد بود.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ThemeVisualCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isDarkDemo: Boolean?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(RadiusMD))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(RadiusMD),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else (if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.06f))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Miniature Screen Preview Box
            val boxModifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(RadiusSM))
            val themedBoxModifier = when (isDarkDemo) {
                true -> boxModifier.background(Color(0xFF090D16))
                false -> boxModifier.background(Color(0xFFF1F5F9))
                null -> boxModifier.background(
                    Brush.linearGradient(listOf(Color(0xFFF1F5F9), Color(0xFF090D16)))
                )
            }

            Box(
                modifier = themedBoxModifier.padding(6.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Miniature Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimaryLight)
                        )
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (isDarkDemo == true) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.3f)
                                )
                        )
                    }

                    // Miniature Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isDarkDemo == true) Color(0xFF1E293B) else Color.White
                            )
                            .padding(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(EmeraldPrimaryLight.copy(alpha = 0.4f))
                            )
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (isDarkDemo == true) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.4f)
                                    )
                            )
                        }
                    }
                }
            }

            // Title and Selection indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )

                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun AccentColorRow(
    option: AccentColorOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMD))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(RadiusMD),
        color = if (isSelected) {
            option.primaryColor.copy(alpha = if (isDark) 0.16f else 0.10f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.3f else 0.4f)
        },
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) option.primaryColor else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(option.primaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = option.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.5.sp
                    ),
                    color = if (isSelected) option.primaryColor else MaterialTheme.colorScheme.onSurface
                )
            }

            // Color gradient preview bar
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(RadiusSM))
                    .background(
                        Brush.horizontalGradient(
                            listOf(option.primaryColor, option.containerColor)
                        )
                    )
            )
        }
    }
}
