package space.linuxct.pulseloop.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun Long.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

fun LocalDate.toMidnightEpochMs(zone: ZoneId = ZoneId.systemDefault()): Long =
    atStartOfDay(zone).toInstant().toEpochMilli()

fun Long.toZonedDateTime(zone: ZoneId = ZoneId.systemDefault()): ZonedDateTime =
    Instant.ofEpochMilli(this).atZone(zone)

fun todayMidnightMs(zone: ZoneId = ZoneId.systemDefault()): Long =
    LocalDate.now(zone).toMidnightEpochMs(zone)

fun daysAgoMidnightMs(daysAgo: Int, zone: ZoneId = ZoneId.systemDefault()): Long =
    LocalDate.now(zone).minusDays(daysAgo.toLong()).toMidnightEpochMs(zone)

fun Long.formatTime(pattern: String = "HH:mm", zone: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ofPattern(pattern).format(toZonedDateTime(zone))

fun Long.formatDate(pattern: String = "MMM d", zone: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ofPattern(pattern).format(toZonedDateTime(zone))

fun durationMinutesToHm(totalMinutes: Int): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
