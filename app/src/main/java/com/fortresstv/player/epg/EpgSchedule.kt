package com.fortresstv.player.epg

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class ProgrammeSnapshot(
    val current: EpgProgramme?,
    val next: EpgProgramme?
)

fun EpgGuide.snapshot(
    channelId: String?,
    now: Instant = Instant.now(),
    timeShiftHours: Int = 0
): ProgrammeSnapshot {
    if (channelId == null) return ProgrammeSnapshot(null, null)
    val list = programmes[channelId] ?: return ProgrammeSnapshot(null, null)
    val targetNow = now.toEpochMilli() - (timeShiftHours * 3600_000L)

    var current: EpgProgramme? = null
    var next: EpgProgramme? = null

    for (i in list.indices) {
        val prog = list[i]
        val start = prog.startEpochMilli
        val stop = prog.stopEpochMilli
        if (start <= targetNow && (stop == null || stop > targetNow)) {
            current = prog
        } else if (start > targetNow) {
            next = prog
            break
        }
    }
    return ProgrammeSnapshot(current, next)
}

object XmlTvTime {
    private val valuePattern = Regex("""^(\d{14}|\d{12}|\d{8})(?:\s*([+-]\d{4}|Z))?""")

    fun parseToEpoch(value: String): Long? = parse(value, 0)?.toEpochMilli()

    fun parse(value: String, timeShiftHours: Int = 0): Instant? = runCatching {
        val match = valuePattern.find(value.trim()) ?: return null
        val digits = match.groupValues[1]
        val pattern = when (digits.length) {
            8 -> "yyyyMMdd"
            12 -> "yyyyMMddHHmm"
            else -> "yyyyMMddHHmmss"
        }
        val dateTime = when (digits.length) {
            8 -> java.time.LocalDate.parse(digits, DateTimeFormatter.ofPattern(pattern)).atStartOfDay()
            else -> LocalDateTime.parse(digits, DateTimeFormatter.ofPattern(pattern))
        }
        val offsetText = match.groupValues[2]
        val instant = if (offsetText.isBlank()) dateTime.atZone(ZoneId.systemDefault()).toInstant()
        else dateTime.toInstant(if (offsetText == "Z") ZoneOffset.UTC else ZoneOffset.of(offsetText.substring(0, 3) + ":" + offsetText.substring(3)))
        if (timeShiftHours != 0) instant.plus(java.time.Duration.ofHours(timeShiftHours.toLong())) else instant
    }.getOrNull()
}
