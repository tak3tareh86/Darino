package com.example.vehicle.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
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

    var showBrandDialog by remember { mutableStateOf(false) }

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

            // Full-width Brand Selector Field
            Text(
                text = "برند یا سازنده خودرو",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedCard(
                onClick = { showBrandDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("brand_selector_card"),
                shape = RoundedCornerShape(12.dp)
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DirectionsCar,
                            contentDescription = "برند خودرو",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (brand == "سایر" && customBrand.isNotBlank()) "سایر ($customBrand)" else brand,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = "انتخاب برند",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (showBrandDialog) {
                AlertDialog(
                    onDismissRequest = { showBrandDialog = false },
                    title = {
                        Text(
                            text = "انتخاب برند یا سازنده خودرو",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    text = {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(brands) { b ->
                                val isSelected = brand == b
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            brand = b
                                            showBrandDialog = false
                                        },
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = b,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showBrandDialog = false }) {
                            Text("انصراف")
                        }
                    }
                )
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
                    onValueChange = { year = it },
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
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = engine,
                    onValueChange = { engine = it },
                    label = { Text("نوع / کد موتور (در صورت اطلاع)", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp) },
                    placeholder = { Text("مثلاً TU5، EF7") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = transmission,
                    onValueChange = { transmission = it },
                    label = { Text("نوع گیربکس", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp) },
                    placeholder = { Text("دنده‌ای، اتوماتیک") },
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

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = platePart1,
                        onValueChange = { if (it.length <= 2) platePart1 = it },
                        label = { Text("۲ رقم") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    var showLetterMenu by remember { mutableStateOf(false) }
                    val letters = remember { listOf("الف", "ب", "ت", "ج", "د", "ژ", "س", "ص", "ط", "ع", "ق", "گ", "ل", "م", "ن", "و", "ه", "ی") }
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Box(
                            modifier = Modifier.weight(0.8f)
                        ) {
                            OutlinedTextField(
                                value = plateLetter,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("حرف", maxLines = 1, fontSize = 11.sp) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowDropDown,
                                        contentDescription = "انتخاب حرف پلاک",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { showLetterMenu = true }
                                    .testTag("plate_letter_selector")
                            )
                            DropdownMenu(
                                expanded = showLetterMenu,
                                onDismissRequest = { showLetterMenu = false },
                                modifier = Modifier.heightIn(max = 240.dp)
                            ) {
                                letters.forEach { letter ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = letter,
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = TextAlign.Center,
                                                fontWeight = if (plateLetter == letter) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            plateLetter = letter
                                            showLetterMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = platePart2,
                        onValueChange = { if (it.length <= 3) platePart2 = it },
                        label = { Text("۳ رقم") },
                        modifier = Modifier.weight(1.1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = plateIranCode,
                        onValueChange = { if (it.length <= 2) plateIranCode = it },
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

            // Submit Button
            Button(
                onClick = {
                    if (model.isNotBlank()) {
                        val mileage = IranianPhoneUtils.convertDigitsToEnglish(mileageText).filter { it.isDigit() }.toIntOrNull() ?: 0
                        val estVal = IranianAmountUtils.parseAmountToLong(estimatedValueText)
                        val plate = "$platePart1 $plateLetter $platePart2 ایران $plateIranCode"
                        onAddVehicle(if (brand == "سایر") customBrand.trim() else brand, model, year, color, plate, vin, mileage, estVal, engine.trim(), transmission.trim())
                    }
                },
                enabled = model.isNotBlank() && (brand != "سایر" || customBrand.isNotBlank()),
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
