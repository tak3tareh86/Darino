package com.example.vehicle.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
    var showYearDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showTransmissionDialog by remember { mutableStateOf(false) }
    val vehicleColors = remember {
        listOf(
            "سفید" to Color(0xFFF5F5F0), "سفید صدفی" to Color(0xFFECE8DC), "سفید یخچالی" to Color(0xFFFFFFFF),
            "مشکی" to Color(0xFF171717), "مشکی متالیک" to Color(0xFF303238), "نقره‌ای" to Color(0xFFC0C0C0),
            "نقره‌ای متالیک" to Color(0xFFAEB6BE), "خاکستری" to Color(0xFF888888), "خاکستری متالیک" to Color(0xFF70777D),
            "نوک‌مدادی" to Color(0xFF454A50), "دودی" to Color(0xFF55565A), "سرمه‌ای" to Color(0xFF172B50),
            "آبی" to Color(0xFF1769AA), "آبی کبود" to Color(0xFF263B73), "آبی روشن" to Color(0xFF72B7E5),
            "قرمز" to Color(0xFFC62828), "زرشکی" to Color(0xFF800020), "عنابی" to Color(0xFF6D263A),
            "سبز" to Color(0xFF27834A), "سبز یشمی" to Color(0xFF176B55), "سبز زیتونی" to Color(0xFF687343),
            "کرم" to Color(0xFFE5D2A8), "بژ" to Color(0xFFD5C2A1), "طلایی" to Color(0xFFC6A04A),
            "قهوه‌ای" to Color(0xFF79513A), "مسی" to Color(0xFFB66A45), "نارنجی" to Color(0xFFED861F),
            "زرد" to Color(0xFFF2D13D), "بنفش" to Color(0xFF75458A), "زیتونی متالیک" to Color(0xFF73784A)
        )
    }
    val availableYears = remember(currentYear) {
        (1380..maxOf(1405, currentYear.toIntOrNull() ?: 1405)).map { it.toString() }
    }
    var brandSearchQuery by remember { mutableStateOf("") }
    val filteredBrands = remember(brands, brandSearchQuery) {
        brands.filter { it.contains(brandSearchQuery.trim(), ignoreCase = true) }
    }

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
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = brandSearchQuery,
                                onValueChange = { brandSearchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("brand_search_input"),
                                label = { Text("جست‌وجوی برند") },
                                placeholder = { Text("نام برند را وارد کنید") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.DirectionsCar,
                                        contentDescription = null
                                    )
                                },
                                trailingIcon = {
                                    if (brandSearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { brandSearchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "پاک کردن جست‌وجو"
                                            )
                                        }
                                    }
                                },
                                singleLine = true
                            )
                            if (filteredBrands.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "برندی با این نام پیدا نشد",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(4),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 320.dp)
                                        .testTag("brand_options_grid"),
                                    contentPadding = PaddingValues(2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(filteredBrands) { b ->
                                        val isSelected = brand == b
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 56.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    brand = b
                                                    showBrandDialog = false
                                                    brandSearchQuery = ""
                                                },
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            border = if (isSelected) {
                                                androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                                                )
                                            } else null
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 3.dp, vertical = 8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = "انتخاب‌شده",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                }
                                                Text(
                                                    text = b,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    ),
                                                    color = if (isSelected) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                    },
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
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

            if (showYearDialog) {
                AlertDialog(
                    onDismissRequest = { showYearDialog = false },
                    title = { Text("انتخاب سال ساخت") },
                    text = {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                            contentPadding = PaddingValues(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(availableYears) { option ->
                                val selected = year == option
                                Surface(
                                    modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(9.dp))
                                        .clickable { year = option; showYearDialog = false },
                                    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(9.dp),
                                    border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 2.dp)) {
                                        Text(option, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center, fontSize = 13.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { showYearDialog = false }) { Text("بستن") } }
                )
            }

            if (showColorDialog) {
                AlertDialog(
                    onDismissRequest = { showColorDialog = false },
                    title = { Text("انتخاب رنگ بدنه") },
                    text = {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                            contentPadding = PaddingValues(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(vehicleColors) { option ->
                                val selected = color == option.first
                                Surface(
                                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp))
                                        .clickable { color = option.first; showColorDialog = false },
                                    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 3.dp, vertical = 7.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(Modifier.size(34.dp).clip(RoundedCornerShape(50)).background(option.second))
                                        Spacer(Modifier.height(7.dp))
                                        Text(option.first, modifier = Modifier.fillMaxWidth(), maxLines = 1, softWrap = false,
                                            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, fontSize = 10.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { showColorDialog = false }) { Text("بستن") } }
                )
            }

            if (showTransmissionDialog) {
                AlertDialog(
                    onDismissRequest = { showTransmissionDialog = false },
                    title = { Text("انتخاب نوع گیربکس") },
                    text = {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(listOf("دنده‌ای", "اتوماتیک")) { option ->
                                val selected = transmission == option
                                Surface(
                                    modifier = Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(10.dp))
                                        .clickable { transmission = option; showTransmissionDialog = false },
                                    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                                        Text(option, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { showTransmissionDialog = false }) { Text("بستن") } }
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
                    trailingIcon = {
                        IconButton(onClick = { showYearDialog = true }) {
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = "انتخاب سال ساخت")
                        }
                    },
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
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("نوع گیربکس", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp) },
                    placeholder = { Text("انتخاب کنید") },
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        IconButton(onClick = { showTransmissionDialog = true }) {
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = "انتخاب نوع گیربکس")
                        }
                    },
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
                    trailingIcon = {
                        IconButton(onClick = { showColorDialog = true }) {
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = "انتخاب رنگ بدنه")
                        }
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = mileageText,
                    onValueChange = { mileageText = it },
                    label = { Text("کیلومتر فعلی", maxLines = 1) },
                    placeholder = { Text("45000") },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
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
                        ExposedDropdownMenuBox(
                            expanded = showLetterMenu,
                            onExpandedChange = { showLetterMenu = it },
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
                                    .testTag("plate_letter_selector")
                            )
                            ExposedDropdownMenu(
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
