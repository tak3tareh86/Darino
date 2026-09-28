package com.example.backend

import com.example.backend.exception.ApiException
import com.example.backend.service.phone.PhoneNormalizationService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PhoneNormalizationTest {

    private val normalizationService = PhoneNormalizationService()

    @Test
    fun testNormalizeIranianNumbers() {
        // Standard Iranian 09xx
        assertEquals("+989121234567", normalizationService.normalizeIranianPhoneNumber("09121234567"))
        
        // 00989xx
        assertEquals("+989121234567", normalizationService.normalizeIranianPhoneNumber("00989121234567"))
        
        // +989xx
        assertEquals("+989121234567", normalizationService.normalizeIranianPhoneNumber("+989121234567"))
        
        // 9121234567
        assertEquals("+989121234567", normalizationService.normalizeIranianPhoneNumber("9121234567"))
    }

    @Test
    fun testInvalidNumbersThrowException() {
        assertThrows(ApiException::class.java) {
            normalizationService.normalizeIranianPhoneNumber("098765") // Too short
        }

        assertThrows(ApiException::class.java) {
            normalizationService.normalizeIranianPhoneNumber("02188776655") // Landline
        }

        assertThrows(ApiException::class.java) {
            normalizationService.normalizeIranianPhoneNumber("09812345678") // Invalid operator prefix 98
        }
    }

    @Test
    fun testMasking() {
        assertEquals("0912••••4567", normalizationService.maskPhoneNumber("+989121234567"))
        assertEquals("0935••••7890", normalizationService.maskPhoneNumber("09351237890"))
    }
}
