package com.example.vehicle.data

import android.content.Context
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.data.security.SessionManager
import com.example.data.security.SessionState
import kotlinx.coroutines.flow.collectLatest
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Repository providing vehicle dossier data and Room SQLite Database persistence
 */
class VehicleRepository {

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
     * Initializes the Room SQLite database and loads any persisted vehicle store.
     */
    fun initDatabase(context: Context) {
        val appCtx = context.applicationContext
        dbContext = appCtx
        
        if (!sessionObserverStarted) {
            sessionObserverStarted = true
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
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
        val prefs = appCtx.getSharedPreferences("darino_general_preferences", Context.MODE_PRIVATE)
        val isCleanSlate = prefs.getBoolean("pref_is_clean_slate", false)
        try {
            val db = com.example.data.database.AppDatabase.getDatabase(appCtx)
            val vDao = db.vehicleDao()

            // Legacy JSON migration is owner-scoped and only runs for the authenticated user.
            val store = vDao.getVehicleStore(userId)
                if (store != null && store.vehiclesJson.isNotBlank()) {
                    val migratedVehicles = jsonToVehicles(store.vehiclesJson)
                    val migratedServices = jsonToServices(store.servicesJson)
                    val migratedExpenses = jsonToExpenses(store.expensesJson)
                    val migratedInsurances = jsonToInsurances(store.insurancesJson)
                    val migratedInspections = jsonToInspections(store.inspectionsJson)

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
                                notes = v.color
                            )
                        )
                    }

                    migratedServices.forEach { s ->
                        val targetDateStr = s.nextReminderDate?.takeIf { it.isNotBlank() } ?: s.date
                        val dueDateMs = runCatching {
                            com.example.util.PersianCalendarHelper.parseJalaliToTimestamp(targetDateStr)
                        }.getOrNull()

                        vDao.insertService(
                            com.example.data.database.VehicleServiceEntity(
                                serverId = s.id,
                                userId = userId,
                                vehicleId = s.vehicleId,
                                type = s.serviceType.name,
                                title = s.title,
                                dueDate = dueDateMs,
                                dueMileage = s.mileage,
                                status = "PENDING",
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

                    // Clear legacy JSON store to prevent dual-authority
                    vDao.insertVehicleStore(com.example.data.database.VehicleStoreEntity(userId = userId, vehiclesJson = ""))
                }

                // 2. Authoritative Load from Room DAOs
            loadFromRoom(db, userId, isCleanSlate)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun loadFromRoom(db: com.example.data.database.AppDatabase, userId: String, isCleanSlate: Boolean) {
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
                    currentMileage = v.currentMileage
                )
            }
            val dbExpenses = vDao.getAllExpensesList(userId)
            if (dbExpenses.isNotEmpty()) {
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
            }
            val dbServices = vDao.getAllServicesList(userId)
            _services.value = dbServices.map { s ->
                val (parsedCost, cleanDescription) = parseCostAndDescription(s.notes)
                val matchedVehicle = _vehicles.value.find { it.id == s.vehicleId.toString() }
                val realVehicleId = matchedVehicle?.id ?: s.vehicleId.toString()

                val realCost = if (parsedCost > 0L) {
                    parsedCost
                } else {
                    _expenses.value.find { it.vehicleId == realVehicleId && it.title == s.title }?.amount
                        ?: _expenses.value.find { it.title == s.title }?.amount
                        ?: 0L
                }

                val realDate = if (s.dueDate != null && s.dueDate > 0L) {
                    com.example.util.PersianCalendarHelper.fromEpochMillis(s.dueDate).toFormattedDate()
                } else if (s.updatedAt > 0L) {
                    com.example.util.PersianCalendarHelper.fromEpochMillis(s.updatedAt).toFormattedDate()
                } else {
                    "۱۴۰۴/۰۱/۰۱"
                }

                VehicleServiceEntity(
                    id = s.serverId ?: s.id.toString(),
                    vehicleId = realVehicleId,
                    title = s.title,
                    serviceType = try { ServiceType.valueOf(s.type) } catch (e: Exception) { ServiceType.OIL_CHANGE },
                    date = realDate,
                    mileage = s.dueMileage ?: 0,
                    cost = realCost,
                    description = cleanDescription,
                    nextReminderDate = if (s.dueDate != null && s.dueDate > 0L) realDate else null,
                    nextReminderMileage = s.dueMileage,
                    isReminderEnabled = s.status != "COMPLETED"
                )
            }
            val dbInsurances = vDao.getAllInsurancesList(userId)
            if (dbInsurances.isNotEmpty()) {
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
            }
            val dbInspections = vDao.getAllInspectionsList(userId)
            if (dbInspections.isNotEmpty()) {
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
            }
        }
    }

