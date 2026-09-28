package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class PersianCalendarHelperTest {

    @Test
    fun testGregorianToJalaliKnownDates() {
        // 2024-03-20 is 1403-01-01 (Nowruz)
        val (jy, jm, jd) = PersianCalendarHelper.gregorianToJalali(2024, 3, 20)
        assertEquals(1403, jy)
        assertEquals(1, jm)
        assertEquals(1, jd)

        // And reverse
        val (gy, gm, gd) = PersianCalendarHelper.jalaliToGregorian(1403, 1, 1)
        assertEquals(2024, gy)
        assertEquals(3, gm)
        assertEquals(20, gd)
    }

    @Test
    fun testRoundTripFromEpochMillis() {
        val now = System.currentTimeMillis()
        val pdt = PersianCalendarHelper.fromEpochMillis(now)
        val backToMillis = pdt.toEpochMillis()

        // Difference should be less than 60 seconds (since seconds/millis are rounded to minute in PersianDateTime)
        val diff = Math.abs(now - backToMillis)
        assertTrue("Epoch roundtrip diff ($diff ms) should be within 60 seconds", diff < 60000)
    }

    @Test
    fun testFormatPersian() {
        val dt = PersianCalendarHelper.PersianDateTime(
            year = 1405,
            month = 6,
            day = 30,
            hour = 10,
            minute = 30
        )
        assertEquals("۱۴۰۵/۰۶/۳۰", dt.toFormattedDate())
        assertEquals("۱۰:۳۰", dt.toFormattedTime())
        assertEquals("۳۰ شهریور ۱۴۰۵ - ساعت ۱۰:۳۰", dt.toFullPersianString())
    }
}
