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
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.RadiusMD

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSubmitVehicle: (
        name: String,
        brand: String,
        modelYear: String,
        odometer: String,
        plate: String,
        colorName: String
    ) -> Unit
) {
    var vehicleName by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("ایران خودرو") }
    var modelYear by remember { mutableStateOf("۱۴۰۲") }
    var odometer by remember { mutableStateOf("۳۵,۰۰۰") }
    var plate by remember { mutableStateOf("ایران ۳۳ - ۵۶۷ د ۱۲") }
    var selectedColor by remember { mutableStateOf("سفید روغنی") }

    val brands = listOf("ایران خودرو", "سایپا", "مدیران خودرو (MVM)", "کرمان موتور", "بهمن موتور", "تویوتا", "هیوندای", "کیا", "سایر")
    val colors = listOf("سفید روغنی", "مشکی متالیک", "خاکستری متالیک", "نقره‌ای", "نوک‌مدادی", "آبی کاربنی", "قرمز")

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
                        imageVector = Icons.Rounded.DirectionsCar,
                        contentDescription = "افزودن خودرو",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "افزودن خودرو جدید",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_add_vehicle_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "بستن",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Vehicle Name Input
            OutlinedTextField(
                value = vehicleName,
                onValueChange = { vehicleName = it },
                label = { Text("نام خودرو (مثال: تارا اتوماتیک، شاهین، پژو ۲۰۷)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_veh_name_input"),
                shape = RoundedCornerShape(RadiusMD),
                singleLine = true
            )

            // Brand Selector
            Text(
                text = "برند خودرو",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(brands) { b ->
                    val isSelected = b == brand
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(RadiusMD))
                            .clickable { brand = b },
                        shape = RoundedCornerShape(RadiusMD),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.3f else 0.15f)
                        else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 0.8.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                    ) {
                        Text(
                            text = b,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Year & Odometer Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = modelYear,
                    onValueChange = { modelYear = com.example.util.IranianPhoneUtils.convertDigitsToEnglish(it).filter { ch -> ch.isDigit() } },
                    label = { Text("سال ساخت (شمسی)") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("new_veh_year_input"),
                    shape = RoundedCornerShape(RadiusMD),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = odometer,
                    onValueChange = { odometer = com.example.util.IranianPhoneUtils.convertDigitsToEnglish(it).filter { ch -> ch.isDigit() } },
                    label = { Text("کیلومتر فعلی") },
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("new_veh_odometer_input"),
                    shape = RoundedCornerShape(RadiusMD),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            // License Plate Input
            OutlinedTextField(
                value = plate,
                onValueChange = { plate = it },
                label = { Text("شماره پلاک (مثال: ایران ۱۱ - ۷۸۹ ج ۲۳)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_veh_plate_input"),
                shape = RoundedCornerShape(RadiusMD),
                singleLine = true
            )

            // Color Selector
            Text(
                text = "رنگ بدنه",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(colors) { col ->
                    val isSelected = col == selectedColor
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(RadiusMD))
                            .clickable { selectedColor = col },
                        shape = RoundedCornerShape(RadiusMD),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.3f else 0.15f)
                        else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 0.8.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                    ) {
                        Text(
                            text = col,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Mock Photo Upload Box
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RadiusMD))
                    .clickable { /* Photo Picker Mock */ },
                shape = RoundedCornerShape(RadiusMD),
                color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddPhotoAlternate,
                        contentDescription = "تصویر خودرو",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "انتخاب تصویر شخصی خودرو (اختیاری)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Submit Button
            Button(
                onClick = {
                    onSubmitVehicle(
                        vehicleName.ifBlank { "خودرو جدید" },
                        brand,
                        modelYear,
                        odometer,
                        plate,
                        selectedColor
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(RadiusMD),
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    .testTag("submit_add_vehicle_btn"),
                shape = RoundedCornerShape(RadiusMD),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
                        text = "افزودن و ذخیره خودرو",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
