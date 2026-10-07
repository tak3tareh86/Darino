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
                            services = emptyList(),
                            expenses = emptyList(),
                            insurance = emptyList(),
                            inspections = emptyList(),
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
        viewModelScope.launch {
            val result = repository.addVehicle(
                brand = brand,
                model = model,
                year = year,
                color = color,
                plate = plate,
                vin = vin,
                currentMileage = currentMileage,
                estimatedValue = estimatedValue
            )
            result.onSuccess { created ->
                selectVehicle(created)
                _uiState.update {
                    it.copy(
                        showAddVehicleSheet = false,
                        snackBarMessage = "خودروی $brand $model با موفقیت به پرونده خودروها افزوده شد."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        snackBarMessage = "خطا در ثبت خودرو: ${err.localizedMessage ?: "اطلاعات نامعتبر است."}"
                    )
                }
            }
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
        viewModelScope.launch {
            val result = repository.updateVehicle(updated)
            result.onSuccess {
                selectVehicle(updated)
                _uiState.update {
                    it.copy(
                        snackBarMessage = "اطلاعات پرونده خودرو به‌روزرسانی شد."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        snackBarMessage = "خطا در به‌روزرسانی مشخصات: ${err.localizedMessage ?: "اطلاعات نامعتبر است."}"
                    )
                }
            }
        }
    }

    fun deleteVehicle(vehicleId: String) {
        viewModelScope.launch {
            val result = repository.deleteVehicle(vehicleId)
            result.onSuccess {
                val remaining = repository.vehicles.value.filter { it.id != vehicleId }
                val nextSelected = remaining.firstOrNull()
                if (nextSelected != null) {
                    selectVehicle(nextSelected)
                } else {
                    _uiState.update {
                        it.copy(
                            selectedVehicle = null,
                            showProfileScreen = false,
                            snackBarMessage = "خودرو از پرونده حذف شد."
                        )
                    }
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        snackBarMessage = "خطا در حذف خودرو: ${err.localizedMessage ?: "خطا در اتصال به پایگاه داده."}"
                    )
                }
            }
        }
    }

    fun updateCurrentMileage(newMileage: Int) {
        val current = _uiState.value.selectedVehicle ?: return
        viewModelScope.launch {
            val result = repository.updateMileage(current.id, newMileage)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        showMileageDialog = false,
                        snackBarMessage = "کیلومتر خودرو به $newMileage تغییر یافت."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        snackBarMessage = "خطا در به‌روزرسانی کیلومتر: ${err.localizedMessage ?: "اطلاعات نامعتبر است."}"
                    )
                }
            }
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
        viewModelScope.launch {
            val result = repository.addService(
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
            result.onSuccess { created ->
                _uiState.update {
                    it.copy(
                        showAddServiceSheet = false,
                        lastAddedService = created,
                        showReminderConfirmationDialog = isReminderEnabled,
                        snackBarMessage = if (!isReminderEnabled) "سرویس $title با موفقیت در پرونده خودرو ثبت شد." else null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        snackBarMessage = "خطا در ثبت سرویس: ${err.localizedMessage ?: "اطلاعات نامعتبر است."}"
                    )
                }
            }
        }
    }

    fun updateServiceRecord(
        serviceId: String,
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
        viewModelScope.launch {
            val result = repository.updateService(
                serviceId = serviceId,
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
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        snackBarMessage = "سرویس $title با موفقیت ویرایش شد."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        snackBarMessage = "خطا در ویرایش سرویس: ${err.localizedMessage ?: "اطلاعات نامعتبر است."}"
                    )
                }
            }
        }
    }

    fun completeServiceRecord(serviceId: String, completedDate: String) {
        viewModelScope.launch {
            val result = repository.completeService(serviceId, completedDate)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        snackBarMessage = "سرویس با موفقیت به عنوان انجام شده ثبت شد."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        snackBarMessage = "خطا در ثبت انجام سرویس: ${err.localizedMessage ?: "خطای پایگاه داده."}"
                    )
                }
            }
        }
    }

    fun deleteServiceRecord(serviceId: String) {
        viewModelScope.launch {
            val result = repository.deleteService(serviceId)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        snackBarMessage = "سرویس دوره‌ای از پرونده حذف شد."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        snackBarMessage = "خطا در حذف سرویس: ${err.localizedMessage ?: "خطای پایگاه داده."}"
                    )
                }
            }
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
        viewModelScope.launch {
            val result = repository.addExpense(
                vehicleId = current.id,
                title = title,
                category = category,
                amount = amount,
                date = date,
                description = description
            )
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        showAddExpenseSheet = false,
                        snackBarMessage = "هزینه $title در مخارج خودرو ثبت گردید."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        snackBarMessage = "خطا در ثبت هزینه: ${err.localizedMessage ?: "اطلاعات نامعتبر است."}"
                    )
                }
            }
        }
    }

    fun deleteExpenseRecord(expenseId: String) {
        viewModelScope.launch {
            val result = repository.deleteExpense(expenseId)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        snackBarMessage = "هزینه از پرونده حذف گردید."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        snackBarMessage = "خطا در حذف هزینه: ${err.localizedMessage ?: "خطای پایگاه داده."}"
                    )
                }
            }
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
        viewModelScope.launch {
            val result = repository.saveInsurance(
                vehicleId = current.id,
                company = company,
                type = type,
                startDate = startDate,
                endDate = endDate,
                amount = amount,
                policyNumber = policyNumber
            )
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        showInsuranceSheet = false,
                        snackBarMessage = "بیمه‌نامه $type با موفقیت ثبت و یادآور انقضا تنظیم شد."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        snackBarMessage = "خطا در ثبت بیمه‌نامه: ${err.localizedMessage ?: "اطلاعات نامعتبر است."}"
                    )
                }
            }
        }
    }

    fun renewInsuranceRecord(insuranceId: String, newEndDate: String) {
        viewModelScope.launch {
            val result = repository.renewInsurance(insuranceId, newEndDate)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        snackBarMessage = "بیمه‌نامه با موفقیت تمدید شد."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        snackBarMessage = "خطا در تمدید بیمه‌نامه: ${err.localizedMessage ?: "خطای پایگاه داده."}"
                    )
                }
            }
        }
    }

    fun deleteInsuranceRecord(insuranceId: String) {
        viewModelScope.launch {
            val result = repository.deleteInsurance(insuranceId)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        snackBarMessage = "بیمه‌نامه از پرونده حذف شد."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        snackBarMessage = "خطا در حذف بیمه‌نامه: ${err.localizedMessage ?: "خطای پایگاه داده."}"
                    )
                }
            }
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
        viewModelScope.launch {
            val result = repository.saveInspection(
                vehicleId = current.id,
                lastInspectionDate = lastInspectionDate,
                expiryDate = expiryDate,
                cost = cost,
                status = status,
                centerName = centerName
            )
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        showInsuranceSheet = false,
                        snackBarMessage = "اطلاعات معاینه فنی خودرو ثبت شد."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        snackBarMessage = "خطا در ثبت معاینه فنی: ${err.localizedMessage ?: "اطلاعات نامعتبر است."}"
                    )
                }
            }
        }
    }

    fun deleteInspectionRecord(inspectionId: String) {
        viewModelScope.launch {
            val result = repository.deleteInspection(inspectionId)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        snackBarMessage = "گواهی معاینه فنی از پرونده حذف شد."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        snackBarMessage = "خطا در حذف معاینه فنی: ${err.localizedMessage ?: "خطای پایگاه داده."}"
                    )
                }
            }
        }
    }
}
