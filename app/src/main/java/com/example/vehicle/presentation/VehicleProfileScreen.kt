package com.example.vehicle.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.Layered3DCard
import com.example.ui.components.PersianAmountInputField
import com.example.ui.components.Soft3DIcon
import com.example.util.IranianAmountUtils
import com.example.util.IranianPhoneUtils
import com.example.vehicle.data.VehicleEntity
import com.example.ui.components.IranianLicensePlate
import java.text.NumberFormat
import java.util.Locale

/**
 * Screen 3: Full Vehicle Profile & Dossier (پرونده مشخصات خودرو)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleProfileScreen(
    vehicle: VehicleEntity,
    onBack: () -> Unit,
    onSaveSpecs: (
        brand: String,
        model: String,
        year: String,
        color: String,
        plate: String,
        vin: String,
        estimatedValue: Long
    ) -> Unit,
    onUpdateMileage: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var brand by remember(vehicle) { mutableStateOf(vehicle.brand) }
    var model by remember(vehicle) { mutableStateOf(vehicle.model) }
    var year by remember(vehicle) { mutableStateOf(vehicle.year) }
    var color by remember(vehicle) { mutableStateOf(vehicle.color) }
    val initialPlateParts = remember(vehicle) { parseVehiclePlateParts(vehicle.plate) }
    var platePart1 by remember(vehicle) { mutableStateOf(initialPlateParts.part1) }
    var plateLetter by remember(vehicle) { mutableStateOf(initialPlateParts.letter) }
    var platePart2 by remember(vehicle) { mutableStateOf(initialPlateParts.part2) }
    var plateIranCode by remember(vehicle) { mutableStateOf(initialPlateParts.iranCode) }
    var vin by remember(vehicle) { mutableStateOf(vehicle.vin) }
    var estimatedValueText by remember(vehicle) {
        mutableStateOf(if (vehicle.estimatedValue > 0) vehicle.estimatedValue.toString() else "")
    }
    var currentMileageText by remember(vehicle) { mutableStateOf(vehicle.currentMileage.toString()) }
    val plate = "$platePart1 $plateLetter $platePart2 ایران $plateIranCode"

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("vehicle_profile_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "پرونده فنی خودرو: ${vehicle.brand} ${vehicle.model}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Plate Display
            Layered3DCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 3.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Soft3DIcon(
                            imageRes = R.drawable.img_3d_car,
                            contentDescription = vehicle.model,
                            size = 40.dp,
                            accentColor = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "شناسه و پلاک انتظامی",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "ثبت شده در سامانه دارینو",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IranianLicensePlate(
                        plate = plate,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            // Specs Form
            Layered3DCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "مشخصات پایه خودرو",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = brand,
                            onValueChange = { brand = it },
                            label = { Text("برند / سازنده") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it },
                            label = { Text("مدل و تیپ") },
                            modifier = Modifier.weight(1.2f),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = year,
                            onValueChange = { year = it },
                            label = { Text("سال ساخت (شمسی)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = color,
                            onValueChange = { color = it },
                            label = { Text("رنگ بدنه") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

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
                                value = IranianPhoneUtils.convertDigitsToPersian(platePart1),
                                onValueChange = {
                                    val digits = IranianPhoneUtils.convertDigitsToEnglish(it).filter { ch -> ch in '0'..'9' }
                                    if (digits.length <= 2) platePart1 = digits
                                },
                                label = { Text("۲ رقم") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )

                            var showPlateLetterMenu by remember { mutableStateOf(false) }
                            val plateLetters = remember {
                                listOf("الف", "ب", "ت", "ج", "د", "ژ", "س", "ص", "ط", "ع", "ق", "گ", "ل", "م", "ن", "و", "ه", "ی")
                            }
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                ExposedDropdownMenuBox(
                                    expanded = showPlateLetterMenu,
                                    onExpandedChange = { showPlateLetterMenu = it },
                                    modifier = Modifier.weight(1f)
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
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                            .testTag("profile_plate_letter_selector")
                                    )
                                    ExposedDropdownMenu(
                                        expanded = showPlateLetterMenu,
                                        onDismissRequest = { showPlateLetterMenu = false },
                                        modifier = Modifier.heightIn(max = 240.dp)
                                    ) {
                                        plateLetters.forEach { letter ->
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
                                                    showPlateLetterMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = IranianPhoneUtils.convertDigitsToPersian(platePart2),
                                onValueChange = {
                                    val digits = IranianPhoneUtils.convertDigitsToEnglish(it).filter { ch -> ch in '0'..'9' }
                                    if (digits.length <= 3) platePart2 = digits
                                },
                                label = { Text("۳ رقم") },
                                modifier = Modifier.weight(1.1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = IranianPhoneUtils.convertDigitsToPersian(plateIranCode),
                                onValueChange = {
                                    val digits = IranianPhoneUtils.convertDigitsToEnglish(it).filter { ch -> ch in '0'..'9' }
                                    if (digits.length <= 2) plateIranCode = digits
                                },
                                label = { Text("ایران") },
                                modifier = Modifier.weight(0.9f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                    }

                    OutlinedTextField(
                        value = vin,
                        onValueChange = { vin = it },
                        label = { Text("شماره شاسی (VIN)") },
                        placeholder = { Text("IRAN-PEUG-207-...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = IranianPhoneUtils.convertDigitsToPersian(currentMileageText),
                            onValueChange = {
                                currentMileageText = IranianPhoneUtils.convertDigitsToEnglish(it).filter { ch -> ch in '0'..'9' }
                            },
                            label = { Text("کیلومتر فعلی", maxLines = 1) },
                            modifier = Modifier
                                .weight(0.9f)
                                .height(64.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        PersianAmountInputField(
                            value = estimatedValueText,
                            onValueChange = { estimatedValueText = it },
                            label = "ارزش تقریبی",
                            unitLabel = "تومان",
                            placeholder = "مثال: ۸۵۰,۰۰۰,۰۰۰",
                            showWordsPreview = false,
                            modifier = Modifier.weight(1.3f),
                            fieldHeight = 64.dp,
                            testTag = "edit_vehicle_estimated_value_input"
                        )
                    }

                    Button(
                        onClick = {
                            val estVal = IranianAmountUtils.parseAmountToLong(estimatedValueText)
                            val km = IranianPhoneUtils.convertDigitsToEnglish(currentMileageText).filter { it.isDigit() }.toIntOrNull() ?: vehicle.currentMileage
                            onUpdateMileage(km)
                            onSaveSpecs(brand, model, year, color, plate, vin, estVal)
                            onBack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_profile_specs_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ذخیره تغییرات پرونده", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private data class VehiclePlateParts(
    val part1: String,
    val letter: String,
    val part2: String,
    val iranCode: String
)

private fun parseVehiclePlateParts(rawPlate: String): VehiclePlateParts {
    val normalized = IranianPhoneUtils.convertDigitsToEnglish(rawPlate)
        .replace("ي", "ی")
        .replace("ك", "ک")
        .trim()

    val iranCode = Regex("""ایران\s*(\d{1,2})""").find(normalized)?.groupValues?.get(1).orEmpty()
    val withoutIranCode = normalized.replace(Regex("""ایران\s*\d{1,2}"""), " ")
    val letter = Regex("""[^\d\s]""").find(withoutIranCode)?.value ?: "الف"
    val withoutLetter = withoutIranCode.replace(letter, " ")
    val digitGroups = Regex("""\d+""").findAll(withoutLetter).map { it.value }.toList()

    val twoDigit = digitGroups.firstOrNull { it.length == 2 }.orEmpty()
    val threeDigit = digitGroups.firstOrNull { it.length == 3 }.orEmpty()
    val fallbackPart1 = digitGroups.getOrNull(0).orEmpty().take(2)
    val fallbackPart2 = digitGroups.getOrNull(1).orEmpty().take(3)

    return VehiclePlateParts(
        part1 = twoDigit.ifBlank { fallbackPart1 },
        letter = letter,
        part2 = threeDigit.ifBlank { fallbackPart2 },
        iranCode = iranCode.ifBlank { digitGroups.lastOrNull { it.length <= 2 && it != twoDigit }.orEmpty() }.take(2)
    )
}
