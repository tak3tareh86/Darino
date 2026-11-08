package com.financemanager.backend.controller

import com.financemanager.backend.common.ApiResponse
import com.financemanager.backend.controller.dto.InstallmentRequest
import com.financemanager.backend.controller.dto.SmsWebhookPayload
import com.financemanager.backend.controller.dto.VehicleRequest
import com.financemanager.backend.domain.Installment
import com.financemanager.backend.domain.Notification
import com.financemanager.backend.domain.Vehicle
import com.financemanager.backend.security.UserPrincipal
import com.financemanager.backend.service.InstallmentService
import com.financemanager.backend.service.NotificationService
import com.financemanager.backend.service.SmsWebhookService
import com.financemanager.backend.service.VehicleService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "مرکز اعلان‌های درون برنامه‌ای")
class NotificationController(
    private val notificationService: NotificationService
) {

    @GetMapping
    @Operation(summary = "لیست اعلان‌های کاربر به همراه صفحه‌بندی")
    fun getNotifications(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<Page<Notification>>> {
        val notifications = notificationService.getUserNotifications(principal.id, page, size)
        return ResponseEntity.ok(ApiResponse.success(notifications))
    }

    @GetMapping("/unread-count")
    @Operation(summary = "تعداد اعلان‌های خوانده نشده")
    fun getUnreadCount(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<ApiResponse<Map<String, Long>>> {
        val count = notificationService.getUnreadCount(principal.id)
        return ResponseEntity.ok(ApiResponse.success(mapOf("unreadCount" to count)))
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "نشانه‌گذاری اعلان به عنوان خوانده شده")
    fun markAsRead(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String
    ): ResponseEntity<ApiResponse<Notification>> {
        val updated = notificationService.markAsRead(id, principal.id)
        return ResponseEntity.ok(ApiResponse.success(updated))
    }

    @PostMapping("/read-all")
    @Operation(summary = "نشانه‌گذاری تمام اعلان‌ها به عنوان خوانده شده")
    fun markAllAsRead(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<ApiResponse<Unit>> {
        notificationService.markAllAsRead(principal.id)
        return ResponseEntity.ok(ApiResponse.successMessage("تمام اعلان‌ها خوانده شدند"))
    }
}

@RestController
@RequestMapping("/api/v1/vehicles")
@Tag(name = "Vehicles", description = "مدیریت اطلاعات و سرویس‌های خودرو")
class VehicleController(
    private val vehicleService: VehicleService
) {
    @GetMapping
    @Operation(summary = "لیست خودروهای کاربر")
    fun getVehicles(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<ApiResponse<List<Vehicle>>> {
        return ResponseEntity.ok(ApiResponse.success(vehicleService.getUserVehicles(principal.id)))
    }

    @PostMapping
    @Operation(summary = "افزودن خودروی جدید و ایجاد خودکار یادآور سرویس")
    fun createVehicle(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody req: VehicleRequest
    ): ResponseEntity<ApiResponse<Vehicle>> {
        val vehicle = vehicleService.createVehicle(
            userId = principal.id,
            name = req.name,
            brand = req.brand,
            modelYear = req.modelYear,
            plateNumber = req.plateNumber,
            odometerKm = req.odometerKm,
            color = req.color
        )
        return ResponseEntity.ok(ApiResponse.success(vehicle, "خودرو با موفقیت ثبت شد"))
    }

    @PutMapping("/{id}")
    @Operation(summary = "ویرایش مشخصات خودرو")
    fun updateVehicle(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String,
        @Valid @RequestBody req: VehicleRequest
    ): ResponseEntity<ApiResponse<Vehicle>> {
        val vehicle = vehicleService.updateVehicle(
            id = id,
            userId = principal.id,
            name = req.name,
            brand = req.brand,
            modelYear = req.modelYear,
            plateNumber = req.plateNumber,
            odometerKm = req.odometerKm,
            color = req.color
        )
        return ResponseEntity.ok(ApiResponse.success(vehicle, "مشخصات خودرو به‌روزرسانی شد"))
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "حذف خودرو")
    fun deleteVehicle(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String
    ): ResponseEntity<ApiResponse<Unit>> {
        vehicleService.deleteVehicle(id, principal.id)
        return ResponseEntity.ok(ApiResponse.successMessage("خودرو با موفقیت حذف شد"))
    }
}

@RestController
@RequestMapping("/api/v1/installments")
@Tag(name = "Installments", description = "مدیریت اقساط، وام‌ها و یادآورهای سررسید پرداخت")
class InstallmentController(
    private val installmentService: InstallmentService
) {
    @GetMapping
    @Operation(summary = "لیست تمام اقساط کاربر")
    fun getInstallments(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<ApiResponse<List<Installment>>> {
        return ResponseEntity.ok(ApiResponse.success(installmentService.getUserInstallments(principal.id)))
    }

    @PostMapping
    @Operation(summary = "ثبت قسط جدید و ایجاد خودکار یادآور سررسید")
    fun createInstallment(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody req: InstallmentRequest
    ): ResponseEntity<ApiResponse<Installment>> {
        val installment = installmentService.createInstallment(
            userId = principal.id,
            title = req.title,
            providerName = req.providerName,
            category = req.category,
            totalAmount = req.totalAmount,
            monthlyPayment = req.monthlyPayment,
            totalInstallments = req.totalInstallments,
            paidInstallments = req.paidInstallments,
            dueDay = req.dueDay,
            startDate = req.startDate
        )
        return ResponseEntity.ok(ApiResponse.success(installment, "قسط جدید با موفقیت ثبت شد"))
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "ثبت پرداخت یک قسط")
    fun payInstallment(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String
    ): ResponseEntity<ApiResponse<Installment>> {
        val installment = installmentService.payInstallment(id, principal.id)
        return ResponseEntity.ok(ApiResponse.success(installment, "پرداخت قسط ثبت شد"))
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "حذف قسط")
    fun deleteInstallment(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String
    ): ResponseEntity<ApiResponse<Unit>> {
        installmentService.deleteInstallment(id, principal.id)
        return ResponseEntity.ok(ApiResponse.successMessage("قسط حذف شد"))
    }
}

@RestController
@RequestMapping("/api/v1/webhooks/sms")
@Tag(name = "SMS Webhook", description = "دریافت گزارش وضعیت تحویل پیامک از درگاه‌های پیامکی با اعتبارسنجی امضا")
class SmsWebhookController(
    private val smsWebhookService: SmsWebhookService
) {

    @PostMapping
    @Operation(summary = "وب‌هوک گزارش تحویل پیامک (Delivery Report Callback)")
    fun receiveDeliveryReport(
        @RequestParam(name = "messageid", required = false) messageIdParam: String?,
        @RequestParam(name = "status", required = false) statusParam: String?,
        @RequestBody(required = false) body: SmsWebhookPayload?,
        @RequestHeader(name = "X-Signature", required = false) signatureHeader: String?,
        request: HttpServletRequest
    ): ResponseEntity<Map<String, Any>> {
        val msgId = body?.messageId ?: messageIdParam ?: ""
        val status = body?.status ?: statusParam ?: "DELIVERED"
        val signature = body?.signature ?: signatureHeader

        val success = smsWebhookService.handleDeliveryReport(
            providerMessageId = msgId,
            status = status,
            signature = signature,
            rawPayload = request.queryString ?: (body?.toString() ?: "")
        )

        return ResponseEntity.ok(
            mapOf(
                "received" to true,
                "processed" to success,
                "messageId" to msgId
            )
        )
    }
}
