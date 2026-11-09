package com.fortresstv.player.player

import com.fortresstv.player.playlist.PlaylistChannel
import com.fortresstv.player.telemetry.ChannelHealthRepository
import com.fortresstv.player.telemetry.ConnectionHealthState
import java.util.Locale

/**
 * Resolves and ranks backup streams and alternative channels across enabled sources
 * when the current active live stream fails.
 */
object ChannelFailoverResolver {

    private val PREFIX_REGEX = Regex("(?i)^(?:pt|pt-pt|portugal|pt\\s*:|pt\\s*\\||pt\\s*-)\\s*")
    private val TAG_REGEX = Regex("(?i)\\b(?:fhd|uhd|4k|8k|hevc|h265|raw|50fps|60fps|1080p|720p|sd|backup|bkp|alt|feed)\\b")
    private val HD_REGEX = Regex("(?i)\\bhd\\b")
    private val NON_ALPHANUMERIC = Regex("[^a-zA-Z0-9]")

    /**
     * Normalizes channel names for cross-source matching.
     * E.g. "PT: RTP 1 HD (50fps)" -> "rtp1"
     */
    fun normalizeChannelName(rawName: String): String {
        return rawName
            .replace("+", "plus")
            .replace(PREFIX_REGEX, "")
            .replace(TAG_REGEX, "")
            .replace(HD_REGEX, "")
            .replace(NON_ALPHANUMERIC, "")
            .lowercase(Locale.ROOT)
            .trim()
    }

    /**
     * Determines whether [candidate] can serve as a failover alternative for [target].
     *
     * A valid candidate must:
     * 1. Have a different stream URL from [target].
     * 2. Share a common tvg-id (case-insensitive) OR normalize to the exact same channel name.
     */
    fun isCandidate(target: PlaylistChannel, candidate: PlaylistChannel): Boolean {
        if (candidate.streamUrl.trim() == target.streamUrl.trim()) return false

        val targetTvg = target.tvgId?.trim()?.lowercase(Locale.ROOT).orEmpty()
        val candidateTvg = candidate.tvgId?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (targetTvg.isNotEmpty() && targetTvg == candidateTvg) {
            return true
        }

        val normTarget = normalizeChannelName(target.name)
        val normCandidate = normalizeChannelName(candidate.name)
        return normTarget.isNotEmpty() && normTarget == normCandidate
    }

    /**
     * Finds and ranks candidate failover channels for [failedChannel] from [allChannels],
     * excluding URLs that have already been tried in [attemptedUrls].
     *
     * Candidates are ranked by:
     * 1. Connection health (EXCELLENT > MODERATE > UNKNOWN/MEASURING > POOR)
     * 2. Alternative source providers over same source
     * 3. Original playlist ordering
     */
    fun findFailoverCandidates(
        failedChannel: PlaylistChannel,
        allChannels: List<PlaylistChannel>,
        attemptedUrls: Set<String>,
        healthRepo: ChannelHealthRepository? = null
    ): List<PlaylistChannel> {
        return allChannels.asSequence()
            .filter { candidate ->
                !attemptedUrls.contains(candidate.streamUrl) && isCandidate(failedChannel, candidate)
            }
            .sortedWith(
                compareBy<PlaylistChannel> { candidate ->
                    val health = candidate.sourceItemId.takeIf { it.isNotBlank() }?.let { healthRepo?.getHealth(it) }
                        ?: candidate.tvgId?.takeIf { it.isNotBlank() }?.let { healthRepo?.getHealth(it) }
                        ?: healthRepo?.getHealth(candidate.name)
                    val state = health?.state ?: ConnectionHealthState.UNKNOWN
                    when (state) {
                        ConnectionHealthState.EXCELLENT -> 0
                        ConnectionHealthState.MODERATE -> 1
                        ConnectionHealthState.MEASURING, ConnectionHealthState.UNKNOWN -> 2
                        ConnectionHealthState.POOR -> 3
                    }
                }.thenBy { candidate ->
                    if (candidate.sourceId.isNotBlank() && candidate.sourceId != failedChannel.sourceId) 0 else 1
                }.thenBy { candidate ->
                    candidate.position
                }
            )
            .toList()
    }
}
