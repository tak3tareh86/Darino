package com.example.data.sms

import com.example.ui.screens.finance.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
            slots = listOf(slot(sample, "1,000", SmsPatternSlotKind.AMOUNT_RIAL)),
            senderAddress = "unknown-sender"
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
            slots = emptyList(),
            senderAddress = "unknown"
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
                SmsPatternSlot(sample.indexOf("123456"), sample.indexOf("123456") + "123456".length, SmsPatternSlotKind.SOURCE_ACCOUNT, listOf("123456", "998877")),
                slot(sample, "1,000", SmsPatternSlotKind.AMOUNT_TOMAN),
                slot(sample, "1405/08/09", SmsPatternSlotKind.DATE),
                slot(sample, "18:09", SmsPatternSlotKind.TIME)
            ),
            senderAddress = "unknown"
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
            ),
            senderAddress = "unknown"
        )
        val parsed = BankSmsParser.parseMatchedPattern(
            "sms4", "unknown", "تاریخ 09/02 مبلغ 200 تومان", 1_700_000_000_000L, pattern
        )
        assertEquals("1402/02/09", parsed?.dateText)
        assertEquals(200L, parsed?.amount)
    }

    @Test
    fun fixedTextRequiresExactCaseWhitespaceAndLineBreaks() {
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
        assertNull(SmsPatternMatcher.findUniqueMatch("PAYMENT\n2,000 OK", listOf(pattern)))
        assertNull(SmsPatternMatcher.findUniqueMatch("PAYMENT\r\n2,000 OK", listOf(pattern)))
        assertNull(SmsPatternMatcher.findUniqueMatch("PAYMENT 2,000 OK extra", listOf(pattern)))
    }

    @Test
    fun fixedPersianTextAndHalfSpaceAreNotNormalizedAway() {
        val sample = "برداشت از حساب\u200cجاری مبلغ 1,000 تومان"
        val pattern = SmsPattern(
            id = "half-space",
            name = "نیم‌فاصله",
            type = SmsPatternType.EXPENSE,
            sampleMessage = sample,
            slots = listOf(slot(sample, "1,000", SmsPatternSlotKind.AMOUNT_TOMAN)),
            senderAddress = "BankSender"
        )
        assertNotNull(SmsPatternMatcher.findUniqueMatch(
            "برداشت از حساب\u200cجاری مبلغ 2,000 تومان", listOf(pattern), "BankSender"
        ))
        assertNull(SmsPatternMatcher.findUniqueMatch(
            "برداشت از حساب جاری مبلغ 2,000 تومان", listOf(pattern), "BankSender"
        ))
        assertNull(SmsPatternMatcher.findUniqueMatch(
            "برداشت از حساب\u200dجاری مبلغ 2,000 تومان", listOf(pattern), "BankSender"
        ))
    }

    @Test
    fun senderIdMustBePresentAndMatchExactlyForInboxMatching() {
        val sample = "واریز 1,000 تومان"
        val legacy = SmsPattern(
            id = "legacy-no-sender",
            name = "قدیمی",
            type = SmsPatternType.INCOME,
            sampleMessage = sample,
            slots = listOf(slot(sample, "1,000", SmsPatternSlotKind.AMOUNT_TOMAN))
        )
        val configured = legacy.copy(id = "configured", senderAddress = "Bank Sender")
        val actual = "واریز 2,000 تومان"

        assertTrue(SmsPatternMatcher.findMatchingPatterns(actual, listOf(legacy), "Bank Sender").isEmpty())
        assertTrue(SmsPatternMatcher.findMatchingPatterns(actual, listOf(configured), null).isEmpty())
        assertTrue(SmsPatternMatcher.findMatchingPatterns(actual, listOf(configured), "").isEmpty())
        assertTrue(SmsPatternMatcher.findMatchingPatterns(actual, listOf(configured), "bank Sender").isEmpty())
        assertEquals(1, SmsPatternMatcher.findMatchingPatterns(actual, listOf(configured), "Bank Sender").size)
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
            ),
            senderAddress = "sender"
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
            ),
            senderAddress = "sender"
        )
        val parsed = BankSmsParser.parseMatchedPattern(
            "sms-dmy", "sender", "تاریخ 06/08/10 مبلغ 200 تومان",
            com.example.util.PersianCalendarHelper.jalaliToEpochMillis(1410, 8, 10, 12, 0), pattern
        )
        assertEquals("1410/08/06", parsed?.dateText)
        assertEquals(200L, parsed?.amount)
    }
    @Test
    fun automaticInferenceKeepsLongAccountNumberFixedAndSelectsLabeledAmount() {
        val sample = "واریز به حساب 123456789012 مبلغ 500000 ریال تاریخ 1405/08/09 ساعت 18:20"
        val slots = SmsPatternMatcher.inferVariableSlots(sample)

        assertEquals("500000", slots.single { it.kind == SmsPatternSlotKind.AMOUNT_RIAL }
            .let { sample.substring(it.start, it.endExclusive) })
        assertEquals("1405/08/09", slots.single { it.kind == SmsPatternSlotKind.DATE }
            .let { sample.substring(it.start, it.endExclusive) })
        assertEquals("18:20", slots.single { it.kind == SmsPatternSlotKind.TIME }
            .let { sample.substring(it.start, it.endExclusive) })
        assertEquals(3, slots.size)

        val pattern = SmsPattern("auto", "نمونه", SmsPatternType.INCOME, sample, slots, senderAddress = "bank")
        assertNotNull(SmsPatternMatcher.findUniqueMatch(
            "واریز به حساب 123456789012 مبلغ 750000 ریال تاریخ 1405/08/10 ساعت 19:25",
            listOf(pattern)
        ))
        assertNull(SmsPatternMatcher.findUniqueMatch(
            "واریز به حساب 999999999999 مبلغ 750000 ریال تاریخ 1405/08/10 ساعت 19:25",
            listOf(pattern)
        ))
    }

    @Test
    fun automaticInferenceSupportsShortAmountWithAttachedTomanSuffix() {
        val sample = "برداشت از کارت 6037991234567890 مبلغ ۵۰۰تومان"
        val slots = SmsPatternMatcher.inferVariableSlots(sample)
        val amountSlot = slots.single { it.kind == SmsPatternSlotKind.AMOUNT_TOMAN }
        assertEquals("۵۰۰تومان", sample.substring(amountSlot.start, amountSlot.endExclusive))

        val pattern = SmsPattern("short", "نمونه", SmsPatternType.EXPENSE, sample, slots, senderAddress = "bank")
        val parsed = BankSmsParser.parseMatchedPattern(
            "sms-short",
            "bank",
            "برداشت از کارت 6037991234567890 مبلغ ۷۵۰تومان",
            1_700_000_000_000L,
            pattern
        )
        assertEquals(750L, parsed?.amount)
    }

    @Test
    fun automaticInferenceSeparatesBalanceAndTransactionAmountWhenBothAppearInSequence() {
        val sample = "حساب***** برداشت400,000 مانده8,013,304 05/08/11-06:53"
        val slots = SmsPatternMatcher.inferVariableSlots(sample)

        assertEquals("400,000", slots.single { it.kind == SmsPatternSlotKind.AMOUNT_TOMAN }
            .let { sample.substring(it.start, it.endExclusive) })
        assertEquals("8,013,304", slots.single { it.kind == SmsPatternSlotKind.BALANCE_TOMAN }
            .let { sample.substring(it.start, it.endExclusive) })
        assertEquals("05/08/11", slots.single { it.kind == SmsPatternSlotKind.DATE_YEAR_MONTH_DAY }
            .let { sample.substring(it.start, it.endExclusive) })
        assertEquals("06:53", slots.single { it.kind == SmsPatternSlotKind.TIME }
            .let { sample.substring(it.start, it.endExclusive) })
    }

    @Test
    fun automaticInferenceDoesNotMistakeBalanceForTransactionAmount() {
        val sample = "واریز مبلغ 2500 تومان موجودی 9876543210"
        val slots = SmsPatternMatcher.inferVariableSlots(sample)
        assertEquals("2500", slots.single { it.kind == SmsPatternSlotKind.AMOUNT_TOMAN }
            .let { sample.substring(it.start, it.endExclusive) })
        assertEquals(1, slots.count { it.kind == SmsPatternSlotKind.AMOUNT_TOMAN })
    }


    @Test
    fun amountDigitCountAndThousandsGroupingCanGrowOrShrink() {
        val sample = "برداشت مبلغ 50,000 تومان"
        val pattern = SmsPattern(
            id = "variable-amount",
            name = "مبلغ متغیر",
            type = SmsPatternType.EXPENSE,
            sampleMessage = sample,
            slots = listOf(slot(sample, "50,000", SmsPatternSlotKind.AMOUNT_TOMAN)),
            senderAddress = "bank"
        )
        listOf("برداشت مبلغ 10,000 تومان", "برداشت مبلغ 500000 تومان",
            "برداشت مبلغ 1,000,000 تومان", "برداشت مبلغ 1000000 تومان")
            .forEach { assertNotNull(SmsPatternMatcher.findUniqueMatch(it, listOf(pattern))) }
        val parsed = BankSmsParser.parseMatchedPattern(
            "sms-variable-amount", "bank", "برداشت مبلغ 1,000,000 تومان", 1_700_000_000_000L, pattern
        )
        assertEquals(1_000_000L, parsed?.amount)
        assertNull(SmsPatternMatcher.findUniqueMatch("برداشت مبلغ 1.000.000 تومان", listOf(pattern)))
    }

    @Test
    fun multipleAccountNumbersAreExplicitlyWhitelistedForOneTemplate() {
        val sample = "برداشت از:9876543210987 مبلغ:50,000 تومان"
        val inferred = SmsPatternMatcher.inferVariableSlots(sample)
        val slots = SmsPatternMatcher.configureAllowedAccountNumbers(
            sample, inferred, listOf("9876543210987", "9876543210988", "9876543210989")
        )
        val pattern = SmsPattern("multi-account", "بانک", SmsPatternType.EXPENSE, sample, slots, senderAddress = "bank")
        assertNotNull(SmsPatternMatcher.findUniqueMatch(
            "برداشت از:9876543210988 مبلغ:1,000,000 تومان", listOf(pattern)
        ))
        assertNull(SmsPatternMatcher.findUniqueMatch(
            "برداشت از:9876543210990 مبلغ:1,000,000 تومان", listOf(pattern)
        ))
        val parsed = BankSmsParser.parseMatchedPattern(
            "sms-account", "bank", "برداشت از:9876543210988 مبلغ:1,000,000 تومان",
            1_700_000_000_000L, pattern
        )
        assertEquals("حساب 9876543210988", parsed?.sourceAccount)
    }

    @Test
    fun twoDigitYearFirstJalaliDateUsesReceiveYearCenturyAndKeepsDateOrder() {
        val sample = "تاریخ 05/08/11 مبلغ 100 تومان"
        val pattern = SmsPattern(
            id = "short-year-first",
            name = "سال دورقمی",
            type = SmsPatternType.INCOME,
            sampleMessage = sample,
            slots = listOf(
                slot(sample, "05/08/11", SmsPatternSlotKind.DATE_YEAR_MONTH_DAY),
                slot(sample, "100", SmsPatternSlotKind.AMOUNT_TOMAN)
            ),
            senderAddress = "sender"
        )
        val receivedIn1405 = com.example.util.PersianCalendarHelper.jalaliToEpochMillis(1405, 8, 11, 12, 0)
        val parsed = BankSmsParser.parseMatchedPattern(
            "sms-short-year", "sender", "تاریخ 05/09/12 مبلغ 200 تومان", receivedIn1405, pattern
        )
        assertEquals("1405/09/12", parsed?.dateText)
        assertEquals(200L, parsed?.amount)
    }

    @Test
    fun automaticInferenceRecognizesDayMonthWithTrailingFourDigitYear() {
        val sample = "تاریخ 11/08/1405 مبلغ 100 تومان"
        val dateSlot = SmsPatternMatcher.inferVariableSlots(sample).single {
            it.kind == SmsPatternSlotKind.DATE_DAY_MONTH_YEAR
        }
        assertEquals("11/08/1405", sample.substring(dateSlot.start, dateSlot.endExclusive))
    }

    @Test
    fun rejectsPasswordKeywordEvenWhenAnExactUserTemplateMatches() {
        val sample = "برداشت 1,000 تومان رمز خرید"
        val pattern = SmsPattern(
            id = "password-template",
            name = "قالب آزمایشی",
            type = SmsPatternType.EXPENSE,
            sampleMessage = sample,
            slots = listOf(slot(sample, "1,000", SmsPatternSlotKind.AMOUNT_TOMAN)),
            senderAddress = "30001234"
        )

        assertNotNull(SmsPatternMatcher.findUniqueMatch(sample, listOf(pattern)))
        assertNull(
            BankSmsParser.parseMatchedPattern(
                "password-sms", "30001234", sample, 1_700_000_000_000L, pattern
            )
        )
    }
}
