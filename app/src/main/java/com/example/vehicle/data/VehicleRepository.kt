package com.example.vehicle.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Repository providing vehicle dossier data and in-memory persistence
 */
class VehicleRepository {

    private val _vehicles = MutableStateFlow<List<VehicleEntity>>(createInitialVehicles())
    val vehicles: StateFlow<List<VehicleEntity>> = _vehicles.asStateFlow()

    private val _services = MutableStateFlow<List<VehicleServiceEntity>>(createInitialServices())
    val services: StateFlow<List<VehicleServiceEntity>> = _services.asStateFlow()

    private val _expenses = MutableStateFlow<List<VehicleExpenseEntity>>(createInitialExpenses())
    val expenses: StateFlow<List<VehicleExpenseEntity>> = _expenses.asStateFlow()

    private val _insurances = MutableStateFlow<List<VehicleInsuranceEntity>>(createInitialInsurances())
    val insurances: StateFlow<List<VehicleInsuranceEntity>> = _insurances.asStateFlow()

    private val _inspections = MutableStateFlow<List<VehicleInspectionEntity>>(createInitialInspections())
    val inspections: StateFlow<List<VehicleInspectionEntity>> = _inspections.asStateFlow()

    // Add Vehicle
    fun addVehicle(
        brand: String,
        model: String,
        year: String,
        color: String,
        plate: String,
        vin: String,
        currentMileage: Int,
        estimatedValue: Long
    ): VehicleEntity {
        val newVehicle = VehicleEntity(
            id = UUID.randomUUID().toString(),
            brand = brand,
            model = model,
            year = year,
            color = color,
            plate = plate,
            vin = vin,
            currentMileage = currentMileage,
            estimatedValue = estimatedValue
        )
        _vehicles.value = _vehicles.value + newVehicle
        return newVehicle
    }

    // Update Vehicle
    fun updateVehicle(updated: VehicleEntity) {
        _vehicles.value = _vehicles.value.map { if (it.id == updated.id) updated else it }
    }

    // Update Mileage
    fun updateMileage(vehicleId: String, newMileage: Int) {
        _vehicles.value = _vehicles.value.map {
            if (it.id == vehicleId) it.copy(currentMileage = newMileage) else it
        }
    }

