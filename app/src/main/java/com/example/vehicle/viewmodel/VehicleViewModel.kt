package com.example.vehicle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vehicle.data.ServiceType
import com.example.vehicle.data.VehicleEntity
import com.example.vehicle.data.VehicleExpenseCategory
import com.example.vehicle.data.VehicleRepository
import com.example.vehicle.domain.VehicleAnalyzer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VehicleViewModel(
    private val repository: VehicleRepository = VehicleRepository.instance
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleState(isLoading = true))
    val uiState: StateFlow<VehicleState> = _uiState.asStateFlow()

    fun initRepository(context: android.content.Context) {
        repository.initDatabase(context)
    }

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                repository.vehicles,
                repository.services,
                repository.expenses,
                repository.insurances,
                repository.inspections
            ) { vehicles, services, expenses, insurances, inspections ->
                val currentSelected = _uiState.value.selectedVehicle?.let { sel ->
                    vehicles.find { it.id == sel.id }
                } ?: vehicles.firstOrNull()

                if (currentSelected != null) {
                    val health = VehicleAnalyzer.analyzeHealth(
                        vehicle = currentSelected,
                        services = services,
                        insurances = insurances,
                        inspections = inspections
                    )
                    val stats = VehicleAnalyzer.computeStatistics(
                        vehicleId = currentSelected.id,
                        expenses = expenses,
                        services = services
                    )
                    val timeline = VehicleAnalyzer.buildTimeline(
                        vehicleId = currentSelected.id,
                        services = services,
                        expenses = expenses,
                        insurances = insurances
                    )

                    _uiState.update { state ->
                        state.copy(
                            vehicles = vehicles,
                            selectedVehicle = currentSelected,
                            services = services.filter { it.vehicleId == currentSelected.id },
                            expenses = expenses.filter { it.vehicleId == currentSelected.id },
                            insurance = insurances.filter { it.vehicleId == currentSelected.id },
                            inspections = inspections.filter { it.vehicleId == currentSelected.id },
                            healthReport = health,
                            statistics = stats,
                            timelineEvents = timeline,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            vehicles = vehicles,
                            selectedVehicle = null,
                            isLoading = false
                        )
                    }
                }
            }.collect {}
        }
    }

    fun selectVehicle(vehicle: VehicleEntity) {
        val health = VehicleAnalyzer.analyzeHealth(
            vehicle = vehicle,
            services = repository.services.value,
            insurances = repository.insurances.value,
            inspections = repository.inspections.value
        )
        val stats = VehicleAnalyzer.computeStatistics(
            vehicleId = vehicle.id,
            expenses = repository.expenses.value,
            services = repository.services.value
        )
        val timeline = VehicleAnalyzer.buildTimeline(
            vehicleId = vehicle.id,
            services = repository.services.value,
            expenses = repository.expenses.value,
            insurances = repository.insurances.value
        )

        _uiState.update {
            it.copy(
                selectedVehicle = vehicle,
                services = repository.services.value.filter { s -> s.vehicleId == vehicle.id },
                expenses = repository.expenses.value.filter { e -> e.vehicleId == vehicle.id },
                insurance = repository.insurances.value.filter { ins -> ins.vehicleId == vehicle.id },
                inspections = repository.inspections.value.filter { insp -> insp.vehicleId == vehicle.id },
                healthReport = health,
                statistics = stats,
                timelineEvents = timeline
            )
        }
    }

    fun selectTab(tab: VehicleTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun openAddVehicle() {
        _uiState.update { it.copy(showAddVehicleSheet = true) }
    }

    fun closeAddVehicle() {
        _uiState.update { it.copy(showAddVehicleSheet = false) }
    }

    fun openAddService() {
        _uiState.update { it.copy(showAddServiceSheet = true) }
    }

    fun closeAddService() {
        _uiState.update { it.copy(showAddServiceSheet = false) }
    }

    fun openAddExpense() {
        _uiState.update { it.copy(showAddExpenseSheet = true) }
    }

    fun closeAddExpense() {
        _uiState.update { it.copy(showAddExpenseSheet = false) }
    }

    fun openInsuranceSheet() {
        _uiState.update { it.copy(showInsuranceSheet = true) }
    }

    fun closeInsuranceSheet() {
        _uiState.update { it.copy(showInsuranceSheet = false) }
    }

    fun openProfile() {
        _uiState.update { it.copy(showProfileScreen = true) }
    }

    fun closeProfile() {
        _uiState.update { it.copy(showProfileScreen = false) }
    }

    fun openMileageDialog() {
        _uiState.update { it.copy(showMileageDialog = true) }
    }

    fun closeMileageDialog() {
        _uiState.update { it.copy(showMileageDialog = false) }
    }

    fun dismissReminderConfirmation() {
        _uiState.update { it.copy(showReminderConfirmationDialog = false, lastAddedService = null) }
    }

    fun confirmReminderSchedule() {
        val service = _uiState.value.lastAddedService
        _uiState.update {
            it.copy(
                showReminderConfirmationDialog = false,
                lastAddedService = null,
                snackBarMessage = "یادآور سرویس بعدی با موفقیت در تقویم مالی و بخش یادآورها فعال شد."
            )
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackBarMessage = null) }
    }

    fun addVehicle(
        brand: String,
        model: String,
        year: String,
        color: String,
        plate: String,
        vin: String,
        currentMileage: Int,
        estimatedValue: Long
    ) {
        val created = repository.addVehicle(
            brand = brand,
            model = model,
            year = year,
            color = color,
            plate = plate,
            vin = vin,
            currentMileage = currentMileage,
            estimatedValue = estimatedValue
        )
        selectVehicle(created)
        _uiState.update {
            it.copy(
                showAddVehicleSheet = false,
                snackBarMessage = "خودروی $brand $model با موفقیت به پرونده خودروها افزوده شد."
            )
        }
    }

    fun updateVehicleSpecs(
        brand: String,
        model: String,
        year: String,
        color: String,
        plate: String,
        vin: String,
        estimatedValue: Long
    ) {
        val current = _uiState.value.selectedVehicle ?: return
        val updated = current.copy(
            brand = brand,
            model = model,
            year = year,
            color = color,
            plate = plate,
            vin = vin,
            estimatedValue = estimatedValue
        )
        repository.updateVehicle(updated)
        selectVehicle(updated)
        _uiState.update {
            it.copy(
                snackBarMessage = "اطلاعات پرونده خودرو به‌روزرسانی شد."
            )
        }
    }

    fun updateCurrentMileage(newMileage: Int) {
        val current = _uiState.value.selectedVehicle ?: return
        repository.updateMileage(current.id, newMileage)
        _uiState.update {
            it.copy(
                showMileageDialog = false,
                snackBarMessage = "کیلومتر خودرو به $newMileage تغییر یافت."
            )
        }
    }

    fun addServiceRecord(
        title: String,
        serviceType: ServiceType,
        date: String,
        mileage: Int,
        cost: Long,
        description: String,
        nextReminderDate: String?,
        nextReminderMileage: Int?,
        isReminderEnabled: Boolean
    ) {
        val current = _uiState.value.selectedVehicle ?: return
        val created = repository.addService(
            vehicleId = current.id,
            title = title,
            serviceType = serviceType,
            date = date,
            mileage = mileage,
            cost = cost,
            description = description,
            nextReminderDate = nextReminderDate,
            nextReminderMileage = nextReminderMileage,
            isReminderEnabled = isReminderEnabled
        )

        _uiState.update {
            it.copy(
                showAddServiceSheet = false,
                lastAddedService = created,
                showReminderConfirmationDialog = isReminderEnabled,
                snackBarMessage = if (!isReminderEnabled) "سرویس $title با موفقیت در پرونده خودرو ثبت شد." else null
            )
        }
    }

    fun addExpenseRecord(
        title: String,
        category: VehicleExpenseCategory,
        amount: Long,
        date: String,
        description: String
    ) {
        val current = _uiState.value.selectedVehicle ?: return
        repository.addExpense(
            vehicleId = current.id,
            title = title,
            category = category,
            amount = amount,
            date = date,
            description = description
        )
        _uiState.update {
            it.copy(
                showAddExpenseSheet = false,
                snackBarMessage = "هزینه $title در مخارج خودرو ثبت گردید."
            )
        }
    }

    fun saveInsuranceRecord(
        company: String,
        type: String,
        startDate: String,
        endDate: String,
        amount: Long,
        policyNumber: String
    ) {
        val current = _uiState.value.selectedVehicle ?: return
        repository.saveInsurance(
            vehicleId = current.id,
            company = company,
            type = type,
            startDate = startDate,
            endDate = endDate,
            amount = amount,
            policyNumber = policyNumber
        )
        _uiState.update {
            it.copy(
                showInsuranceSheet = false,
                snackBarMessage = "بیمه‌نامه $type با موفقیت ثبت و یادآور انقضا تنظیم شد."
            )
        }
    }

    fun saveInspectionRecord(
        lastInspectionDate: String,
        expiryDate: String,
        cost: Long,
        status: String,
        centerName: String
    ) {
        val current = _uiState.value.selectedVehicle ?: return
        repository.saveInspection(
            vehicleId = current.id,
            lastInspectionDate = lastInspectionDate,
            expiryDate = expiryDate,
            cost = cost,
            status = status,
            centerName = centerName
        )
        _uiState.update {
            it.copy(
                showInsuranceSheet = false,
                snackBarMessage = "اطلاعات معاینه فنی خودرو ثبت شد."
            )
        }
    }
}
