package com.financemanager.backend

import com.financemanager.backend.util.IranianOperator
import com.financemanager.backend.util.IranianPhoneNormalizer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class IranianPhoneNormalizerTest {

    @Test
    fun `should normalize various Iranian mobile formats to E164`() {
        assertEquals("+989121234567", IranianPhoneNormalizer.normalize("09121234567"))
        assertEquals("+989121234567", IranianPhoneNormalizer.normalize("00989121234567"))
        assertEquals("+989121234567", IranianPhoneNormalizer.normalize("989121234567"))
        assertEquals("+989121234567", IranianPhoneNormalizer.normalize("+989121234567"))
        assertEquals("+989121234567", IranianPhoneNormalizer.normalize("9121234567"))
    }

    @Test
    fun `should normalize Persian digits correctly`() {
        assertEquals("+989121234567", IranianPhoneNormalizer.normalize("۰۹۱۲۱۲۳۴۵۶۷"))
        assertEquals("+989351234567", IranianPhoneNormalizer.normalize("۰۹۳۵۱۲۳۴۵۶۷"))
    }

    @Test
    fun `should reject invalid Iranian phone numbers`() {
        assertNull(IranianPhoneNormalizer.normalize("02188776655")) // Landline
        assertNull(IranianPhoneNormalizer.normalize("12345"))
        assertNull(IranianPhoneNormalizer.normalize("invalid_phone"))
        assertNull(IranianPhoneNormalizer.normalize("+14155552671")) // US Number
    }

    @Test
    fun `should mask phone numbers properly`() {
        assertEquals("0912••••4567", IranianPhoneNormalizer.mask("+989121234567"))
        assertEquals("0935••••8901", IranianPhoneNormalizer.mask("09351238901"))
    }

    @Test
    fun `should detect correct Iranian mobile operator`() {
        assertEquals(IranianOperator.MCI, IranianPhoneNormalizer.detectOperator("09121234567"))
        assertEquals(IranianOperator.IRANCELL, IranianPhoneNormalizer.detectOperator("09351234567"))
        assertEquals(IranianOperator.RIGHTEL, IranianPhoneNormalizer.detectOperator("09211234567"))
        assertEquals(IranianOperator.SHATEL_MOBILE, IranianPhoneNormalizer.detectOperator("09981001234"))
    }
}
