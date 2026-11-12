package com.fortresstv.player.sports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsMotorsports
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.fortresstv.player.epg.EpgGuide
import com.fortresstv.player.playlist.PlaylistChannel
import com.fortresstv.player.ui.LocalAppStrings
import com.fortresstv.player.ui.tvFocusHighlight
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal object SportsPlayableCache {
    var cached: List<Pair<SportsFixture, PlaylistChannel>> = emptyList()
}

@Composable
fun SportsHubSection(
    fixtures: List<SportsFixture>,
    channels: List<PlaylistChannel>,
    epgGuide: EpgGuide?,
    onWatchChannel: (PlaylistChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    if (fixtures.isEmpty()) return

    // Stream Filter Rule: Only show matches/events that can actually be played on the user's active stream
    // Resolve off the main thread: EPG matching scans the whole guide and must never block composition.
    val playableFixtures by androidx.compose.runtime.produceState(
        initialValue = SportsPlayableCache.cached,
        fixtures, channels, epgGuide
    ) {
        val resolved = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            SportsBindingResolver.resolveAll(fixtures, channels, epgGuide)
        }
        SportsPlayableCache.cached = resolved
        value = resolved
        android.util.Log.d("FortressTV", "[SportsHub] fixtures=${fixtures.size}, playable=${value.size}, channels=${channels.size}")
    }

    if (playableFixtures.isEmpty()) return
    val strings = LocalAppStrings.current
    var selectedMatch by remember { mutableStateOf<Pair<SportsFixture, PlaylistChannel>?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Sports,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = strings.upcomingMatches,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(playableFixtures, key = { it.first.id }) { (fixture, resolvedChannel) ->
                SportsFixtureCard(
                    fixture = fixture,
                    resolvedChannel = resolvedChannel,
                    onWatchChannel = onWatchChannel,
                    onCardClick = { selectedMatch = fixture to resolvedChannel },
                    modifier = Modifier.width(280.dp)
                )
            }
        }
    }

    selectedMatch?.let { (fixture, channel) ->
        MatchDetailsDialog(
            fixture = fixture,
            resolvedChannel = channel,
            onDismiss = { selectedMatch = null },
            onWatchChannel = {
                onWatchChannel(channel)
                selectedMatch = null
            }
        )
    }
}

