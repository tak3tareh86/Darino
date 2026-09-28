package com.example.vehicle.viewmodel

import com.example.vehicle.data.VehicleEntity
import com.example.vehicle.data.VehicleExpenseEntity
import com.example.vehicle.data.VehicleInspectionEntity
import com.example.vehicle.data.VehicleInsuranceEntity
import com.example.vehicle.data.VehicleServiceEntity
import com.example.vehicle.data.VehicleTimelineEvent
import com.example.vehicle.domain.VehicleHealthReport
import com.example.vehicle.domain.VehicleStatistics

enum class VehicleTab(val title: String) {
    SERVICES("پرونده و سرویس‌ها"),
    TIMELINE("تاریخچه و تایم‌لاین"),
    REPORTS("گزارش و هزینه‌ها"),
    INSURANCE("بیمه و معاینه فنی")
}

/**
 * State for Vehicle Management Assistant
 */
data class VehicleState(
    val vehicles: List<VehicleEntity> = emptyList(),
    val selectedVehicle: VehicleEntity? = null,
    val services: List<VehicleServiceEntity> = emptyList(),
    val expenses: List<VehicleExpenseEntity> = emptyList(),
    val insurance: List<VehicleInsuranceEntity> = emptyList(),
    val inspections: List<VehicleInspectionEntity> = emptyList(),
    val healthReport: VehicleHealthReport? = null,
    val statistics: VehicleStatistics? = null,
    val timelineEvents: List<VehicleTimelineEvent> = emptyList(),
    val activeTab: VehicleTab = VehicleTab.SERVICES,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showAddVehicleSheet: Boolean = false,
    val showAddServiceSheet: Boolean = false,
    val showAddExpenseSheet: Boolean = false,
    val showInsuranceSheet: Boolean = false,
    val showProfileScreen: Boolean = false,
    val showMileageDialog: Boolean = false,
    val showReminderConfirmationDialog: Boolean = false,
    val lastAddedService: VehicleServiceEntity? = null,
    val snackBarMessage: String? = null
)
