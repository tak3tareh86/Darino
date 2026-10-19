package com.example.vehicle.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
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
        estimatedValue: Long,
        engine: String,
        transmission: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val currentYear = com.example.util.PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).year.toString()
    var brand by remember { mutableStateOf("ایران‌خودرو") }
    var customBrand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var engine by remember { mutableStateOf("") }
    var transmission by remember { mutableStateOf("") }
    var year by remember { mutableStateOf(currentYear) }
    var color by remember { mutableStateOf("سفید") }
    var platePart1 by remember { mutableStateOf("") }
    var plateLetter by remember { mutableStateOf("الف") }
    var platePart2 by remember { mutableStateOf("") }
    var plateIranCode by remember { mutableStateOf("") }
    var vin by remember { mutableStateOf("") }
    var mileageText by remember { mutableStateOf("") }
    var estimatedValueText by remember { mutableStateOf("") }

    val brands = listOf(
        "ایران‌خودرو", "سایپا", "مدیران‌خودرو", "کرمان‌موتور", "بهمن‌موتور",
        "چری", "جک", "ام‌وی‌ام", "فونیکس", "فیدلیتی", "دیگنیتی", "لاماری", "BYD",
        "هیوندای", "کیا", "تویوتا", "نیسان", "مزدا", "هوندا", "رنو", "سوزوکی",
        "میتسوبیشی", "لکسوس", "بنز", "بی‌ام‌و", "فولکس‌واگن", "سایر"
    )
    val normalizedYear = IranianPhoneUtils.convertDigitsToEnglish(year).filter { it.isDigit() }.toIntOrNull()
    val parsedMileage = IranianPhoneUtils.convertDigitsToEnglish(mileageText).filter { it.isDigit() }.toIntOrNull()
    val validPlate = platePart1.length == 2 && plateLetter.isNotBlank() &&
        platePart2.length == 3 && plateIranCode.length == 2 &&
        listOf(platePart1, platePart2, plateIranCode).all { part -> part.all(Char::isDigit) }
    val validYear = normalizedYear != null &&
        normalizedYear in 1300..(com.example.util.PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).year + 1)

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
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                brands.forEach { b ->
                    FilterChip(
                        selected = brand == b,
                        onClick = { brand = b },
                        label = { Text(b, fontSize = 11.sp) }
                    )
                }
            }

            if (brand == "سایر") {
                OutlinedTextField(
                    value = customBrand,
                    onValueChange = { customBrand = it },
                    label = { Text("نام سازنده / برند") },
                    placeholder = { Text("نام دقیق برند خودرو") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
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
                    onValueChange = { year = IranianPhoneUtils.convertDigitsToEnglish(it).filter { ch -> ch.isDigit() }.take(4) },
                    label = { Text("سال ساخت") },
                    placeholder = { Text("1402") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            // Engine and transmission are needed to distinguish service schedules for variants.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = engine,
                    onValueChange = { engine = it },
                    label = { Text("نوع / کد موتور (در صورت اطلاع)") },
                    placeholder = { Text("مثلاً TU5، EF7، 1.5T") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = transmission,
                    onValueChange = { transmission = it },
                    label = { Text("نوع گیربکس") },
                    placeholder = { Text("دنده‌ای، اتوماتیک، CVT") },
                    modifier = Modifier.weight(1f),
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
                    onValueChange = { mileageText = IranianPhoneUtils.convertDigitsToEnglish(it).filter { ch -> ch.isDigit() } },
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

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = platePart1,
                        onValueChange = { value -> platePart1 = IranianPhoneUtils.convertDigitsToEnglish(value).filter { it.isDigit() }.take(2) },
                        label = { Text("۲ رقم") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = plateLetter,
                        onValueChange = { value -> plateLetter = value.filter { it.isLetter() }.take(1) },
                        label = { Text("حرف") },
                        modifier = Modifier.weight(0.8f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = platePart2,
                        onValueChange = { value -> platePart2 = IranianPhoneUtils.convertDigitsToEnglish(value).filter { it.isDigit() }.take(3) },
                        label = { Text("۳ رقم") },
                        modifier = Modifier.weight(1.1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = plateIranCode,
                        onValueChange = { value -> plateIranCode = IranianPhoneUtils.convertDigitsToEnglish(value).filter { it.isDigit() }.take(2) },
                        label = { Text("ایران") },
                        modifier = Modifier.weight(0.9f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
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

            // Required fields are validated before invoking the repository.
            Text(
                text = "مدل، سال ساخت معتبر و تمام بخش‌های پلاک الزامی هستند.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = {
                    val mileage = parsedMileage ?: 0
                    val estVal = IranianAmountUtils.parseAmountToLong(estimatedValueText)
                    val plate = "$platePart1 $plateLetter $platePart2 ایران $plateIranCode"
                    if (model.isNotBlank() && validYear && validPlate && mileage >= 0 && estVal >= 0L &&
                        (brand != "سایر" || customBrand.isNotBlank())) {
                        onAddVehicle(if (brand == "سایر") customBrand.trim() else brand, model.trim(), normalizedYear.toString(), color, plate, vin, mileage, estVal, engine.trim(), transmission.trim())
                    }
                },
                enabled = model.isNotBlank() && validYear && validPlate &&
                    (parsedMileage == null || parsedMileage >= 0) &&
                    IranianAmountUtils.parseAmountToLong(estimatedValueText) >= 0L &&
                    (brand != "سایر" || customBrand.isNotBlank()),
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
