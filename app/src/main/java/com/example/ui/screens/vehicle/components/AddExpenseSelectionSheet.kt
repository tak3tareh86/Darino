package com.example.ui.screens.vehicle.components

import androidx.annotation.DrawableRes
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.HomeRepairService
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.WarningAmberLight

enum class ExpenseOptionType(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color
) {
    SERVICE("ثبت سرویس", "تعویض روغن، فیلترها و سرویس‌های دوره‌ای", Icons.Rounded.Build, WarningAmberLight),
    REPAIR("ثبت تعمیرات", "تعمیر قطعات، جلوبندی، برق و موارد فنی", Icons.Rounded.HomeRepairService, Color(0xFFEF4444)),
    FUEL("ثبت سوخت", "بنزین، گاز و مکمل‌های سوخت", Icons.Rounded.LocalGasStation, EmeraldPrimaryLight),
    INSURANCE("ثبت بیمه", "تمدید بیمه شخص ثالث و بدنه", Icons.Rounded.Description, InfoIndigoLight),
    OTHER("سایر هزینه‌ها", "کارواش، عوارض، جریمه و لوازم جانبی", Icons.Rounded.MoreHoriz, Color(0xFF8B5CF6))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseSelectionSheet(
    sheetState: SheetState,
    vehicleName: String,
    onDismiss: () -> Unit,
    onOptionSelect: (ExpenseOptionType) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "ثبت هزینه جدید",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "نوع هزینه مورد نظر برای «$vehicleName» را انتخاب کنید",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Options List
            ExpenseOptionType.values().forEach { option ->
                ExpenseOptionRow(
                    option = option,
                    onClick = {
                        onOptionSelect(option)
                    }
                )
            }
        }
    }
}

@Composable
fun ExpenseOptionRow(
    option: ExpenseOptionType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMD))
            .clickable(onClick = onClick)
            .testTag("expense_option_${option.name}"),
        shape = RoundedCornerShape(RadiusMD),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, option.accentColor.copy(alpha = 0.25f))
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
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(option.accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = option.title,
                        tint = option.accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = option.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = option.description,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
