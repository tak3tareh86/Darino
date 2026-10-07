package com.example.vehicle.data

import android.content.Context
import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.database.VehicleExpenseRoomEntity
import com.example.data.database.VehicleInspectionRoomEntity
import com.example.data.database.VehicleInsuranceRoomEntity
import com.example.data.security.SessionManager
import com.example.data.security.SessionState
import com.example.util.PersianCalendarHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.UUID

/**
 * Production-ready Repository providing strictly-typed, atomic, Room SQLite Database operations for Vehicles.
 * DB is the single source of truth; all mutations are suspendable with atomic transactions.
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
    private var roomCollectorsJob: Job? = null

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
                        roomCollectorsJob?.cancel()
                        clearAllVehiclesData()
                        return@collectLatest
                    }
                    setupRoomCollectors(appCtx, userId)
                }
            }
        }
    }

    private suspend fun setupRoomCollectors(appCtx: Context, userId: String) {
        roomCollectorsJob?.cancel()
        val db = AppDatabase.getDatabase(appCtx)
        val vDao = db.vehicleDao()

        // 1. One-time legacy JSON migration if present
        try {
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

                        if (s.cost > 0L) {
                            vDao.insertExpense(
                                VehicleExpenseRoomEntity(
                                    id = "exp_svc_${s.id}",
                                    userId = userId,
                                    vehicleId = s.vehicleId,
                                    title = s.title,
                                    category = VehicleExpenseCategory.SERVICE.name,
                                    amount = s.cost,
                                    date = s.date,
                                    description = "سرویس دوره‌ای خودرو: ${s.description}"
                                )
                            )
                        }
                    }

                    vDao.insertExpenses(migratedExpenses.map { e ->
                        VehicleExpenseRoomEntity(
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
                        VehicleInsuranceRoomEntity(
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
                        VehicleInspectionRoomEntity(
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
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Observe Room DAOs as authoritative Source of Truth
        roomCollectorsJob = repositoryScope.launch {
            launch {
                vDao.getAllVehicles(userId).collectLatest { dbVehicles ->
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
                }
            }

            launch {
                vDao.getAllServices(userId).collectLatest { dbServices ->
                    _services.value = dbServices.map { s ->
                        val (parsedCost, cleanDescription) = parseCostAndDescription(s.notes)
                        val realCost = if (s.cost > 0L) s.cost else parsedCost

                        val realServiceDate = if (s.serviceDate != null && s.serviceDate > 0L) {
                            PersianCalendarHelper.fromEpochMillis(s.serviceDate).toFormattedDate()
                        } else {
                            ""
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
                }
            }

            launch {
                vDao.getAllExpenses(userId).collectLatest { dbExpenses ->
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
            }

            launch {
                vDao.getAllInsurances(userId).collectLatest { dbInsurances ->
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
            }

            launch {
                vDao.getAllInspections(userId).collectLatest { dbInspections ->
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
    }

    suspend fun reloadFromDatabase(context: Context) = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        dbContext = appCtx
        val userId = SessionManager.userId ?: return@withContext
        setupRoomCollectors(appCtx, userId)
    }

    private suspend fun syncReminderInternal(
        context: Context,
        userId: String,
        id: String,
        title: String,
        description: String,
        date: String,
        type: String = "VEHICLE"
    ): Result<Unit> {
        return runCatching {
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
            val db = com.example.data.database.AppDatabase.getDatabase(context)
            val existing = db.smartReminderDao().getReminderById(userId, id)
            if (existing != null) {
                manager.updateReminder(reminder)
            } else {
                manager.createReminder(reminder)
            }
        }
    }

    // 1. Add Vehicle (Synchronous suspend contract)
    suspend fun addVehicle(
        brand: String,
        model: String,
        year: String,
        color: String,
        plate: String,
        vin: String = "",
        currentMileage: Int = 0,
        estimatedValue: Long = 0L
    ): Result<VehicleEntity> = withContext(Dispatchers.IO) {
        val context = dbContext ?: return@withContext Result.failure(IllegalStateException("دسترسی به پایگاه داده مقداردهی نشده است."))
        val userId = SessionManager.userId ?: return@withContext Result.failure(IllegalStateException("کاربر احراز هویت نشده است."))

        // Validation
        if (brand.isBlank()) return@withContext Result.failure(IllegalArgumentException("نام برند خودرو نمی‌تواند خالی باشد."))
        if (model.isBlank()) return@withContext Result.failure(IllegalArgumentException("مدل خودرو نمی‌تواند خالی باشد."))
        if (year.isBlank()) return@withContext Result.failure(IllegalArgumentException("سال ساخت خودرو نمی‌تواند خالی باشد."))
        if (plate.isBlank()) return@withContext Result.failure(IllegalArgumentException("شماره پلاک خودرو الزامی است."))
        if (currentMileage < 0) return@withContext Result.failure(IllegalArgumentException("کیلومتر کارکرد نمی‌تواند منفی باشد."))
        if (estimatedValue < 0L) return@withContext Result.failure(IllegalArgumentException("ارزش تقریبی خودرو نمی‌تواند منفی باشد."))

        try {
            val db = AppDatabase.getDatabase(context)
            val newId = UUID.randomUUID().toString()
            val entity = com.example.data.database.VehicleEntity(
                serverId = newId,
                userId = userId,
                brand = brand.trim(),
                model = model.trim(),
                year = year.trim(),
                plate = plate.trim(),
                currentMileage = currentMileage,
                notes = color.trim().ifBlank { "سفید" },
                vin = vin.trim(),
                estimatedValue = estimatedValue,
                updatedAt = System.currentTimeMillis()
            )

            db.vehicleDao().insertVehicle(entity)

            val createdDomain = VehicleEntity(
                id = newId,
                brand = entity.brand,
                model = entity.model,
                year = entity.year,
                color = entity.notes ?: "سفید",
                plate = entity.plate,
                vin = entity.vin,
                currentMileage = entity.currentMileage,
                estimatedValue = entity.estimatedValue,
                createdAt = entity.updatedAt
            )
            Result.success(createdDomain)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 2. Update Vehicle (Synchronous suspend contract)
    suspend fun updateVehicle(updated: VehicleEntity): Result<VehicleEntity> = withContext(Dispatchers.IO) {
        val context = dbContext ?: return@withContext Result.failure(IllegalStateException("دسترسی به پایگاه داده مقداردهی نشده است."))
        val userId = SessionManager.userId ?: return@withContext Result.failure(IllegalStateException("کاربر احراز هویت نشده است."))

        if (updated.brand.isBlank()) return@withContext Result.failure(IllegalArgumentException("برند خودرو نمی‌تواند خالی باشد."))
        if (updated.model.isBlank()) return@withContext Result.failure(IllegalArgumentException("مدل خودرو نمی‌تواند خالی باشد."))
        if (updated.year.isBlank()) return@withContext Result.failure(IllegalArgumentException("سال ساخت خودرو نمی‌تواند خالی باشد."))
        if (updated.plate.isBlank()) return@withContext Result.failure(IllegalArgumentException("شماره پلاک خودرو الزامی است."))
        if (updated.currentMileage < 0) return@withContext Result.failure(IllegalArgumentException("کیلومتر کارکرد نمی‌تواند منفی باشد."))
        if (updated.estimatedValue < 0L) return@withContext Result.failure(IllegalArgumentException("ارزش تقریبی خودرو نمی‌تواند منفی باشد."))

        try {
            val db = AppDatabase.getDatabase(context)
            val existing = db.vehicleDao().getVehicleByServerId(userId, updated.id)
                ?: return@withContext Result.failure(NoSuchElementException("خودروی مورد نظر در پرونده یافت نشد."))

            val updatedEntity = existing.copy(
                brand = updated.brand.trim(),
                model = updated.model.trim(),
                year = updated.year.trim(),
                notes = updated.color.trim(),
                plate = updated.plate.trim(),
                vin = updated.vin.trim(),
                currentMileage = updated.currentMileage,
                estimatedValue = updated.estimatedValue,
                updatedAt = System.currentTimeMillis()
            )

            val affectedRows = db.vehicleDao().updateVehicle(updatedEntity)
            if (affectedRows == 0) {
                return@withContext Result.failure(IllegalStateException("بروزرسانی مشخصات خودرو در پایگاه داده شکست خورد."))
            }
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 3. Update Mileage (Synchronous suspend contract)
    suspend fun updateMileage(vehicleId: String, newMileage: Int): Result<Int> = withContext(Dispatchers.IO) {
        val context = dbContext ?: return@withContext Result.failure(IllegalStateException("دسترسی به پایگاه داده مقداردهی نشده است."))
        val userId = SessionManager.userId ?: return@withContext Result.failure(IllegalStateException("کاربر احراز هویت نشده است."))

        if (newMileage < 0) return@withContext Result.failure(IllegalArgumentException("کیلومتر نمی‌تواند منفی باشد."))

        try {
            val db = AppDatabase.getDatabase(context)
            val existing = db.vehicleDao().getVehicleByServerId(userId, vehicleId)
                ?: return@withContext Result.failure(NoSuchElementException("خودرو یافت نشد."))

            val affectedRows = db.vehicleDao().updateMileage(userId, vehicleId, newMileage)
            if (affectedRows == 0) {
                return@withContext Result.failure(IllegalStateException("به‌روزرسانی کیلومتر خودرو انجام نشد."))
            }
            Result.success(newMileage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 4. Delete Vehicle (Atomic cascading delete in Room transaction with child reminder cleanup)
    suspend fun deleteVehicle(vehicleId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val context = dbContext ?: return@withContext Result.failure(IllegalStateException("دسترسی به پایگاه داده مقداردهی نشده است."))
        val userId = SessionManager.userId ?: return@withContext Result.failure(IllegalStateException("کاربر احراز هویت نشده است."))

        try {
            val db = AppDatabase.getDatabase(context)
            val reminderManager = com.example.reminder.domain.ReminderManager(context)

            // Gather child entity IDs to clean up their reminders
            val serviceIds = db.vehicleDao().getServiceServerIdsByVehicleId(userId, vehicleId)
            val insuranceIds = db.vehicleDao().getInsuranceIdsByVehicleId(userId, vehicleId)
            val inspectionIds = db.vehicleDao().getInspectionIdsByVehicleId(userId, vehicleId)

            val affectedRows = db.withTransaction {
                val vehicleAffected = db.vehicleDao().deleteVehicleByServerId(userId, vehicleId)
                db.vehicleDao().deleteServicesByVehicleId(userId, vehicleId)
                db.vehicleDao().deleteExpensesByVehicleId(userId, vehicleId)
                db.vehicleDao().deleteInsurancesByVehicleId(userId, vehicleId)
                db.vehicleDao().deleteInspectionsByVehicleId(userId, vehicleId)
                vehicleAffected
            }

            if (affectedRows == 0) {
                return@withContext Result.failure(IllegalStateException("هیچ خودرویی برای حذف یافت نشد."))
            }

            // Cleanup associated reminders to prevent orphans
            serviceIds.forEach { id -> reminderManager.deleteReminder(id) }
            insuranceIds.forEach { id -> reminderManager.deleteReminder(id) }
            inspectionIds.forEach { id -> reminderManager.deleteReminder(id) }
            reminderManager.deleteReminder(vehicleId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 5. Add Service (Atomic Service + Expense transaction)
    suspend fun addService(
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
    ): Result<VehicleServiceEntity> = withContext(Dispatchers.IO) {
        val context = dbContext ?: return@withContext Result.failure(IllegalStateException("دسترسی به پایگاه داده مقداردهی نشده است."))
        val userId = SessionManager.userId ?: return@withContext Result.failure(IllegalStateException("کاربر احراز هویت نشده است."))

        if (title.isBlank()) return@withContext Result.failure(IllegalArgumentException("عنوان سرویس نمی‌تواند خالی باشد."))
        if (cost < 0L) return@withContext Result.failure(IllegalArgumentException("مبلغ سرویس نمی‌تواند منفی باشد."))
        if (mileage < 0) return@withContext Result.failure(IllegalArgumentException("کیلومتر نمی‌تواند منفی باشد."))

        val serviceDateMs = parseJalaliToTimestamp(date)
            ?: return@withContext Result.failure(IllegalArgumentException("تاریخ سرویس وارد شده نامعتبر است."))

        val dueDateMs = if (!nextReminderDate.isNullOrBlank()) {
            parseJalaliToTimestamp(nextReminderDate)
                ?: return@withContext Result.failure(IllegalArgumentException("تاریخ یادآور سرویس نامعتبر است."))
        } else null

        try {
            val db = AppDatabase.getDatabase(context)
            val vehicle = db.vehicleDao().getVehicleByServerId(userId, vehicleId)
                ?: return@withContext Result.failure(NoSuchElementException("خودروی مورد نظر یافت نشد."))

            val serviceId = UUID.randomUUID().toString()
            val encodedNotes = if (cost > 0L) "COST:$cost|$description" else description

            val newService = VehicleServiceEntity(
                id = serviceId,
                vehicleId = vehicleId,
                title = title.trim(),
                serviceType = serviceType,
                date = date.trim(),
                mileage = mileage,
                cost = cost,
                description = description.trim(),
                nextReminderDate = nextReminderDate?.trim(),
                nextReminderMileage = nextReminderMileage,
                isReminderEnabled = isReminderEnabled
            )

            db.withTransaction {
                // Insert Service Record
                db.vehicleDao().insertService(
                    com.example.data.database.VehicleServiceEntity(
                        serverId = serviceId,
                        userId = userId,
                        vehicleId = vehicleId,
                        type = serviceType.name,
                        title = title.trim(),
                        serviceDate = serviceDateMs,
                        dueDate = dueDateMs,
                        cost = cost,
                        dueMileage = mileage,
                        status = if (isReminderEnabled) "PENDING" else "COMPLETED",
                        notes = encodedNotes
                    )
                )

                // Atomic Expense Record for Service
                if (cost > 0L) {
                    db.vehicleDao().insertExpense(
                        VehicleExpenseRoomEntity(
                            id = "exp_svc_$serviceId",
                            userId = userId,
                            vehicleId = vehicleId,
                            title = title.trim(),
                            category = VehicleExpenseCategory.SERVICE.name,
                            amount = cost,
                            date = date.trim(),
                            description = "سرویس دوره‌ای خودرو: ${description.trim()}"
                        )
                    )
                }

                // Update Vehicle Mileage if higher
                if (mileage > vehicle.currentMileage) {
                    db.vehicleDao().updateMileage(userId, vehicleId, mileage)
                }
            }

            // Sync Notification Reminder if enabled
            if (isReminderEnabled && !nextReminderDate.isNullOrBlank()) {
                val syncResult = syncReminderInternal(
                    context = context,
                    userId = userId,
                    id = serviceId,
                    title = "یادآور سرویس: ${vehicle.brand} ${vehicle.model}",
                    description = "موعد تعویض و سرویس دوره‌ای: $title (${description.trim()})",
                    date = nextReminderDate.trim(),
                    type = "VEHICLE"
                )
                if (syncResult.isFailure) {
                    return@withContext Result.failure(syncResult.exceptionOrNull() ?: Exception("خطا در همگام‌سازی یادآور"))
                }
            }

            Result.success(newService)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 5.5 Update Service (Atomic full-edit Service + Expense + Reminder sync)
    suspend fun updateService(
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
    ): Result<VehicleServiceEntity> = withContext(Dispatchers.IO) {
        val context = dbContext ?: return@withContext Result.failure(IllegalStateException("دسترسی به پایگاه داده مقداردهی نشده است."))
        val userId = SessionManager.userId ?: return@withContext Result.failure(IllegalStateException("کاربر احراز هویت نشده است."))

        if (title.isBlank()) return@withContext Result.failure(IllegalArgumentException("عنوان سرویس نمی‌تواند خالی باشد."))
        if (cost < 0L) return@withContext Result.failure(IllegalArgumentException("مبلغ سرویس نمی‌تواند منفی باشد."))
        if (mileage < 0) return@withContext Result.failure(IllegalArgumentException("کیلومتر نمی‌تواند منفی باشد."))

        val serviceDateMs = parseJalaliToTimestamp(date)
            ?: return@withContext Result.failure(IllegalArgumentException("تاریخ سرویس وارد شده نامعتبر است."))

        val dueDateMs = if (!nextReminderDate.isNullOrBlank()) {
            parseJalaliToTimestamp(nextReminderDate)
                ?: return@withContext Result.failure(IllegalArgumentException("تاریخ یادآور سرویس نامعتبر است."))
        } else null

        try {
            val db = AppDatabase.getDatabase(context)
            val existingSvc = db.vehicleDao().getServiceByServerId(userId, serviceId)
                ?: return@withContext Result.failure(NoSuchElementException("سرویس مورد نظر یافت نشد."))

            val vehicle = db.vehicleDao().getVehicleByServerId(userId, existingSvc.vehicleId)
                ?: return@withContext Result.failure(NoSuchElementException("خودروی مربوط به سرویس یافت نشد."))

            val encodedNotes = if (cost > 0L) "COST:$cost|$description" else description

            val updatedService = VehicleServiceEntity(
                id = serviceId,
                vehicleId = vehicle.serverId ?: vehicle.id.toString(),
                title = title.trim(),
                serviceType = serviceType,
                date = date.trim(),
                mileage = mileage,
                cost = cost,
                description = description.trim(),
                nextReminderDate = nextReminderDate?.trim(),
                nextReminderMileage = nextReminderMileage,
                isReminderEnabled = isReminderEnabled
            )

            db.withTransaction {
                // Update Service Record
                val affectedRows = db.vehicleDao().updateService(
                    com.example.data.database.VehicleServiceEntity(
                        id = existingSvc.id,
                        serverId = serviceId,
                        userId = userId,
                        vehicleId = vehicle.serverId ?: vehicle.id.toString(),
                        type = serviceType.name,
                        title = title.trim(),
                        serviceDate = serviceDateMs,
                        dueDate = dueDateMs,
                        cost = cost,
                        dueMileage = mileage,
                        status = if (isReminderEnabled) "PENDING" else "COMPLETED",
                        notes = encodedNotes
                    )
                )

                if (affectedRows == 0) {
                    throw IllegalStateException("بروزرسانی سرویس شکست خورد.")
                }

                // Handle Expense stable identity lifecycle
                val existingExpense = db.vehicleDao().getExpenseById(userId, "exp_svc_$serviceId")
                if (cost > 0L) {
                    val expEntity = VehicleExpenseRoomEntity(
                        id = "exp_svc_$serviceId",
                        userId = userId,
                        vehicleId = vehicle.serverId ?: vehicle.id.toString(),
                        title = title.trim(),
                        category = VehicleExpenseCategory.SERVICE.name,
                        amount = cost,
                        date = date.trim(),
                        description = "سرویس دوره‌ای خودرو: ${description.trim()}"
                    )
                    if (existingExpense != null) {
                        db.vehicleDao().updateExpense(expEntity)
                    } else {
                        db.vehicleDao().insertExpense(expEntity)
                    }
                } else {
                    db.vehicleDao().deleteExpenseById(userId, "exp_svc_$serviceId")
                }

                // Update Vehicle Mileage if higher
                if (mileage > vehicle.currentMileage) {
                    db.vehicleDao().updateMileage(userId, vehicle.serverId ?: vehicle.id.toString(), mileage)
                }
            }

            // Sync Notification Reminder safely without duplicates
            if (isReminderEnabled && !nextReminderDate.isNullOrBlank()) {
                val syncResult = syncReminderInternal(
                    context = context,
                    userId = userId,
                    id = serviceId,
                    title = "یادآور سرویس: ${vehicle.brand} ${vehicle.model}",
                    description = "موعد تعویض و سرویس دوره‌ای: $title (${description.trim()})",
                    date = nextReminderDate.trim(),
                    type = "VEHICLE"
                )
                if (syncResult.isFailure) {
                    return@withContext Result.failure(syncResult.exceptionOrNull() ?: Exception("خطا در همگام‌سازی یادآور"))
                }
            } else {
                val reminderManager = com.example.reminder.domain.ReminderManager(context)
                val reminderDb = com.example.data.database.AppDatabase.getDatabase(context)
                val existingReminder = reminderDb.smartReminderDao().getReminderById(userId, serviceId)
                if (existingReminder != null) {
                    reminderManager.deleteReminder(serviceId)
                }
            }

            Result.success(updatedService)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 6. Complete Service (Atomic Status update with proper reminder sync & affected rows check)
    suspend fun completeService(serviceId: String, completedDate: String): Result<Unit> = withContext(Dispatchers.IO) {
        val context = dbContext ?: return@withContext Result.failure(IllegalStateException("دسترسی به پایگاه داده مقداردهی نشده است."))
        val userId = SessionManager.userId ?: return@withContext Result.failure(IllegalStateException("کاربر احراز هویت نشده است."))

        val completedDateMs = parseJalaliToTimestamp(completedDate)
            ?: return@withContext Result.failure(IllegalArgumentException("تاریخ انجام سرویس نامعتبر است."))

        try {
            val db = AppDatabase.getDatabase(context)
            val existing = db.vehicleDao().getServiceByServerId(userId, serviceId)
                ?: return@withContext Result.failure(NoSuchElementException("سرویس دوره‌ای یافت نشد."))

            val affectedRows = db.vehicleDao().completeService(userId, serviceId, completedDateMs)
            if (affectedRows == 0) {
                return@withContext Result.failure(IllegalStateException("ثبت اتمام سرویس انجام نشد."))
            }

            // Sync expense date to completed date
            val expense = db.vehicleDao().getExpenseById(userId, "exp_svc_$serviceId")
            if (expense != null) {
                db.vehicleDao().updateExpense(expense.copy(date = completedDate.trim()))
            }

            // Mark associated reminder as completed
            val reminderManager = com.example.reminder.domain.ReminderManager(context)
            val reminderDb = com.example.data.database.AppDatabase.getDatabase(context)
            val existingReminder = reminderDb.smartReminderDao().getReminderById(userId, serviceId)
            if (existingReminder != null) {
                reminderManager.completeReminder(serviceId)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 7. Delete Service (Atomic Service + linked Expense delete and reminder cancellation)
    suspend fun deleteService(serviceId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val context = dbContext ?: return@withContext Result.failure(IllegalStateException("دسترسی به پایگاه داده مقداردهی نشده است."))
        val userId = SessionManager.userId ?: return@withContext Result.failure(IllegalStateException("کاربر احراز هویت نشده است."))

        try {
            val db = AppDatabase.getDatabase(context)

            // Delete associated reminder
            val reminderManager = com.example.reminder.domain.ReminderManager(context)
            reminderManager.deleteReminder(serviceId)

            val affectedRows = db.withTransaction {
                val serviceAffected = db.vehicleDao().deleteServiceByServerId(userId, serviceId)
                db.vehicleDao().deleteExpenseById(userId, "exp_svc_$serviceId")
                serviceAffected
            }

            if (affectedRows == 0) {
                return@withContext Result.failure(IllegalStateException("هیچ سرویسی برای حذف یافت نشد."))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 8. Add Expense (Synchronous suspend contract)
    suspend fun addExpense(
        vehicleId: String,
        title: String,
        category: VehicleExpenseCategory,
        amount: Long,
        date: String,
        description: String = ""
    ): Result<VehicleExpenseEntity> = withContext(Dispatchers.IO) {
        val context = dbContext ?: return@withContext Result.failure(IllegalStateException("دسترسی به پایگاه داده مقداردهی نشده است."))
        val userId = SessionManager.userId ?: return@withContext Result.failure(IllegalStateException("کاربر احراز هویت نشده است."))

        if (title.isBlank()) return@withContext Result.failure(IllegalArgumentException("عنوان هزینه نمی‌تواند خالی باشد."))
        if (amount <= 0L) return@withContext Result.failure(IllegalArgumentException("مبلغ هزینه باید بزرگتر از صفر باشد."))

        val parsedMs = parseJalaliToTimestamp(date)
            ?: return@withContext Result.failure(IllegalArgumentException("تاریخ هزینه نامعتبر است."))

        try {
            val db = AppDatabase.getDatabase(context)
            val vehicle = db.vehicleDao().getVehicleByServerId(userId, vehicleId)
                ?: return@withContext Result.failure(NoSuchElementException("خودرو یافت نشد."))

            val expenseId = UUID.randomUUID().toString()
            val newExpense = VehicleExpenseEntity(
                id = expenseId,
                vehicleId = vehicleId,
                title = title.trim(),
                category = category,
                amount = amount,
                date = date.trim(),
                description = description.trim()
            )

            db.vehicleDao().insertExpense(
                VehicleExpenseRoomEntity(
                    id = expenseId,
                    userId = userId,
                    vehicleId = vehicleId,
                    title = title.trim(),
                    category = category.name,
                    amount = amount,
                    date = date.trim(),
                    description = description.trim()
                )
            )

            Result.success(newExpense)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 9. Delete Expense (Robust affected rows and ownership checks)
    suspend fun deleteExpense(expenseId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val context = dbContext ?: return@withContext Result.failure(IllegalStateException("دسترسی به پایگاه داده مقداردهی نشده است."))
        val userId = SessionManager.userId ?: return@withContext Result.failure(IllegalStateException("کاربر احراز هویت نشده است."))

        try {
            val db = AppDatabase.getDatabase(context)
            val affectedRows = db.vehicleDao().deleteExpenseById(userId, expenseId)
            if (affectedRows == 0) {
                return@withContext Result.failure(IllegalStateException("هیچ هزینه‌ای برای حذف یافت نشد."))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 10. Save Insurance (Atomic Insurance + Expense + Reminder sync with failure propagation)
    suspend fun saveInsurance(
        vehicleId: String,
        company: String,
        type: String,
        startDate: String,
        endDate: String,
        amount: Long,
        policyNumber: String
    ): Result<VehicleInsuranceEntity> = withContext(Dispatchers.IO) {
        val context = dbContext ?: return@withContext Result.failure(IllegalStateException("دسترسی به پایگاه داده مقداردهی نشده است."))
        val userId = SessionManager.userId ?: return@withContext Result.failure(IllegalStateException("کاربر احراز هویت نشده است."))

        if (company.isBlank()) return@withContext Result.failure(IllegalArgumentException("نام شرکت بیمه نمی‌تواند خالی باشد."))
        if (amount < 0L) return@withContext Result.failure(IllegalArgumentException("مبلغ حق بیمه نمی‌تواند منفی باشد."))

        val startMs = parseJalaliToTimestamp(startDate)
            ?: return@withContext Result.failure(IllegalArgumentException("تاریخ شروع بیمه‌نامه نامعتبر است."))
        val endMs = parseJalaliToTimestamp(endDate)
            ?: return@withContext Result.failure(IllegalArgumentException("تاریخ پایان بیمه‌نامه نامعتبر است."))

        if (endMs < startMs) {
            return@withContext Result.failure(IllegalArgumentException("تاریخ پایان بیمه نمی‌تواند قبل از تاریخ شروع باشد."))
        }

        try {
            val db = AppDatabase.getDatabase(context)
            val vehicle = db.vehicleDao().getVehicleByServerId(userId, vehicleId)
                ?: return@withContext Result.failure(NoSuchElementException("خودرو یافت نشد."))

            val insuranceId = UUID.randomUUID().toString()
            val newInsurance = VehicleInsuranceEntity(
                id = insuranceId,
                vehicleId = vehicleId,
                company = company.trim(),
                type = type.trim(),
                startDate = startDate.trim(),
                endDate = endDate.trim(),
                amount = amount,
                policyNumber = policyNumber.trim()
            )

            db.withTransaction {
                db.vehicleDao().insertInsurance(
                    VehicleInsuranceRoomEntity(
                        id = insuranceId,
                        userId = userId,
                        vehicleId = vehicleId,
                        company = company.trim(),
                        type = type.trim(),
                        startDate = startDate.trim(),
                        endDate = endDate.trim(),
                        amount = amount,
                        policyNumber = policyNumber.trim(),
                        reminderDays = newInsurance.reminderDays.joinToString(",")
                    )
                )

                if (amount > 0L) {
                    db.vehicleDao().insertExpense(
                        VehicleExpenseRoomEntity(
                            id = "exp_ins_$insuranceId",
                            userId = userId,
                            vehicleId = vehicleId,
                            title = "تمدید ${type.trim()} (${company.trim()})",
                            category = VehicleExpenseCategory.INSURANCE.name,
                            amount = amount,
                            date = startDate.trim(),
                            description = "شماره بیمه‌نامه: ${policyNumber.trim()}"
                        )
                    )
                }
            }

            // Sync Reminder for Insurance with failure propagation
            val syncResult = syncReminderInternal(
                context = context,
                userId = userId,
                id = insuranceId,
                title = "تمدید $type خودرو",
                description = "سررسید انقضای بیمه‌نامه برای خودرو ${vehicle.brand} ${vehicle.model}. شماره بیمه‌نامه: $policyNumber",
                date = endDate.trim(),
                type = "INSURANCE"
            )
            if (syncResult.isFailure) {
                return@withContext Result.failure(syncResult.exceptionOrNull() ?: Exception("خطا در همگام‌سازی یادآور"))
            }

            Result.success(newInsurance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 11. Renew Insurance (Affected rows validation, expense sync, reminder sync, ownership check)
    suspend fun renewInsurance(insuranceId: String, newEndDate: String): Result<Unit> = withContext(Dispatchers.IO) {
        val context = dbContext ?: return@withContext Result.failure(IllegalStateException("دسترسی به پایگاه داده مقداردهی نشده است."))
        val userId = SessionManager.userId ?: return@withContext Result.failure(IllegalStateException("کاربر احراز هویت نشده است."))

        val today = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis()).toFormattedDate()
        val endMs = parseJalaliToTimestamp(newEndDate)
            ?: return@withContext Result.failure(IllegalArgumentException("تاریخ پایان بیمه نامعتبر است."))

        try {
            val db = AppDatabase.getDatabase(context)
            // Check ownership and existence
            val existing = db.vehicleDao().getInsuranceById(userId, insuranceId)
                ?: return@withContext Result.failure(NoSuchElementException("بیمه‌نامه مورد نظر یافت نشد."))

            val startMs = parseJalaliToTimestamp(existing.startDate) ?: 0L
            if (endMs < startMs) {
                return@withContext Result.failure(IllegalArgumentException("تاریخ پایان جدید نمی‌تواند قبل از تاریخ شروع بیمه‌نامه باشد."))
            }

            val affectedRows = db.vehicleDao().renewInsurance(userId, insuranceId, today, newEndDate.trim())
            if (affectedRows == 0) {
                return@withContext Result.failure(IllegalStateException("بروزرسانی بیمه‌نامه شکست خورد."))
            }

            // Sync Expense associated with Insurance to today's date
            val expense = db.vehicleDao().getExpenseById(userId, "exp_ins_$insuranceId")
            if (expense != null) {
                db.vehicleDao().updateExpense(expense.copy(date = today))
            }

            // Sync Reminder for Insurance expiration
            val vehicle = db.vehicleDao().getVehicleByServerId(userId, existing.vehicleId)
            if (vehicle != null) {
                val syncResult = syncReminderInternal(
                    context = context,
                    userId = userId,
                    id = insuranceId,
                    title = "تمدید بیمه ${existing.type} خودرو",
                    description = "سررسید انقضای بیمه‌نامه برای خودرو ${vehicle.brand} ${vehicle.model}. شماره بیمه‌نامه: ${existing.policyNumber}",
                    date = newEndDate.trim(),
                    type = "INSURANCE"
                )
                if (syncResult.isFailure) {
                    return@withContext Result.failure(syncResult.exceptionOrNull() ?: Exception("خطا در همگام‌سازی یادآور"))
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 12. Delete Insurance (Removes associated reminders, checks affected rows and ownership)
    suspend fun deleteInsurance(insuranceId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val context = dbContext ?: return@withContext Result.failure(IllegalStateException("دسترسی به پایگاه داده مقداردهی نشده است."))
        val userId = SessionManager.userId ?: return@withContext Result.failure(IllegalStateException("کاربر احراز هویت نشده است."))

        try {
            val db = AppDatabase.getDatabase(context)

            // Delete associated reminder
            val reminderManager = com.example.reminder.domain.ReminderManager(context)
            reminderManager.deleteReminder(insuranceId)

            val affectedRows = db.withTransaction {
                val insAffected = db.vehicleDao().deleteInsuranceById(userId, insuranceId)
                db.vehicleDao().deleteExpenseById(userId, "exp_ins_$insuranceId")
                insAffected
            }

            if (affectedRows == 0) {
                return@withContext Result.failure(IllegalStateException("هیچ بیمه‌نامه‌ای برای حذف یافت نشد."))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 13. Save Inspection (Atomic Inspection + Expense + Reminder sync with failure propagation)
    suspend fun saveInspection(
        vehicleId: String,
        lastInspectionDate: String,
        expiryDate: String,
        cost: Long,
        status: String,
        centerName: String
    ): Result<VehicleInspectionEntity> = withContext(Dispatchers.IO) {
        val context = dbContext ?: return@withContext Result.failure(IllegalStateException("دسترسی به پایگاه داده مقداردهی نشده است."))
        val userId = SessionManager.userId ?: return@withContext Result.failure(IllegalStateException("کاربر احراز هویت نشده است."))

        if (cost < 0L) return@withContext Result.failure(IllegalArgumentException("هزینه معاینه فنی نمی‌تواند منفی باشد."))

        val lastMs = parseJalaliToTimestamp(lastInspectionDate)
            ?: return@withContext Result.failure(IllegalArgumentException("تاریخ آخرین معاینه فنی نامعتبر است."))
        val expMs = parseJalaliToTimestamp(expiryDate)
            ?: return@withContext Result.failure(IllegalArgumentException("تاریخ انقضای معاینه فنی نامعتبر است."))

        try {
            val db = AppDatabase.getDatabase(context)
            val vehicle = db.vehicleDao().getVehicleByServerId(userId, vehicleId)
                ?: return@withContext Result.failure(NoSuchElementException("خودرو یافت نشد."))

            val inspectionId = UUID.randomUUID().toString()
            val newInspection = VehicleInspectionEntity(
                id = inspectionId,
                vehicleId = vehicleId,
                lastInspectionDate = lastInspectionDate.trim(),
                expiryDate = expiryDate.trim(),
                cost = cost,
                status = status.trim(),
                centerName = centerName.trim()
            )

            db.withTransaction {
                db.vehicleDao().insertInspection(
                    VehicleInspectionRoomEntity(
                        id = inspectionId,
                        userId = userId,
                        vehicleId = vehicleId,
                        lastInspectionDate = lastInspectionDate.trim(),
                        expiryDate = expiryDate.trim(),
                        cost = cost,
                        status = status.trim(),
                        centerName = centerName.trim()
                    )
                )

                if (cost > 0L) {
                    db.vehicleDao().insertExpense(
                        VehicleExpenseRoomEntity(
                            id = "exp_insp_$inspectionId",
                            userId = userId,
                            vehicleId = vehicleId,
                            title = "معاینه فنی خودرو ($centerName)",
                            category = VehicleExpenseCategory.INSPECTION.name,
                            amount = cost,
                            date = lastInspectionDate.trim(),
                            description = "مرکز معاینه فنی: $centerName"
                        )
                    )
                }
            }

            // Sync Reminder for Inspection with failure propagation
            val syncResult = syncReminderInternal(
                context = context,
                userId = userId,
                id = inspectionId,
                title = "معاینه فنی خودرو",
                description = "موعد سررسید معاینه فنی برای خودرو ${vehicle.brand} ${vehicle.model}. مرکز $centerName",
                date = expiryDate.trim(),
                type = "VEHICLE"
            )
            if (syncResult.isFailure) {
                return@withContext Result.failure(syncResult.exceptionOrNull() ?: Exception("خطا در همگام‌سازی یادآور"))
            }

            Result.success(newInspection)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 14. Delete Inspection (Removes associated reminders, checks affected rows and ownership)
    suspend fun deleteInspection(inspectionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val context = dbContext ?: return@withContext Result.failure(IllegalStateException("دسترسی به پایگاه داده مقداردهی نشده است."))
        val userId = SessionManager.userId ?: return@withContext Result.failure(IllegalStateException("کاربر احراز هویت نشده است."))

        try {
            val db = AppDatabase.getDatabase(context)

            // Delete associated reminder
            val reminderManager = com.example.reminder.domain.ReminderManager(context)
            reminderManager.deleteReminder(inspectionId)

            val affectedRows = db.withTransaction {
                val inspAffected = db.vehicleDao().deleteInspectionById(userId, inspectionId)
                db.vehicleDao().deleteExpenseById(userId, "exp_insp_$inspectionId")
                inspAffected
            }

            if (affectedRows == 0) {
                return@withContext Result.failure(IllegalStateException("هیچ معاینه فنی برای حذف یافت نشد."))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
