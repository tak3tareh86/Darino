package com.example.data.sms

import android.util.Log
import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.screens.home.domain.BankSmsSuggestion
import com.example.util.IranianPhoneUtils
import com.example.util.MoneyFormatter
import com.example.util.PersianCalendarHelper
import java.util.regex.Pattern

object BankSmsParser {

    private data class BankDefinition(
        val displayName: String,
        val senderTokens: List<String>,
        val bodyPatterns: List<String>
    )

    private val ALL_BANKS = listOf(
        BankDefinition(
            displayName = "بانک ملت",
            senderTokens = listOf("mellat", "bmellat", "b.mellat", "bankmellat", "1556", "20004000", "20000", "50004000"),
            bodyPatterns = listOf("بانک ملت", "ملت")
        ),
        BankDefinition(
            displayName = "بانک ملی",
            senderTokens = listOf("melli", "bmi", "bmelli", "b.melli", "bankmelli", "20006414", "30006414", "30009414", "10006414", "404077", "200000"),
            bodyPatterns = listOf("بانک ملی", "بانک ملی ایران", "سامانه بام")
        ),
        BankDefinition(
            displayName = "بانک سامان",
            senderTokens = listOf("saman", "samanbank", "banksaman", "20008434", "30008434", "10008434"),
            bodyPatterns = listOf("بانک سامان", "سامان")
        ),
        BankDefinition(
            displayName = "بانک پاسارگاد",
            senderTokens = listOf("pasargad", "bpi", "bpasargad", "b.pasargad", "bankpasargad", "1000900", "2000900", "3000900", "5000900"),
            bodyPatterns = listOf("بانک پاسارگاد", "پاسارگاد")
        ),
        BankDefinition(
            displayName = "بانک صادرات",
            senderTokens = listOf("saderat", "bsi", "bsaderat", "b.saderat", "banksaderat", "2000940", "3000940", "1000940"),
            bodyPatterns = listOf("بانک صادرات", "بانک صادرات ایران", "صادرات")
        ),
        BankDefinition(
            displayName = "بانک تجارت",
            senderTokens = listOf("tejarat", "btejarat", "b.tejarat", "tejaratbank", "banktejarat", "200054", "300054", "100054"),
            bodyPatterns = listOf("بانک تجارت", "تجارت")
        ),
        BankDefinition(
            displayName = "بلوبانک",
            senderTokens = listOf("blubank", "blu", "200084", "300084"),
            bodyPatterns = listOf("بلوبانک", "بلو بانک", "بلو")
        ),
        BankDefinition(
            displayName = "بانک سپه",
            senderTokens = listOf("sepah", "bsepah", "b.sepah", "banksepah", "100020", "200020", "300020", "2000020", "3000020"),
            bodyPatterns = listOf("بانک سپه", "سپه", "انصار", "قوامین", "حکمت", "کوثر")
        ),
        BankDefinition(
            displayName = "بانک کشاورزی",
            senderTokens = listOf("keshavarzi", "bki", "bkeshavarzi", "b.keshavarzi", "300091", "100091", "200091"),
            bodyPatterns = listOf("بانک کشاورزی", "کشاورزی")
        ),
        BankDefinition(
            displayName = "بانک پارسیان",
            senderTokens = listOf("parsian", "bparsian", "b.parsian", "parsianbank", "300072", "100072", "200072"),
            bodyPatterns = listOf("بانک پارسیان", "پارسیان")
        ),
        BankDefinition(
            displayName = "بانک رفاه",
            senderTokens = listOf("refah", "brefah", "b.refah", "refahbank", "300066", "100066", "200066"),
            bodyPatterns = listOf("بانک رفاه", "بانک رفاه کارگران", "رفاه")
        ),
        BankDefinition(
            displayName = "بانک مسکن",
            senderTokens = listOf("maskan", "bmaskan", "b.maskan", "maskanbank", "300014", "100014", "200014"),
            bodyPatterns = listOf("بانک مسکن", "مسکن")
        ),
        BankDefinition(
            displayName = "بانک رسالت",
            senderTokens = listOf("resalat", "20004747", "10004747", "30004747"),
            bodyPatterns = listOf("بانک رسالت", "قرض الحسنه رسالت", "رسالت")
        ),
        BankDefinition(
            displayName = "بانک آینده",
            senderTokens = listOf("ayandeh", "bayandeh", "ayandehbank", "2000270", "3000270", "1000270"),
            bodyPatterns = listOf("بانک آینده", "آینده")
        ),
        BankDefinition(
            displayName = "بانک شهر",
            senderTokens = listOf("shahr", "shahrbank", "bshahr", "100081", "200081", "300081"),
            bodyPatterns = listOf("بانک شهر", "شهر")
        ),
        BankDefinition(
            displayName = "بانک مهر ایران",
            senderTokens = listOf("mehreiran", "qmb", "mehre_iran", "30008520", "20008520", "300008520"),
            bodyPatterns = listOf("بانک مهر ایران", "قرض الحسنه مهر ایران", "مهر ایران")
        ),
        BankDefinition(
            displayName = "بانک دی",
            senderTokens = listOf("dey", "bankdey", "20002828", "30002828"),
            bodyPatterns = listOf("بانک دی", "دی")
        ),
        BankDefinition(
            displayName = "بانک سینا",
            senderTokens = listOf("sina", "sinabank", "2000300", "3000300"),
            bodyPatterns = listOf("بانک سینا", "سینا")
        ),
        BankDefinition(
            displayName = "پست بانک",
            senderTokens = listOf("postbank", "post_bank", "300094", "200094"),
            bodyPatterns = listOf("پست بانک", "پست‌بانک")
        ),
        BankDefinition(
            displayName = "بانک اقتصاد نوین",
            senderTokens = listOf("enbank", "eghtesadnovin", "20008585", "30008585"),
            bodyPatterns = listOf("بانک اقتصاد نوین", "اقتصاد نوین")
        ),
        BankDefinition(
            displayName = "بانک ایران زمین",
            senderTokens = listOf("izbank", "iranzamin", "30006905", "20006905"),
            bodyPatterns = listOf("بانک ایران زمین", "ایران زمین")
        ),
        BankDefinition(
            displayName = "بانک گردشگری",
            senderTokens = listOf("gardeshgari", "tourismbank", "2000310", "3000310"),
            bodyPatterns = listOf("بانک گردشگری", "گردشگری")
        ),
        BankDefinition(
            displayName = "بانک کارآفرین",
            senderTokens = listOf("karafarin", "200065", "300065"),
            bodyPatterns = listOf("بانک کارآفرین", "کارآفرین")
        ),
        BankDefinition(
            displayName = "بانک خاورمیانه",
            senderTokens = listOf("middleeastbank", "mebank", "200096", "300096"),
            bodyPatterns = listOf("بانک خاورمیانه", "خاورمیانه")
        ),
        BankDefinition(
            displayName = "بانک سرمایه",
            senderTokens = listOf("sarmayeh", "200093", "300093"),
            bodyPatterns = listOf("بانک سرمایه", "سرمایه")
        ),
        BankDefinition(
            displayName = "شاپرک",
            senderTokens = listOf("shaparak", "404070"),
            bodyPatterns = listOf("شاپرک", "شبکه الکترونیکی پرداخت")
        )
    )

