package com.financemanager.backend.controller

import com.financemanager.backend.common.ApiResponse
import com.financemanager.backend.controller.dto.*
import com.financemanager.backend.domain.Reminder
import com.financemanager.backend.domain.ReminderSchedule
import com.financemanager.backend.domain.SmsJob
import com.financemanager.backend.security.UserPrincipal
import com.financemanager.backend.service.CreateReminderCommand
import com.financemanager.backend.service.CreateScheduleCommand
import com.financemanager.backend.service.ReminderService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/reminders")
@Tag(name = "Reminders", description = "مدیریت یادآورهای عمومی، اقساط و خودرو همراه با زمان‌بندی چندگانه و وضعیت پیامک")
class ReminderController(
    private val reminderService: ReminderService
) {

    @GetMapping
    @Operation(summary = "دریافت لیست تمام یادآورهای کاربر جاری")
    fun getAllReminders(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<ApiResponse<List<Reminder>>> {
        val reminders = reminderService.getUserReminders(principal.id)
        return ResponseEntity.ok(ApiResponse.success(reminders))
    }

    @PostMapping
    @Operation(summary = "ثبت یادآور جدید با زمان‌بندی چندگانه و پیامک")
    fun createReminder(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: ReminderRequest
    ): ResponseEntity<ApiResponse<Reminder>> {
        val schedules = request.schedules.map {
            CreateScheduleCommand(it.triggerType, it.offsetValue, it.offsetUnit)
        }
        val command = CreateReminderCommand(
            type = request.type,
            title = request.title,
            description = request.description,
            dueAt = request.dueAt,
            timezone = request.timezone,
            priority = request.priority,
            notificationEnabled = request.notificationEnabled,
            smsEnabled = request.smsEnabled,
            phoneNumber = request.phoneNumber,
            repeatRule = request.repeatRule,
            schedules = schedules
        )
        val reminder = reminderService.createReminder(principal.id, command)
        return ResponseEntity.ok(ApiResponse.success(reminder, "یادآور با موفقیت ثبت شد"))
    }

    @GetMapping("/{id}")
    @Operation(summary = "دریافت جزئیات یادآور بر اساس شناسه")
    fun getReminderById(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String
    ): ResponseEntity<ApiResponse<Reminder>> {
        val reminder = reminderService.getReminderById(id, principal.id)
        return ResponseEntity.ok(ApiResponse.success(reminder))
    }

    @PutMapping("/{id}")
    @Operation(summary = "ویرایش یادآور")
    fun updateReminder(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String,
        @Valid @RequestBody request: ReminderRequest
    ): ResponseEntity<ApiResponse<Reminder>> {
        val schedules = request.schedules.map {
            CreateScheduleCommand(it.triggerType, it.offsetValue, it.offsetUnit)
        }
        val command = CreateReminderCommand(
            type = request.type,
            title = request.title,
            description = request.description,
            dueAt = request.dueAt,
            timezone = request.timezone,
            priority = request.priority,
            notificationEnabled = request.notificationEnabled,
            smsEnabled = request.smsEnabled,
            phoneNumber = request.phoneNumber,
            repeatRule = request.repeatRule,
            schedules = schedules
        )
        val reminder = reminderService.updateReminder(id, principal.id, command)
        return ResponseEntity.ok(ApiResponse.success(reminder, "یادآور به‌روزرسانی شد"))
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "حذف یادآور")
    fun deleteReminder(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String
    ): ResponseEntity<ApiResponse<Unit>> {
        reminderService.deleteReminder(id, principal.id)
        return ResponseEntity.ok(ApiResponse.successMessage("یادآور با موفقیت حذف شد"))
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "تکمیل یادآور")
    fun markComplete(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String
    ): ResponseEntity<ApiResponse<Reminder>> {
        val reminder = reminderService.markComplete(id, principal.id)
        return ResponseEntity.ok(ApiResponse.success(reminder, "یادآور تکمیل شد"))
    }

    @PostMapping("/{id}/snooze")
    @Operation(summary = "به تعویق انداختن یادآور (اسنوز)")
    fun snoozeReminder(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String,
        @RequestBody(required = false) snoozeRequest: SnoozeRequest?
    ): ResponseEntity<ApiResponse<Reminder>> {
        val minutes = snoozeRequest?.snoozeMinutes ?: 60L
        val reminder = reminderService.snooze(id, principal.id, minutes)
        return ResponseEntity.ok(ApiResponse.success(reminder, "یادآور به تعویق افتاد"))
    }

    @PostMapping("/{id}/enable")
    @Operation(summary = "فعال‌سازی یادآور")
    fun enableReminder(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String
    ): ResponseEntity<ApiResponse<Reminder>> {
        val reminder = reminderService.toggleStatus(id, principal.id, true)
        return ResponseEntity.ok(ApiResponse.success(reminder, "یادآور فعال شد"))
    }

    @PostMapping("/{id}/disable")
    @Operation(summary = "غیرفعال‌سازی یادآور")
    fun disableReminder(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String
    ): ResponseEntity<ApiResponse<Reminder>> {
        val reminder = reminderService.toggleStatus(id, principal.id, false)
        return ResponseEntity.ok(ApiResponse.success(reminder, "یادآور غیرفعال شد"))
    }

    @GetMapping("/{id}/sms-status")
    @Operation(summary = "مشاهده وضعیت پیامک‌های ارسال شده برای این یادآور")
    fun getSmsStatus(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String
    ): ResponseEntity<ApiResponse<List<SmsJob>>> {
        val jobs = reminderService.getSmsStatusForReminder(id, principal.id)
        return ResponseEntity.ok(ApiResponse.success(jobs))
    }
}
