package com.example.calendar.domain

import com.example.util.IranianPhoneUtils
import com.example.util.PersianCalendarHelper
import java.util.Calendar

object CalendarDateUtils {

    /**
     * Converts any Persian or English digit date string to standard ASCII "YYYY/MM/DD"
     */
    fun normalizeDate(date: String): String {
        val english = IranianPhoneUtils.convertDigitsToEnglish(date.trim())
        val parts = english.split('/', '-')
        if (parts.size == 3) {
            val y = parts[0].padStart(4, '0')
            val m = parts[1].padStart(2, '0')
            val d = parts[2].padStart(2, '0')
            return "$y/$m/$d"
        }
        return english
    }

    fun toPersianDisplay(date: String): String {
        return IranianPhoneUtils.convertDigitsToPersian(normalizeDate(date))
    }

    /**
     * Extracts Year, Month, Day as integers (Jalali).
     */
    fun parseJalali(date: String): Triple<Int, Int, Int>? {
        val norm = normalizeDate(date)
        val parts = norm.split('/')
        if (parts.size == 3) {
            val y = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            val d = parts[2].toIntOrNull() ?: return null
            return Triple(y, m, d)
        }
        return null
    }

    /**
     * Formats Jalali year, month, day to standard "YYYY/MM/DD"
     */
    fun formatJalali(year: Int, month: Int, day: Int): String {
        return "${year.toString().padStart(4, '0')}/${month.toString().padStart(2, '0')}/${day.toString().padStart(2, '0')}"
    }

    /**
     * Get current Jalali date (year, month, day).
     */
    fun getCurrentJalaliDate(): Triple<Int, Int, Int> {
        val now = System.currentTimeMillis()
        val dt = PersianCalendarHelper.fromEpochMillis(now)
        return Triple(dt.year, dt.month, dt.day)
    }

    /**
     * Returns the weekday of the 1st of a given Jalali month:
     * 0 = Shanbeh (Saturday)
     * 1 = Yekshanbeh (Sunday)
     * 2 = Doshanbeh (Monday)
     * 3 = Seshanbeh (Tuesday)
     * 4 = Chaharshanbeh (Wednesday)
     * 5 = Panjshanbeh (Thursday)
     * 6 = Jomeh (Friday)
     */
    fun getFirstDayOfWeekForJalaliMonth(year: Int, month: Int): Int {
        val (gy, gm, gd) = PersianCalendarHelper.jalaliToGregorian(year, month, 1)
        val cal = Calendar.getInstance().apply {
            set(gy, gm - 1, gd)
        }
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
    }

    fun getDaysInJalaliMonth(year: Int, month: Int): Int {
        return PersianCalendarHelper.getDaysInMonth(year, month)
    }

    fun getMonthName(month: Int): String {
        return PersianCalendarHelper.PERSIAN_MONTH_NAMES.getOrElse(month - 1) { "" }
    }

    /**
     * Validates if a string is a valid Jalali date (YYYY/MM/DD)
     */
    fun isValidJalaliDate(date: String): Boolean {
        val parts = parseJalali(date) ?: return false
        val (y, m, d) = parts
        
        if (y < 1000 || y > 1600) return false
        if (m < 1 || m > 12) return false
        if (d < 1) return false
        
        val maxDays = getDaysInJalaliMonth(y, m)
        return d <= maxDays
    }
}
