package com.example.vehicle.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.PersianAmountInputField
import com.example.ui.components.Soft3DIcon
import com.example.util.IranianAmountUtils
import com.example.util.IranianPhoneUtils

/**
 * Bottom sheet to add a new vehicle profile
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleSheet(
    onAddVehicle: (
        brand: String,
        model: String,
        year: String,
        color: String,
        plate: String,
        vin: String,
        currentMileage: Int,
        estimatedValue: Long
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var brand by remember { mutableStateOf("ایران‌خودرو") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("1403") }
    var color by remember { mutableStateOf("سفید") }
    var platePart1 by remember { mutableStateOf("12") }
    var plateLetter by remember { mutableStateOf("ب") }
    var platePart2 by remember { mutableStateOf("345") }
    var plateIranCode by remember { mutableStateOf("68") }
    var vin by remember { mutableStateOf("") }
    var mileageText by remember { mutableStateOf("0") }
    var estimatedValueText by remember { mutableStateOf("") }

    val brands = listOf("ایران‌خودرو", "سایپا", "مدیران‌خودرو", "کرمان‌موتور", "بهمن‌موتور", "هیوندای", "کیا", "تویوتا", "سایر")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("add_vehicle_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Soft3DIcon(
                        imageRes = R.drawable.img_3d_car,
                        contentDescription = "ثبت خودرو جدید",
                        size = 36.dp,
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "ثبت خودرو جدید در پرونده",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ایجاد پرونده هوشمند و یادآورهای دوره‌ای",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "بستن",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            // Brand Selection Chips
            Text(
                text = "برند یا سازنده خودرو",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                brands.take(4).forEach { b ->
                    FilterChip(
                        selected = brand == b,
                        onClick = { brand = b },
                        label = { Text(b, fontSize = 11.sp) }
                    )
                }
            }

            // Model & Year
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("مدل / تیپ خودرو") },
                    placeholder = { Text("مثلاً: ۲۰۷i، تارا، دنا") },
                    modifier = Modifier.weight(1.5f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = { Text("سال ساخت") },
                    placeholder = { Text("1402") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            // Color & Mileage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text("رنگ بدنه") },
                    placeholder = { Text("سفید") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = mileageText,
                    onValueChange = { mileageText = it },
                    label = { Text("کیلومتر فعلی") },
                    placeholder = { Text("45000") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            // License Plate Inputs (Persian standard plate 4 fields)
            Text(
                text = "شماره پلاک خودرو",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = plateIranCode,
                    onValueChange = { plateIranCode = it },
                    label = { Text("ایران") },
                    modifier = Modifier.weight(0.9f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = platePart2,
                    onValueChange = { platePart2 = it },
                    label = { Text("۳ رقم") },
                    modifier = Modifier.weight(1.1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = plateLetter,
                    onValueChange = { plateLetter = it },
                    label = { Text("حرف") },
                    modifier = Modifier.weight(0.8f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = platePart1,
                    onValueChange = { platePart1 = it },
                    label = { Text("۲ رقم") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            // VIN (Optional) & Estimated Value
            OutlinedTextField(
                value = vin,
                onValueChange = { vin = it },
                label = { Text("شماره شاسی / VIN (اختیاری)") },
                placeholder = { Text("IRAN-PEUG-207-...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            PersianAmountInputField(
                value = estimatedValueText,
                onValueChange = { estimatedValueText = it },
                label = "ارزش تقریبی خودرو (اختیاری)",
                unitLabel = "تومان",
                placeholder = "مثال: ۸۵۰,۰۰۰,۰۰۰",
                showWordsPreview = true,
                testTag = "vehicle_estimated_value_input"
            )

            // Submit Button
            Button(
                onClick = {
                    if (model.isNotBlank()) {
                        val mileage = IranianPhoneUtils.convertDigitsToEnglish(mileageText).filter { it.isDigit() }.toIntOrNull() ?: 0
                        val estVal = IranianAmountUtils.parseAmountToLong(estimatedValueText)
                        val plate = "ایران $plateIranCode - $platePart2 $plateLetter $platePart1"
                        onAddVehicle(brand, model, year, color, plate, vin, mileage, estVal)
                    }
                },
                enabled = model.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_add_vehicle_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ثبت و ایجاد پرونده خودرو",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
