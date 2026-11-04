package com.example.data.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsPatternMatcherNormalizationTest {

    @Test
    fun matchesVariableDigitGlyphsAndToleratesDifferentLineEndings() {
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
        val actual = "حساب4785914725\nبرداشت۵۰,۰۰۰\nمانده۷,۹۶۳,۳۰۴\n05/08/12-07:14"

        assertNotNull(SmsPatternMatcher.findUniqueMatch(actual, listOf(pattern)))
        assertNotNull(SmsPatternMatcher.extractMatchedFields(actual, pattern))
        assertNotNull(SmsPatternMatcher.findUniqueMatch(actual.replace("\n", "\r\n"), listOf(pattern)))
    }

    @Test
    fun fixedArabicAndPersianLetterVariantsMustRemainExact() {
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

        assertNull(SmsPatternMatcher.findUniqueMatch("موجودي ۲۰۰ تومان", listOf(pattern)))
        assertNotNull(SmsPatternMatcher.findUniqueMatch("موجودی ۲۰۰ تومان", listOf(pattern)))
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

        val fullYearSample = "واریز 80,000 تومان\\n1405/08/05-06:53"
        val fullYearPattern = pattern.copy(
            sampleMessage = fullYearSample,
            slots = SmsPatternMatcher.inferVariableSlots(fullYearSample),
            dateFormat = "YYYY/MM/DD"
        )
        val fullYearActual = "واریز 1,250,000 تومان\\n1405/08/06-07:14"
        val fullYearParsed = BankSmsParser.parseMatchedPattern(
            "full-year", "ResalatBank", fullYearActual, receivedAt, fullYearPattern
        )
        assertNotNull(fullYearParsed)
        assertEquals("1405/08/06", fullYearParsed?.dateText)
    }

    @Test
    fun numericSenderIgnoresWhitespaceAndPersianArabicDigitGlyphsButKeepsExactDigits() {
        val pattern = SmsPattern(
            id = "numeric-sender",
            name = "نمونه",
            type = SmsPatternType.INCOME,
            sampleMessage = "واریز مبلغ 100 تومان",
            slots = SmsPatternMatcher.inferVariableSlots("واریز مبلغ 100 تومان"),
            senderAddress = "۹۸۲۱ ۲۳۴"
        )

        assertTrue(SmsPatternMatcher.senderMatchesPattern(pattern.senderAddress, "9821234"))
        assertTrue(SmsPatternMatcher.senderMatchesPattern("+۹۸۲۱ ۲۳۴", "9821234"))
        assertTrue(SmsPatternMatcher.senderMatchesPattern("9821234", "+98 21 234"))
        assertTrue(SmsPatternMatcher.senderMatchesPattern("+98 21 234", "۹۸۲۱۲۳۴"))
        assertFalse(SmsPatternMatcher.senderMatchesPattern("9821234", "++9821234"))
        assertFalse(SmsPatternMatcher.senderMatchesPattern("9821234", "+98212345"))
        assertTrue(SmsPatternMatcher.senderMatchesPattern("98 21 234", "۹۸۲۱۲۳۴"))
        assertFalse(SmsPatternMatcher.senderMatchesPattern(pattern.senderAddress, "98212345"))
        assertFalse(SmsPatternMatcher.senderMatchesPattern(pattern.senderAddress, "982124"))
        assertEquals(1, SmsPatternMatcher.findMatchingPatterns("واریز مبلغ 250 تومان", listOf(pattern), "98 21 234").size)
        assertNotNull(BankSmsParser.parseMatchedPattern(
            "numeric-sender", "98 21 234", "واریز مبلغ 250 تومان", 1_700_000_000_000L, pattern
        ))
        assertNull(BankSmsParser.parseMatchedPattern(
            "wrong-numeric-sender", "98212345", "واریز مبلغ 250 تومان", 1_700_000_000_000L, pattern
        ))
    }

    @Test
    fun alphanumericSenderRemainsExactApartFromOuterWhitespace() {
        assertTrue(SmsPatternMatcher.senderMatchesPattern("ResalatBank", " ResalatBank "))
        assertFalse(SmsPatternMatcher.senderMatchesPattern("ResalatBank", "resalatbank"))
        assertFalse(SmsPatternMatcher.senderMatchesPattern("ResalatBank", "Resalat Bank"))
    }

    @Test
    fun bodyMatchingToleratesWhitespaceLayoutButKeepsFixedWords() {
        val sample = "واریز مبلغ 100 تومان\nمانده 500 تومان"
        val pattern = SmsPattern(
            id = "whitespace",
            name = "نمونه",
            type = SmsPatternType.INCOME,
            sampleMessage = sample,
            slots = SmsPatternMatcher.inferVariableSlots(sample),
            senderAddress = "BankSender"
        )

        assertNotNull(SmsPatternMatcher.findUniqueMatch(
            "واریز   مبلغ 250 تومان\r\nمانده 700 تومان", listOf(pattern), "BankSender"
        ))
        assertNull(SmsPatternMatcher.findUniqueMatch(
            "برداشت   مبلغ 250 تومان\r\nمانده 700 تومان", listOf(pattern), "BankSender"
        ))
    }

    @Test
    fun keepsLongUnlabeledDottedAccountIdentifierFixed() {
        val sample = """10.8805139.1
+80,000
07/13_12:16
مانده: 381,835,350"""
        val slots = SmsPatternMatcher.inferVariableSlots(sample)
        val identifierStart = sample.indexOf("10.8805139.1")
        assertFalse(slots.any { it.start < identifierStart + 12 && it.endExclusive > identifierStart })

        val pattern = SmsPattern(
            id = "dotted-account",
            name = "رسالت",
            type = SmsPatternType.INCOME,
            sampleMessage = sample,
            slots = slots,
            senderAddress = "ResalatBank"
        )
        val sameAccountDifferentAmounts = """10.8805139.1
+125,000
07/14_13:20
مانده: 1,381,835,350"""
        val changedAccount = sameAccountDifferentAmounts.replace("10.8805139.1", "10.8805139.2")

        assertTrue(SmsPatternMatcher.findMatchingPatterns(sameAccountDifferentAmounts, listOf(pattern), "ResalatBank").isNotEmpty())
        assertTrue(SmsPatternMatcher.findMatchingPatterns(changedAccount, listOf(pattern), "ResalatBank").isEmpty())
    }

    @Test
    fun transferAccountNumberUsesTheExplicitlySelectedSide() {
        val sample = "انتقال به حساب 123456789 مبلغ 100 تومان"
        val inferred = SmsPatternMatcher.inferVariableSlots(sample)
        val sourceSlots = SmsPatternMatcher.configureAllowedAccountNumbers(
            sample, inferred, listOf("123456789"), SmsPatternType.TRANSFER, SmsPatternAccountSide.SOURCE
        )
        val destinationSlots = SmsPatternMatcher.configureAllowedAccountNumbers(
            sample, inferred, listOf("123456789"), SmsPatternType.TRANSFER, SmsPatternAccountSide.DESTINATION
        )

        assertTrue(sourceSlots.any { it.kind == SmsPatternSlotKind.SOURCE_ACCOUNT })
        assertFalse(sourceSlots.any { it.kind == SmsPatternSlotKind.DESTINATION_ACCOUNT })
        assertTrue(destinationSlots.any { it.kind == SmsPatternSlotKind.DESTINATION_ACCOUNT })
        assertFalse(destinationSlots.any { it.kind == SmsPatternSlotKind.SOURCE_ACCOUNT })
    }

    @Test
    fun transferAccountNumberIsNotAssignedWithoutUserSelectionWhenNoDirectionIsExplicit() {
        val sample = "انتقال 123456789 مبلغ 100 تومان"
        val inferred = SmsPatternMatcher.inferVariableSlots(sample)
        val configured = SmsPatternMatcher.configureAllowedAccountNumbers(
            sample, inferred, listOf("123456789"), SmsPatternType.TRANSFER, null
        )

        assertFalse(configured.any {
            it.kind == SmsPatternSlotKind.SOURCE_ACCOUNT || it.kind == SmsPatternSlotKind.DESTINATION_ACCOUNT
        })
    }

}
