package com.example.data.sms

import com.example.ui.screens.finance.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BankSmsParserTest {

    private fun slot(sample: String, value: String, kind: SmsPatternSlotKind): SmsPatternSlot {
        val start = sample.indexOf(value)
        require(start >= 0)
        return SmsPatternSlot(start, start + value.length, kind)
    }

    @Test
    fun onlyConfiguredSlotsCanChangeAndAllOtherTextRemainsLiteral() {
        val sample = "برداشت: 1,000 ریال | مانده: 2,000 ریال"
        val pattern = SmsPattern(
            id = "p1",
            name = "قالب آزمایشی",
            type = SmsPatternType.EXPENSE,
            sampleMessage = sample,
            slots = listOf(
                slot(sample, "1,000", SmsPatternSlotKind.AMOUNT_RIAL),
                slot(sample, "2,000", SmsPatternSlotKind.BALANCE_RIAL)
            )
        )

        assertNotNull(SmsPatternMatcher.findUniqueMatch("برداشت: 1,250 ریال | مانده: 3,500 ریال", listOf(pattern)))
        assertNull(SmsPatternMatcher.findUniqueMatch("واریز: 1,250 ریال | مانده: 3,500 ریال", listOf(pattern)))
        assertNull(SmsPatternMatcher.findUniqueMatch("برداشت: 1,250 تومان | مانده: 3,500 ریال", listOf(pattern)))
    }

    @Test
    fun configuredTypeIsAuthoritativeAndAmountComesOnlyFromItsSlot() {
        val sample = "عبارت ثابت: 1,000 ریال"
        val pattern = SmsPattern(
            id = "p2",
            name = "قالب ورودی",
            type = SmsPatternType.INCOME,
            sampleMessage = sample,
            slots = listOf(slot(sample, "1,000", SmsPatternSlotKind.AMOUNT_RIAL))
        )

        val parsed = BankSmsParser.parseMatchedPattern(
            "sms1", "unknown-sender", "عبارت ثابت: 2,500 ریال", 1_700_000_000_000L, pattern
        )

        assertEquals(TransactionType.INCOME, parsed?.type)
        assertEquals(250L, parsed?.amount)
        assertEquals(true, parsed?.isAmountValid)
    }

    @Test
    fun missingOrInvalidAmountSlotNeverProducesSuggestion() {
        val sample = "تراکنش 1,000 ریال"
        val noAmountPattern = SmsPattern(
            id = "p3",
            name = "بدون مبلغ",
            type = SmsPatternType.EXPENSE,
            sampleMessage = sample,
            slots = emptyList()
        )
        assertNull(SmsPatternMatcher.findUniqueMatch("تراکنش 2,000 ریال", listOf(noAmountPattern)))

        val amountPattern = noAmountPattern.copy(
            slots = listOf(slot(sample, "1,000", SmsPatternSlotKind.AMOUNT_RIAL))
        )
        assertNull(
            BankSmsParser.parseMatchedPattern(
                "sms2", "unknown", "تراکنش 0 ریال", 1_700_000_000_000L, amountPattern
            )
        )
    }

    @Test
    fun selectedDateTimeAndAccountSlotsAreExtractedFromTheirExactPositions() {
        val sample = "حساب 123456 مبلغ 1,000 تومان تاریخ 1405/08/09 ساعت 18:09"
        val pattern = SmsPattern(
            id = "p4",
            name = "قالب بانک",
            type = SmsPatternType.EXPENSE,
            sampleMessage = sample,
            slots = listOf(
                slot(sample, "123456", SmsPatternSlotKind.SOURCE_ACCOUNT),
                slot(sample, "1,000", SmsPatternSlotKind.AMOUNT_TOMAN),
                slot(sample, "1405/08/09", SmsPatternSlotKind.DATE),
                slot(sample, "18:09", SmsPatternSlotKind.TIME)
            )
        )

        val parsed = BankSmsParser.parseMatchedPattern(
            "sms3",
            "unknown",
            "حساب 998877 مبلغ 2,500 تومان تاریخ 1405/08/10 ساعت 19:10",
            1_700_000_000_000L,
            pattern
        )

        assertEquals(2_500L, parsed?.amount)
        assertEquals("حساب 998877", parsed?.sourceAccount)
        assertEquals("1405/08/10", parsed?.dateText)
        assertEquals("19:10", parsed?.timeText)
    }

    @Test
    fun shortDateOrderMustBeExplicitlySelected() {
        val sample = "تاریخ 08/01 مبلغ 100 تومان"
        val pattern = SmsPattern(
            id = "p5",
            name = "تاریخ روز ماه",
            type = SmsPatternType.INCOME,
            sampleMessage = sample,
            slots = listOf(
                slot(sample, "08/01", SmsPatternSlotKind.DATE_DAY_MONTH),
                slot(sample, "100", SmsPatternSlotKind.AMOUNT_TOMAN)
            )
        )
        val parsed = BankSmsParser.parseMatchedPattern(
            "sms4", "unknown", "تاریخ 09/02 مبلغ 200 تومان", 1_700_000_000_000L, pattern
        )
        assertEquals("1402/02/09", parsed?.dateText)
        assertEquals(200L, parsed?.amount)
    }

    @Test
    fun fixedTextIsCaseAndWhitespaceSensitive() {
        val sample = "PAYMENT 1,000 OK"
        val pattern = SmsPattern(
            id = "strict",
            name = "Strict",
            type = SmsPatternType.EXPENSE,
            sampleMessage = sample,
            slots = listOf(slot(sample, "1,000", SmsPatternSlotKind.AMOUNT_TOMAN))
        )

        assertNotNull(SmsPatternMatcher.findUniqueMatch("PAYMENT 2,000 OK", listOf(pattern)))
        assertNull(SmsPatternMatcher.findUniqueMatch("payment 2,000 OK", listOf(pattern)))
        assertNull(SmsPatternMatcher.findUniqueMatch("PAYMENT  2,000 OK", listOf(pattern)))
        assertNull(SmsPatternMatcher.findUniqueMatch("PAYMENT 2,000 OK extra", listOf(pattern)))
    }

    @Test
    fun opaqueConfiguredSlotAcceptsAlphanumericTrackingCodeWithoutGuessingItsMeaning() {
        val sample = "کد ABC-123 مبلغ 100 تومان"
        val pattern = SmsPattern(
            id = "opaque",
            name = "Opaque",
            type = SmsPatternType.TRANSFER,
            sampleMessage = sample,
            slots = listOf(
                slot(sample, "ABC-123", SmsPatternSlotKind.TRACKING),
                slot(sample, "100", SmsPatternSlotKind.AMOUNT_TOMAN)
            )
        )

        val parsed = BankSmsParser.parseMatchedPattern(
            "sms-opaque", "sender", "کد XYZ-987 مبلغ 250 تومان", 1_700_000_000_000L, pattern
        )
        assertEquals("XYZ-987", parsed?.trackingCode)
        assertEquals(TransactionType.TRANSFER, parsed?.type)
        assertEquals(250L, parsed?.amount)
    }

    @Test
    fun ambiguousIdenticalUserTemplatesAreRejectedWithoutPriority() {
        val sample = "برداشت 100 تومان"
        val pattern = SmsPattern(
            id = "duplicate",
            name = "Duplicate",
            type = SmsPatternType.EXPENSE,
            sampleMessage = sample,
            slots = listOf(slot(sample, "100", SmsPatternSlotKind.AMOUNT_TOMAN))
        )
        assertNull(SmsPatternMatcher.findUniqueMatch("برداشت 200 تومان", listOf(pattern, pattern.copy(id = "duplicate-2"))))
    }

    @Test
    fun fullDateOrderMustBeChosenExplicitlyByTheUser() {
        val sample = "تاریخ 05/08/10 مبلغ 100 تومان"
        val pattern = SmsPattern(
            id = "dmy",
            name = "روز ماه سال",
            type = SmsPatternType.EXPENSE,
            sampleMessage = sample,
            slots = listOf(
                slot(sample, "05/08/10", SmsPatternSlotKind.DATE_DAY_MONTH_YEAR),
                slot(sample, "100", SmsPatternSlotKind.AMOUNT_TOMAN)
            )
        )
        val parsed = BankSmsParser.parseMatchedPattern(
            "sms-dmy", "sender", "تاریخ 06/08/10 مبلغ 200 تومان", 1_700_000_000_000L, pattern
        )
        assertEquals("1410/08/06", parsed?.dateText)
        assertEquals(200L, parsed?.amount)
    }
}