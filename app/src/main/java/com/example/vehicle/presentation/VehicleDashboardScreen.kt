package com.example.vehicle.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.components.IranianLicensePlate
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.util.IranianPhoneUtils
import com.example.vehicle.data.VehicleEntity
import com.example.vehicle.data.VehicleInspectionEntity
import com.example.vehicle.data.VehicleInsuranceEntity
import com.example.vehicle.data.VehicleServiceEntity
import com.example.vehicle.domain.VehicleAnalyzer
import com.example.vehicle.presentation.components.AddServiceSheet
import com.example.vehicle.presentation.components.AddVehicleSheet
import com.example.vehicle.presentation.components.*
import com.example.vehicle.viewmodel.VehicleTab
import com.example.vehicle.viewmodel.VehicleViewModel
import com.example.util.MoneyFormatter
import java.text.NumberFormat
import java.util.Locale

/**
 * Main Vehicle Dashboard: Darino Smart Vehicle Assistant (دستیار مدیریت خودرو در دارینو)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarinoVehicleMainDashboard(
    onNavigateToHome: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    viewModel: VehicleViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.initRepository(context)
    }

    LaunchedEffect(state.snackBarMessage) {
        state.snackBarMessage?.let { msg ->
            snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Short)
            viewModel.clearSnackbar()
        }
    }

    var serviceToDeleteDashboard by remember { mutableStateOf<VehicleServiceEntity?>(null) }
    var vehicleToDeleteDashboard by remember { mutableStateOf<VehicleEntity?>(null) }
    var insuranceToEdit by remember { mutableStateOf<VehicleInsuranceEntity?>(null) }
    var inspectionToEdit by remember { mutableStateOf<VehicleInspectionEntity?>(null) }
    var insuranceToDelete by remember { mutableStateOf<VehicleInsuranceEntity?>(null) }
    var inspectionToDelete by remember { mutableStateOf<VehicleInspectionEntity?>(null) }

    if (insuranceToDelete != null) {
        val target = insuranceToDelete!!
        AlertDialog(
            onDismissRequest = { insuranceToDelete = null },
            title = { Text("حذف بیمه‌نامه") },
            text = { Text("آیا از حذف بیمه‌نامه «${target.type} - ${target.company}» اطمینان دارید؟ یادآورهای مرتبط نیز لغو می‌شوند.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteInsuranceRecord(target.id); insuranceToDelete = null }) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { insuranceToDelete = null }) { Text("انصراف") } }
        )
    }
    if (inspectionToDelete != null) {
        val target = inspectionToDelete!!
        AlertDialog(
            onDismissRequest = { inspectionToDelete = null },
            title = { Text("حذف معاینه فنی") },
            text = { Text("آیا از حذف این گواهی معاینه فنی اطمینان دارید؟ یادآورهای مرتبط نیز لغو می‌شوند.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteInspectionRecord(target.id); inspectionToDelete = null }) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { inspectionToDelete = null }) { Text("انصراف") } }
        )
    }

    if (serviceToDeleteDashboard != null) {
        val targetService = serviceToDeleteDashboard!!
        AlertDialog(
            onDismissRequest = { serviceToDeleteDashboard = null },
            title = {
                Text(
                    text = "حذف سرویس دوره‌ای",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "آیا از حذف سرویس «${targetService.title}» اطمینان دارید؟ این عمل قابل بازگشت نیست.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteServiceRecord(targetService.id)
                        serviceToDeleteDashboard = null
                    }
                ) {
                    Text("حذف", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { serviceToDeleteDashboard = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    if (vehicleToDeleteDashboard != null) {
        val targetVehicle = vehicleToDeleteDashboard!!
        AlertDialog(
            onDismissRequest = { vehicleToDeleteDashboard = null },
            title = {
                Text(
                    text = "حذف خودرو",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "آیا از حذف خودروی «${targetVehicle.brand} ${targetVehicle.model}» و کلیه سوابق سرویس و یادآورهای مربوط به آن اطمینان دارید؟ این عمل قابل بازگشت نیست.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteVehicle(targetVehicle.id)
                        vehicleToDeleteDashboard = null
                    }
                ) {
                    Text("حذف خودرو", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { vehicleToDeleteDashboard = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    if (state.showProfileScreen && state.selectedVehicle != null) {
        VehicleProfileScreen(
            vehicle = state.selectedVehicle!!,
            onBack = { viewModel.closeProfile() },
            onSaveSpecs = { b, m, y, c, p, v, e ->
                viewModel.updateVehicleSpecs(b, m, y, c, p, v, e)
            },
            onUpdateMileage = { km ->
                viewModel.updateCurrentMileage(km)
            }
        )
        return
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("vehicle_dashboard_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = bottomBar,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Top Header: Title + Add Vehicle Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Soft3DIcon(
                        imageRes = R.drawable.img_3d_car,
                        contentDescription = "دستیار خودرو دارینو",
                        size = 22.dp,
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "خودروهای من",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "دستیار هوشمند نگهداری، سرویس و هزینه‌ها",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FilledTonalButton(
                    onClick = { viewModel.openAddVehicle() },
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("top_add_vehicle_btn").height(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "ثبت خودرو",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp)
                    )
                }
            }

            // 2. Multi-Vehicle Collapsible Selector (Modern Design)
            var isExpanded by remember { mutableStateOf(false) }

            if (state.vehicles.isNotEmpty()) {
                val selected = state.selectedVehicle ?: state.vehicles.first()
                val primaryColor = MaterialTheme.colorScheme.primary

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header (Currently Selected)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = primaryColor.copy(alpha = 0.1f),
                        border = BorderStroke(1.5.dp, primaryColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { isExpanded = !isExpanded }
                            .testTag("vehicle_selector_header")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DirectionsCar,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${selected.brand} ${selected.model}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Expand/Collapse",
                                tint = primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Expanded List
                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.vehicles.filter { it.id != selected.id }.forEach { car ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.selectVehicle(car)
                                            isExpanded = false
                                        }
                                        .testTag("car_item_${car.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DirectionsCar,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${car.brand} ${car.model}",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (state.selectedVehicle != null) {
                val currentCar = state.selectedVehicle!!

                // 3. Tab Switcher (4 Navigation Tabs)
                VehicleTabRow(
                    selectedTab = state.activeTab,
                    onTabSelected = { viewModel.selectTab(it) }
                )

                // 4. Tab Content Crossfade
                Crossfade(
                    targetState = state.activeTab,
                    label = "VehicleTabCrossfade",
                    modifier = Modifier.weight(1f)
                ) { targetTab ->
                    when (targetTab) {
                        VehicleTab.SERVICES -> {
                            VehicleServicesTabContent(
                                vehicle = currentCar,
                                healthReport = state.healthReport,
                                services = state.services,
                                onEditProfile = { viewModel.openProfile() },
                                onMileageClick = { viewModel.openMileageDialog() },
                                onAddService = { viewModel.openAddService() },
                                onAddExpense = { viewModel.openAddExpense() },
                                onInsuranceClick = { viewModel.openInsuranceSheet() },
                                onDeleteService = { serviceToDeleteDashboard = it },
                                onDeleteVehicle = { vehicleToDeleteDashboard = currentCar },
                                onRetryReminder = { service -> viewModel.retryServiceReminder(service.id, service.title) }
                            )
                        }
                        VehicleTab.TIMELINE -> {
                            VehicleTimelineScreen(
                                events = state.timelineEvents
                            )
                        }
                        VehicleTab.REPORTS -> {
                            VehicleReportTab(
                                statistics = state.statistics,
                                expenses = state.expenses
                            )
                        }
                        VehicleTab.INSURANCE -> {
                            VehicleInsuranceDocumentsTabContent(
                                insurances = state.insurance,
                                inspections = state.inspections,
                                onOpenManage = {
                                    insuranceToEdit = null
                                    inspectionToEdit = null
                                    viewModel.openInsuranceSheet()
                                },
                                onEditInsurance = { item ->
                                    insuranceToEdit = item
                                    inspectionToEdit = null
                                    viewModel.openInsuranceSheet()
                                },
                                onEditInspection = { item ->
                                    inspectionToEdit = item
                                    insuranceToEdit = null
                                    viewModel.openInsuranceSheet()
                                },
                                onDeleteInsurance = { insuranceToDelete = it },
                                onDeleteInspection = { inspectionToDelete = it }
                            )
                        }
                    }
                }
            } else {
                // Empty State when no vehicle is registered yet
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Soft3DIcon(
                            imageRes = R.drawable.img_3d_empty_garage,
                            contentDescription = "گاراژ خالی",
                            size = 80.dp,
                            accentColor = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "هنوز خودرویی ثبت نکرده‌اید",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "با ثبت خودرو در پرونده دارینو، هزینه‌های بنزین، بیمه، معاینه فنی و موعد تعویض روغن و سرویس‌های دوره‌ای خودکار یادآوری و تحلیل می‌شوند.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.openAddVehicle() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("empty_state_add_vehicle_btn")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ثبت اولین خودرو", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Bottom Sheets & Dialogs
    if (state.showAddVehicleSheet) {
        AddVehicleSheet(
            onAddVehicle = { b: String, m: String, y: String, c: String, p: String, v: String, km: Int, est: Long, engine: String, transmission: String ->
                viewModel.addVehicle(b, m, y, c, p, v, km, est, engine, transmission)
            },
            onDismiss = { viewModel.closeAddVehicle() }
        )
    }

    if (state.showAddServiceSheet && state.selectedVehicle != null) {
        AddServiceSheet(
            vehicle = state.selectedVehicle!!,
            onAddService = { t, type, d, km, c, desc, remD, remKm, en, offsets, time ->
                viewModel.addServiceRecord(t, type, d, km, c, desc, remD, remKm, en, offsets, time)
            },
            onDismiss = { viewModel.closeAddService() }
        )
    }

    if (state.showAddExpenseSheet && state.selectedVehicle != null) {
        AddVehicleExpenseSheet(
            vehicle = state.selectedVehicle!!,
            onAddExpense = { t: String, cat: com.example.vehicle.data.VehicleExpenseCategory, amt: Long, d: String, desc: String ->
                viewModel.addExpenseRecord(t, cat, amt, d, desc)
            },
            onDismiss = { viewModel.closeAddExpense() }
        )
    }

    if (state.showInsuranceSheet && state.selectedVehicle != null) {
        VehicleInsuranceSheet(
            vehicle = state.selectedVehicle!!,
            editingInsurance = insuranceToEdit,
            editingInspection = inspectionToEdit,
            onSaveInsurance = { comp: String, typ: String, s: String, e: String, amt: Long, num: String, reminderEnabled: Boolean, reminderTime: String, reminderDays: List<Int>, recordId: String? ->
                viewModel.saveInsuranceRecord(comp, typ, s, e, amt, num, reminderEnabled, reminderTime, reminderDays, recordId)
            },
            onSaveInspection = { lastD: String, expD: String, c: Long, st: String, cntr: String, reminderEnabled: Boolean, reminderTime: String, reminderDays: List<Int>, recordId: String? ->
                viewModel.saveInspectionRecord(lastD, expD, c, st, cntr, reminderEnabled, reminderTime, reminderDays, recordId)
            },
            onDismiss = {
                insuranceToEdit = null
                inspectionToEdit = null
                viewModel.closeInsuranceSheet()
            }
        )
    }

    if (state.showMileageDialog && state.selectedVehicle != null) {
        VehicleMileageDialog(
            currentMileage = state.selectedVehicle!!.currentMileage,
            onConfirm = { km -> viewModel.updateCurrentMileage(km) },
            onDismiss = { viewModel.closeMileageDialog() }
        )
    }

    if (state.showReminderConfirmationDialog && state.lastAddedService != null) {
        ReminderConfirmationDialog(
            service = state.lastAddedService!!,
            onConfirm = { viewModel.confirmReminderSchedule() },
            onDismiss = { viewModel.dismissReminderConfirmation() }
        )
    }
}

/**
 * 4-Tab Navigation Bar
 */
