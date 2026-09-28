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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.example.ui.screens.settings.model.SettingsMockDataSource
import com.example.ui.screens.settings.model.VehicleSettingItem
import com.example.ui.theme.ButtonShape
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM

@Composable
fun VehicleManagementScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vehicles = remember { mutableStateListOf(*SettingsMockDataSource.vehicles.toTypedArray()) }
    var isAddSheetOpen by remember { mutableStateOf(false) }
    var editingVehicle by remember { mutableStateOf<VehicleSettingItem?>(null) }
    var vehicleToDelete by remember { mutableStateOf<VehicleSettingItem?>(null) }

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
                            text = "لیست خودروهای فعال در گاراژ (${vehicles.size})",
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
            onSave = { savedVeh ->
                if (editingVehicle != null) {
                    val index = vehicles.indexOfFirst { it.id == editingVehicle?.id }
                    if (index >= 0) vehicles[index] = savedVeh
                } else {
                    vehicles.add(savedVeh)
                }
                isAddSheetOpen = false
            }
        )
    }

    // Confirmation Dialog
    SettingsConfirmationDialog(
        isOpen = vehicleToDelete != null,
        title = "حذف خودرو ${vehicleToDelete?.name ?: ""}",
        message = "آیا از حذف این خودرو از گاراژ خود مطمئن هستید؟ سوابق سرویس‌ها و هزینه‌های این خودرو حذف خواهند شد.",
        confirmButtonText = "حذف خودرو",
        onConfirm = {
            vehicleToDelete?.let { vehicles.remove(it) }
            vehicleToDelete = null
        },
        onDismiss = { vehicleToDelete = null }
    )
}

@Composable
fun VehicleManageCard(
    vehicle: VehicleSettingItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    var showMenu by remember { mutableStateOf(false) }

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
                            painter = painterResource(id = vehicle.iconRes),
                            contentDescription = vehicle.name,
                            modifier = Modifier.size(46.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = vehicle.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "${vehicle.modelYear} • ${vehicle.plateNumber}",
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
                        text = vehicle.mileageFormatted,
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
                        .background(vehicle.statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = vehicle.statusText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = vehicle.statusColor
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditVehicleSheet(
    vehicle: VehicleSettingItem?,
    onDismiss: () -> Unit,
    onSave: (VehicleSettingItem) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf(vehicle?.name ?: "") }
    var modelYear by remember { mutableStateOf(vehicle?.modelYear ?: "مدل ۱۴۰۲") }
    var mileage by remember { mutableStateOf(vehicle?.mileageFormatted ?: "") }
    var plate by remember { mutableStateOf(vehicle?.plateNumber ?: "") }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (vehicle != null) "ویرایش خودرو" else "افزودن خودرو جدید",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("نام و مدل خودرو (مثلاً تارا اتوماتیک)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(RadiusMD)
            )

            OutlinedTextField(
                value = modelYear,
                onValueChange = { modelYear = it },
                label = { Text("سال ساخت (مثلاً مدل ۱۴۰۱)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(RadiusMD)
            )

            OutlinedTextField(
                value = mileage,
                onValueChange = { mileage = it },
                label = { Text("کیلومتر کارکرد فعلی (کیلومتر)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(RadiusMD)
            )

            OutlinedTextField(
                value = plate,
                onValueChange = { plate = it },
                label = { Text("شماره پلاک (مثلاً ایران ۷۷ - ۲۴۵ ب ۱۲)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(RadiusMD)
            )

            Button(
                onClick = {
                    val saved = VehicleSettingItem(
                        id = vehicle?.id ?: "veh_${System.currentTimeMillis()}",
                        name = name.ifBlank { "خودروی من" },
                        modelYear = modelYear.ifBlank { "مدل ۱۴۰۰" },
                        mileageFormatted = if (mileage.contains("کیلومتر")) mileage else "$mileage کیلومتر",
                        plateNumber = plate.ifBlank { "ایران ۱۱ - ۱۱۱ الف ۱۱" },
                        statusText = "وضعیت عادی",
                        statusColor = EmeraldPrimaryLight,
                        iconRes = if (name.contains("دنا")) R.drawable.img_3d_car_dena else if (name.contains("206") || name.contains("۲۰۶")) R.drawable.img_3d_car_peugeot else R.drawable.img_3d_car
                    )
                    onSave(saved)
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
