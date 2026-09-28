package com.example.ui.screens.settings.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brightness4
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.screens.settings.components.SettingsHeader
import com.example.ui.screens.settings.model.AlertDeliveryPreference
import com.example.ui.screens.settings.model.AppCurrency
import com.example.ui.screens.settings.model.AppLanguage
import com.example.ui.screens.settings.model.AppThemeMode
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM

@Composable
fun GeneralPreferencesScreen(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    currentCurrency: AppCurrency,
    onCurrencySelected: (AppCurrency) -> Unit,
    currentTheme: AppThemeMode,
    onThemeChanged: (AppThemeMode) -> Unit,
    currentDeliveryMode: AlertDeliveryPreference = AlertDeliveryPreference.BOTH,
    onDeliveryModeChanged: (AlertDeliveryPreference) -> Unit = {},
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isEn = currentLanguage == AppLanguage.ENGLISH

    CompositionLocalProvider(
        LocalLayoutDirection provides if (isEn) LayoutDirection.Ltr else LayoutDirection.Rtl
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                SettingsHeader(
                    title = if (isEn) "General Settings" else "تنظیمات عمومی",
                    subtitle = if (isEn) "Language, currency, theme, and alert delivery" else "زبان، واحد پول، تم شب و روز، روش ارسال هشدارها",
                    showBack = true,
                    showSearch = false,
                    onBackClick = onBackClick
                )
            }
        ) { paddingValues ->
            // Compact layout fitting comfortably without awkward cutoffs
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // 1. Language Setting (تنظیمات زبان برنامه)
                GeneralCompactCard(
                    title = if (isEn) "App Language" else "زبان برنامه",
                    subtitle = if (isEn) "Display language for texts and numbers" else "زبان نمایش متون و اعداد",
                    icon = Icons.Rounded.Language,
                    iconColor = Color(0xFF0EA5E9)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactOptionChip(
                            title = if (isEn) "Persian" else "فارسی",
                            extra = "🇮🇷",
                            isSelected = currentLanguage == AppLanguage.PERSIAN,
                            onClick = {
                                onLanguageSelected(AppLanguage.PERSIAN)
                                Toast.makeText(context, "زبان برنامه به فارسی تنظیم شد", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CompactOptionChip(
                            title = "English",
                            extra = "🇬🇧",
                            isSelected = currentLanguage == AppLanguage.ENGLISH,
                            onClick = {
                                onLanguageSelected(AppLanguage.ENGLISH)
                                Toast.makeText(context, "Language switched to English", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 2. Currency Setting (تنظیمات واحد پول: ریال و تومان)
                GeneralCompactCard(
                    title = if (isEn) "Currency" else "واحد پول",
                    subtitle = if (isEn) "Transaction calculation unit" else "واحد محاسبه تراکنش‌ها",
                    icon = Icons.Rounded.MonetizationOn,
                    iconColor = EmeraldPrimaryLight
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactOptionChip(
                            title = if (isEn) "Toman" else "تومان",
                            extra = "🪙",
                            isSelected = currentCurrency == AppCurrency.TOMAN,
                            onClick = {
                                onCurrencySelected(AppCurrency.TOMAN)
                                Toast.makeText(
                                    context,
                                    if (isEn) "Currency set to Toman" else "واحد پول به تومان تنظیم شد (مبنای اصلی)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CompactOptionChip(
                            title = if (isEn) "Rial (IRR)" else "ریال (IRR)",
                            extra = "💰",
                            isSelected = currentCurrency == AppCurrency.RIAL,
                            onClick = {
                                onCurrencySelected(AppCurrency.RIAL)
                                Toast.makeText(
                                    context,
                                    if (isEn) "Currency set to Rial (1 Toman = 10 Rials)" else "واحد پول به ریال تنظیم شد (هر ۱ تومان = ۱۰ ریال)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 3. Day & Night Mode (حالت شب و روز)
                GeneralCompactCard(
                    title = if (isEn) "Day & Night Mode" else "حالت شب و روز",
                    subtitle = if (isEn) "Application visual theme" else "پوسته ظاهری برنامه",
                    icon = Icons.Rounded.Brightness4,
                    iconColor = Color(0xFF8B5CF6)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactThemeChip(
                            title = if (isEn) "Light" else "روز",
                            icon = Icons.Rounded.LightMode,
                            isSelected = currentTheme == AppThemeMode.LIGHT,
                            onClick = {
                                onThemeChanged(AppThemeMode.LIGHT)
                                Toast.makeText(
                                    context,
                                    if (isEn) "Light theme activated" else "پوسته روز (روشن) فعال شد",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CompactThemeChip(
                            title = if (isEn) "Dark" else "شب",
                            icon = Icons.Rounded.DarkMode,
                            isSelected = currentTheme == AppThemeMode.DARK,
                            onClick = {
                                onThemeChanged(AppThemeMode.DARK)
                                Toast.makeText(
                                    context,
                                    if (isEn) "Dark theme activated" else "پوسته شب (تاریک) فعال شد",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CompactThemeChip(
                            title = if (isEn) "System" else "سیستم",
                            icon = Icons.Rounded.BrightnessAuto,
                            isSelected = currentTheme == AppThemeMode.SYSTEM,
                            onClick = {
                                onThemeChanged(AppThemeMode.SYSTEM)
                                Toast.makeText(
                                    context,
                                    if (isEn) "Follow system theme activated" else "پوسته هماهنگ با سیستم فعال شد",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 4. Alert Delivery Mode (تنظیمات ارسال پیامک یا نوتیفیکیشن یا هر دو)
                GeneralCompactCard(
                    title = if (isEn) "Alert & Reminder Delivery" else "روش ارسال یادآورها و هشدارها",
                    subtitle = if (isEn) "Choose how to receive installment & service alerts" else "انتخاب نحوه دریافت یادآوری اقساط و سرویس‌ها",
                    icon = Icons.Rounded.NotificationsActive,
                    iconColor = EmeraldPrimaryLight
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DeliveryModeCompactItem(
                            title = if (isEn) "Both (SMS & Notification)" else "هر دو (پیامک و نوتیفیکیشن)",
                            subtitle = if (isEn) "SMS text message with smart notification" else "پیامک متنی همراه با اعلان هوشمند",
                            emoji = "🔔📲",
                            isSelected = currentDeliveryMode == AlertDeliveryPreference.BOTH,
                            onClick = {
                                onDeliveryModeChanged(AlertDeliveryPreference.BOTH)
                                Toast.makeText(
                                    context,
                                    if (isEn) "Alert method: Both (SMS & Notification)" else "روش ارسال به پیامک و نوتیفیکیشن تنظیم شد",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )

                        DeliveryModeCompactItem(
                            title = if (isEn) "Notification Only" else "فقط نوتیفیکیشن",
                            subtitle = if (isEn) "Display alert on screen only" else "تنها نمایش اعلان روی صفحه گوشی",
                            emoji = "🔔",
                            isSelected = currentDeliveryMode == AlertDeliveryPreference.NOTIFICATION_ONLY,
                            onClick = {
                                onDeliveryModeChanged(AlertDeliveryPreference.NOTIFICATION_ONLY)
                                Toast.makeText(
                                    context,
                                    if (isEn) "Alert method: Notification Only" else "روش ارسال به فقط نوتیفیکیشن تنظیم شد",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )

                        DeliveryModeCompactItem(
                            title = if (isEn) "SMS Only" else "فقط پیامک (SMS)",
                            subtitle = if (isEn) "Send text SMS without app notification" else "ارسال پیامک متنی بدون اعلان برنامه",
                            emoji = "💬",
                            isSelected = currentDeliveryMode == AlertDeliveryPreference.SMS_ONLY,
                            onClick = {
                                onDeliveryModeChanged(AlertDeliveryPreference.SMS_ONLY)
                                Toast.makeText(
                                    context,
                                    if (isEn) "Alert method: SMS Only" else "روش ارسال به فقط پیامک (SMS) تنظیم شد",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneralCompactCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLG),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(RadiusSM))
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            content()
        }
    }
}

@Composable
private fun CompactOptionChip(
    title: String,
    extra: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(RadiusMD))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(RadiusMD),
        color = if (isSelected) EmeraldPrimaryLight.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) EmeraldPrimaryLight else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = extra, fontSize = 16.sp)
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) EmeraldPrimaryLight else MaterialTheme.colorScheme.onSurface
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "انتخاب شده",
                    tint = EmeraldPrimaryLight,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactThemeChip(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(RadiusMD))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(RadiusMD),
        color = if (isSelected) EmeraldPrimaryLight.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) EmeraldPrimaryLight else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) EmeraldPrimaryLight else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) EmeraldPrimaryLight else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DeliveryModeCompactItem(
    title: String,
    subtitle: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMD))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(RadiusMD),
        color = if (isSelected) EmeraldPrimaryLight.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) EmeraldPrimaryLight else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = emoji, fontSize = 17.sp)
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) EmeraldPrimaryLight else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = if (isSelected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) EmeraldPrimaryLight else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
