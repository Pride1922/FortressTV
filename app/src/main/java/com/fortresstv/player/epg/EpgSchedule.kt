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
): ProgrammeSnapshot = snapshot(channelId, null, now, timeShiftHours)

fun EpgGuide.snapshot(
    channelId: String?,
    channelName: String?,
    now: Instant = Instant.now(),
    timeShiftHours: Int = 0
): ProgrammeSnapshot {
    val resolvedId = resolveChannelId(channelId, channelName) ?: return ProgrammeSnapshot(null, null)
    val list = programmes[resolvedId]
    if (list.isNullOrEmpty()) return ProgrammeSnapshot(null, null)
    val targetNow = now.toEpochMilli() - (timeShiftHours * 3600_000L)

    val binaryIndex = list.binarySearchBy(targetNow) { it.startEpochMilli }
    val candidateIndex = if (binaryIndex >= 0) binaryIndex else (-binaryIndex - 1) - 1

    var current: EpgProgramme? = null
    var next: EpgProgramme? = null

    if (candidateIndex in list.indices) {
        val candidate = list[candidateIndex]
        val stop = candidate.stopEpochMilli
        if (candidate.startEpochMilli <= targetNow && (stop == null || stop > targetNow)) {
            current = candidate
            next = list.getOrNull(candidateIndex + 1)
        }
    }

    if (current == null) {
        val nextIdx = if (binaryIndex >= 0) binaryIndex + 1 else -binaryIndex - 1
        if (nextIdx in list.indices) {
            next = list[nextIdx]
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
