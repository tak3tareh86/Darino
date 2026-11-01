package com.example.data.sms

import com.example.ui.screens.finance.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BankSmsParserTest {

    @Test
    fun positiveSignedAmountIsIncome() {
        assertEquals(
            TransactionType.INCOME,
            BankSmsParser.detectTransactionType("مبلغ تراکنش: +۱,۲۵۰,۰۰۰ ریال")
        )
    }

    @Test
    fun negativeSignedAmountIsExpense() {
        assertEquals(
            TransactionType.EXPENSE,
            BankSmsParser.detectTransactionType("مبلغ تراکنش: -۱,۲۵۰,۰۰۰ ریال")
        )
    }

    @Test
    fun clearlyLabeledPositiveSignedAmountPassesBankEvidence() {
        assertEquals(
            true,
            BankSmsParser.hasValidBankEvidence(
                "20008434",
                "مبلغ تراکنش: +۱۲۵,۰۰۰ ریال"
            )
        )
    }

    @Test
    fun clearlyLabeledNegativeSignedAmountPassesBankEvidence() {
        assertEquals(
            true,
            BankSmsParser.hasValidBankEvidence(
                "20008434",
                "مبلغ تراکنش: -۱۲۵,۰۰۰ ریال"
            )
        )
    }

    @Test
    fun signedAccountBalanceAloneIsNotATransactionDirection() {
        assertNull(
            BankSmsParser.detectTransactionType("مانده حساب: +۱,۲۵۰,۰۰۰ ریال")
        )
    }

    @Test
    fun conflictingExplicitDirectionAndSignIsRejected() {
        assertNull(
            BankSmsParser.detectTransactionType("برداشت از حساب، مبلغ تراکنش: +۱,۲۵۰,۰۰۰ ریال")
        )
    }

    @Test
    fun senderShortcodeIdentifiesBankWithoutNameInBody() {
        assertEquals(
            "بانک سامان",
            BankSmsParser.detectBankName("20008434", "واریز وجه به حساب شما")
        )
    }

    @Test
    fun bankNameInBodyIdentifiesBankWithUnknownSender() {
        assertEquals(
            "بانک سامان",
            BankSmsParser.detectBankName("unknown-sender", "بانک سامان؛ واریز وجه")
        )
    }

    @Test
    fun conflictingSenderAndBodyBankIdentityIsRejected() {
        assertEquals(
            false,
            BankSmsParser.hasValidBankEvidence(
                "20004000",
                "بانک سامان برداشت از حساب، مبلغ تراکنش: ۱,۰۰۰ ریال، مانده: ۲,۰۰۰ ریال"
            )
        )
    }

    @Test
    fun colloquialBlueBankOutgoingPhraseIsExpense() {
        assertEquals(
            TransactionType.EXPENSE,
            BankSmsParser.detectTransactionType("۱۲۵,۰۰۰ ریال از حساب شما پرید")
        )
    }

    @Test
    fun colloquialBlueBankIncomingPhraseIsIncome() {
        assertEquals(
            TransactionType.INCOME,
            BankSmsParser.detectTransactionType("۱۲۵,۰۰۰ ریال به حساب شما نشست")
        )
    }
    @Test
    fun knownSenderCanMentionAnotherBankAsTransferCounterparty() {
        assertEquals(
            true,
            BankSmsParser.hasValidBankEvidence(
                "20004000",
                "انتقال وجه به کارت بانک سامان با موفقیت انجام شد، مبلغ: ۱۰۰,۰۰۰ ریال، مانده: ۵۰۰,۰۰۰ ریال"
            )
        )
    }

    @Test
    fun explicitDifferentBankIssuerHeaderIsStillRejected() {
        assertEquals(
            false,
            BankSmsParser.hasValidBankEvidence(
                "20004000",
                "بانک سامان؛ برداشت از حساب، مبلغ تراکنش: ۱۰۰,۰۰۰ ریال، مانده: ۵۰۰,۰۰۰ ریال"
            )
        )
    }

    @Test
    fun completedGenericPaymentIsDetectedAsExpense() {
        assertEquals(
            TransactionType.EXPENSE,
            BankSmsParser.detectTransactionType("پرداخت با موفقیت انجام شد؛ مبلغ تراکنش: ۱۲۵,۰۰۰ ریال")
        )
    }

    @Test
    fun transactionAmountDoesNotInheritCurrencyFromBalance() {
        assertEquals(
            1_250L,
            BankSmsParser.extractAmountToman(
                "برداشت از حساب، مبلغ تراکنش: ۱۲,۵۰۰، مانده: ۵۰۰,۰۰۰ تومان",
                TransactionType.EXPENSE
            )
        )
    }

    @Test
    fun transactionAmountDoesNotInheritCurrencyFromFee() {
        assertEquals(
            1_250L,
            BankSmsParser.extractAmountToman(
                "برداشت از حساب، مبلغ تراکنش: ۱۲,۵۰۰، کارمزد: ۲۰۰ تومان",
                TransactionType.EXPENSE
            )
        )
    }

    @Test
    fun transactionAmountIsNotConfusedWithDescribedTransferFee() {
        val sms = "کارمزد انتقال وجه: ۲۰۰ ریال؛ برداشت از حساب با موفقیت انجام شد؛ مبلغ تراکنش: ۱۲,۵۰۰ ریال؛ مانده: ۵۰۰,۰۰۰ ریال"
        assertEquals(1_250L, BankSmsParser.extractAmountToman(sms, TransactionType.EXPENSE))
    }

    @Test
    fun transactionAmountIsNotConfusedWithServiceFeeInToman() {
        val sms = "برداشت از حساب با موفقیت انجام شد؛ هزینه خدمات: ۲,۰۰۰ تومان؛ مبلغ تراکنش: ۱۲,۵۰۰ ریال"
        assertEquals(1_250L, BankSmsParser.extractAmountToman(sms, TransactionType.EXPENSE))
    }

    @Test
    fun taxAndTransferFeeAreExcludedFromTransactionAmount() {
        val sms = "پرداخت با موفقیت انجام شد؛ مالیات بر ارزش افزوده: ۹۰۰ ریال؛ کارمزد کارت به کارت: ۱,۰۰۰ ریال؛ مبلغ پرداخت: ۵۰,۰۰۰ ریال"
        assertEquals(5_000L, BankSmsParser.extractAmountToman(sms, TransactionType.EXPENSE))
    }

    @Test
    fun completedPaymentWithBalanceIsNotClassifiedAsBalanceOnlyNotice() {
        assertEquals(
            false,
            BankSmsParser.isNonTransactionalSms(
                "پرداخت با موفقیت انجام شد؛ مبلغ تراکنش: ۱۲۵,۰۰۰ ریال؛ مانده: ۵۰۰,۰۰۰ ریال"
            )
        )
    }

    @Test
    fun signedBalanceDoesNotBecomeTransactionAmountWhenAmountLabelAppearsElsewhere() {
        assertNull(
            BankSmsParser.detectTransactionType(
                "مانده حساب: +۱,۲۵۰,۰۰۰ ریال؛ مبلغ تراکنش"
            )
        )
    }

    @Test
    fun contradictorySignedDirectionIsQueuedForManualReview() {
        val suggestion = BankSmsParser.parse(
            "sms_review_1",
            "20008434",
            "برداشت از حساب، مبلغ تراکنش: +۱۲۵,۰۰۰ ریال",
            1_700_000_000_000L
        )

        assertEquals(true, suggestion?.isTypeUncertain)
        assertNull(suggestion?.type)
        assertEquals(true, suggestion?.isAmountValid)
        assertEquals(12_500L, suggestion?.amount)
    }

    @Test
    fun successfulRefundIsClassifiedAsIncomeAndNotDiscarded() {
        val sms = "برگشت وجه خرید با موفقیت به حساب شما واریز شد؛ مبلغ: ۱۲۵,۰۰۰ ریال؛ مانده: ۵۰۰,۰۰۰ ریال"

        assertEquals(false, BankSmsParser.isNonTransactionalSms(sms))
        assertEquals(TransactionType.INCOME, BankSmsParser.detectTransactionType(sms))
        assertEquals(12_500L, BankSmsParser.extractAmountToman(sms, TransactionType.INCOME))
    }

    @Test
    fun successfulRefundIsQueuedAsAnIncomeSuggestion() {
        val suggestion = BankSmsParser.parse(
            "sms_refund_1",
            "20008434",
            "برگشت وجه خرید با موفقیت به حساب شما واریز شد؛ مبلغ: ۱۲۵,۰۰۰ ریال؛ مانده: ۵۰۰,۰۰۰ ریال",
            1_700_000_000_000L
        )

        assertEquals(TransactionType.INCOME, suggestion?.type)
        assertEquals(12_500L, suggestion?.amount)
        assertEquals("برگشت وجه", suggestion?.category)
        assertEquals(true, suggestion?.isAmountValid)
    }

    @Test
    fun failedRefundOrReversalRemainsRejected() {
        val sms = "برگشت وجه ناموفق؛ تراکنش ناموفق بود؛ مبلغ: ۱۲۵,۰۰۰ ریال"

        assertEquals(true, BankSmsParser.isNonTransactionalSms(sms))
        assertNull(BankSmsParser.detectTransactionType(sms))
    }

    @Test
    fun confirmedReversedDebitIsClassifiedAsIncome() {
        val sms = "مبلغ برداشت شده با موفقیت به حساب شما برگشت داده شد؛ مبلغ: ۱۲۵,۰۰۰ ریال؛ مانده: ۵۰۰,۰۰۰ ریال"

        assertEquals(false, BankSmsParser.isNonTransactionalSms(sms))
        assertEquals(TransactionType.INCOME, BankSmsParser.detectTransactionType(sms))
        assertEquals(12_500L, BankSmsParser.extractAmountToman(sms, TransactionType.INCOME))
    }

    @Test
    fun confirmedReversedDebitIsQueuedAsRefundIncome() {
        val sms = "مبلغ برداشت شده با موفقیت به حساب شما برگشت داده شد؛ مبلغ: ۱۲۵,۰۰۰ ریال؛ مانده: ۵۰۰,۰۰۰ ریال"
        val suggestion = BankSmsParser.parse(
            "sms_reversal_1",
            "20008434",
            sms,
            1_700_000_000_000L
        )

        assertEquals(TransactionType.INCOME, suggestion?.type)
        assertEquals(12_500L, suggestion?.amount)
        assertEquals("برگشت وجه", suggestion?.category)
        assertEquals(true, suggestion?.isAmountValid)
    }

    @Test
    fun reversedDebitWithoutConfirmedCreditIsNotIncome() {
        val sms = "درخواست برگشت برداشت ثبت شد؛ مبلغ: ۱۲۵,۰۰۰ ریال"

        assertEquals(true, BankSmsParser.isNonTransactionalSms(sms))
        assertEquals(null, BankSmsParser.detectTransactionType(sms))
    }

    @Test
    fun unrecognizedSenderWithoutExplicitBankHeaderIsNotQueued() {
        val suggestion = BankSmsParser.parse(
            "sms_review_2",
            "unknown-sender",
            "برداشت از حساب، مبلغ تراکنش: ۱۲۵,۰۰۰ ریال",
            1_700_000_000_000L
        )

        assertEquals(null, suggestion)
    }

    @Test
    fun bankTransactionWithMissingAmountIsQueuedForManualCorrection() {
        val suggestion = BankSmsParser.parse(
            "sms_review_3",
            "20008434",
            "برداشت از حساب با موفقیت انجام شد؛ مانده: ۵۰۰,۰۰۰ ریال",
            1_700_000_000_000L
        )

        assertEquals(true, suggestion?.isTypeUncertain)
        assertEquals(false, suggestion?.isAmountValid)
        assertEquals(0L, suggestion?.amount)
    }

}
