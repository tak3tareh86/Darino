package com.financemanager.backend.service

import com.financemanager.backend.common.AppException
import com.financemanager.backend.common.ErrorCode
import com.financemanager.backend.domain.*
import com.financemanager.backend.repository.InstallmentRepository
import com.financemanager.backend.repository.SmsJobRepository
import com.financemanager.backend.repository.SmsLogRepository
import com.financemanager.backend.repository.VehicleRepository
import com.financemanager.backend.sms.provider.SmsProviderFactory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Service
class VehicleService(
    private val vehicleRepository: VehicleRepository,
    private val reminderService: ReminderService
) {
    fun getUserVehicles(userId: String): List<Vehicle> = vehicleRepository.findAllByUserId(userId)

    fun getVehicleById(id: String, userId: String): Vehicle =
        vehicleRepository.findByIdAndUserId(id, userId)
            .orElseThrow { AppException(ErrorCode.NOT_FOUND, "خودرو با شناسه $id یافت نشد.") }

    @Transactional
    fun createVehicle(
        userId: String,
        name: String,
        brand: String,
        modelYear: String?,
        plateNumber: String?,
        odometerKm: Long,
        color: String?
    ): Vehicle {
        val vehicle = Vehicle(
            userId = userId,
            name = name,
            brand = brand,
            modelYear = modelYear,
            plateNumber = plateNumber,
            odometerKm = odometerKm,
            color = color
        )
        val saved = vehicleRepository.save(vehicle)

        // Auto-create initial periodic maintenance reminder (e.g., 3 months from now)
        val nextMaintenance = Instant.now().plus(java.time.Duration.ofDays(90))
        reminderService.createReminder(
            userId = userId,
            command = CreateReminderCommand(
                type = ReminderType.VEHICLE,
                title = "سرویس دوره‌ای $name ($brand)",
                description = "بررسی روغن موتور، فیلتر هوا و لنت‌های ترمز",
                dueAt = nextMaintenance,
                sourceType = "VEHICLE",
                sourceId = saved.id,
                notificationEnabled = true,
                smsEnabled = false,
                schedules = listOf(
                    CreateScheduleCommand(TriggerType.BEFORE_DUE, 3, OffsetUnit.DAYS),
                    CreateScheduleCommand(TriggerType.EXACT_TIME, 0, OffsetUnit.MINUTES)
                )
            )
        )

        return saved
    }

    @Transactional
    fun updateVehicle(
        id: String,
        userId: String,
        name: String,
        brand: String,
        modelYear: String?,
        plateNumber: String?,
        odometerKm: Long,
        color: String?
    ): Vehicle {
        val vehicle = getVehicleById(id, userId)
        vehicle.name = name
        vehicle.brand = brand
        vehicle.modelYear = modelYear
        vehicle.plateNumber = plateNumber
        vehicle.odometerKm = odometerKm
        vehicle.color = color
        vehicle.updatedAt = Instant.now()
        return vehicleRepository.save(vehicle)
    }

    @Transactional
    fun deleteVehicle(id: String, userId: String) {
        val vehicle = getVehicleById(id, userId)
        vehicleRepository.delete(vehicle)
    }
}

