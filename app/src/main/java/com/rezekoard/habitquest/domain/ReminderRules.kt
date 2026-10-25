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
