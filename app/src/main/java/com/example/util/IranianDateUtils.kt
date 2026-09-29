package com.example.util

/**
 * Utility functions for Persian date formatting & input masking:
 * - Persian/Arabic/English digits normalization
 * - Automatic slash insertion (14030715 -> 1403/07/15 / ۱۴۰۳/۰۷/۱۵)
 * - Safe extraction of Year, Month, Day
 * - Validation of Persian dates
 */
object IranianDateUtils {

    /**
     * Cleans input to digits only (English digits 0-9), capping at 8 characters (YYYYMMDD).
     */
    fun cleanDateDigits(input: String): String {
        val englishStr = IranianPhoneUtils.convertDigitsToEnglish(input)
        val digits = englishStr.filter { it in '0'..'9' }
        return if (digits.length > 8) digits.substring(0, 8) else digits
    }

    /**
     * Formats digits into a Persian date string with slashes.
     * Handles typing in progress:
     * - "1403" -> "1403" (or "۱۴۰۳")
     * - "14030" -> "1403/0" (or "۱۴۰۳/۰")
     * - "140307" -> "1403/07" (or "۱۴۰۳/۰۷")
     * - "1403071" -> "1403/07/1" (or "۱۴۰۳/۰۷/۱")
     * - "14030715" -> "1403/07/15" (or "۱۴۰۳/۰۷/۱۵")
     */
    fun formatWithSlashes(input: String, inPersian: Boolean = true): String {
        val digits = cleanDateDigits(input)
        if (digits.isEmpty()) return ""

        val builder = StringBuilder()
        for (i in digits.indices) {
            builder.append(digits[i])
            if (i == 3 && digits.length > 4) {
                builder.append('/')
            } else if (i == 5 && digits.length > 6) {
                builder.append('/')
            }
        }

        val result = builder.toString()
        return if (inPersian) {
            IranianPhoneUtils.convertDigitsToPersian(result)
        } else {
            result
        }
    }

    /**
     * Parses a date string (formatted as YYYY/MM/DD or YYYYMMDD in Persian or English)
     * into (year, month, day). Returns defaults (current date) if invalid or incomplete.
     */
    fun parsePersianDate(dateString: String): Triple<Int, Int, Int> {
        val digits = cleanDateDigits(dateString)
        val currentPdt = PersianCalendarHelper.fromEpochMillis(System.currentTimeMillis())

        if (digits.length >= 8) {
            val y = digits.substring(0, 4).toIntOrNull() ?: currentPdt.year
            val m = digits.substring(4, 6).toIntOrNull()?.coerceIn(1, 12) ?: currentPdt.month
            val d = digits.substring(6, 8).toIntOrNull()?.coerceIn(1, 31) ?: currentPdt.day
            return Triple(y, m, d)
        } else if (digits.length >= 4) {
            val y = digits.substring(0, 4).toIntOrNull() ?: currentPdt.year
            return Triple(y, currentPdt.month, currentPdt.day)
        }

        return Triple(currentPdt.year, currentPdt.month, currentPdt.day)
    }

    /**
     * Formats (year, month, day) into standard Persian date string: "۱۴۰۴/۰۷/۱۵"
     */
    fun createFormattedDate(year: Int, month: Int, day: Int, inPersian: Boolean = true): String {
        val y = year.toString().padStart(4, '0')
        val m = month.toString().padStart(2, '0')
        val d = day.toString().padStart(2, '0')
        val str = "$y/$m/$d"
        return if (inPersian) IranianPhoneUtils.convertDigitsToPersian(str) else str
    }

    /**
     * Returns true if string is a complete and valid Persian date (YYYY/MM/DD with 8 digits).
     */
    fun isValidPersianDate(input: String): Boolean {
        val digits = cleanDateDigits(input)
        if (digits.length != 8) return false
        val y = digits.substring(0, 4).toIntOrNull() ?: return false
        val m = digits.substring(4, 6).toIntOrNull() ?: return false
        val d = digits.substring(6, 8).toIntOrNull() ?: return false
        if (y < 1300 || y > 1500) return false
        if (m !in 1..12) return false
        val maxDays = PersianCalendarHelper.getDaysInMonth(y, m)
        return d in 1..maxDays
    }
}
