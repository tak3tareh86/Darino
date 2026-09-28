package com.example.ui.screens.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.settings.model.AppCalendar
import com.example.ui.screens.settings.model.AppCurrency
import com.example.ui.screens.settings.model.AppLanguage
import com.example.ui.screens.settings.model.WeekStartDay
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionSheet(
    isOpen: Boolean,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = RadiusLG, topEnd = RadiusLG)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "انتخاب زبان برنامه",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "زبان مورد نظر خود را جهت نمایش متون و ارقام در برنامه انتخاب نمایید:",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppLanguage.values().forEach { lang ->
                    val isSelected = lang == currentLanguage
                    SelectableOptionCard(
                        title = lang.title,
                        subtitle = lang.nativeName,
                        isSelected = isSelected,
                        onClick = {
                            onLanguageSelected(lang)
                            onDismiss()
                        },
                        iconEmoji = if (lang == AppLanguage.PERSIAN) "🇮🇷" else "🌐"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySelectionSheet(
    isOpen: Boolean,
    currentCurrency: AppCurrency,
    onCurrencySelected: (AppCurrency) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = RadiusLG, topEnd = RadiusLG)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "واحد پول پیش‌فرض",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "تمامی مبالغ، اقساط، مخارج خودرو و گزارش‌ها با این واحد نمایش داده می‌شوند:",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppCurrency.values().forEach { curr ->
                    val isSelected = curr == currentCurrency
                    SelectableOptionCard(
                        title = curr.title,
                        subtitle = "نماد: ${curr.symbol} (${curr.code})",
                        isSelected = isSelected,
                        onClick = {
                            onCurrencySelected(curr)
                            onDismiss()
                        },
                        iconEmoji = when (curr) {
                            AppCurrency.TOMAN -> "🪙"
                            AppCurrency.RIAL -> "💰"
                            AppCurrency.USD -> "💵"
                            AppCurrency.EUR -> "💶"
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarSelectionSheet(
    isOpen: Boolean,
    currentCalendar: AppCalendar,
    onCalendarSelected: (AppCalendar) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = RadiusLG, topEnd = RadiusLG)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "تقویم و تاریخ",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "سیستم تقویم مورد نظر جهت ثبت و نمایش تاریخ‌های سررسید و گزارشات:",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppCalendar.values().forEach { cal ->
                    val isSelected = cal == currentCalendar
                    SelectableOptionCard(
                        title = cal.title,
                        subtitle = cal.description,
                        isSelected = isSelected,
                        onClick = {
                            onCalendarSelected(cal)
                            onDismiss()
                        },
                        iconEmoji = if (cal == AppCalendar.SHAMSI) "☀️" else "🌍"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekStartSelectionSheet(
    isOpen: Boolean,
    currentDay: WeekStartDay,
    onDaySelected: (WeekStartDay) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = RadiusLG, topEnd = RadiusLG)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "شروع هفته",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WeekStartDay.values().forEach { day ->
                    val isSelected = day == currentDay
                    SelectableOptionCard(
                        title = day.title,
                        subtitle = null,
                        isSelected = isSelected,
                        onClick = {
                            onDaySelected(day)
                            onDismiss()
                        },
                        iconEmoji = "🗓️"
                    )
                }
            }
        }
    }
}

@Composable
fun SelectableOptionCard(
    title: String,
    subtitle: String?,
    isSelected: Boolean,
    iconEmoji: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMD))
            .clickable(onClick = onClick)
            .testTag("option_card_$title"),
        shape = RoundedCornerShape(RadiusMD),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.18f else 0.10f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.4f else 0.5f)
        },
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = iconEmoji, fontSize = 22.sp)

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.5.sp
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )

                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "انتخاب شده",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
