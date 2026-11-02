package com.example.data.sms

import org.junit.Assert.assertNotNull
import org.junit.Test

class SmsPatternMatcherNormalizationTest {

    @Test
    fun matchesCompactMultilineBankMessageAcrossDigitAndLineEndingVariants() {
        val sample = """حساب4785914725
برداشت50,000
مانده7,963,304
05/08/11-06:53"""
        val pattern = SmsPattern(
            id = "compact",
            name = "نمونه",
            type = SmsPatternType.EXPENSE,
            sampleMessage = sample,
            slots = SmsPatternMatcher.inferVariableSlots(sample)
        )
        val actual = "حساب4785914725\r\nبرداشت۵۰,۰۰۰\r\nمانده۷,۹۶۳,۳۰۴\r\n05/08/12-07:14"

        assertNotNull(SmsPatternMatcher.findUniqueMatch(actual, listOf(pattern)))
        assertNotNull(SmsPatternMatcher.extractMatchedFields(actual, pattern))
    }

    @Test
    fun matchesArabicAndPersianLetterVariantsWithoutChangingTemplateOffsets() {
        val sample = "موجودی 100 تومان"
        val pattern = SmsPattern(
            id = "letters",
            name = "نمونه",
            type = SmsPatternType.INCOME,
            sampleMessage = sample,
            slots = listOf(
                SmsPatternSlot(7, 10, SmsPatternSlotKind.AMOUNT_TOMAN)
            )
        )

        assertNotNull(SmsPatternMatcher.findUniqueMatch("موجودي ۲۰۰ تومان", listOf(pattern)))
    }
}
