package com.example.util

import java.util.Calendar
import java.util.TimeZone

/**
 * Robust Persian (Jalali / Solar Hijri) date conversion and formatting utility.
 */
object PersianCalendarHelper {

    val PERSIAN_MONTH_NAMES = listOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    val WEEKDAY_NAMES = listOf(
        "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه", "شنبه"
    )

    data class PersianDateTime(
        val year: Int,
        val month: Int, // 1..12
        val day: Int,   // 1..31
        val hour: Int = 9,
        val minute: Int = 0
    ) {
        val monthName: String
            get() = PERSIAN_MONTH_NAMES.getOrElse(month - 1) { "" }

        fun toFormattedDate(): String {
            val y = IranianPhoneUtils.convertDigitsToPersian(year.toString())
            val m = IranianPhoneUtils.convertDigitsToPersian(month.toString().padStart(2, '0'))
            val d = IranianPhoneUtils.convertDigitsToPersian(day.toString().padStart(2, '0'))
            return "$y/$m/$d"
        }

        fun toFormattedTime(): String {
            val h = IranianPhoneUtils.convertDigitsToPersian(hour.toString().padStart(2, '0'))
            val min = IranianPhoneUtils.convertDigitsToPersian(minute.toString().padStart(2, '0'))
            return "$h:$min"
        }

        fun toFullPersianString(): String {
            val y = IranianPhoneUtils.convertDigitsToPersian(year.toString())
            val d = IranianPhoneUtils.convertDigitsToPersian(day.toString())
            val time = toFormattedTime()
            return "$d $monthName $y - ساعت $time"
        }

        fun toEpochMillis(): Long {
            return jalaliToEpochMillis(year, month, day, hour, minute)
        }
    }

    /**
     * Converts a millisecond timestamp to PersianDateTime in default timezone.
     */
    fun fromEpochMillis(millis: Long): PersianDateTime {
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = millis
        }
        val gy = cal.get(Calendar.YEAR)
        val gm = cal.get(Calendar.MONTH) + 1
        val gd = cal.get(Calendar.DAY_OF_MONTH)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)

        val (jy, jm, jd) = gregorianToJalali(gy, gm, gd)
        return PersianDateTime(
            year = jy,
            month = jm,
            day = jd,
            hour = hour,
            minute = minute
        )
    }

    /**
     * Converts Persian date & time to epoch milliseconds.
     */
    fun jalaliToEpochMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        val (gy, gm, gd) = jalaliToGregorian(year, month, day)
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            set(Calendar.YEAR, gy)
            set(Calendar.MONTH, gm - 1)
            set(Calendar.DAY_OF_MONTH, gd)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * Calculates days in a Persian month.
     */
    fun getDaysInMonth(year: Int, month: Int): Int {
        return when {
            month in 1..6 -> 31
            month in 7..11 -> 30
            month == 12 -> if (isLeapJalaliYear(year)) 30 else 29
            else -> 30
        }
    }

    fun isLeapJalaliYear(year: Int): Boolean {
        val r = year % 33
        return r == 1 || r == 5 || r == 9 || r == 13 || r == 17 || r == 22 || r == 26 || r == 30
    }

    /**
     * Converts Gregorian Year/Month/Day to Jalali Year/Month/Day.
     */
    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
        val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        val gy2 = gy - 1600
        val gm2 = gm - 1
        val gd2 = gd - 1

        var gDayNo = 365 * gy2 + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400

        for (i in 0 until gm2) {
            gDayNo += gDaysInMonth[i]
        }
        if (gm2 > 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) {
            gDayNo++
        }
        gDayNo += gd2

        var jDayNo = gDayNo - 79

        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }

        var jm = 0
        for (i in 0..11) {
            val days = jDaysInMonth[i]
            if (jDayNo < days) {
                jm = i + 1
                break
            }
            jDayNo -= days
        }
        val jd = jDayNo + 1
        return Triple(jy, jm, jd)
    }

    /**
     * Converts Jalali Year/Month/Day to Gregorian Year/Month/Day.
     */
    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): Triple<Int, Int, Int> {
        val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        val jy2 = jy - 979
        val jm2 = jm - 1
        val jd2 = jd - 1

        var jDayNo = 365 * jy2 + (jy2 / 33) * 8 + ((jy2 % 33 + 3) / 4)
        for (i in 0 until jm2) {
            jDayNo += jDaysInMonth[i]
        }
        jDayNo += jd2

        var gDayNo = jDayNo + 79

        var gy = 1600 + 400 * (gDayNo / 146097)
        gDayNo %= 146097

        var leap = true
        if (gDayNo >= 36525) {
            gDayNo--
            gy += 100 * (gDayNo / 36524)
            gDayNo %= 36524

            if (gDayNo >= 365) {
                gDayNo++
            } else {
                leap = false
            }
        }

        gy += 4 * (gDayNo / 1461)
        gDayNo %= 1461

        if (gDayNo >= 366) {
            leap = false
            gDayNo--
            gy += gDayNo / 365
            gDayNo %= 365
        }

        var gm = 0
        for (i in 0..11) {
            var days = gDaysInMonth[i]
            if (i == 1 && leap) {
                days = 29
            }
            if (gDayNo < days) {
                gm = i + 1
                break
            }
            gDayNo -= days
        }
        val gd = gDayNo + 1
        return Triple(gy, gm, gd)
    }

    /**
     * Produces a humanized Persian relative time, e.g. "در ۲ ساعت آینده" or "۳ روز پیش".
     */
    fun formatRelativeTimePersian(targetMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val diff = targetMillis - nowMillis
        val isFuture = diff >= 0
        val absDiff = kotlin.math.abs(diff)

        val minutes = absDiff / (60 * 1000L)
        val hours = absDiff / (3600 * 1000L)
        val days = absDiff / (24 * 3600 * 1000L)

        val text = when {
            minutes < 1 -> "همین الان"
            minutes < 60 -> "${IranianPhoneUtils.convertDigitsToPersian(minutes.toString())} دقیقه"
            hours < 24 -> {
                val remMin = minutes % 60
                if (remMin > 0) {
                    "${IranianPhoneUtils.convertDigitsToPersian(hours.toString())} ساعت و ${IranianPhoneUtils.convertDigitsToPersian(remMin.toString())} دقیقه"
                } else {
                    "${IranianPhoneUtils.convertDigitsToPersian(hours.toString())} ساعت"
                }
            }
            else -> {
                val remHours = hours % 24
                if (remHours > 0) {
                    "${IranianPhoneUtils.convertDigitsToPersian(days.toString())} روز و ${IranianPhoneUtils.convertDigitsToPersian(remHours.toString())} ساعت"
                } else {
                    "${IranianPhoneUtils.convertDigitsToPersian(days.toString())} روز"
                }
            }
        }

        return if (minutes < 1) {
            text
        } else if (isFuture) {
            "در $text آینده"
        } else {
            "$text پیش (گذشته)"
        }
    }
}
