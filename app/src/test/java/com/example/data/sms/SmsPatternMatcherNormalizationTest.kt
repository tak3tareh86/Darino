package com.example.data.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    @Test
    fun configuredSenderIsMatchedExactlyAndShortJalaliYearUsesExplicitOrder() {
        val sample = """واریز 80,000 تومان
05/08/05-06:53
مانده: 1,000,000 تومان"""
        val slots = SmsPatternMatcher.inferVariableSlots(sample).map { slot ->
            if (slot.kind == SmsPatternSlotKind.DATE_YEAR_MONTH_DAY) {
                slot.copy(kind = SmsPatternSlotKind.DATE_YEAR_MONTH_DAY)
            } else slot
        }
        val pattern = SmsPattern(
            id = "user-defined-resalat",
            name = "رسالت - واریز",
            type = SmsPatternType.INCOME,
            sampleMessage = sample,
            slots = slots,
            bankName = "بانک قرض‌الحسنه رسالت",
            senderAddress = "ResalatBank",
            accountNumber = null,
            dateFormat = "YY/MM/DD",
            timeFormat = "HH:mm"
        )
        val actual = """واریز 1,250,000 تومان
05/08/06-07:14
مانده: 125,000,000 تومان"""

        assertEquals(1, SmsPatternMatcher.findMatchingPatterns(actual, listOf(pattern), "ResalatBank").size)
        assertTrue(SmsPatternMatcher.findMatchingPatterns(actual, listOf(pattern), "resalatbank").isEmpty())
        assertTrue(SmsPatternMatcher.findMatchingPatterns(actual, listOf(pattern), "Resalat Bank").isEmpty())
        assertTrue(SmsPatternMatcher.matchesTemplateBody(actual, pattern))

        val receivedAt = com.example.util.PersianCalendarHelper.jalaliToEpochMillis(1405, 8, 6, 8, 0)
        val parsed = BankSmsParser.parseMatchedPattern("sample", "ResalatBank", actual, receivedAt, pattern)
        assertNotNull(parsed)
        assertEquals("بانک قرض‌الحسنه رسالت", parsed?.bankName)
        assertEquals(1_250_000L, parsed?.amount)
        assertEquals(125_000_000L, parsed?.balanceAmount)
        assertEquals("1405/08/06", parsed?.dateText)
        assertEquals("07:14", parsed?.timeText)
        assertNull(BankSmsParser.parseMatchedPattern("wrong-sender", "resalatbank", actual, receivedAt, pattern))
    }

}