@Composable
private fun VehicleTabRow(
    selectedTab: VehicleTab,
    onTabSelected: (VehicleTab) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            VehicleTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                val primaryColor = MaterialTheme.colorScheme.primary

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onTabSelected(tab) }
                        .testTag("vehicle_tab_${tab.name}"),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) primaryColor else Color.Transparent,
                    shadowElevation = if (isSelected) 2.dp else 0.dp
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Main Services & Dossier Tab Content
 */
@Composable
private fun VehicleServicesTabContent(
    vehicle: VehicleEntity,
    healthReport: com.example.vehicle.domain.VehicleHealthReport?,
    services: List<VehicleServiceEntity>,
    onEditProfile: () -> Unit,
    onMileageClick: () -> Unit,
    onAddService: () -> Unit,
    onAddExpense: () -> Unit,
    onInsuranceClick: () -> Unit,
    onDeleteService: ((VehicleServiceEntity) -> Unit)? = null,
    onDeleteVehicle: (() -> Unit)? = null,
    onRetryReminder: ((VehicleServiceEntity) -> Unit)? = null
) {
    var isHealthExpanded by remember { mutableStateOf(false) }
    var isServicesExpanded by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Vehicle Summary Hero Card
        item {
            VehicleSummaryCard(
                vehicle = vehicle,
                onEditClick = onEditProfile,
                onMileageClick = onMileageClick,
                onDeleteClick = onDeleteVehicle
            )
        }

        // 2. Quick Action Launchers (Clean & Compact)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    title = "ثبت سرویس",
                    icon = Icons.Rounded.Build,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onAddService,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    title = "ثبت هزینه",
                    icon = Icons.Rounded.LocalGasStation,
                    color = Color(0xFF3B82F6),
                    onClick = onAddExpense,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    title = "بیمه و مدارک",
                    icon = Icons.Rounded.Shield,
                    color = Color(0xFFF59E0B),
                    onClick = onInsuranceClick,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    title = "پرونده فنی",
                    icon = Icons.Rounded.Assignment,
                    color = Color(0xFF8B5CF6),
                    onClick = onEditProfile,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Collapsible Vehicle Health Card
        if (healthReport != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isHealthExpanded = !isHealthExpanded }
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.HealthAndSafety,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "وضعیت سلامت و استهلاک خودرو",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "شاخص سلامت قطعات، روغن و لنت‌ها",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (isHealthExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = isHealthExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            VehicleHealthCard(report = healthReport)
                        }
                    }
                }
            }
        }

        // 4. Collapsible Services List Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isServicesExpanded = !isServicesExpanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MiscellaneousServices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "سرویس‌های دوره‌ای",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${IranianPhoneUtils.convertDigitsToPersian(services.size.toString())} سرویس",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = if (isServicesExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 5. Service Records List
        if (isServicesExpanded) {
            items(services, key = { it.id }) { service ->
                ServiceRecordCard(
                    service = service,
                    onDeleteService = onDeleteService,
                    onRetryReminder = onRetryReminder
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(20.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                color = color
            )
        }
    }
}

