package com.example

import com.example.util.IranianPhoneUtils
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testIranianPhoneNormalization() {
        assertEquals("09123456789", IranianPhoneUtils.normalizeIranianPhoneNumber("09123456789"))
        assertEquals("09123456789", IranianPhoneUtils.normalizeIranianPhoneNumber("+989123456789"))
        assertEquals("09123456789", IranianPhoneUtils.normalizeIranianPhoneNumber("00989123456789"))
        assertEquals("09123456789", IranianPhoneUtils.normalizeIranianPhoneNumber("9123456789"))
        // Persian digits conversion check
        assertEquals("09123456789", IranianPhoneUtils.normalizeIranianPhoneNumber("۰۹۱۲۳۴۵۶۷۸۹"))
    }

    @Test
    fun testIranianPhoneValidation() {
        assertTrue(IranianPhoneUtils.isValidIranianMobile("09123456789"))
        assertTrue(IranianPhoneUtils.isValidIranianMobile("09351234567"))
        assertTrue(IranianPhoneUtils.isValidIranianMobile("09211234567"))
        assertFalse(IranianPhoneUtils.isValidIranianMobile("02112345678"))
        assertFalse(IranianPhoneUtils.isValidIranianMobile("0912345"))
        assertFalse(IranianPhoneUtils.isValidIranianMobile("abcdef"))
    }

    @Test
    fun testOperatorDetection() {
        assertTrue(IranianPhoneUtils.getOperatorName("09121234567").contains("همراه اول"))
        assertTrue(IranianPhoneUtils.getOperatorName("09351234567").contains("ایرانسل"))
        assertTrue(IranianPhoneUtils.getOperatorName("09211234567").contains("رایتل"))
        assertTrue(IranianPhoneUtils.getOperatorName("09981234567").contains("شاتل"))
    }

    @Test
    fun testPhoneMasking() {
        val masked = IranianPhoneUtils.maskPhoneNumber("09123456789")
        assertEquals("0912••••6789", masked)
    }

    @Test
    fun testPersianDigitConversion() {
        val persian = IranianPhoneUtils.convertDigitsToPersian("123450")
        assertEquals("۱۲۳۴۵۰", persian)
        val english = IranianPhoneUtils.convertDigitsToEnglish("۱۲۳۴۵۰")
        assertEquals("123450", english)
    }
}