@Composable
fun SportsFixtureCard(
    fixture: SportsFixture,
    resolvedChannel: PlaylistChannel,
    onWatchChannel: (PlaylistChannel) -> Unit,
    onCardClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val formattedTime = remember(fixture.kickoffUtc) {
        formatKickoffTime(fixture.kickoffUtc)
    }

    val sportIcon = when (fixture.sport) {
        SportsKind.FOOTBALL -> Icons.Default.SportsSoccer
        SportsKind.MOTORSPORT -> Icons.Default.SportsMotorsports
        SportsKind.BASKETBALL -> Icons.Default.SportsBasketball
        SportsKind.TENNIS -> Icons.Default.SportsTennis
        SportsKind.OTHER -> Icons.Default.Sports
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .clickable(enabled = onCardClick != null) { onCardClick?.invoke() },
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Competition + Time / Live status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (!fixture.competitionLogoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = fixture.competitionLogoUrl,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.width(6.dp))
                    } else {
                        Icon(
                            imageVector = sportIcon,
                            contentDescription = null,
                            tint = Color(0xFF9EA3B0),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = fixture.competition,
                        color = Color(0xFF9EA3B0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (fixture.isLive()) {
                    val liveLabel = if (!fixture.minute.isNullOrBlank() && fixture.minute.lowercase() != "null") {
                        fixture.minute
                    } else {
                        strings.liveNow
                    }
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE53935), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = liveLabel,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = formattedTime,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Teams and Logos Matchup or Motorsport Presentation
            if (fixture.sport == SportsKind.MOTORSPORT) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TeamLogo(
                        url = fixture.homeTeamLogoUrl ?: fixture.competitionLogoUrl,
                        name = fixture.homeTeam,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fixture.homeTeam,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (fixture.awayTeam.isNotBlank() && fixture.awayTeam != fixture.homeTeam) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = fixture.awayTeam,
                                color = Color(0xFF9EA3B0),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Home Team
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TeamLogo(url = fixture.homeTeamLogoUrl, name = fixture.homeTeam)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = fixture.homeTeam,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Score or VS
                    Box(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (fixture.homeScore != null && fixture.awayScore != null) {
                            Text(
                                text = "${fixture.homeScore} - ${fixture.awayScore}",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "vs",
                                color = Color(0xFF707684),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Away Team
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TeamLogo(url = fixture.awayTeamLogoUrl, name = fixture.awayTeam)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = fixture.awayTeam,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Where to watch: a compact channel chip, so the match is the focus rather than a big
            // button. Red while the match is live, matching the LIVE badge; cyan otherwise.
            val live = fixture.isLive()
            val chipTint = if (live) Color(0xFFE53935) else MaterialTheme.colorScheme.primary
            val chipShape = RoundedCornerShape(18.dp)
            Surface(
                onClick = { onWatchChannel(resolvedChannel) },
                shape = chipShape,
                color = chipTint.copy(alpha = if (live) 0.22f else 0.14f),
                modifier = Modifier.height(34.dp).tvFocusHighlight(shape = chipShape)
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = chipTint,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = strings.broadcastOn(resolvedChannel.name.ifBlank { fixture.broadcasterOfficial }),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamLogo(url: String?, name: String, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    if (!url.isNullOrBlank()) {
        val request = remember(url) {
            coil.request.ImageRequest.Builder(context)
                .data(url)
                .setHeader("User-Agent", "Mozilla/5.0")
                .crossfade(true)
                .build()
        }
        AsyncImage(
            model = request,
            contentDescription = name,
            modifier = modifier.size(38.dp),
            contentScale = ContentScale.Fit
        )
    } else {
        Box(
            modifier = modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFF2C3140)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(2).uppercase(),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatKickoffTime(instant: Instant): String {
    val zone = ZoneId.systemDefault()
    val fixtureDateTime = instant.atZone(zone)
    val fixtureDate = fixtureDateTime.toLocalDate()
    val today = LocalDate.now(zone)
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    return when (fixtureDate) {
        today -> "Hoje ${timeFormatter.format(fixtureDateTime)}"
        today.plusDays(1) -> "Amanhã ${timeFormatter.format(fixtureDateTime)}"
        else -> DateTimeFormatter.ofPattern("dd/MM HH:mm").format(fixtureDateTime)
    }
}

@Composable
fun MatchDetailsDialog(
    fixture: SportsFixture,
    resolvedChannel: PlaylistChannel,
    onDismiss: () -> Unit,
    onWatchChannel: () -> Unit
) {
    val strings = LocalAppStrings.current
    val summaryDetails by produceState<MatchSummaryDetails?>(initialValue = null, key1 = fixture.id) {
        value = SportsMatchSummaryApi.fetchSummary(fixture)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .width(480.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFF2A2E39), RoundedCornerShape(16.dp)),
            color = Color(0xFF14171F),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Header: Competition + Live / Kickoff + Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!fixture.competitionLogoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = fixture.competitionLogoUrl,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = fixture.competition,
                            color = Color(0xFFB0B5C0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (fixture.isLive()) {
                            val liveText = if (!fixture.minute.isNullOrBlank() && fixture.minute.lowercase() != "null") {
                                fixture.minute
                            } else {
                                strings.liveNow
                            }
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFE53935), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = liveText,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF9EA3B0),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // 2. Scoreboard presentation
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1B1E28)),
                    color = Color(0xFF1B1E28),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Home Team
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            TeamLogo(
                                url = fixture.homeTeamLogoUrl,
                                name = fixture.homeTeam,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = fixture.homeTeam,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Score or VS
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            if (fixture.homeScore != null && fixture.awayScore != null) {
                                Text(
                                    text = "${fixture.homeScore} - ${fixture.awayScore}",
                                    color = Color.White,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            } else {
                                Text(
                                    text = "vs",
                                    color = Color(0xFF707684),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = formatKickoffTime(fixture.kickoffUtc),
                                    color = Color(0xFF4CAF50),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Away Team
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            TeamLogo(
                                url = fixture.awayTeamLogoUrl,
                                name = fixture.awayTeam,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = fixture.awayTeam,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Venue (if present)
                val venueText = summaryDetails?.venue ?: fixture.venue
                if (!venueText.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = Color(0xFF707684),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = venueText,
                            color = Color(0xFF8E93A0),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF232733), thickness = 1.dp)

                // 3. Goalscorers / Key Events Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = strings.matchEvents,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    when {
                        summaryDetails == null -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFF1E88E5),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                        summaryDetails!!.events.isNotEmpty() -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (event in summaryDetails!!.events) {
                                    val icon = when (event.type) {
                                        MatchEventType.PENALTY -> "⚽ (Pen)"
                                        MatchEventType.OWN_GOAL -> "⚽ (OG)"
                                        MatchEventType.RED_CARD -> "🟥"
                                        MatchEventType.YELLOW_CARD -> "🟨"
                                        else -> "⚽"
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF1A1D27), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = event.minute,
                                            color = Color(0xFF4CAF50),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(36.dp)
                                        )
                                        Text(
                                            text = icon,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        )
                                        Text(
                                            text = "${event.playerName} (${event.teamName})",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        summaryDetails!!.leaders.isNotEmpty() -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (leader in summaryDetails!!.leaders) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF1A1D27), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "🏀", fontSize = 13.sp)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = leader,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                        fixture.isLive() -> {
                            Text(
                                text = strings.noEventsYet,
                                color = Color(0xFF707684),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        else -> {
                            Text(
                                text = strings.matchNotStarted,
                                color = Color(0xFF707684),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // 4. Action Button: Watch Live on Channel
                Button(
                    onClick = onWatchChannel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E88E5),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = strings.watchLiveOn(resolvedChannel.name.ifBlank { fixture.broadcasterOfficial }),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
