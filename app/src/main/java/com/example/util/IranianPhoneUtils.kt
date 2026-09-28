package com.example.util

/**
 * Utility functions for Iranian phone numbers:
 * - Persian/Arabic digit conversion (۰-۹, ٠-٩ -> 0-9)
 * - Number normalization (e.g. +98, 0098, 98, 09... -> standard 09xxxxxxxxx)
 * - Formatting & Masking (e.g. 0912••••789 or ۰۹۱۲••••۷۸۹)
 * - Validation for Iranian mobile operators (MCI, MTN Irancell, Rightel, Shatel, etc.)
 */
object IranianPhoneUtils {

    /**
     * Converts Persian and Arabic digits to standard ASCII English digits.
     */
    fun convertDigitsToEnglish(input: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        
        val sb = StringBuilder()
        for (char in input) {
            val pIndex = persianDigits.indexOf(char)
            if (pIndex != -1) {
                sb.append(pIndex)
                continue
            }
            val aIndex = arabicDigits.indexOf(char)
            if (aIndex != -1) {
                sb.append(aIndex)
                continue
            }
            sb.append(char)
        }
        return sb.toString()
    }

    /**
     * Converts standard English digits to Persian digits for display.
     */
    fun convertDigitsToPersian(input: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val sb = StringBuilder()
        for (char in input) {
            if (char in '0'..'9') {
                sb.append(persianDigits[char - '0'])
            } else {
                sb.append(char)
            }
        }
        return sb.toString()
    }

    /**
     * Normalizes Iranian phone number into standard 11-digit format starting with "09".
     * Handles:
     * - "+989123456789" -> "09123456789"
     * - "00989123456789" -> "09123456789"
     * - "989123456789" -> "09123456789"
     * - "9123456789" -> "09123456789"
     * - "09123456789" -> "09123456789"
     * Returns null if the number does not match valid Iranian mobile patterns.
     */
    fun normalizeIranianPhoneNumber(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        
        var clean = convertDigitsToEnglish(raw.trim())
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
            .replace("/", "")

        if (clean.startsWith("+98")) {
            clean = "0" + clean.substring(3)
        } else if (clean.startsWith("0098")) {
            clean = "0" + clean.substring(4)
        } else if (clean.startsWith("98") && clean.length == 12) {
            clean = "0" + clean.substring(2)
        } else if (clean.startsWith("9") && clean.length == 10) {
            clean = "0$clean"
        }

        if (isValidIranianMobile(clean)) {
            return clean
        }
        return null
    }

    /**
     * Validates if standard 11-digit phone string is a valid Iranian mobile number.
     * Pattern: 09 followed by 9 digits.
     */
    fun isValidIranianMobile(phoneNumber: String): Boolean {
        if (phoneNumber.length != 11) return false
        if (!phoneNumber.startsWith("09")) return false
        return phoneNumber.all { it.isDigit() }
    }

    /**
     * Masks an Iranian phone number for secure display, e.g. 0912••••789
     */
    fun maskPhoneNumber(phoneNumber: String?, inPersianDigits: Boolean = false): String {
        if (phoneNumber.isNullOrBlank()) return ""
        val normalized = normalizeIranianPhoneNumber(phoneNumber) ?: phoneNumber
        val masked = if (normalized.length == 11) {
            "${normalized.substring(0, 4)}••••${normalized.substring(7)}"
        } else if (normalized.length >= 7) {
            "${normalized.take(3)}••••${normalized.takeLast(3)}"
        } else {
            normalized
        }
        return if (inPersianDigits) convertDigitsToPersian(masked) else masked
    }

    /**
     * Formats phone number into spaced groups: "0912 345 6789"
     */
    fun formatPhoneNumber(phoneNumber: String?, inPersianDigits: Boolean = false): String {
        if (phoneNumber.isNullOrBlank()) return ""
        val norm = normalizeIranianPhoneNumber(phoneNumber) ?: phoneNumber
        val formatted = if (norm.length == 11) {
            "${norm.substring(0, 4)} ${norm.substring(4, 7)} ${norm.substring(7)}"
        } else {
            norm
        }
        return if (inPersianDigits) convertDigitsToPersian(formatted) else formatted
    }

    /**
     * Converts an Iranian phone number to standard E.164 international format (+989123456789)
     */
    fun toE164Format(phoneNumber: String?): String {
        val norm = normalizeIranianPhoneNumber(phoneNumber) ?: return phoneNumber ?: ""
        return "+98" + norm.substring(1)
    }

    /**
     * Identifies operator name in Persian (همراه اول, ایرانسل, رایتل, شاتل موبایل, ...)
     */
    fun getOperatorName(phoneNumber: String?): String {
        val norm = normalizeIranianPhoneNumber(phoneNumber) ?: return "نامشخص"
        if (norm.length < 4) return "نامشخص"
        val prefix = norm.substring(0, 4)
        return when (prefix) {
            "0910", "0911", "0912", "0913", "0914", "0915", "0916", "0917", "0918", "0919", "0990", "0991", "0992", "0993", "0994" -> "همراه اول (MCI)"
            "0930", "0933", "0935", "0936", "0937", "0938", "0939", "0901", "0902", "0903", "0904", "0905" -> "ایرانسل (MTN)"
            "0920", "0921", "0922" -> "رایتل (Rightel)"
            "0998" -> "شاتل موبایل (Shatel)"
            "0999" -> "سامانتل / آپتل"
            else -> "اپراتور تلفن همراه"
        }
    }
}