    /**
     * Standardizes digits and characters across Persian, Arabic, and English inputs.
     */
    fun normalizeText(input: String): String {
        return IranianPhoneUtils.convertDigitsToEnglish(input)
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace('−', '-')
            .replace('﹣', '-')
            .replace('－', '-')
            .replace('\u200C', ' ')
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    /**
     * Standardizes sender address / shortcode by stripping country code (+98, 0098, 98) and symbols.
     */
    fun normalizeSender(sender: String): String {
        val english = IranianPhoneUtils.convertDigitsToEnglish(sender)
        var clean = english.lowercase().trim()
            .replace(" ", "")
            .replace("-", "")
            .replace("_", "")
            .replace(".", "")

        if (clean.startsWith("+98")) clean = clean.removePrefix("+98")
        else if (clean.startsWith("0098")) clean = clean.removePrefix("0098")
        else if (clean.startsWith("98") && clean.length > 5) clean = clean.removePrefix("98")

        return clean
    }

    /**
     * Strictly detects and flags non-transactional messages.
     * Takes absolute priority over transaction acceptance rules.
     *
     * Note: Avoids false-positives on transaction codes like "کد پیگیری" or "کد مرجع".
     */
    fun isNonTransactionalSms(text: String): Boolean {
        val norm = normalizeText(text).lowercase()

        // 1. One-Time Passwords (OTP), Dynamic Passwords, Verification Codes, Activation Codes
        val otpPatterns = listOf(
            """(?:رمز\s*(?:پویا|یکبار\s*مصرف|یکبارمصرف|دوم|موقت|اینترنتی|ت[اأآ]یید))""",
            """(?:کد\s*(?:ت[اأآ]یید|فعالسازی|فعال\s*سازی|ورود|احراز\s*هویت|اعتبارسنجی|امنیتی|یکبار\s*مصرف|صحت\s*سنجی))""",
            """\b(?:otp|one[- ]time\s*password|verification\s*code|activation\s*code|security\s*code)\b""",
            """(?:در\s*اختیار\s*(?:دیگران|غیر|هیچ\s*کس))""",
            """(?:محرمانه\s*(?:است|بوده|می\s*باشد)|از\s*افشای\s*آن\s*خودداری)""",
            """(?:معتبر\s*تا\s*\d+\s*(?:دقیقه|ثانیه)|مدت\s*اعتبار\s*رمز|اعتبار\s*رمز)""",
            """(?:جهت\s*(?:خرید|انتقال|تراکنش)[\s\S]{1,40}رمز\s*(?:پویا|یکبار\s*مصرف|دوم))""",
            """(?:رمز\s*پویای\s*کارت)"""
        )
        for (pat in otpPatterns) {
            if (Pattern.compile(pat, Pattern.CASE_INSENSITIVE).matcher(norm).find()) {
                return true
            }
        }

        // 2. Authentication, Login Alerts, Account / Card Security Actions
        val securityPatterns = listOf(
            """(?:ورود\s*(?:موفق\s*)?به\s*(?:سامانه|همراه\s*بانک|اینترنت\s*بانک|همراه\s*کارت|سیستم|اپلیکیشن|بام|ریما|ایوا|بانکدار|همت|آبان|آیپاد))""",
            """(?:تغییر\s*(?:رمز|شماره|تلفن)|بازیابی\s*(?:رمز|کلمه\s*عبور|حساب))""",
            """(?:کارت\s*(?:شما\s*)?(?:مسدود|غیرفعال|صادر|منقضی|باطل)|حساب\s*(?:شما\s*)?(?:مسدود|غیرفعال))""",
            """(?:مسدودسازی\s*کارت|صدور\s*کارت|کارت\s*المثنی|هشدار\s*امنیتی|تلاش\s*ناموفق)""",
            """(?:فعالسازی\s*(?:پیامک|همراه\s*بانک|خدمات))"""
        )
        for (pat in securityPatterns) {
            if (Pattern.compile(pat, Pattern.CASE_INSENSITIVE).matcher(norm).find()) {
                return true
            }
        }

        // 3. Promotions, Marketing, Contests, Loan Offers, App Downloads
        val promoPatterns = listOf(
            """(?:جشنواره|قرعه\s*کشی|برنده\s*(?:شدید|شوید|باشید)|جوایز|باشگاه\s*مشتریان)""",
            """(?:امتیاز\s*شما|کد\s*تخفیف|درصد\s*تخفیف|تسهیلات\s*بدون\s*ضامن|وام\s*بدون\s*ضامن|وام\s*فوری|وام\s*قرض\s*الحسنه)""",
            """(?:افتتاح\s*حساب\s*(?:آنلاین|غیرحضوری)|نصب\s*(?:اپلیکیشن|همراه\s*بانک|برنامه)|دانلود\s*(?:اپلیکیشن|همراه\s*بانک|برنامه|نسخه)|لینک\s*دانلود|پیشنهاد\s*ویژه)""",
            """(?:خرید\s*اقساطی|لغو\s*11|ارسال\s*11|لغو\s*پیامک|ارسال\s*عدد|ارسال\s*\d+\s*به)"""
        )
        for (pat in promoPatterns) {
            if (Pattern.compile(pat, Pattern.CASE_INSENSITIVE).matcher(norm).find()) {
                return true
            }
        }

        // 4. Failed, Cancelled, or Reversed Transactions
        val failedPatterns = listOf(
            """(?:تراکنش\s*ناموفق|عملیات\s*ناموفق|ناموفق\s*بود|عدم\s*موفقیت)""",
            """(?:تراکنش\s*لغو|لغو\s*(?:تراکنش|عملیات|گردید|شد))""",
            """(?:برگشت\s*(?:تراکنش|وجه|خورد|داده\s*شد))""",
            """(?:موجودی\s*ناکافی|کسری\s*موجودی|خطا\s*در\s*(?:تراکنش|پرداخت|عملیات)|تراکنش\s*انجام\s*نشد|تراکنش\s*با\s*خطا)"""
        )
        for (pat in failedPatterns) {
            if (Pattern.compile(pat, Pattern.CASE_INSENSITIVE).matcher(norm).find()) {
                return true
            }
        }

        // 5. Inquiries, Sayad Cheques, Installment Reminders, Pending Requests
        val noticePatterns = listOf(
            """(?:چک\s*صیاد|سامانه\s*صیاد|استعلام\s*چک|ت[اأآ]یید\s*چک|ثبت\s*چک|انتقال\s*چک)""",
            """(?:سررسید\s*(?:قسط|تسهیلات|وام)|یادآوری\s*(?:قسط|پرداخت|سررسید)|پرونده\s*تسهیلاتی)""",
            """(?:درخواست\s*(?:شما|انتقال|تسهیلات|دسته\s*چک)\s*(?:ثبت|بررسی|در\s*حال\s*بررسی|پذیرفته))""",
            """(?:صورتحساب\s*(?:دوره|ماهانه|صادر)|محاسبه\s*سود\s*سپرده)"""
        )
        for (pat in noticePatterns) {
            if (Pattern.compile(pat, Pattern.CASE_INSENSITIVE).matcher(norm).find()) {
                return true
            }
        }

        // 6. Pure Balance Inquiry (contains balance keyword BUT no actual transaction verbs)
        val hasBalanceWord = norm.contains("مانده") || norm.contains("موجودی") || norm.contains("موجودي")
        val hasTransactionVerb = norm.contains("برداشت") ||
                norm.contains("واریز") ||
                norm.contains("خرید") ||
                norm.contains("کسر") ||
                norm.contains("انتقال") ||
                norm.contains("بدهکار") ||
                norm.contains("بستانکار") ||
                norm.contains("منظور شد") ||
                norm.contains("پرید") ||
                norm.contains("نشست")

        if (hasBalanceWord && !hasTransactionVerb) {
            return true
        }

        return false
    }

    /**
     * Checks if the sender matches known bank shortcodes or alphanumeric identity.
     */
    fun isSenderRecognized(sender: String): Boolean {
        val clean = normalizeSender(sender)
        if (clean.isBlank()) return false

        return ALL_BANKS.any { bank ->
            bank.senderTokens.any { token ->
                val cleanToken = token.lowercase().replace(".", "")
                if (cleanToken.all { it.isDigit() }) {
                    clean == cleanToken || clean.removePrefix("0") == cleanToken
                } else {
                    clean == cleanToken || clean.replace(".", "") == cleanToken ||
                            (cleanToken.length >= 4 && (clean == "bank$cleanToken" || clean == "b$cleanToken"))
                }
            } || bank.bodyPatterns.any { clean.contains(it.replace(" ", "")) } ||
                    clean.contains(bank.displayName.replace(" ", ""))
        }
    }

    /**
     * Checks if any bank name is present in the SMS text.
     */
    fun hasBankNameInBody(body: String): Boolean {
        val lower = body.lowercase()
        return ALL_BANKS.any { bank ->
            bank.bodyPatterns.any { pattern -> lower.contains(pattern) }
        }
    }

    /**
     * Rigorous validation of bank identity and structural integrity:
     * - Known bank senders are accepted for further transaction/type/amount validation.
     * - Unknown senders need either strict bank/card/account metadata or strong transaction wording
     *   paired with an unambiguous direction and a positive amount. OTP, failed, and promotional
     *   messages are filtered before this fallback can be used.
     */
    fun hasValidBankEvidence(sender: String, cleanBody: String): Boolean {
        if (isSenderRecognized(sender)) {
            return true
        }

        val lower = cleanBody.lowercase()

        // 1. Account / Card pattern with digits or masking
        val hasCardPattern = Regex("""(?i)(?:کارت|card)[\s:]*([0-9\s*#-]{4,19})""").containsMatchIn(cleanBody)
        val hasAccountPattern = Regex("""(?i)(?:حساب|شماره\s*حساب|به\s*حساب|از\s*حساب)[\s:]*([0-9\s*#-]{4,20})""").containsMatchIn(cleanBody)
        val hasCardOrAccount = hasCardPattern || hasAccountPattern

        // 2. Ledger balance or tracking/reference code with digits
        val hasBalance = Regex("""(?i)(?:مانده|موجودی|موجودي)[\s:]*[+-]?\s*([0-9,.\u060C\u066B\u066C]+)""").containsMatchIn(cleanBody)
        val hasRefCode = Regex("""(?i)(?:پیگیری|کد\s*پیگیری|شماره\s*پیگیری|مرجع|شناسه\s*مرجع|شماره\s*مرجع|ارجاع|شماره\s*ارجاع|کد\s*ارجاع|پایانه|کد\s*رهگیری)[\s:]*([0-9]+)""").containsMatchIn(cleanBody)
        val hasLedgerMetadata = hasBalance || hasRefCode

        // 3. Bank identity in body
        val hasBankInBody = hasBankNameInBody(cleanBody) || lower.contains("بانک")

        if (hasCardOrAccount && hasLedgerMetadata && hasBankInBody) {
            return true
        }

        // Unknown bank senders may change; only allow a fallback when strong transaction
        // wording, a clear direction, and a positive amount are all present.
        val hasStrongTransactionLanguage = Regex(
            """(?:برداشت|واریز|بستانکار|بدهکار|کسر\s*(?:از\s*)?حساب|خرید|پرید|نشست|مبلغ\s*(?:تراکنش|برداشت|واریز)?|تراکنش\s*(?:موفق|انجام\s*شد)|منظور\s*شد|-\s*[0-9])"""
        ).containsMatchIn(lower)
        if (!hasStrongTransactionLanguage) return false

        val detectedType = detectTransactionType(cleanBody) ?: return false
        val detectedAmount = extractAmountToman(cleanBody, detectedType) ?: return false
        return detectedAmount > 0L
    }

    /**
     * Resolves the bank display name from sender first, then body text.
     */
    fun detectBankName(sender: String, cleanBody: String): String {
        val cleanSender = normalizeSender(sender)
        val lowerBody = cleanBody.lowercase()

        // 1. Check sender
        if (cleanSender.isNotBlank()) {
            for (bank in ALL_BANKS) {
                if (bank.senderTokens.any { token ->
                        val cleanToken = token.lowercase().replace(".", "")
                        if (cleanToken.all { it.isDigit() }) {
                            cleanSender == cleanToken || cleanSender.removePrefix("0") == cleanToken
                        } else {
                            cleanSender == cleanToken || cleanSender.replace(".", "") == cleanToken ||
                                    (cleanToken.length >= 4 && (cleanSender == "bank$cleanToken" || cleanSender == "b$cleanToken"))
                        }
                    } || bank.bodyPatterns.any { cleanSender.contains(it.replace(" ", "")) } ||
                    cleanSender.contains(bank.displayName.replace(" ", ""))) {
                    return bank.displayName
                }
            }
        }

        // 2. Check body patterns
        for (bank in ALL_BANKS) {
            if (bank.bodyPatterns.any { lowerBody.contains(it) }) {
                return bank.displayName
            }
        }

        return if (sender.isNotBlank()) "بانک ($sender)" else "پیامک بانکی"
    }

    /**
     * Context-aware detection of transaction type (EXPENSE, INCOME, TRANSFER).
     * Strictly verifies that the message represents a completed financial debit or credit.
     * Rejects ambiguous, failed, pending, or contradictory messages.
     */
    fun detectTransactionType(text: String): TransactionType? {
        val lower = normalizeText(text).lowercase()

        // Reject failed, reversed, cancelled, and not-yet-completed operations first.
        if (lower.contains("ناموفق") ||
            lower.contains("عدم موفقیت") ||
            lower.contains("موجودی ناکافی") ||
            lower.contains("تراکنش لغو") ||
            lower.contains("لغو تراکنش") ||
            lower.contains("برگشت تراکنش") ||
            lower.contains("برگشت وجه")
        ) return null

        if (lower.contains("درخواست انتقال") ||
            lower.contains("درخواست پایا") ||
            lower.contains("درخواست ساتنا") ||
            lower.contains("درخواست پرداخت") ||
            lower.contains("درخواست شما") ||
            lower.contains("در حال انجام") ||
            lower.contains("در حال بررسی") ||
            lower.contains("در حال پردازش") ||
            lower.contains("در صف انجام") ||
            lower.contains("در صف ارسال") ||
            lower.contains("در صف") ||
            lower.contains("ثبت شد و در حال") ||
            lower.contains("دستور پایا ثبت") ||
            lower.contains("دستور پرداخت ثبت") ||
            lower.contains("دستور انتقال ثبت")
        ) return null

        // Some banks (including virtual banks) use verbs such as "پرید" and "نشست".
        // When a single SMS mentions both directions, use the first explicit money event.
        val expenseMatch = Regex(
            """(?:برداشت|کسر\s*(?:از\s*)?حساب|کسر|بدهکار|خرید|پرداخت\s*(?:قبض|قبوض|صورتحساب)|پرید|کم\s*شد)"""
        ).find(lower)
        val incomeMatch = Regex(
            """(?:واریز|بستانکار|نشست|اضافه\s*شد|وارد\s*(?:حساب|شد)|منظور\s*شد|سود\s*سپرده|حقوق|یارانه)"""
        ).find(lower)

        if (expenseMatch != null && incomeMatch != null) {
            return if (expenseMatch.range.first <= incomeMatch.range.first) {
                TransactionType.EXPENSE
            } else {
                TransactionType.INCOME
            }
        }
        if (expenseMatch != null) return TransactionType.EXPENSE
        if (incomeMatch != null) return TransactionType.INCOME

        // A few banks encode a debit as a negative amount instead of using a word.
        val hasNegativeAmount = Regex("""-\s*[0-9][0-9,٫٬.]*""").containsMatchIn(lower)
        val hasTransactionContext = Regex("""(?:حساب|کارت|بانک|تراکنش|مبلغ|مانده|موجودی|سپرده)""")
            .containsMatchIn(lower)
        if (hasNegativeAmount && hasTransactionContext) return TransactionType.EXPENSE

        // If no explicit debit/credit verb exists, retain transfer only when its wording
        // really describes a transfer; an explicit incoming transfer is an income.
        val hasTransferWord = lower.contains("انتقال") ||
            lower.contains("کارت به کارت") || lower.contains("پایا") || lower.contains("ساتنا")
        if (hasTransferWord) {
            if (lower.contains("انتقال به حساب شما") ||
                lower.contains("واریز از طریق پایا") ||
                lower.contains("واریز از طریق ساتنا") ||
                lower.contains("واریز پایا") ||
                lower.contains("واریز ساتنا") ||
                lower.contains("واریز کارت به کارت") ||
                lower.contains("واریز حواله") ||
                lower.contains("به حساب شما") ||
                lower.contains("به کارت شما")
            ) return TransactionType.INCOME

            if (lower.contains("موفق") || lower.contains("به کارت") || lower.contains("به حساب")) {
                return TransactionType.TRANSFER
            }
        }
        return null
    }

    private enum class CurrencyType {
        TOMAN,
        RIAL,
        UNKNOWN
    }

    /**
     * Robust extraction of transaction amount in Toman.
     * Strictly verifies currency and conversion without arbitrary rounding or truncated digits.
     * Masks balances, card numbers, accounts, fees, and tracking/reference IDs.
     */
    fun extractAmountToman(text: String, expectedType: TransactionType? = null): Long? {
        val normalizedText = normalizeText(text)
        val cleanNumber = { value: String ->
            value.replace(",", "").replace("،", "").replace("٫", "")
                .replace("٬", "").replace(".", "").replace(" ", "").replace("+", "").replace("-", "").trim()
        }
        var maskedText = normalizedText

        // Mask non-transaction numbers before looking for an amount.
        maskedText = maskedText.replace(Regex("""(?i)(?:کارت|card)[\s:]*([0-9\s*#-]{4,19})""")) {
            " کارت [MASKED_CARD] "
        }
        maskedText = maskedText.replace(Regex("""(?i)(?:شماره\s*حساب|به\s*حساب|از\s*حساب|حساب)[\s:]*([0-9\s*#-]{5,20})""")) {
            " حساب [MASKED_ACCOUNT] "
        }
        maskedText = maskedText.replace(
            Regex("""(?i)(?:مانده(?:ی|\s+حساب|\s+واقعی|\s+کل)?|موجودی(?:ی|\s+حساب|\s+فعلی|\s+کل)?|موجودي)[\s:]*[+-]?\s*([0-9,\.\u060C\u066B\u066C]+)\s*(?:ریال|تومان|Rls|IRR)?""")
        ) { " [MASKED_BALANCE] " }
        maskedText = maskedText.replace(
            Regex("""(?i)(?:پیگیری|کد\s*پیگیری|شماره\s*پیگیری|مرجع|شناسه\s*مرجع|شماره\s*مرجع|ارجاع|شماره\s*ارجاع|کد\s*ارجاع|شناسه\s*قبض|شناسه\s*پرداخت|ترمینال|پایانه|کد\s*رهگیری|کد\s*پذیرنده)[\s:]*([0-9]+)""")
        ) { " [MASKED_REFERENCE] " }
        maskedText = maskedText.replace(
            Regex("""(?i)کارمزد[\s:]*[+-]?\s*([0-9,\.\u060C\u066B\u066C]+)\s*(?:ریال|تومان|Rls|IRR)?""")
        ) { " [MASKED_FEE] " }
        maskedText = maskedText.replace(Regex("""\b140[0-9][/-](?:0?[1-9]|1[0-2])[/-](?:0?[1-9]|[12][0-9]|3[01])\b""")) {
            " [MASKED_DATE] "
        }
        maskedText = maskedText.replace(Regex("""\b[0-2]?[0-9]:[0-5][0-9]\b""")) { " [MASKED_TIME] " }

        val lowerMasked = maskedText.lowercase()
        val eventPattern = when (expectedType) {
            TransactionType.EXPENSE -> Regex("""برداشت|کسر|بدهکار|خرید|پرداخت|پرید|کم\s*شد|-\s*[0-9]""")
            TransactionType.INCOME -> Regex("""واریز|بستانکار|نشست|حقوق|یارانه|سود\s*سپرده|اضافه\s*شد|وارد\s*(?:حساب|شد)|منظور\s*شد""")
            TransactionType.TRANSFER -> Regex("""کارت\s*به\s*کارت|انتقال|پایا|ساتنا""")
            null -> Regex("""برداشت|واریز|خرید|کسر|بدهکار|بستانکار|انتقال|کارت\s*به\s*کارت|پایا|ساتنا|پرید|نشست""")
        }
        val eventMatch = eventPattern.find(lowerMasked)

        val hasGlobalRial = normalizedText.contains("ریال", ignoreCase = true) ||
            normalizedText.contains("Rls", ignoreCase = true) ||
            normalizedText.contains("IRR", ignoreCase = true)
        val hasGlobalToman = normalizedText.contains("تومان", ignoreCase = true) ||
            normalizedText.contains("Toman", ignoreCase = true)
        val globalCurrency = when {
            hasGlobalToman && !hasGlobalRial -> CurrencyType.TOMAN
            hasGlobalRial && !hasGlobalToman -> CurrencyType.RIAL
            else -> CurrencyType.UNKNOWN
        }

        data class AmountCandidate(val start: Int, val rawNumber: Long, val currency: CurrencyType?)
        val candidates = Regex(
            """([+-]?\s*[0-9][0-9,\.\u060C\u066B\u066C]*)\s*(ریال|تومان|Rls|IRR)?""",
            RegexOption.IGNORE_CASE
        ).findAll(maskedText).mapNotNull { match ->
            val raw = cleanNumber(match.groupValues[1]).toLongOrNull() ?: return@mapNotNull null
            if (raw <= 0L) return@mapNotNull null
            val unit = match.groupValues.getOrNull(2).orEmpty()
            val currency = when {
                unit.contains("ریال", ignoreCase = true) ||
                    unit.contains("Rls", ignoreCase = true) ||
                    unit.contains("IRR", ignoreCase = true) -> CurrencyType.RIAL
                unit.contains("تومان", ignoreCase = true) ||
                    unit.contains("Toman", ignoreCase = true) -> CurrencyType.TOMAN
                else -> null
            }
            AmountCandidate(match.range.first, raw, currency)
        }.toList()
        if (candidates.isEmpty()) return null

        // Most templates put the transaction verb before its amount. Virtual banks may place
        // it after the amount (e.g. "125,000 ریال از کارت پرید" / "... به حساب نشست").
        val selected = if (eventMatch != null) {
            val actionText = eventMatch.value
            val textAfterAction = lowerMasked.substring((eventMatch.range.last + 1).coerceAtMost(lowerMasked.length))
            val isPostposedAction = actionText == "پرید" ||
                actionText == "نشست" ||
                actionText.contains("کم") ||
                actionText.contains("اضافه") ||
                (actionText == "کسر" && Regex("""^\s*(?:شد|گردید)""").containsMatchIn(textAfterAction)) ||
                (actionText == "بدهکار" && Regex("""^\s*شد""").containsMatchIn(textAfterAction))

            val amountWithinAction = candidates.firstOrNull {
                it.start >= eventMatch.range.first && it.start <= eventMatch.range.last
            }
            val amountBeforeAction = candidates.lastOrNull {
                it.start < eventMatch.range.first &&
                    eventMatch.range.first - it.start <= 120
            }
            val amountAfterAction = candidates.firstOrNull {
                it.start > eventMatch.range.last &&
                    it.start - eventMatch.range.last <= 240
            }

            if (isPostposedAction) {
                amountBeforeAction ?: amountAfterAction ?: amountWithinAction
                    ?: if (candidates.size == 1) candidates.first() else null
            } else {
                amountWithinAction ?: amountAfterAction ?: amountBeforeAction
                    ?: if (candidates.size == 1) candidates.first() else null
            }
        } else if (candidates.size == 1) {
            candidates.first()
        } else null
        val candidate = selected ?: return null

        val currency = candidate.currency ?: when {
            globalCurrency != CurrencyType.UNKNOWN -> globalCurrency
            // Amounts without a printed unit are commonly rial-denominated in bank SMS.
            // Users can correct the suggested amount in the review dialog before saving.
            eventMatch != null && expectedType != null -> CurrencyType.RIAL
            else -> CurrencyType.UNKNOWN
        }
        val amountToman = when (currency) {
            CurrencyType.TOMAN -> candidate.rawNumber
            CurrencyType.RIAL -> {
                if (candidate.rawNumber < 10L || candidate.rawNumber % 10L != 0L) return null
                candidate.rawNumber / 10L
            }
            CurrencyType.UNKNOWN -> return null
        }
        return amountToman.takeIf { it > 0L && it <= 100_000_000_000L }
    }

    /**
     * Unified pre-filtering gate:
     * Guarantees that only authentic debit or credit bank transaction SMS messages
     * with valid positive amounts and verified banking identity are accepted.
     */
    fun isPotentialBankSms(sender: String, body: String): Boolean {
        val cleanBody = normalizeText(body)

        // Stage 1: Absolute rejection of non-transactional messages (OTP, security, marketing, etc.)
        if (isNonTransactionalSms(cleanBody)) {
            return false
        }

        // Stage 2: Must have verified bank identity or strict banking structure
        if (!hasValidBankEvidence(sender, cleanBody)) {
            return false
        }

        // Stage 3: Must have an unambiguous debit or credit transaction type
        val detectedType = detectTransactionType(cleanBody) ?: return false

        // Stage 4: Must have a valid, positive extracted amount
        val amountToman = extractAmountToman(cleanBody, detectedType) ?: return false
        if (amountToman <= 0L) {
            return false
        }

        return true
    }

    /**
     * Complete parse pipeline:
     * Validates and constructs suggestion ONLY when all criteria are met.
     * Returns null if non-transactional, ambiguous, or invalid.
     */
    fun parse(smsId: String, sender: String, body: String, timestampMillis: Long): BankSmsSuggestion? {
        try {
            val cleanBody = normalizeText(body)

            // Stage 1: Definite rejection of non-transactional messages
            if (isNonTransactionalSms(cleanBody)) {
                return null
            }

            // Stage 2: Validate bank identity or authentic transaction structure
            if (!hasValidBankEvidence(sender, cleanBody)) {
                return null
            }

            // Stage 3: Strict detection of transaction type (must be clear withdrawal, deposit, or transfer)
            val detectedType = detectTransactionType(cleanBody) ?: return null

            // Stage 4: Strict extraction and validation of transaction amount
            val amountToman = extractAmountToman(cleanBody, detectedType)
            if (amountToman == null || amountToman <= 0L) {
                return null
            }

            // Stage 5: Detect bank display name
            val bankName = detectBankName(sender, cleanBody)

            // Stage 6: Category, dates, and accounts
            val category = suggestCategory(cleanBody, detectedType)
            val formattedAmount = when (detectedType) {
                TransactionType.EXPENSE -> MoneyFormatter.formatSignedToman(amountToman, isExpense = true)
                TransactionType.INCOME -> MoneyFormatter.formatSignedToman(amountToman, isExpense = false)
                TransactionType.TRANSFER -> MoneyFormatter.formatToman(amountToman)
            }

            val (dateText, timeText, finalTimestamp) = formatDateAndTime(timestampMillis, cleanBody)
            val (sourceAcc, destAcc) = extractAccounts(cleanBody, bankName)

            return BankSmsSuggestion(
                id = smsId,
                bankName = bankName,
                amount = amountToman,
                formattedAmount = formattedAmount,
                isAmountValid = true,
                type = detectedType,
                isTypeUncertain = false,
                smsText = body.trim(),
                dateText = dateText,
                timeText = timeText,
                category = category,
                sourceAccount = sourceAcc,
                destinationAccount = destAcc,
                rawSender = sender,
                parseError = null,
                timestampMillis = finalTimestamp
            )
        } catch (e: Exception) {
            // Keep logs secure: avoid printing SMS body or private details
            Log.w("BankSmsParser", "Failed to parse SMS $smsId")
            return null
        }
    }

    private fun suggestCategory(text: String, type: TransactionType): String {
        return when (type) {
            TransactionType.INCOME -> {
                when {
                    text.contains("حقوق") || text.contains("دستمزد") -> "حقوق و دستمزد"
                    text.contains("سود") || text.contains("سپرده") -> "سود سپرده"
                    text.contains("یارانه") -> "یارانه و کمک‌معیشتی"
                    else -> "درآمد و واریز"
                }
            }
            TransactionType.TRANSFER -> "انتقال بین‌بانکی"
            TransactionType.EXPENSE -> {
                when {
                    text.contains("افق کوروش") || text.contains("فروشگاه") || text.contains("هایپر") || text.contains("سوپر") || text.contains("دیلی مارکت") -> "سوپرمارکت و خرید"
                    text.contains("بنزین") || text.contains("جایگاه") || text.contains("پمپ") -> "خودرو و سوخت"
                    text.contains("اسنپ") || text.contains("تپسی") || text.contains("تاکسی") || text.contains("مترو") -> "حمل و نقل"
                    text.contains("رستوران") || text.contains("کافه") || text.contains("فست فود") || text.contains("اسنپ فود") -> "غذا و رستوران"
                    text.contains("داروخانه") || text.contains("درمان") || text.contains("بیمارستان") || text.contains("پزشک") -> "سلامت و درمان"
                    text.contains("قبض") || text.contains("همراه اول") || text.contains("ایرانسل") || text.contains("مخابرات") -> "قبوض و خدمات"
                    text.contains("قسط") || text.contains("تسهیلات") || text.contains("وام") -> "اقساط و تسهیلات"
                    else -> "خرید روزمره"
                }
            }
        }
    }

    private fun formatDateAndTime(timestampMillis: Long, cleanBody: String): Triple<String, String, Long> {
        val defaultPdt = PersianCalendarHelper.fromEpochMillis(timestampMillis)

        val datePattern = Pattern.compile("""\b(140[0-9])[/-](0?[1-9]|1[0-2])[/-](0?[1-9]|[12][0-9]|3[01])\b""")
        val timePattern = Pattern.compile("""(?:ساعت[\s:]+)([0-2]?[0-9]:[0-5][0-9])""")

        val dateMatcher = datePattern.matcher(cleanBody)
        val timeMatcher = timePattern.matcher(cleanBody)

        val extractedDate = if (dateMatcher.find()) {
            val y = dateMatcher.group(1)?.toIntOrNull() ?: defaultPdt.year
            val m = dateMatcher.group(2)?.toIntOrNull() ?: defaultPdt.month
            val d = dateMatcher.group(3)?.toIntOrNull() ?: defaultPdt.day
            Triple(y, m, d)
        } else null

        val extractedTime = if (timeMatcher.find()) {
            timeMatcher.group(1)
        } else null

        val finalDateText = if (extractedDate != null) {
            String.format("%04d/%02d/%02d", extractedDate.first, extractedDate.second, extractedDate.third)
        } else {
            defaultPdt.toFormattedDate()
        }

        val finalTimeText = extractedTime ?: defaultPdt.toFormattedTime()

        val finalTimestamp = if (extractedDate != null) {
            val (h, min) = if (extractedTime != null && extractedTime.contains(":")) {
                val parts = extractedTime.split(":")
                Pair(parts[0].toIntOrNull() ?: 12, parts[1].toIntOrNull() ?: 0)
            } else {
                Pair(defaultPdt.hour, defaultPdt.minute)
            }
            try {
                PersianCalendarHelper.jalaliToEpochMillis(
                    extractedDate.first,
                    extractedDate.second,
                    extractedDate.third,
                    h,
                    min
                )
            } catch (e: Exception) {
                timestampMillis
            }
        } else {
            timestampMillis
        }

        return Triple(finalDateText, finalTimeText, finalTimestamp)
    }

    private fun extractAccounts(cleanBody: String, defaultBankName: String): Pair<String?, String?> {
        val destPattern = Pattern.compile("""(?i)(?:به\s*کارت|به\s*حساب|مقصد|به\s*شماره\s*کارت)[\s:]*([0-9*#-]{4,19})""")
        val destMatcher = destPattern.matcher(cleanBody)
        var explicitDest: String? = null
        if (destMatcher.find()) {
            val num = destMatcher.group(1)?.trim()
            if (!num.isNullOrBlank() && num.length >= 4) {
                explicitDest = "کارت $num"
            }
        }

        val cardPattern = Pattern.compile("""(?i)(?:کارت|card)[\s:]*([0-9*#-]{4,19})""")
        val cardMatcher = cardPattern.matcher(cleanBody)
        val foundCards = mutableListOf<String>()

        while (cardMatcher.find()) {
            val card = cardMatcher.group(1)?.trim()
            if (!card.isNullOrBlank() && card.length >= 4) {
                foundCards.add("کارت $card")
            }
        }

        val sourceAccount: String?
        val destAccount: String?

        if (explicitDest != null) {
            destAccount = explicitDest
            val remainingCards = foundCards.filter { it != explicitDest }
            sourceAccount = if (remainingCards.isNotEmpty()) remainingCards.first() else defaultBankName
        } else {
            sourceAccount = if (foundCards.isNotEmpty()) foundCards.first() else defaultBankName
            destAccount = if (foundCards.size > 1) foundCards[1] else null
        }

        return Pair(sourceAccount, destAccount)
    }
}