    private fun saveToDb() {
        val context = dbContext ?: return
        saveToDbInternal(context)
    }

    private fun saveToDbInternal(context: Context) {
        val appCtx = context.applicationContext
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val db = com.example.data.database.AppDatabase.getDatabase(appCtx)
                val vDao = db.vehicleDao()
                val userId = SessionManager.userId ?: return@launch

                // Save strictly to Room relational DAOs
                vDao.clearAllVehicles(userId)
                _vehicles.value.forEach { v ->
                    vDao.insertVehicle(
                        com.example.data.database.VehicleEntity(
                            serverId = v.id,
                            userId = userId,
                            brand = v.brand,
                            model = v.model,
                            year = v.year,
                            plate = v.plate,
                            currentMileage = v.currentMileage,
                            notes = v.color
                        )
                    )
                }

                vDao.clearAllServices(userId)
                _services.value.forEach { s ->
                    val targetDateStr = s.nextReminderDate?.takeIf { it.isNotBlank() } ?: s.date
                    val dueDateMs = runCatching {
                        com.example.util.PersianCalendarHelper.parseJalaliToTimestamp(targetDateStr)
                    }.getOrNull()
                    val encodedNotes = if (s.cost > 0L) "COST:${s.cost}|${s.description}" else s.description

                    vDao.insertService(
                        com.example.data.database.VehicleServiceEntity(
                            serverId = s.id,
                            userId = userId,
                            vehicleId = s.vehicleId,
                            type = s.serviceType.name,
                            title = s.title,
                            dueDate = dueDateMs,
                            dueMileage = s.mileage,
                            status = if (s.isReminderEnabled) "PENDING" else "COMPLETED",
                            notes = encodedNotes
                        )
                    )
                }

                vDao.clearAllExpenses(userId)
                vDao.insertExpenses(_expenses.value.map { e ->
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

                vDao.clearAllInsurances(userId)
                vDao.insertInsurances(_insurances.value.map { ins ->
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

                vDao.clearAllInspections(userId)
                vDao.insertInspections(_inspections.value.map { insp ->
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun reloadFromDatabase(context: Context) {
        val appCtx = context.applicationContext
        dbContext = appCtx
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val db = com.example.data.database.AppDatabase.getDatabase(appCtx)
                SessionManager.userId?.let { loadFromRoom(db, it, isCleanSlate = false) }
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
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
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
        saveToDb()
        return newVehicle
    }

    // Update Vehicle
    fun updateVehicle(updated: VehicleEntity) {
        _vehicles.value = _vehicles.value.map { if (it.id == updated.id) updated else it }
        saveToDb()
    }

    // Update Mileage
    fun updateMileage(vehicleId: String, newMileage: Int) {
        _vehicles.value = _vehicles.value.map {
            if (it.id == vehicleId) it.copy(currentMileage = newMileage) else it
        }
        saveToDb()
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

        saveToDb()
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
        saveToDb()
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

        // Schedule Alarm Notification Reminder for Insurance expiry date
        val currentCar = _vehicles.value.find { it.id == vehicleId }
        syncReminder(
            id = newInsurance.id,
            title = "تمدید $type خودرو",
            description = "سررسید انقضای بیمه‌نامه برای خودرو ${currentCar?.brand ?: ""} ${currentCar?.model ?: ""}. شماره بیمه‌نامه: $policyNumber",
            date = endDate,
            type = "INSURANCE"
        )

        saveToDb()
        return newInsurance
    }

    fun renewInsurance(insuranceId: String, newEndDate: String) {
        _insurances.value = _insurances.value.map { ins ->
            if (ins.id == insuranceId) {
                ins.copy(
                    startDate = com.example.util.PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate(),
                    endDate = newEndDate
                )
            } else ins
        }
        saveToDb()
    }

    fun completeService(serviceId: String, completedDate: String) {
        _services.value = _services.value.map { svc ->
            if (svc.id == serviceId) {
                svc.copy(
                    date = completedDate,
                    nextReminderDate = null,
                    nextReminderMileage = null
                )
            } else svc
        }
        saveToDb()
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

        saveToDb()
        return newInspection
    }

    private fun vehiclesToJson(list: List<VehicleEntity>): String {
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("brand", item.brand)
            obj.put("model", item.model)
            obj.put("year", item.year)
            obj.put("color", item.color)
            obj.put("plate", item.plate)
            obj.put("vin", item.vin)
            obj.put("currentMileage", item.currentMileage)
            obj.put("estimatedValue", item.estimatedValue)
            obj.put("createdAt", item.createdAt)
            arr.put(obj)
        }
        return arr.toString()
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

    private fun servicesToJson(list: List<VehicleServiceEntity>): String {
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("vehicleId", item.vehicleId)
            obj.put("title", item.title)
            obj.put("serviceType", item.serviceType.name)
            obj.put("date", item.date)
            obj.put("mileage", item.mileage)
            obj.put("cost", item.cost)
            obj.put("description", item.description)
            obj.put("nextReminderDate", item.nextReminderDate ?: "")
            obj.put("nextReminderMileage", item.nextReminderMileage ?: 0)
            obj.put("isReminderEnabled", item.isReminderEnabled)
            arr.put(obj)
        }
        return arr.toString()
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

    private fun expensesToJson(list: List<VehicleExpenseEntity>): String {
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("vehicleId", item.vehicleId)
            obj.put("title", item.title)
            obj.put("category", item.category.name)
            obj.put("amount", item.amount)
            obj.put("date", item.date)
            obj.put("description", item.description)
            obj.put("receiptImageUri", item.receiptImageUri ?: "")
            arr.put(obj)
        }
        return arr.toString()
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

    private fun insurancesToJson(list: List<VehicleInsuranceEntity>): String {
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("vehicleId", item.vehicleId)
            obj.put("company", item.company)
            obj.put("type", item.type)
            obj.put("startDate", item.startDate)
            obj.put("endDate", item.endDate)
            obj.put("amount", item.amount)
            obj.put("policyNumber", item.policyNumber)
            arr.put(obj)
        }
        return arr.toString()
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

    private fun inspectionsToJson(list: List<VehicleInspectionEntity>): String {
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("vehicleId", item.vehicleId)
            obj.put("lastInspectionDate", item.lastInspectionDate)
            obj.put("expiryDate", item.expiryDate)
            obj.put("cost", item.cost)
            obj.put("status", item.status)
            obj.put("centerName", item.centerName)
            arr.put(obj)
        }
        return arr.toString()
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

        val instance: VehicleRepository by lazy { VehicleRepository() }
    }

    fun clearAllVehiclesData() {
        _vehicles.value = emptyList()
        _services.value = emptyList()
        _expenses.value = emptyList()
        _insurances.value = emptyList()
        _inspections.value = emptyList()
    }

    fun restoreSampleVehicles() {
        _vehicles.value = createInitialVehicles()
        _services.value = createInitialServices()
        _expenses.value = createInitialExpenses()
        _insurances.value = createInitialInsurances()
        _inspections.value = createInitialInspections()
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
}