@Service
class InstallmentService(
    private val installmentRepository: InstallmentRepository,
    private val reminderService: ReminderService
) {
    fun getUserInstallments(userId: String): List<Installment> =
        installmentRepository.findAllByUserId(userId)

    fun getInstallmentById(id: String, userId: String): Installment =
        installmentRepository.findByIdAndUserId(id, userId)
            .orElseThrow { AppException(ErrorCode.NOT_FOUND, "قسط با شناسه $id یافت نشد.") }

    @Transactional
    fun createInstallment(
        userId: String,
        title: String,
        providerName: String,
        category: String,
        totalAmount: BigDecimal,
        monthlyPayment: BigDecimal,
        totalInstallments: Int,
        paidInstallments: Int,
        dueDay: Int,
        startDate: LocalDate
    ): Installment {
        val installment = Installment(
            userId = userId,
            title = title,
            providerName = providerName,
            category = category,
            totalAmount = totalAmount,
            monthlyPayment = monthlyPayment,
            totalInstallments = totalInstallments,
            paidInstallments = paidInstallments,
            dueDay = dueDay,
            startDate = startDate
        )
        val saved = installmentRepository.save(installment)

        // Automatically schedule next due installment reminder
        val nextDueLocalDate = LocalDate.now().withDayOfMonth(dueDay.coerceIn(1, 28))
        val dueInstant = nextDueLocalDate.atStartOfDay(ZoneId.of("Asia/Tehran")).toInstant()

        reminderService.createReminder(
            userId = userId,
            command = CreateReminderCommand(
                type = ReminderType.INSTALLMENT,
                title = "سررسید قسط $title ($providerName)",
                description = "مبلغ قسط: ${monthlyPayment.toPlainString()} تومان - قسط ${paidInstallments + 1} از $totalInstallments",
                dueAt = dueInstant,
                sourceType = "INSTALLMENT",
                sourceId = saved.id,
                notificationEnabled = true,
                smsEnabled = false,
                schedules = listOf(
                    CreateScheduleCommand(TriggerType.BEFORE_DUE, 2, OffsetUnit.DAYS),
                    CreateScheduleCommand(TriggerType.EXACT_TIME, 0, OffsetUnit.MINUTES)
                )
            )
        )

        return saved
    }

    @Transactional
    fun payInstallment(id: String, userId: String): Installment {
        val installment = getInstallmentById(id, userId)
        if (installment.paidInstallments < installment.totalInstallments) {
            installment.paidInstallments += 1
            installment.updatedAt = Instant.now()
        }
        return installmentRepository.save(installment)
    }

    @Transactional
    fun deleteInstallment(id: String, userId: String) {
        val installment = getInstallmentById(id, userId)
        installmentRepository.delete(installment)
    }
}

@Service
class SmsWebhookService(
    private val smsJobRepository: SmsJobRepository,
    private val smsLogRepository: SmsLogRepository,
    private val smsProviderFactory: SmsProviderFactory
) {
    private val logger = LoggerFactory.getLogger(SmsWebhookService::class.java)

    /**
     * Handles real delivery callbacks from SMS Gateways (Kavenegar/Faraz/etc).
     * Validates webhook signature, performs idempotent status transition to DELIVERED/FAILED.
     */
    @Transactional
    fun handleDeliveryReport(
        providerMessageId: String,
        status: String,
        signature: String?,
        rawPayload: String
    ): Boolean {
        val provider = smsProviderFactory.getActiveProvider()
        if (!provider.verifyWebhookSignature(rawPayload, signature)) {
            logger.warn("SMS Webhook signature verification failed for provider {}", provider.providerName)
            return false
        }

        val jobOpt = smsJobRepository.findByProviderMessageId(providerMessageId)
        if (jobOpt.isEmpty) {
            logger.warn("Webhook received for unknown providerMessageId: {}", providerMessageId)
            return false
        }

        val job = jobOpt.get()
        val normalizedStatus = status.uppercase()

        logger.info("SMS Delivery Webhook received: ID={}, Status={}", providerMessageId, normalizedStatus)

        when (normalizedStatus) {
            "DELIVERED", "10", "SUCCESS" -> {
                job.status = SmsJobStatus.DELIVERED
                job.deliveredAt = Instant.now()
            }
            "FAILED", "UNDELIVERED", "100" -> {
                job.status = SmsJobStatus.FAILED
                job.failedAt = Instant.now()
                job.failureReason = "Provider Webhook reported delivery failure: $status"
            }
            "SENT", "4", "5" -> {
                job.status = SmsJobStatus.SENT
                job.sentAt = Instant.now()
            }
        }
        smsJobRepository.save(job)

        // Update corresponding SmsLog
        smsLogRepository.findByProviderMessageId(providerMessageId).ifPresent { log ->
            log.status = job.status.name
            log.updatedAt = Instant.now()
            smsLogRepository.save(log)
        }

        return true
    }
}