@Composable
private fun ServiceRecordCard(
    service: VehicleServiceEntity,
    onDeleteService: ((VehicleServiceEntity) -> Unit)? = null,
    onRetryReminder: ((VehicleServiceEntity) -> Unit)? = null
) {
    val costFormatted = IranianPhoneUtils.convertDigitsToPersian(
        NumberFormat.getNumberInstance(Locale.US).format(service.cost)
    )

    Layered3DCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Service title and mileage stay together on the right; date and cost sit on the left.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Soft3DIcon(
                            imageRes = service.serviceType.iconRes,
                            contentDescription = service.title,
                            size = 28.dp,
                            accentColor = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = service.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "کیلومتر ${IranianPhoneUtils.convertDigitsToPersian(service.mileage.toString())}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (service.description.isNotBlank()) {
                        Text(
                            text = service.description,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "تاریخ ${IranianPhoneUtils.convertDigitsToPersian(service.date)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = costFormatted,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        if (onDeleteService != null) {
                            IconButton(
                                onClick = { onDeleteService(service) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "حذف سرویس",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (service.nextReminderMileage != null || service.nextReminderDate != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "موعد بعدی: ${service.nextReminderMileage?.let { "${IranianPhoneUtils.convertDigitsToPersian(it.toString())} کیلومتر" } ?: ""} ${service.nextReminderDate?.let { "(${IranianPhoneUtils.convertDigitsToPersian(it)})" } ?: ""}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (service.reminderAlarmFailed) {
                HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "فعال‌سازی هشدار ناموفق بود" + if (!service.reminderAlarmErrorMessage.isNullOrBlank()) ": ${service.reminderAlarmErrorMessage}" else "",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        if (onRetryReminder != null) {
                            FilledTonalButton(
                                onClick = { onRetryReminder(service) },
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("retry_reminder_${service.id}"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "تلاش مجدد",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 4: Insurance Policies & Technical Inspection View
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehicleInsuranceDocumentsTabContent(
    insurances: List<VehicleInsuranceEntity>,
    inspections: List<VehicleInspectionEntity>,
    onOpenManage: () -> Unit,
    onEditInsurance: (VehicleInsuranceEntity) -> Unit,
    onEditInspection: (VehicleInspectionEntity) -> Unit,
    onDeleteInsurance: (VehicleInsuranceEntity) -> Unit,
    onDeleteInspection: (VehicleInspectionEntity) -> Unit
) {
    var selectedDocumentTab by remember { mutableIntStateOf(0) }
    val today = remember {
        com.example.util.IranianPhoneUtils.convertDigitsToEnglish(
            com.example.util.PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()
        ).replace("-", "/")
    }
    val visibleInsurances = when (selectedDocumentTab) {
        0 -> insurances.filter { it.type.contains("ثالث") }
        1 -> insurances.filter { it.type.contains("بدنه") }
        else -> emptyList()
    }

    Column(
        modifier = Modifier.fillMaxSize().testTag("vehicle_insurance_tab"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("بیمه و مدارک خودرو", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            FilledTonalButton(onClick = onOpenManage, shape = RoundedCornerShape(10.dp)) {
                Text("ثبت اطلاعات", style = MaterialTheme.typography.labelSmall)
            }
        }
        PrimaryTabRow(selectedTabIndex = selectedDocumentTab) {
            Tab(selected = selectedDocumentTab == 0, onClick = { selectedDocumentTab = 0 }, text = { Text("شخص ثالث", maxLines = 1) })
            Tab(selected = selectedDocumentTab == 1, onClick = { selectedDocumentTab = 1 }, text = { Text("بیمه بدنه", maxLines = 1) })
            Tab(selected = selectedDocumentTab == 2, onClick = { selectedDocumentTab = 2 }, text = { Text("معاینه فنی", maxLines = 1) })
        }
        if (selectedDocumentTab == 0 || selectedDocumentTab == 1) {
            if (visibleInsurances.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("برای این بخش هنوز بیمه‌نامه‌ای ثبت نشده است.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visibleInsurances, key = { it.id }) { ins ->
                        val costStr = MoneyFormatter.formatToman(ins.amount)
                        val normalizedExpiry = VehicleAnalyzer.parseAndValidateJalaliDate(ins.endDate)
                        val expired = normalizedExpiry != null && normalizedExpiry < today
                        Layered3DCard(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = 3.dp,
                            borderColor = (if (expired) MaterialTheme.colorScheme.error else Color(0xFFF59E0B)).copy(alpha = 0.3f)
                        ) {
                            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Soft3DIcon(imageRes = R.drawable.img_3d_insurance, contentDescription = ins.type, size = 30.dp, accentColor = Color(0xFFF59E0B))
                                        Column {
                                            Text("بیمه ${ins.type} (${ins.company})", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                            Text("شماره بیمه‌نامه: ${ins.policyNumber.ifEmpty { "ثبت نشده" }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(8.dp), color = (if (expired || normalizedExpiry == null) MaterialTheme.colorScheme.error else Color(0xFF10B981)).copy(alpha = 0.12f)) {
                                        Text(
                                            text = when {
                                                normalizedExpiry == null -> "تاریخ نامعتبر"
                                                expired -> "منقضی شده"
                                                else -> "دارای اعتبار"
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                            color = if (expired || normalizedExpiry == null) MaterialTheme.colorScheme.error else Color(0xFF10B981),
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("اعتبار: ${IranianPhoneUtils.convertDigitsToPersian(ins.startDate)} تا ${IranianPhoneUtils.convertDigitsToPersian(ins.endDate)}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                                        Text(costStr, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B)))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { onEditInsurance(ins) }, modifier = Modifier.size(40.dp)) {
                                            Icon(Icons.Rounded.Edit, contentDescription = "ویرایش بیمه‌نامه", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = { onDeleteInsurance(ins) }, modifier = Modifier.size(40.dp)) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "حذف بیمه‌نامه", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (inspections.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("هنوز اطلاعات معاینه فنی ثبت نشده است.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(inspections, key = { it.id }) { insp ->
                        val normalizedExpiry = VehicleAnalyzer.parseAndValidateJalaliDate(insp.expiryDate)
                        val expired = normalizedExpiry != null && normalizedExpiry < today
                        Layered3DCard(modifier = Modifier.fillMaxWidth(), elevation = 2.dp, borderColor = (if (expired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant).copy(alpha = 0.35f)) {
                            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(insp.centerName.ifBlank { "مرکز معاینه فنی ثبت نشده" }, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Surface(shape = RoundedCornerShape(8.dp), color = (if (expired) MaterialTheme.colorScheme.error else Color(0xFF10B981)).copy(alpha = 0.12f)) {
                                        Text(
                                            when {
                                                normalizedExpiry == null -> "تاریخ نامعتبر"
                                                expired -> "منقضی شده"
                                                else -> insp.status.ifBlank { "دارای اعتبار" }
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                            color = if (expired || normalizedExpiry == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("تاریخ معاینه: ${IranianPhoneUtils.convertDigitsToPersian(insp.lastInspectionDate)} • انقضا: ${IranianPhoneUtils.convertDigitsToPersian(insp.expiryDate)}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { onEditInspection(insp) }, modifier = Modifier.size(40.dp)) {
                                            Icon(Icons.Rounded.Edit, contentDescription = "ویرایش معاینه فنی", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = { onDeleteInspection(insp) }, modifier = Modifier.size(40.dp)) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "حذف معاینه فنی", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
