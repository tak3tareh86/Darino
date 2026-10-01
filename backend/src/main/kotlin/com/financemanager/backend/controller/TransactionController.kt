package com.financemanager.backend.controller

import com.financemanager.backend.common.ApiResponse
import com.financemanager.backend.domain.Transaction
import com.financemanager.backend.security.UserPrincipal
import com.financemanager.backend.service.TransactionService
import com.financemanager.backend.service.UpsertTransactionCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.Instant

data class TransactionRequest(
    @field:NotBlank @field:Size(max = 100) val clientId: String,
    @field:NotNull val amount: Long,
    @field:NotBlank @field:Size(max = 30) val type: String,
    @field:NotBlank @field:Size(max = 150) val category: String,
    @field:NotBlank @field:Size(max = 150) val accountName: String,
    @field:Size(max = 10000) val description: String = "",
    @field:NotNull val occurredAt: Instant,
    @field:Size(max = 50) val timeFormatted: String = "",
    @field:Size(max = 255) val title: String = "",
    @field:Size(max = 150) val subCategory: String? = null,
    @field:Size(max = 50) val datePersian: String = "",
    @field:Size(max = 50) val paymentMethod: String = "BANK_CARD",
    @field:Size(max = 50) val sourceType: String = "MANUAL",
    @field:Size(max = 100) val sourceId: String? = null,
    val isRecurring: Boolean = false
)

@RestController
@RequestMapping("/api/v1/transactions")
class TransactionController(private val service: TransactionService) {
    @GetMapping
    fun all(@AuthenticationPrincipal p: UserPrincipal) =
        ResponseEntity.ok(ApiResponse.success(service.getAll(p.id)))

    @GetMapping("/{id}")
    fun one(@AuthenticationPrincipal p: UserPrincipal, @PathVariable id: String): ResponseEntity<ApiResponse<Transaction>> {
        val transaction = service.getById(id, p.id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(ApiResponse.success(transaction))
    }

    @PostMapping
    fun create(@AuthenticationPrincipal p: UserPrincipal, @Valid @RequestBody r: TransactionRequest) =
        ResponseEntity.ok(ApiResponse.success(service.save(p.id, r.toCommand()), "تراکنش ثبت شد"))

    @PutMapping("/{id}")
    fun update(@AuthenticationPrincipal p: UserPrincipal, @PathVariable id: String, @Valid @RequestBody r: TransactionRequest): ResponseEntity<ApiResponse<Transaction?>> {
        val current = service.getById(id, p.id) ?: return ResponseEntity.notFound().build()
        if (current.clientId != r.clientId) return ResponseEntity.badRequest().body(ApiResponse.success(null, "clientId نامعتبر است"))
        return ResponseEntity.ok(ApiResponse.success(service.save(p.id, r.toCommand()), "تراکنش به‌روزرسانی شد"))
    }

    @DeleteMapping("/{id}")
    fun remove(@AuthenticationPrincipal p: UserPrincipal, @PathVariable id: String): ResponseEntity<ApiResponse<Unit>> {
        if (!service.tombstone(id, p.id)) return ResponseEntity.notFound().build()
        return ResponseEntity.ok(ApiResponse.successMessage("تراکنش حذف شد"))
    }
}

private fun TransactionRequest.toCommand() = UpsertTransactionCommand(
    clientId, amount, type, category, accountName, description, occurredAt, timeFormatted,
    title, subCategory, datePersian, paymentMethod, sourceType, sourceId, isRecurring
)
