package com.example.ui.screens.settings.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.screens.settings.components.SettingsConfirmationDialog
import com.example.ui.screens.settings.components.SettingsEmptyState
import com.example.ui.screens.settings.components.SettingsHeader
import com.example.ui.theme.ButtonShape
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM
import com.example.util.IranianPhoneUtils
import com.example.vehicle.data.VehicleEntity
import com.example.vehicle.data.VehicleRepository

@Composable
fun VehicleManagementScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember {
        VehicleRepository.instance.apply {
            initDatabase(context)
        }
    }
    val vehicles by repository.vehicles.collectAsState()
    var isAddSheetOpen by remember { mutableStateOf(false) }
    var editingVehicle by remember { mutableStateOf<VehicleEntity?>(null) }
    var vehicleToDelete by remember { mutableStateOf<VehicleEntity?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SettingsHeader(
                title = "مدیریت خودروها",
                subtitle = "خودروهای ثبت‌شده و اطلاعات پلاک و پیمایش",
                showBack = true,
                showSearch = false,
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingVehicle = null
                    isAddSheetOpen = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(RadiusMD),
                modifier = Modifier.testTag("add_vehicle_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "افزودن خودرو",
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "افزودن خودرو",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (vehicles.isEmpty()) {
                SettingsEmptyState(
                    title = "هیچ خودرویی ثبت نشده است",
                    description = "برای مدیریت سرویس‌های دوره‌ای، تعویض روغن و هزینه‌های بنزین و تعمیرات، خودروی خود را اضافه کنید.",
                    iconRes = R.drawable.img_3d_empty_garage,
                    actionButtonText = "افزودن خودرو جدید",
                    onActionClick = {
                        editingVehicle = null
                        isAddSheetOpen = true
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            text = "لیست خودروهای فعال در گاراژ (${IranianPhoneUtils.convertDigitsToPersian(vehicles.size.toString())})",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                    }

                    items(vehicles, key = { it.id }) { veh ->
                        VehicleManageCard(
                            vehicle = veh,
                            onEdit = {
                                editingVehicle = veh
                                isAddSheetOpen = true
                            },
                            onDelete = {
                                vehicleToDelete = veh
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    // Add/Edit Vehicle Sheet
    if (isAddSheetOpen) {
        AddEditVehicleSheet(
            vehicle = editingVehicle,
            onDismiss = { isAddSheetOpen = false },
            onSave = { brand, model, year, color, plate, vin, mileage, estValue ->
                if (editingVehicle != null) {
                    val current = editingVehicle!!
                    repository.updateVehicle(
                        current.copy(
                            brand = brand,
                            model = model,
                            year = year,
                            color = color,
                            plate = plate,
                            vin = vin,
                            currentMileage = mileage,
                            estimatedValue = estValue
                        )
                    )
                } else {
                    repository.addVehicle(
                        brand = brand,
                        model = model,
                        year = year,
                        color = color,
                        plate = plate,
                        vin = vin,
                        currentMileage = mileage,
                        estimatedValue = estValue
                    )
                }
                isAddSheetOpen = false
            }
        )
    }

    // Confirmation Dialog
    SettingsConfirmationDialog(
        isOpen = vehicleToDelete != null,
        title = "حذف خودرو ${vehicleToDelete?.brand ?: ""} ${vehicleToDelete?.model ?: ""}",
        message = "آیا از حذف این خودرو از گاراژ خود مطمئن هستید؟ سوابق سرویس‌ها و هزینه‌های این خودرو حذف خواهند شد.",
        confirmButtonText = "حذف خودرو",
        onConfirm = {
            vehicleToDelete?.let { repository.deleteVehicle(it.id) }
            vehicleToDelete = null
        },
        onDismiss = { vehicleToDelete = null }
    )
}

@Composable
fun VehicleManageCard(
    vehicle: VehicleEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    var showMenu by remember { mutableStateOf(false) }

    val iconRes = if (vehicle.brand.contains("دنا") || vehicle.model.contains("دنا")) {
        R.drawable.img_3d_car_dena
    } else if (vehicle.brand.contains("206") || vehicle.model.contains("206") || vehicle.brand.contains("۲۰۶") || vehicle.model.contains("۲۰۶")) {
        R.drawable.img_3d_car_peugeot
    } else {
        R.drawable.img_3d_car
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 6.dp else 2.dp,
                shape = RoundedCornerShape(RadiusLG)
            ),
        shape = RoundedCornerShape(RadiusLG),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            .size(54.dp)
                            .clip(RoundedCornerShape(RadiusMD))
                            .background(Color(0xFF3B82F6).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = "${vehicle.brand} ${vehicle.model}",
                            modifier = Modifier.size(46.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "${vehicle.brand} ${vehicle.model}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "${vehicle.year} • ${vehicle.plate}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "عملیات",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        shape = RoundedCornerShape(RadiusMD)
                    ) {
                        DropdownMenuItem(
                            text = { Text("ویرایش خودرو") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("حذف از گاراژ", color = ExpenseRoseLight) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Delete, contentDescription = null, tint = ExpenseRoseLight)
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // Specs row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RadiusSM))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.35f else 0.5f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "کارکرد فعلی:",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${IranianPhoneUtils.convertDigitsToPersian(vehicle.currentMileage.toString())} کیلومتر",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(EmeraldPrimaryLight.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "وضعیت فعال",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = EmeraldPrimaryLight
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditVehicleSheet(
    vehicle: VehicleEntity?,
    onDismiss: () -> Unit,
    onSave: (brand: String, model: String, year: String, color: String, plate: String, vin: String, mileage: Int, estimatedValue: Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var brand by remember { mutableStateOf(vehicle?.brand ?: "") }
    var model by remember { mutableStateOf(vehicle?.model ?: "") }
    var modelYear by remember { mutableStateOf(vehicle?.year ?: "") }
    var color by remember { mutableStateOf(vehicle?.color ?: "سفید") }
    var mileage by remember { mutableStateOf(vehicle?.currentMileage?.toString() ?: "") }
    var plate by remember { mutableStateOf(vehicle?.plate ?: "") }
    var vin by remember { mutableStateOf(vehicle?.vin ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = RadiusLG, topEnd = RadiusLG)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (vehicle != null) "ویرایش خودرو" else "افزودن خودرو جدید",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it; errorMessage = null },
                    label = { Text("برند (مثلاً پژو، تارا)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(RadiusMD)
                )

                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it; errorMessage = null },
                    label = { Text("مدل (مثلاً ۲۰۶ تیپ ۵)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(RadiusMD)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = modelYear,
                    onValueChange = { modelYear = it; errorMessage = null },
                    label = { Text("سال ساخت (مثلاً ۱۴۰۱)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(RadiusMD)
                )

                OutlinedTextField(
                    value = mileage,
                    onValueChange = { mileage = it; errorMessage = null },
                    label = { Text("کارکرد فعلی (کیلومتر)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(RadiusMD)
                )
            }

            OutlinedTextField(
                value = plate,
                onValueChange = { plate = it; errorMessage = null },
                label = { Text("شماره پلاک (مثلاً ایران ۷۷ - ۲۴۵ ب ۱۲)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(RadiusMD)
            )

            Button(
                onClick = {
                    if (brand.isBlank() || model.isBlank() || plate.isBlank()) {
                        errorMessage = "لطفاً برند، مدل و شماره پلاک را وارد کنید."
                        return@Button
                    }
                    val mileageInt = mileage.replace(",", "").trim().toIntOrNull() ?: 0
                    onSave(
                        brand.trim(),
                        model.trim(),
                        modelYear.trim().ifBlank { "۱۴۰۰" },
                        color.trim(),
                        plate.trim(),
                        vin.trim(),
                        mileageInt,
                        0L
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_vehicle_button"),
                shape = ButtonShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = "ذخیره خودرو",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = Color.White
                )
            }
        }
    }
}

