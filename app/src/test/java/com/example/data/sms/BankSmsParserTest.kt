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

        assertNotNull(SmsPatternMatcher.findBestMatch("برداشت: 1,250 ریال | مانده: 3,500 ریال", listOf(pattern)))
        assertNull(SmsPatternMatcher.findBestMatch("واریز: 1,250 ریال | مانده: 3,500 ریال", listOf(pattern)))
        assertNull(SmsPatternMatcher.findBestMatch("برداشت: 1,250 تومان | مانده: 3,500 ریال", listOf(pattern)))
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
        assertNull(SmsPatternMatcher.findBestMatch("تراکنش 2,000 ریال", listOf(noAmountPattern)))

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
}