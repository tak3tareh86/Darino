package com.example.vehicle.data

import android.content.Context
import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.security.SessionManager
import com.example.data.security.SessionState
import com.example.util.PersianCalendarHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Production-ready Repository providing vehicle dossier data and persistent Room SQLite Database operations.
 */
class VehicleRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _vehicles = MutableStateFlow<List<VehicleEntity>>(emptyList())
    val vehicles: StateFlow<List<VehicleEntity>> = _vehicles.asStateFlow()

    private val _services = MutableStateFlow<List<VehicleServiceEntity>>(emptyList())
    val services: StateFlow<List<VehicleServiceEntity>> = _services.asStateFlow()

    private val _expenses = MutableStateFlow<List<VehicleExpenseEntity>>(emptyList())
    val expenses: StateFlow<List<VehicleExpenseEntity>> = _expenses.asStateFlow()

    private val _insurances = MutableStateFlow<List<VehicleInsuranceEntity>>(emptyList())
    val insurances: StateFlow<List<VehicleInsuranceEntity>> = _insurances.asStateFlow()

    private val _inspections = MutableStateFlow<List<VehicleInspectionEntity>>(emptyList())
    val inspections: StateFlow<List<VehicleInspectionEntity>> = _inspections.asStateFlow()

    private var dbContext: Context? = null
    private var sessionObserverStarted = false

    /**
     * Initializes the Room SQLite database and loads user-specific vehicle records.
     */
    fun initDatabase(context: Context) {
        val appCtx = context.applicationContext
        dbContext = appCtx

        if (!sessionObserverStarted) {
            sessionObserverStarted = true
            repositoryScope.launch {
                SessionManager.sessionState.collectLatest { state ->
                    val userId = when (state) {
                        is SessionState.Authenticated -> state.user.id
                        is SessionState.PhoneVerificationRequired -> state.user.id
                        else -> null
                    }
                    if (userId == null) {
                        clearAllVehiclesData()
                        return@collectLatest
                    }
                    loadUserData(appCtx, userId)
                }
            }
        }
    }

    private suspend fun loadUserData(appCtx: Context, userId: String) {
        try {
            val db = AppDatabase.getDatabase(appCtx)
            val vDao = db.vehicleDao()

            // 1. One-time legacy JSON migration if present
            val store = vDao.getVehicleStore(userId)
            if (store != null && store.vehiclesJson.isNotBlank()) {
                val migratedVehicles = jsonToVehicles(store.vehiclesJson)
                val migratedServices = jsonToServices(store.servicesJson)
                val migratedExpenses = jsonToExpenses(store.expensesJson)
                val migratedInsurances = jsonToInsurances(store.insurancesJson)
                val migratedInspections = jsonToInspections(store.inspectionsJson)

                db.withTransaction {
                    migratedVehicles.forEach { v ->
                        vDao.insertVehicle(
                            com.example.data.database.VehicleEntity(
                                serverId = v.id,
                                userId = userId,
                                brand = v.brand,
                                model = v.model,
                                year = v.year,
                                plate = v.plate,
                                currentMileage = v.currentMileage,
                                notes = v.color,
                                vin = v.vin,
                                estimatedValue = v.estimatedValue,
                                updatedAt = v.createdAt
                            )
                        )
                    }

                    migratedServices.forEach { s ->
                        val serviceDateMs = parseJalaliToTimestamp(s.date)
                        val dueDateMs = parseJalaliToTimestamp(s.nextReminderDate)

                        vDao.insertService(
                            com.example.data.database.VehicleServiceEntity(
                                serverId = s.id,
                                userId = userId,
                                vehicleId = s.vehicleId,
                                type = s.serviceType.name,
                                title = s.title,
                                serviceDate = serviceDateMs,
                                dueDate = dueDateMs,
                                cost = s.cost,
                                dueMileage = s.mileage,
                                status = if (s.isReminderEnabled) "PENDING" else "COMPLETED",
                                notes = if (s.cost > 0L) "COST:${s.cost}|${s.description}" else s.description
                            )
                        )
                    }

                    vDao.insertExpenses(migratedExpenses.map { e ->
                        com.example.data.database.VehicleExpenseRoomEntity(
                            id = e.id,
                            userId = userId,
                            vehicleId = e.vehicleId,
                            title = e.title,
                            category = e.category.name,
                            amount = e.amount,
                            date = e.date,
                            description = e.description,
                            receiptImageUri = e.receiptImageUri
                        )
                    })

                    vDao.insertInsurances(migratedInsurances.map { ins ->
                        com.example.data.database.VehicleInsuranceRoomEntity(
                            id = ins.id,
                            userId = userId,
                            vehicleId = ins.vehicleId,
                            company = ins.company,
                            type = ins.type,
                            startDate = ins.startDate,
                            endDate = ins.endDate,
                            amount = ins.amount,
                            policyNumber = ins.policyNumber,
                            reminderDays = ins.reminderDays.joinToString(",")
                        )
                    })

                    vDao.insertInspections(migratedInspections.map { insp ->
                        com.example.data.database.VehicleInspectionRoomEntity(
                            id = insp.id,
                            userId = userId,
                            vehicleId = insp.vehicleId,
                            lastInspectionDate = insp.lastInspectionDate,
                            expiryDate = insp.expiryDate,
                            cost = insp.cost,
                            status = insp.status,
                            centerName = insp.centerName
                        )
                    })

                    // Clear legacy JSON store to prevent repeated migration
                    vDao.insertVehicleStore(com.example.data.database.VehicleStoreEntity(userId = userId, vehiclesJson = ""))
                }
            }

            // 2. Authoritative Load from Room DAOs
            loadFromRoom(db, userId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun loadFromRoom(db: AppDatabase, userId: String) {
        val vDao = db.vehicleDao()
        val dbVehicles = vDao.getAllVehiclesList(userId)

        if (dbVehicles.isNotEmpty()) {
            _vehicles.value = dbVehicles.map { v ->
                VehicleEntity(
                    id = v.serverId ?: v.id.toString(),
                    brand = v.brand,
                    model = v.model,
                    year = v.year,
                    color = v.notes ?: "سفید",
                    plate = v.plate,
                    vin = v.vin,
                    currentMileage = v.currentMileage,
                    estimatedValue = v.estimatedValue,
                    createdAt = v.updatedAt
                )
            }

            val dbExpenses = vDao.getAllExpensesList(userId)
            _expenses.value = dbExpenses.map { e ->
                VehicleExpenseEntity(
                    id = e.id,
                    vehicleId = e.vehicleId,
                    title = e.title,
                    category = try { VehicleExpenseCategory.valueOf(e.category) } catch (ex: Exception) { VehicleExpenseCategory.OTHER },
                    amount = e.amount,
                    date = e.date,
                    description = e.description,
                    receiptImageUri = e.receiptImageUri
                )
            }

            val dbServices = vDao.getAllServicesList(userId)
            _services.value = dbServices.map { s ->
                val (parsedCost, cleanDescription) = parseCostAndDescription(s.notes)
                val realCost = if (s.cost > 0L) s.cost else parsedCost

                val realServiceDate = if (s.serviceDate != null && s.serviceDate > 0L) {
                    PersianCalendarHelper.fromEpochMillis(s.serviceDate).toFormattedDate()
                } else if (s.updatedAt > 0L) {
                    PersianCalendarHelper.fromEpochMillis(s.updatedAt).toFormattedDate()
                } else {
                    PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()
                }

                val realNextReminderDate = if (s.dueDate != null && s.dueDate > 0L) {
                    PersianCalendarHelper.fromEpochMillis(s.dueDate).toFormattedDate()
                } else {
                    null
                }

                VehicleServiceEntity(
                    id = s.serverId ?: s.id.toString(),
                    vehicleId = s.vehicleId,
                    title = s.title,
                    serviceType = try { ServiceType.valueOf(s.type) } catch (e: Exception) { ServiceType.OIL_CHANGE },
                    date = realServiceDate,
                    mileage = s.dueMileage ?: 0,
                    cost = realCost,
                    description = cleanDescription,
                    nextReminderDate = realNextReminderDate,
                    nextReminderMileage = s.dueMileage,
                    isReminderEnabled = s.status != "COMPLETED"
                )
            }

            val dbInsurances = vDao.getAllInsurancesList(userId)
            _insurances.value = dbInsurances.map { ins ->
                VehicleInsuranceEntity(
                    id = ins.id,
                    vehicleId = ins.vehicleId,
                    company = ins.company,
                    type = ins.type,
                    startDate = ins.startDate,
                    endDate = ins.endDate,
                    amount = ins.amount,
                    policyNumber = ins.policyNumber,
                    reminderDays = ins.reminderDays.split(",").mapNotNull { it.toIntOrNull() }
                )
            }

            val dbInspections = vDao.getAllInspectionsList(userId)
            _inspections.value = dbInspections.map { insp ->
                VehicleInspectionEntity(
                    id = insp.id,
                    vehicleId = insp.vehicleId,
                    lastInspectionDate = insp.lastInspectionDate,
                    expiryDate = insp.expiryDate,
                    cost = insp.cost,
                    status = insp.status,
                    centerName = insp.centerName
                )
            }
        } else {
            clearAllVehiclesData()
        }
    }

    fun reloadFromDatabase(context: Context) {
        val appCtx = context.applicationContext
        dbContext = appCtx
        repositoryScope.launch {
            try {
                val db = AppDatabase.getDatabase(appCtx)
                SessionManager.userId?.let { loadFromRoom(db, it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun syncReminder(
        id: String,
        title: String,
        description: String,
        date: String,
        type: String = "VEHICLE"
    ) {
        val context = dbContext ?: return
        val userId = SessionManager.userId ?: return
        repositoryScope.launch {
            try {
                val manager = com.example.reminder.domain.ReminderManager(context)
                val persianDate = com.example.util.IranianPhoneUtils.convertDigitsToPersian(date)
                val reminder = com.example.reminder.data.ReminderEntity(
                    id = id,
                    userId = userId,
                    title = title,
                    description = description,
                    type = type,
                    sourceType = "VEHICLE",
                    sourceId = id,
                    date = persianDate,
                    time = "۰۹:۰۰"
                )
                manager.createReminder(reminder)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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

        val context = dbContext
        val userId = SessionManager.userId
        if (context != null && userId != null) {
            repositoryScope.launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    db.vehicleDao().insertVehicle(
                        com.example.data.database.VehicleEntity(
                            serverId = newVehicle.id,
                            userId = userId,
                            brand = newVehicle.brand,
                            model = newVehicle.model,
                            year = newVehicle.year,
                            plate = newVehicle.plate,
                            currentMileage = newVehicle.currentMileage,
                            notes = newVehicle.color,
                            vin = newVehicle.vin,
                            estimatedValue = newVehicle.estimatedValue,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return newVehicle
    }

    // Update Vehicle
    fun updateVehicle(updated: VehicleEntity) {
        _vehicles.value = _vehicles.value.map { if (it.id == updated.id) updated else it }

        val context = dbContext
        val userId = SessionManager.userId
        if (context != null && userId != null) {
            repositoryScope.launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val existing = db.vehicleDao().getVehicleByServerId(userId, updated.id)
                    if (existing != null) {
                        db.vehicleDao().updateVehicle(
                            existing.copy(
                                brand = updated.brand,
                                model = updated.model,
                                year = updated.year,
                                notes = updated.color,
                                plate = updated.plate,
                                vin = updated.vin,
                                currentMileage = updated.currentMileage,
                                estimatedValue = updated.estimatedValue,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    } else {
                        db.vehicleDao().insertVehicle(
                            com.example.data.database.VehicleEntity(
                                serverId = updated.id,
                                userId = userId,
                                brand = updated.brand,
                                model = updated.model,
                                year = updated.year,
                                plate = updated.plate,
                                currentMileage = updated.currentMileage,
                                notes = updated.color,
                                vin = updated.vin,
                                estimatedValue = updated.estimatedValue,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Update Mileage
    fun updateMileage(vehicleId: String, newMileage: Int) {
        _vehicles.value = _vehicles.value.map {
            if (it.id == vehicleId) it.copy(currentMileage = newMileage) else it
        }

        val context = dbContext
        val userId = SessionManager.userId
        if (context != null && userId != null) {
            repositoryScope.launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    db.vehicleDao().updateMileage(userId, vehicleId, newMileage)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Delete Vehicle
    fun deleteVehicle(vehicleId: String) {
        _vehicles.value = _vehicles.value.filter { it.id != vehicleId }
        _services.value = _services.value.filter { it.vehicleId != vehicleId }
        _expenses.value = _expenses.value.filter { it.vehicleId != vehicleId }
        _insurances.value = _insurances.value.filter { it.vehicleId != vehicleId }
        _inspections.value = _inspections.value.filter { it.vehicleId != vehicleId }

        val context = dbContext
        val userId = SessionManager.userId
        if (context != null && userId != null) {
            repositoryScope.launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    db.withTransaction {
                        db.vehicleDao().deleteVehicleByServerId(userId, vehicleId)
                        db.vehicleDao().deleteServicesByVehicleId(userId, vehicleId)
                        db.vehicleDao().deleteExpensesByVehicleId(userId, vehicleId)
                        db.vehicleDao().deleteInsurancesByVehicleId(userId, vehicleId)
                        db.vehicleDao().deleteInspectionsByVehicleId(userId, vehicleId)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
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

        // Also record as a vehicle expense if cost > 0
        if (cost > 0L) {
            addExpense(
                vehicleId = vehicleId,
                title = title,
                category = VehicleExpenseCategory.SERVICE,
                amount = cost,
                date = date,
                description = "سرویس دوره‌ای خودرو: $description"
            )
        }

        // Schedule notification alarm if reminder is enabled and next date is set
        if (isReminderEnabled && !nextReminderDate.isNullOrBlank()) {
            syncReminder(
                id = newService.id,
                title = "یادآور سرویس: ${currentCar?.brand ?: ""} ${currentCar?.model ?: ""}",
                description = "موعد تعویض و سرویس دوره‌ای: $title ($description)",
                date = nextReminderDate,
                type = "VEHICLE"
            )
        }

        val context = dbContext
        val userId = SessionManager.userId
        if (context != null && userId != null) {
            repositoryScope.launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val serviceDateMs = parseJalaliToTimestamp(date)
                    val dueDateMs = parseJalaliToTimestamp(nextReminderDate)
                    val encodedNotes = if (cost > 0L) "COST:$cost|$description" else description

                    db.vehicleDao().insertService(
                        com.example.data.database.VehicleServiceEntity(
                            serverId = newService.id,
                            userId = userId,
                            vehicleId = vehicleId,
                            type = serviceType.name,
                            title = title,
                            serviceDate = serviceDateMs,
                            dueDate = dueDateMs,
                            cost = cost,
                            dueMileage = mileage,
                            status = if (isReminderEnabled) "PENDING" else "COMPLETED",
                            notes = encodedNotes
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return newService
    }

    // Complete Service
    fun completeService(serviceId: String, completedDate: String) {
        _services.value = _services.value.map { svc ->
            if (svc.id == serviceId) {
                svc.copy(
                    date = completedDate,
                    nextReminderDate = null,
                    nextReminderMileage = null,
                    isReminderEnabled = false
                )
            } else svc
        }

        val context = dbContext
        val userId = SessionManager.userId
        if (context != null && userId != null) {
            repositoryScope.launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val completedDateMs = parseJalaliToTimestamp(completedDate) ?: System.currentTimeMillis()
                    db.vehicleDao().completeService(userId, serviceId, completedDateMs)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
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

        val context = dbContext
        val userId = SessionManager.userId
        if (context != null && userId != null) {
            repositoryScope.launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    db.vehicleDao().insertExpense(
                        com.example.data.database.VehicleExpenseRoomEntity(
                            id = newExpense.id,
                            userId = userId,
                            vehicleId = vehicleId,
                            title = title,
                            category = category.name,
                            amount = amount,
                            date = date,
                            description = description
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return newExpense
    }

    // Delete Expense
    fun deleteExpense(expenseId: String) {
        _expenses.value = _expenses.value.filter { it.id != expenseId }

        val context = dbContext
        val userId = SessionManager.userId
        if (context != null && userId != null) {
            repositoryScope.launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    db.vehicleDao().deleteExpenseById(userId, expenseId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
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

        // Also record as vehicle expense if amount > 0
        if (amount > 0L) {
            addExpense(
                vehicleId = vehicleId,
                title = "تمدید $type ($company)",
                category = VehicleExpenseCategory.INSURANCE,
                amount = amount,
                date = startDate,
                description = "شماره بیمه‌نامه: $policyNumber"
            )
        }

        // Schedule Alarm Notification Reminder for Insurance expiry date
        val currentCar = _vehicles.value.find { it.id == vehicleId }
        syncReminder(
            id = newInsurance.id,
            title = "تمدید $type خودرو",
            description = "سررسید انقضای بیمه‌نامه برای خودرو ${currentCar?.brand ?: ""} ${currentCar?.model ?: ""}. شماره بیمه‌نامه: $policyNumber",
            date = endDate,
            type = "INSURANCE"
        )

        val context = dbContext
        val userId = SessionManager.userId
        if (context != null && userId != null) {
            repositoryScope.launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    db.vehicleDao().insertInsurance(
                        com.example.data.database.VehicleInsuranceRoomEntity(
                            id = newInsurance.id,
                            userId = userId,
                            vehicleId = vehicleId,
                            company = company,
                            type = type,
                            startDate = startDate,
                            endDate = endDate,
                            amount = amount,
                            policyNumber = policyNumber,
                            reminderDays = newInsurance.reminderDays.joinToString(",")
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return newInsurance
    }

    fun renewInsurance(insuranceId: String, newEndDate: String) {
        val today = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()
        _insurances.value = _insurances.value.map { ins ->
            if (ins.id == insuranceId) {
                ins.copy(
                    startDate = today,
                    endDate = newEndDate
                )
            } else ins
        }

        val context = dbContext
        val userId = SessionManager.userId
        if (context != null && userId != null) {
            repositoryScope.launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    db.vehicleDao().renewInsurance(userId, insuranceId, today, newEndDate)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
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

        // Schedule Alarm Notification Reminder for Technical Inspection expiry date
        val currentCar = _vehicles.value.find { it.id == vehicleId }
        syncReminder(
            id = newInspection.id,
            title = "معاینه فنی خودرو",
            description = "موعد سررسید معاینه فنی برای خودرو ${currentCar?.brand ?: ""} ${currentCar?.model ?: ""}. مرکز $centerName",
            date = expiryDate,
            type = "VEHICLE"
        )

        val context = dbContext
        val userId = SessionManager.userId
        if (context != null && userId != null) {
            repositoryScope.launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    db.vehicleDao().insertInspection(
                        com.example.data.database.VehicleInspectionRoomEntity(
                            id = newInspection.id,
                            userId = userId,
                            vehicleId = vehicleId,
                            lastInspectionDate = lastInspectionDate,
                            expiryDate = expiryDate,
                            cost = cost,
                            status = status,
                            centerName = centerName
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return newInspection
    }

    fun clearAllVehiclesData() {
        _vehicles.value = emptyList()
        _services.value = emptyList()
        _expenses.value = emptyList()
        _insurances.value = emptyList()
        _inspections.value = emptyList()
    }

    private fun parseCostAndDescription(notes: String?): Pair<Long, String> {
        if (notes.isNullOrBlank()) return Pair(0L, "")
        if (notes.startsWith("COST:")) {
            val parts = notes.split("|", limit = 2)
            val costStr = parts[0].removePrefix("COST:")
            val cost = costStr.toLongOrNull() ?: 0L
            val desc = if (parts.size > 1) parts[1] else ""
            return Pair(cost, desc)
        }
        return Pair(0L, notes)
    }

    private fun parseJalaliToTimestamp(dateStr: String?): Long? {
        if (dateStr.isNullOrBlank()) return null
        val triple = com.example.calendar.domain.CalendarDateUtils.parseJalali(dateStr) ?: return null
        return runCatching {
            PersianCalendarHelper.jalaliToEpochMillis(triple.first, triple.second, triple.third, 9, 0)
        }.getOrNull()
    }

    private fun jsonToVehicles(json: String): List<VehicleEntity> {
        val list = mutableListOf<VehicleEntity>()
        if (json.isBlank()) return list
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                VehicleEntity(
                    id = obj.getString("id"),
                    brand = obj.getString("brand"),
                    model = obj.getString("model"),
                    year = obj.getString("year"),
                    color = obj.getString("color"),
                    plate = obj.getString("plate"),
                    vin = obj.optString("vin", ""),
                    currentMileage = obj.getInt("currentMileage"),
                    estimatedValue = obj.optLong("estimatedValue", 0L),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }
        return list
    }

    private fun jsonToServices(json: String): List<VehicleServiceEntity> {
        val list = mutableListOf<VehicleServiceEntity>()
        if (json.isBlank()) return list
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val stName = obj.optString("serviceType", ServiceType.OIL_CHANGE.name)
            val serviceType = try { ServiceType.valueOf(stName) } catch(e: Exception) { ServiceType.OIL_CHANGE }
            list.add(
                VehicleServiceEntity(
                    id = obj.getString("id"),
                    vehicleId = obj.getString("vehicleId"),
                    title = obj.getString("title"),
                    serviceType = serviceType,
                    date = obj.getString("date"),
                    mileage = obj.getInt("mileage"),
                    cost = obj.getLong("cost"),
                    description = obj.optString("description", ""),
                    nextReminderDate = obj.optString("nextReminderDate", "").let { if (it.isBlank()) null else it },
                    nextReminderMileage = obj.optInt("nextReminderMileage", 0).let { if (it == 0) null else it },
                    isReminderEnabled = obj.optBoolean("isReminderEnabled", true)
                )
            )
        }
        return list
    }

    private fun jsonToExpenses(json: String): List<VehicleExpenseEntity> {
        val list = mutableListOf<VehicleExpenseEntity>()
        if (json.isBlank()) return list
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val catName = obj.optString("category", VehicleExpenseCategory.OTHER.name)
            val category = try { VehicleExpenseCategory.valueOf(catName) } catch(e: Exception) { VehicleExpenseCategory.OTHER }
            list.add(
                VehicleExpenseEntity(
                    id = obj.getString("id"),
                    vehicleId = obj.getString("vehicleId"),
                    title = obj.getString("title"),
                    category = category,
                    amount = obj.getLong("amount"),
                    date = obj.getString("date"),
                    description = obj.optString("description", ""),
                    receiptImageUri = obj.optString("receiptImageUri", "").let { if (it.isBlank()) null else it }
                )
            )
        }
        return list
    }

    private fun jsonToInsurances(json: String): List<VehicleInsuranceEntity> {
        val list = mutableListOf<VehicleInsuranceEntity>()
        if (json.isBlank()) return list
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                VehicleInsuranceEntity(
                    id = obj.getString("id"),
                    vehicleId = obj.getString("vehicleId"),
                    company = obj.getString("company"),
                    type = obj.getString("type"),
                    startDate = obj.getString("startDate"),
                    endDate = obj.getString("endDate"),
                    amount = obj.getLong("amount"),
                    policyNumber = obj.optString("policyNumber", "")
                )
            )
        }
        return list
    }

    private fun jsonToInspections(json: String): List<VehicleInspectionEntity> {
        val list = mutableListOf<VehicleInspectionEntity>()
        if (json.isBlank()) return list
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                VehicleInspectionEntity(
                    id = obj.getString("id"),
                    vehicleId = obj.getString("vehicleId"),
                    lastInspectionDate = obj.getString("lastInspectionDate"),
                    expiryDate = obj.getString("expiryDate"),
                    cost = obj.getLong("cost"),
                    status = obj.optString("status", "معتبر"),
                    centerName = obj.optString("centerName", "")
                )
            )
        }
        return list
    }

    companion object {
        val instance: VehicleRepository by lazy { VehicleRepository() }
    }
}
