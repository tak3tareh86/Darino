package com.financemanager.backend.controller

import com.financemanager.backend.common.ApiResponse
import com.financemanager.backend.security.UserPrincipal
import com.financemanager.backend.service.BazaarSubscriptionService
import com.financemanager.backend.service.BazaarSubscriptionSnapshot
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

data class VerifyBazaarSubscriptionRequest(
    @field:NotBlank val productId: String,
    @field:NotBlank val purchaseToken: String
)

@RestController
@RequestMapping("/api/v1/subscriptions")
class BazaarSubscriptionController(
    private val service: BazaarSubscriptionService
) {
    @PostMapping("/verify")
    fun verify(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: VerifyBazaarSubscriptionRequest
    ): ResponseEntity<ApiResponse<BazaarSubscriptionSnapshot>> =
        ResponseEntity.ok(ApiResponse.success(
            service.verifyAndSave(principal.id, request.productId, request.purchaseToken),
            "اشتراک توسط کافه‌بازار اعتبارسنجی شد"
        ))

    @GetMapping("/current")
    fun current(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<BazaarSubscriptionSnapshot?>> =
        ResponseEntity.ok(ApiResponse.success(service.getCurrent(principal.id)))
}
