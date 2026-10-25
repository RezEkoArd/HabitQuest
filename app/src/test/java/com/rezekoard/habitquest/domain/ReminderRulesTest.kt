package com.rezekoard.habitquest.domain

import java.time.*
import org.junit.Assert.*
import org.junit.Test

class ReminderRulesTest {
    private val time = LocalTime.of(7, 0)
    private val now = ZonedDateTime.of(2026, 10, 25, 6, 0, 0, 0, ZoneId.of("Asia/Jakarta"))

    @Test fun futureReminderUsesTodaysLocalTime() {
        assertEquals(now.withHour(7), nextReminderAt(time, now, false))
    }
    @Test fun missedReminderIsNotReplayedWhenRescheduling() {
        assertEquals(now.plusDays(1).withHour(7), nextReminderAt(time, now.withHour(8), false))
        assertEquals(now.plusDays(1).withHour(7), nextReminderAt(time, now.withHour(7), false))
    }
    @Test fun alreadyHandledTodayCannotReturnAfterClockMovesBack() {
        assertEquals(now.plusDays(1).withHour(7), nextReminderAt(time, now, true))
    }
    @Test fun dstChangeKeepsWallClockTimeInsteadOfAddingTwentyFourHours() {
        val before = ZonedDateTime.of(2026, 3, 7, 8, 0, 0, 0, ZoneId.of("America/New_York"))
        val next = nextReminderAt(time, before, false)
        assertEquals(LocalDate.of(2026, 3, 8), next.toLocalDate())
        assertEquals(time, next.toLocalTime())
        assertEquals(22L, Duration.between(before, next).toHours())
    }
    @Test fun sameDayDelayedDeliveryIsAllowedButCompletedTargetsAreSkipped() {
        assertTrue(shouldDeliverReminder(now.toLocalDate(), time, time, now.withHour(8), false))
        assertFalse(shouldDeliverReminder(now.toLocalDate(), time, time, now.withHour(8), true))
    }
    @Test fun staleDatesChangedTimesAndEarlyTriggersAreRejected() {
        assertFalse(shouldDeliverReminder(now.toLocalDate().minusDays(1), time, time, now.withHour(8), false))
        assertFalse(shouldDeliverReminder(now.toLocalDate(), time, LocalTime.of(9, 0), now.withHour(8), false))
        assertFalse(shouldDeliverReminder(now.toLocalDate(), time, null, now.withHour(8), false))
        assertFalse(shouldDeliverReminder(now.toLocalDate(), time, time, now, false))
    }
}
