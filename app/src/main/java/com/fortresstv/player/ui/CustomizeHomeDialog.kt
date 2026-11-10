package com.fortresstv.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

object HomeSectionId {
    const val SPORTS = "sports"
    const val CONTINUE_WATCHING = "continue_watching"
    const val FAVORITE_CHANNELS = "favorite_channels"
    const val RECENT_CHANNELS = "recent_channels"
    const val FOLLOWED_SERIES = "followed_series"
    const val RECENT_MOVIES = "recent_movies"
    const val FEATURED_SERIES = "featured_series"
    const val TRAKT_WATCHLIST = "trakt_watchlist"
}

@Composable
fun CustomizeHomeDialog(
    hiddenSections: Set<String>,
    onToggleSection: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current

    val items: List<Triple<String, String, ImageVector>> = listOf(
        Triple(HomeSectionId.SPORTS, strings.sectionSports, Icons.Default.SportsSoccer),
        Triple(HomeSectionId.CONTINUE_WATCHING, strings.sectionContinueWatching, Icons.Default.PlayCircle),
        Triple(HomeSectionId.FAVORITE_CHANNELS, strings.sectionFavoriteChannels, Icons.Default.Star),
        Triple(HomeSectionId.RECENT_CHANNELS, strings.sectionRecentChannels, Icons.Default.History),
        Triple(HomeSectionId.FOLLOWED_SERIES, strings.sectionFollowedSeries, Icons.Default.Tv),
        Triple(HomeSectionId.TRAKT_WATCHLIST, strings.sectionTraktWatchlist, Icons.Default.Bookmark),
        Triple(HomeSectionId.RECENT_MOVIES, strings.sectionRecentMovies, Icons.Default.Movie),
        Triple(HomeSectionId.FEATURED_SERIES, strings.sectionFeaturedSeries, Icons.Default.AutoAwesome)
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .widthIn(min = 360.dp, max = 500.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFF2E3342), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF171A23),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.customizeHome,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = strings.customizeHomeDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9EA3B0)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.close,
                            tint = Color(0xFF9EA3B0)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF262A36), thickness = 1.dp)
                Spacer(Modifier.height(10.dp))

                // List of sections with switches
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items.forEach { (id, label, icon) ->
                        val isVisible = id !in hiddenSections
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onToggleSection(id) }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF222633)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isVisible) MaterialTheme.colorScheme.primary else Color(0xFF6B7280),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = label,
                                    color = if (isVisible) Color.White else Color(0xFF888E9E),
                                    fontSize = 14.sp,
                                    fontWeight = if (isVisible) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }

                            Switch(
                                checked = isVisible,
                                onCheckedChange = { onToggleSection(id) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = Color(0xFF707684),
                                    uncheckedTrackColor = Color(0xFF222633)
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = strings.close, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
