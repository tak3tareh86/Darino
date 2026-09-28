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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.util.IranianPhoneUtils
import com.example.vehicle.data.VehicleEntity
import com.example.vehicle.presentation.components.IranianLicensePlate
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
    var plate by remember(vehicle) { mutableStateOf(vehicle.plate) }
    var vin by remember(vehicle) { mutableStateOf(vehicle.vin) }
    var estimatedValueText by remember(vehicle) {
        mutableStateOf(if (vehicle.estimatedValue > 0) vehicle.estimatedValue.toString() else "")
    }
    var currentMileageText by remember(vehicle) { mutableStateOf(vehicle.currentMileage.toString()) }

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
                        modifier = Modifier.fillMaxWidth()
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

                    OutlinedTextField(
                        value = plate,
                        onValueChange = { plate = it },
                        label = { Text("شماره پلاک") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

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
                            value = currentMileageText,
                            onValueChange = { currentMileageText = it },
                            label = { Text("کیلومتر فعلی") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = estimatedValueText,
                            onValueChange = { estimatedValueText = it },
                            label = { Text("ارزش تقریبی (تومان)") },
                            modifier = Modifier.weight(1.2f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    Button(
                        onClick = {
                            val estVal = estimatedValueText.toLongOrNull() ?: 0L
                            val km = currentMileageText.toIntOrNull() ?: vehicle.currentMileage
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
