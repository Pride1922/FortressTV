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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import coil.compose.AsyncImage
import com.fortresstv.player.epg.EpgGuide
import com.fortresstv.player.playlist.PlaylistChannel
import com.fortresstv.player.ui.LocalAppStrings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SportsHubSection(
    fixtures: List<SportsFixture>,
    channels: List<PlaylistChannel>,
    epgGuide: EpgGuide?,
    onWatchChannel: (PlaylistChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    if (fixtures.isEmpty()) return
    val strings = LocalAppStrings.current

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.SportsSoccer,
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
            items(fixtures, key = { it.id }) { fixture ->
                val resolvedChannel = remember(fixture.id, channels, epgGuide) {
                    SportsBindingResolver.resolve(fixture, channels, epgGuide)
                }
                SportsFixtureCard(
                    fixture = fixture,
                    resolvedChannel = resolvedChannel,
                    onWatchChannel = onWatchChannel,
                    modifier = Modifier.width(280.dp)
                )
            }
        }
    }
}

@Composable
fun SportsFixtureCard(
    fixture: SportsFixture,
    resolvedChannel: PlaylistChannel?,
    onWatchChannel: (PlaylistChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val formattedTime = remember(fixture.kickoffUtc) {
        formatKickoffTime(fixture.kickoffUtc)
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF2A2E39), RoundedCornerShape(12.dp)),
        color = Color(0xFF161922),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp
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
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE53935), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = fixture.minute ?: strings.liveNow,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = formattedTime,
                        color = Color(0xFF4CAF50),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Teams and Logos Matchup
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

            // Broadcaster / Action Footer
            if (resolvedChannel != null) {
                Button(
                    onClick = { onWatchChannel(resolvedChannel) },
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E88E5),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = strings.broadcastOn(fixture.broadcasterOfficial),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .background(Color(0xFF222632), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${fixture.broadcasterOfficial} (${strings.channelNotDetected})",
                        color = Color(0xFF888E9E),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
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
    if (!url.isNullOrBlank()) {
        AsyncImage(
            model = url,
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