    // Add Service
    fun addService(
        vehicleId: String,
        title: String,
        serviceType: ServiceType,
        date: String,
        mileage: Int,
        cost: Long,
        description: String,
        nextReminderDate: String?,
        nextReminderMileage: Int?,
        isReminderEnabled: Boolean
    ): VehicleServiceEntity {
        val newService = VehicleServiceEntity(
            id = UUID.randomUUID().toString(),
            vehicleId = vehicleId,
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
        _services.value = listOf(newService) + _services.value

        // Automatically update vehicle mileage if service mileage is higher
        val currentCar = _vehicles.value.find { it.id == vehicleId }
        if (currentCar != null && mileage > currentCar.currentMileage) {
            updateMileage(vehicleId, mileage)
        }

        // Also record as a vehicle expense
        addExpense(
            vehicleId = vehicleId,
            title = title,
            category = VehicleExpenseCategory.SERVICE,
            amount = cost,
            date = date,
            description = "سرویس دوره‌ای خودرو: $description"
        )

        return newService
    }

    // Add Expense
    fun addExpense(
        vehicleId: String,
        title: String,
        category: VehicleExpenseCategory,
        amount: Long,
        date: String,
        description: String = ""
    ): VehicleExpenseEntity {
        val newExpense = VehicleExpenseEntity(
            id = UUID.randomUUID().toString(),
            vehicleId = vehicleId,
            title = title,
            category = category,
            amount = amount,
            date = date,
            description = description
        )
        _expenses.value = listOf(newExpense) + _expenses.value
        return newExpense
    }

    // Add or Update Insurance
    fun saveInsurance(
        vehicleId: String,
        company: String,
        type: String,
        startDate: String,
        endDate: String,
        amount: Long,
        policyNumber: String
    ): VehicleInsuranceEntity {
        val newInsurance = VehicleInsuranceEntity(
            id = UUID.randomUUID().toString(),
            vehicleId = vehicleId,
            company = company,
            type = type,
            startDate = startDate,
            endDate = endDate,
            amount = amount,
            policyNumber = policyNumber
        )
        _insurances.value = listOf(newInsurance) + _insurances.value.filter { it.vehicleId != vehicleId || it.type != type }

        // Also record as vehicle expense
        addExpense(
            vehicleId = vehicleId,
            title = "تمدید $type ($company)",
            category = VehicleExpenseCategory.INSURANCE,
            amount = amount,
            date = startDate,
            description = "شماره بیمه‌نامه: $policyNumber"
        )

        return newInsurance
    }

    // Add or Update Inspection
    fun saveInspection(
        vehicleId: String,
        lastInspectionDate: String,
        expiryDate: String,
        cost: Long,
        status: String,
        centerName: String
    ): VehicleInspectionEntity {
        val newInspection = VehicleInspectionEntity(
            id = UUID.randomUUID().toString(),
            vehicleId = vehicleId,
            lastInspectionDate = lastInspectionDate,
            expiryDate = expiryDate,
            cost = cost,
            status = status,
            centerName = centerName
        )
        _inspections.value = listOf(newInspection) + _inspections.value.filter { it.vehicleId != vehicleId }
        return newInspection
    }

    companion object {
        private fun createInitialVehicles(): List<VehicleEntity> {
            return listOf(
                VehicleEntity(
                    id = "v-1",
                    brand = "پژو",
                    model = "207i پانوراما دنده‌ای",
                    year = "1402",
                    color = "سفید دوپوششه",
                    plate = "ایران ۴۴ - ۸۷۲ س ۳۵",
                    vin = "IRAN-PEUG-207-883921",
                    currentMileage = 45000,
                    estimatedValue = 820_000_000L
                ),
                VehicleEntity(
                    id = "v-2",
                    brand = "سایپا",
                    model = "پراید 131 SE",
                    year = "1398",
                    color = "نقره‌ای متالیک",
                    plate = "ایران ۱۱ - ۱۹۴ د ۶۱",
                    vin = "IRAN-SAIPA-131-447192",
                    currentMileage = 98000,
                    estimatedValue = 340_000_000L
                ),
                VehicleEntity(
                    id = "v-3",
                    brand = "ایران‌خودرو",
                    model = "دنا پلاس توربو اتوماتیک",
                    year = "1401",
                    color = "مشکی آبنوس",
                    plate = "ایران ۲۲ - ۳۱۸ ب ۷۴",
                    vin = "IRAN-IKCO-DENA-992381",
                    currentMileage = 32000,
                    estimatedValue = 1_050_000_000L
                )
            )
        }

        private fun createInitialServices(): List<VehicleServiceEntity> {
            return listOf(
                VehicleServiceEntity(
                    id = "s-1",
                    vehicleId = "v-1",
                    title = "تعویض روغن موتور و فیلترها",
                    serviceType = ServiceType.OIL_CHANGE,
                    date = "1405/06/10",
                    mileage = 45000,
                    cost = 850_000L,
                    description = "روغن بهران سوپر رانا 5W-40 + فیلتر روغن و هوای اصلی ایساکو",
                    nextReminderDate = "1405/12/10",
                    nextReminderMileage = 50000,
                    isReminderEnabled = true
                ),
                VehicleServiceEntity(
                    id = "s-2",
                    vehicleId = "v-1",
                    title = "تعویض لنت ترمز جلو",
                    serviceType = ServiceType.BRAKE_PADS,
                    date = "1405/04/15",
                    mileage = 40000,
                    cost = 1_200_000L,
                    description = "لنت تکستار اصلی فرانسه",
                    nextReminderDate = "1406/04/15",
                    nextReminderMileage = 60000,
                    isReminderEnabled = true
                ),
                VehicleServiceEntity(
                    id = "s-3",
                    vehicleId = "v-1",
                    title = "سرویس شمع و تنظیم موتور",
                    serviceType = ServiceType.ENGINE_TUNE,
                    date = "1405/02/20",
                    mileage = 35000,
                    cost = 950_000L,
                    description = "شمع سوزنی NGK و شستشوی انژکتور",
                    nextReminderDate = "1406/02/20",
                    nextReminderMileage = 55000,
                    isReminderEnabled = true
                ),
                VehicleServiceEntity(
                    id = "s-4",
                    vehicleId = "v-2",
                    title = "تعویض روغن و واسکازین",
                    serviceType = ServiceType.OIL_CHANGE,
                    date = "1405/05/18",
                    mileage = 95000,
                    cost = 600_000L,
                    description = "روغن اسپیدی طلایی 20W-50",
                    nextReminderDate = "1405/11/18",
                    nextReminderMileage = 100000,
                    isReminderEnabled = true
                )
            )
        }

        private fun createInitialExpenses(): List<VehicleExpenseEntity> {
            return listOf(
                VehicleExpenseEntity(
                    id = "e-1",
                    vehicleId = "v-1",
                    title = "سوخت‌گیری بنزین سوپر",
                    category = VehicleExpenseCategory.FUEL,
                    amount = 500_000L,
                    date = "1405/06/20",
                    description = "جایگاه سوخت ولنجک - ۴۰ لیتر"
                ),
                VehicleExpenseEntity(
                    id = "e-2",
                    vehicleId = "v-1",
                    title = "تعویض روغن و فیلتر",
                    category = VehicleExpenseCategory.SERVICE,
                    amount = 850_000L,
                    date = "1405/06/10",
                    description = "روغن بهران سوپر رانا"
                ),
                VehicleExpenseEntity(
                    id = "e-3",
                    vehicleId = "v-1",
                    title = "تمدید بیمه شخص ثالث",
                    category = VehicleExpenseCategory.INSURANCE,
                    amount = 3_000_000L,
                    date = "1405/06/01",
                    description = "بیمه ایران یکساله با تخفیف عدم خسارت"
                ),
                VehicleExpenseEntity(
                    id = "e-4",
                    vehicleId = "v-1",
                    title = "کارواش نانو و صفرشویی",
                    category = VehicleExpenseCategory.WASH,
                    amount = 250_000L,
                    date = "1405/05/28",
                    description = "کارواش پاسداران"
                ),
                VehicleExpenseEntity(
                    id = "e-5",
                    vehicleId = "v-1",
                    title = "شارژ پارکینگ و عوارض آزادراهی",
                    category = VehicleExpenseCategory.PARKING,
                    amount = 150_000L,
                    date = "1405/05/15",
                    description = "عوارض آزادراه تهران-شمال"
                ),
                VehicleExpenseEntity(
                    id = "e-6",
                    vehicleId = "v-1",
                    title = "تعویض دسته موتور و آچارکشی",
                    category = VehicleExpenseCategory.REPAIRS,
                    amount = 1_200_000L,
                    date = "1405/04/22",
                    description = "تعمیرگاه تخصصی پژو"
                )
            )
        }

        private fun createInitialInsurances(): List<VehicleInsuranceEntity> {
            return listOf(
                VehicleInsuranceEntity(
                    id = "ins-1",
                    vehicleId = "v-1",
                    company = "بیمه ایران",
                    type = "شخص ثالث",
                    startDate = "1405/06/01",
                    endDate = "1406/06/01",
                    amount = 3_000_000L,
                    policyNumber = "IR-9820-4491-01"
                ),
                VehicleInsuranceEntity(
                    id = "ins-2",
                    vehicleId = "v-1",
                    company = "بیمه آسیا",
                    type = "بیمه بدنه",
                    startDate = "1405/07/01",
                    endDate = "1406/07/01",
                    amount = 2_400_000L,
                    policyNumber = "AS-1029-7712-09"
                ),
                VehicleInsuranceEntity(
                    id = "ins-3",
                    vehicleId = "v-2",
                    company = "بیمه دانا",
                    type = "شخص ثالث",
                    startDate = "1405/01/15",
                    endDate = "1406/01/15",
                    amount = 2_800_000L,
                    policyNumber = "DN-8812-3321-45"
                )
            )
        }

        private fun createInitialInspections(): List<VehicleInspectionEntity> {
            return listOf(
                VehicleInspectionEntity(
                    id = "insp-1",
                    vehicleId = "v-1",
                    lastInspectionDate = "1404/08/10",
                    expiryDate = "1406/08/10",
                    cost = 92_000L,
                    status = "معتبر (خودرو صفر تا ۳ سال معاف/دارای گواهی)",
                    centerName = "مرکز مکانیزه نیایش"
                ),
                VehicleInspectionEntity(
                    id = "insp-2",
                    vehicleId = "v-2",
                    lastInspectionDate = "1404/10/20",
                    expiryDate = "1405/10/20",
                    cost = 92_000L,
                    status = "معتبر (اعتبار تا دی‌ماه)",
                    centerName = "مرکز بیهقی"
                )
            )
        }
    }
}
