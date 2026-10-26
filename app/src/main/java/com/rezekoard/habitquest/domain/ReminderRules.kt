package com.rezekoard.habitquest.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

fun nextReminderAt(time: LocalTime, now: ZonedDateTime, handledToday: Boolean): ZonedDateTime {
    val candidate = now.toLocalDate().atTime(time).atZone(now.zone)
    return if (!handledToday && candidate.isAfter(now)) candidate
    else now.toLocalDate().plusDays(1).atTime(time).atZone(now.zone)
}

fun shouldDeliverReminder(date: LocalDate, requestedTime: LocalTime, configuredTime: LocalTime?, now: ZonedDateTime, complete: Boolean): Boolean =
    date == now.toLocalDate() && requestedTime == configuredTime && !now.toLocalTime().isBefore(requestedTime) && !complete

fun nextScheduledReminderAt(time: LocalTime, now: ZonedDateTime, handledToday: Boolean, scheduled: (LocalDate) -> Boolean): ZonedDateTime? {
    for (offset in 0L..400L) {
        val date = now.toLocalDate().plusDays(offset)
        if (offset == 0L && handledToday) continue
        val at = date.atTime(time).atZone(now.zone)
        if (at.isAfter(now) && scheduled(date)) return at
    }
    return null
}
