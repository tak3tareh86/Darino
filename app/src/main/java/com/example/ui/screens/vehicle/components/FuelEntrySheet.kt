package com.example.ui.screens.vehicle.components

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Speed
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PersianAmountInputField
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.util.IranianAmountUtils
import com.example.util.IranianPhoneUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelEntrySheet(
    sheetState: SheetState,
    currentOdometer: Long,
    vehicleName: String,
    onDismiss: () -> Unit,
    onSubmitFuelEntry: (
        amount: String,
        liters: String,
        fuelType: String,
        odometer: String,
        stationNote: String
    ) -> Unit
) {
    var amountInput by remember { mutableStateOf("۱۵۰,۰۰۰") }
    var litersInput by remember { mutableStateOf("۵۰") }
    var odometerInput by remember { mutableStateOf(currentOdometer.toString()) }
    var selectedFuelType by remember { mutableStateOf("بنزین سهمیه‌ای (۱,۵۰۰ ت)") }
    var stationInput by remember { mutableStateOf("") }

    val fuelTypes = listOf(
        "بنزین سهمیه‌ای (۱,۵۰۰ ت)",
        "بنزین آزاد (۳,۰۰۰ ت)",
        "بنزین سوپر",
        "گاز طبیعی CNG"
    )

    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 44.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
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
                        imageVector = Icons.Rounded.LocalGasStation,
                        contentDescription = "سوخت",
                        tint = EmeraldPrimaryLight,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "ثبت سوخت‌گیری ($vehicleName)",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_fuel_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "بستن",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Amount Input Field with Live Toman Formatter & Persian Words
            PersianAmountInputField(
                value = amountInput,
                onValueChange = { amountInput = it },
                label = "مبلغ پرداختی سوخت (تومان)",
                placeholder = "۱۵۰,۰۰۰",
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fuel_amount_input")
            )

            // Fuel Type Selector
            Text(
                text = "نوع سوخت",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(fuelTypes) { type ->
                    val isSelected = type == selectedFuelType
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(RadiusMD))
                            .clickable { selectedFuelType = type }
                            .testTag("fuel_type_$type"),
                        shape = RoundedCornerShape(RadiusMD),
                        color = if (isSelected) EmeraldPrimaryLight.copy(alpha = if (isDark) 0.3f else 0.15f)
                        else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 0.8.dp,
                            color = if (isSelected) EmeraldPrimaryLight else Color.Transparent
                        )
                    ) {
                        Text(
                            text = type,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) EmeraldPrimaryLight else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Liters & Odometer Inputs Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = litersInput,
                    onValueChange = { litersInput = IranianPhoneUtils.convertDigitsToEnglish(it).filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("حجم (لیتر)") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("fuel_liters_input"),
                    shape = RoundedCornerShape(RadiusMD),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = odometerInput,
                    onValueChange = { odometerInput = IranianPhoneUtils.convertDigitsToEnglish(it).filter { ch -> ch.isDigit() } },
                    label = { Text("کیلومتر خودرو") },
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("fuel_odometer_input"),
                    shape = RoundedCornerShape(RadiusMD),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            // Station note
            OutlinedTextField(
                value = stationInput,
                onValueChange = { stationInput = it },
                label = { Text("جایگاه سوخت یا یادداشت (اختیاری)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fuel_station_input"),
                shape = RoundedCornerShape(RadiusMD),
                singleLine = true
            )

            // Average fuel consumption preview info pill
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(RadiusMD),
                color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "میانگین مصرف سوخت محاسبه‌شده:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "۷.۴ لیتر در ۱۰۰ کیلومتر",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldPrimaryLight
                    )
                }
            }

            // Submit Button
            Button(
                onClick = {
                    onSubmitFuelEntry(
                        amountInput,
                        litersInput,
                        selectedFuelType,
                        odometerInput,
                        stationInput
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(RadiusMD),
                        ambientColor = EmeraldPrimaryLight.copy(alpha = 0.3f),
                        spotColor = EmeraldPrimaryLight.copy(alpha = 0.5f)
                    )
                    .testTag("submit_fuel_entry_btn"),
                shape = RoundedCornerShape(RadiusMD),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimaryLight)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "ثبت",
                        tint = Color.White
                    )
                    Text(
                        text = "ثبت سوخت‌گیری",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
