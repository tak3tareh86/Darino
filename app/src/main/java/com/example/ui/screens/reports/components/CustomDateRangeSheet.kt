package com.example.ui.screens.reports.components

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDateRangeSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onApplyDateRange: (startDate: String, endDate: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var startDate by remember { mutableStateOf("۱۴۰۴/۰۷/۰۱") }
    var endDate by remember { mutableStateOf("۱۴۰۴/۰۷/۳۰") }
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = RadiusLG, topEnd = RadiusLG),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = EmeraldPrimaryLight,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "انتخاب بازه زمانی سفارشی",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "بستن",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Preset Chips
            Text(
                text = "پیش‌فرض‌های سریع:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickPresetChip(
                    title = "۳۰ روز اخیر",
                    onClick = {
                        startDate = "۱۴۰۴/۰۶/۳۰"
                        endDate = "۱۴۰۴/۰۷/۳۰"
                    }
                )
                QuickPresetChip(
                    title = "ماه قبل",
                    onClick = {
                        startDate = "۱۴۰۴/۰۶/۰۱"
                        endDate = "۱۴۰۴/۰۶/۳۱"
                    }
                )
                QuickPresetChip(
                    title = "۳ ماه گذشته",
                    onClick = {
                        startDate = "۱۴۰۴/۰۴/۰۱"
                        endDate = "۱۴۰۴/۰۷/۳۰"
                    }
                )
            }

            // Start Date Input
            OutlinedTextField(
                value = startDate,
                onValueChange = { startDate = it },
                label = { Text("تاریخ شروع (روز / ماه / سال)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_date_start_input"),
                shape = RoundedCornerShape(RadiusMD),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimaryLight,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                singleLine = true
            )

            // End Date Input
            OutlinedTextField(
                value = endDate,
                onValueChange = { endDate = it },
                label = { Text("تاریخ پایان (روز / ماه / سال)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_date_end_input"),
                shape = RoundedCornerShape(RadiusMD),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimaryLight,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Apply Button
            Button(
                onClick = {
                    onApplyDateRange(startDate, endDate)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(RadiusMD),
                        ambientColor = EmeraldPrimaryLight.copy(alpha = 0.3f),
                        spotColor = EmeraldPrimaryLight.copy(alpha = 0.5f)
                    )
                    .testTag("custom_date_apply_btn"),
                shape = RoundedCornerShape(RadiusMD),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimaryLight)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Text(
                        text = "اعمال بازه زمانی",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun QuickPresetChip(
    title: String,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(RadiusMD))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(RadiusMD),
        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        )
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
