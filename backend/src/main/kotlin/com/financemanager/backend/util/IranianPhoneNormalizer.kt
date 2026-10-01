package com.financemanager.backend.util

object IranianPhoneNormalizer {

    private val PERSIAN_TO_LATIN_DIGITS = mapOf(
        '۰' to '0', '۱' to '1', '۲' to '2', '۳' to '3', '۴' to '4',
        '۵' to '5', '۶' to '6', '۷' to '7', '۸' to '8', '۹' to '9',
        '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
        '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9'
    )

    private val IRANIAN_MOBILE_PATTERN = Regex("^\\+989[0-9]{9}$")

    /**
     * Converts Persian/Arabic digits to ASCII Latin digits and removes all whitespace/punctuation.
     */
    fun cleanDigits(raw: String): String {
        val converted = StringBuilder()
        for (char in raw.trim()) {
            if (char in PERSIAN_TO_LATIN_DIGITS) {
                converted.append(PERSIAN_TO_LATIN_DIGITS[char])
            } else if (char.isDigit() || char == '+') {
                converted.append(char)
            }
        }
        return converted.toString()
    }

    /**
     * Normalizes Iranian mobile phone numbers to canonical E.164 format (+98912xxxxxxx).
     * Returns null if the number is not a valid Iranian mobile number.
     */
    fun normalize(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        var cleaned = cleanDigits(raw)

        if (cleaned.startsWith("0098")) {
            cleaned = "+98" + cleaned.substring(4)
        } else if (cleaned.startsWith("09")) {
            cleaned = "+98" + cleaned.substring(1)
        } else if (cleaned.startsWith("98") && cleaned.length == 12) {
            cleaned = "+$cleaned"
        } else if (cleaned.startsWith("9") && cleaned.length == 10) {
            cleaned = "+98$cleaned"
        }

        return if (isValid(cleaned)) cleaned else null
    }

    /**
     * Validates if the string is a valid E.164 Iranian mobile number (+989xxxxxxxxx).
     */
    fun isValid(normalized: String?): Boolean {
        if (normalized.isNullOrBlank()) return false
        return IRANIAN_MOBILE_PATTERN.matches(normalized)
    }

    /**
     * Masks the phone number for secure display (e.g., 0912••••4567 or +98912••••4567).
     */
    fun mask(phone: String?): String {
        if (phone.isNullOrBlank()) return ""
        val normalized = normalize(phone) ?: phone
        return if (normalized.startsWith("+989") && normalized.length == 13) {
            val localFormat = "0" + normalized.substring(3)
            "${localFormat.substring(0, 4)}••••${localFormat.substring(7)}"
        } else if (phone.length >= 8) {
            "${phone.substring(0, 4)}••••${phone.substring(phone.length - 4)}"
        } else {
            "••••"
        }
    }

    /**
     * Detects Iranian mobile operator.
     */
    fun detectOperator(phone: String?): IranianOperator {
        val normalized = normalize(phone) ?: return IranianOperator.UNKNOWN
        val prefix3 = normalized.substring(3, 6) // e.g. "912", "935", "921"
        val prefix4 = normalized.substring(3, 7) // e.g. "9981"

        return when {
            prefix3 in listOf("910", "911", "912", "913", "914", "915", "916", "917", "918", "919", "990", "991", "992", "993", "994") -> IranianOperator.MCI
            prefix3 in listOf("930", "933", "935", "936", "937", "938", "939", "901", "902", "903", "904", "905", "941") -> IranianOperator.IRANCELL
            prefix3 in listOf("920", "921", "922", "923") -> IranianOperator.RIGHTEL
            prefix4 in listOf("9981") -> IranianOperator.SHATEL_MOBILE
            else -> IranianOperator.OTHER
        }
    }
}

enum class IranianOperator(val persianName: String) {
    MCI("همراه اول"),
    IRANCELL("ایرانسل"),
    RIGHTEL("رایتل"),
    SHATEL_MOBILE("شاتل موبایل"),
    OTHER("سایر اپراتورها"),
    UNKNOWN("نامشخص")
}
