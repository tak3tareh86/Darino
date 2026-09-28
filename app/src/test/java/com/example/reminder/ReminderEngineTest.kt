package com.example.reminder

import com.example.reminder.data.ReminderEntity
import com.example.reminder.data.ReminderScheduleEntity
import com.example.reminder.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ReminderEngineTest {

    @Test
    fun testNotificationIdGeneratorIsDeterministic() {
        val reminderId = "rem_test_123"
        val scheduleId = "sch_test_456"
        val triggerTime = 1735689600000L

        val id1 = NotificationIdGenerator.generateNotificationId(reminderId, scheduleId, triggerTime)
        val id2 = NotificationIdGenerator.generateNotificationId(reminderId, scheduleId, triggerTime)

        assertEquals("Deterministic notification ID must match across executions", id1, id2)
        assertTrue("Notification ID must be positive integer", id1 >= 0)
    }

    @Test
    fun testNotificationIdGeneratorDistinctForDifferentTriggers() {
        val reminderId = "rem_test_123"
        val id1 = NotificationIdGenerator.generateNotificationId(reminderId, "sch_1", 1000L)
        val id2 = NotificationIdGenerator.generateNotificationId(reminderId, "sch_2", 2000L)

        assertNotEquals("Different schedules must produce different notification IDs", id1, id2)
    }

    @Test
    fun testRecurrenceEngineDaily() {
        val baseTime = 1700000000000L
        val nextTime = RecurrenceEngine.calculateNextOccurrence(baseTime, RepeatType.DAILY, 1)

        val diffMillis = nextTime - baseTime
        assertEquals("Daily recurrence should add exactly 24 hours", 24 * 60 * 60 * 1000L, diffMillis)
    }

    @Test
    fun testRecurrenceEngineWeekly() {
        val baseTime = 1700000000000L
        val nextTime = RecurrenceEngine.calculateNextOccurrence(baseTime, RepeatType.WEEKLY, 2)

        val diffMillis = nextTime - baseTime
        assertEquals("2-week recurrence should add 14 days", 14L * 24 * 60 * 60 * 1000L, diffMillis)
    }

    @Test
    fun testReminderPolicyEngineQuietHoursAllowedForHighPriority() {
        val tz = TimeZone.getTimeZone("Asia/Tehran")
        val nightCal = Calendar.getInstance(tz).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 30)
        }

        val highPriorityReminder = ReminderEntity(
            title = "سررسید فوری قسط",
            priority = "HIGH",
            date = "۱۴۰۵/۰۷/۳۰",
            time = "۲۳:۳۰"
        )

        val quietHours = QuietHoursConfig(
            enabled = true,
            startHour = 23,
            startMinute = 0,
            endHour = 7,
            endMinute = 0,
            allowHighPriority = true
        )

        val decision = ReminderPolicyEngine.evaluateDispatch(
            highPriorityReminder,
            quietHours,
            nightCal.timeInMillis
        )

        assertEquals("High priority reminder during quiet hours should deliver immediately when allowed",
            DispatchDecision.DeliverNow, decision)
    }

    @Test
    fun testReminderPolicyEngineQuietHoursPostponedForNormalPriority() {
        val tz = TimeZone.getTimeZone("Asia/Tehran")
        val nightCal = Calendar.getInstance(tz).apply {
            set(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 15)
        }

        val normalReminder = ReminderEntity(
            title = "سرویس دوره‌ای",
            priority = "NORMAL",
            date = "۱۴۰۵/۰۷/۳۰",
            time = "۰۱:۱۵"
        )

        val quietHours = QuietHoursConfig(
            enabled = true,
            startHour = 23,
            startMinute = 0,
            endHour = 7,
            endMinute = 0,
            allowHighPriority = true
        )

        val decision = ReminderPolicyEngine.evaluateDispatch(
            normalReminder,
            quietHours,
            nightCal.timeInMillis
        )

        assertTrue("Normal reminder during quiet hours should be postponed", decision is DispatchDecision.PostponeTo)
    }

    @Test
    fun testMockSmsDispatcherReturnsSuccessAndValidId() = runBlocking {
        val dispatcher = MockSmsDispatcher()
        val result = dispatcher.sendSms(
            phoneNumber = "09121234567",
            message = "تست یادآور دارینو",
            reminderType = "INSTALLMENT"
        )

        assertTrue("Mock SMS should dispatch successfully", result.success)
        assertNotNull("Provider message ID should be generated", result.providerMessageId)
        assertTrue("Message ID should start with sms_mock_", result.providerMessageId!!.startsWith("sms_mock_"))

        val status = dispatcher.getStatus(result.providerMessageId!!)
        assertEquals("Mock SMS status should be DELIVERED", DeliveryStatus.DELIVERED, status)
    }
}
