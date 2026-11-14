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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
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
import androidx.compose.runtime.key
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
    const val TRAKT_RECOMMENDATIONS = "trakt_recommendations"

    val defaultOrder = listOf(
        CONTINUE_WATCHING, FAVORITE_CHANNELS, RECENT_CHANNELS, SPORTS, FOLLOWED_SERIES,
        TRAKT_WATCHLIST, TRAKT_RECOMMENDATIONS, RECENT_MOVIES, FEATURED_SERIES
    )

    fun ordered(saved: List<String>): List<String> = (saved.filter { it in defaultOrder } + defaultOrder).distinct()
}

@Composable
fun CustomizeHomeDialog(
    hiddenSections: Set<String>,
    sectionOrder: List<String>,
    onToggleSection: (String) -> Unit,
    onOrderChange: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    traktConnected: Boolean = false
) {
    val strings = LocalAppStrings.current

    val items: List<Triple<String, String, ImageVector>> = buildList {
        add(Triple(HomeSectionId.SPORTS, strings.sectionSports, Icons.Default.SportsSoccer))
        add(Triple(HomeSectionId.CONTINUE_WATCHING, strings.sectionContinueWatching, Icons.Default.PlayCircle))
        add(Triple(HomeSectionId.FAVORITE_CHANNELS, strings.sectionFavoriteChannels, Icons.Default.Star))
        add(Triple(HomeSectionId.RECENT_CHANNELS, strings.sectionRecentChannels, Icons.Default.History))
        add(Triple(HomeSectionId.FOLLOWED_SERIES, strings.sectionFollowedSeries, Icons.Default.Tv))
        if (traktConnected) {
            add(Triple(HomeSectionId.TRAKT_WATCHLIST, strings.sectionTraktWatchlist, Icons.Default.Bookmark))
            add(Triple(HomeSectionId.TRAKT_RECOMMENDATIONS, strings.sectionTraktRecommendations, Icons.Default.ThumbUp))
        }
        add(
            Triple(
                HomeSectionId.RECENT_MOVIES,
                if (traktConnected) strings.sectionTraktPopularMovies else strings.homeFeaturedMovies,
                Icons.Default.Movie
            )
        )
        add(
            Triple(
                HomeSectionId.FEATURED_SERIES,
                if (traktConnected) strings.sectionTraktPopularSeries else strings.homeRecentlyAddedSeries,
                Icons.Default.AutoAwesome
            )
        )
    }.associateBy { it.first }.let { sections -> sectionOrder.mapNotNull(sections::get) }

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
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.tvFocusHighlight(shape = CircleShape, scaleOnFocus = 1.04f)
                    ) {
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

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items.forEachIndexed { index, (id, label, icon) -> key(id) {
                        val isVisible = id !in hiddenSections
                        fun move(to: Int) {
                            val targetId = items[to].first
                            val fullOrder = HomeSectionId.ordered(sectionOrder).toMutableList()
                            val fromIdx = fullOrder.indexOf(id)
                            val toIdx = fullOrder.indexOf(targetId)
                            if (fromIdx >= 0 && toIdx >= 0) {
                                fullOrder[fromIdx] = targetId
                                fullOrder[toIdx] = id
                                onOrderChange(fullOrder)
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .tvFocusHighlight(shape = RoundedCornerShape(8.dp), scaleOnFocus = 1.01f)
                                    .clickable { onToggleSection(id) }
                                    .padding(horizontal = 4.dp, vertical = 7.dp)
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
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    color = if (isVisible) Color.White else Color(0xFF888E9E),
                                    fontSize = 14.sp,
                                    fontWeight = if (isVisible) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }

                            IconButton(
                                onClick = { move(index - 1) },
                                enabled = index > 0,
                                modifier = Modifier.tvFocusHighlight(shape = CircleShape, scaleOnFocus = 1.04f)
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = strings.moveHomeSectionUp(label))
                            }
                            IconButton(
                                onClick = { move(index + 1) },
                                enabled = index < items.lastIndex,
                                modifier = Modifier.tvFocusHighlight(shape = CircleShape, scaleOnFocus = 1.04f)
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = strings.moveHomeSectionDown(label))
                            }

                            Switch(
                                checked = isVisible,
                                onCheckedChange = { onToggleSection(id) },
                                modifier = Modifier.tvFocusHighlight(shape = RoundedCornerShape(16.dp), scaleOnFocus = 1.03f),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = Color(0xFF707684),
                                    uncheckedTrackColor = Color(0xFF222633)
                                )
                            )
                        }
                    } }
                }

                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .tvFocusHighlight(shape = RoundedCornerShape(12.dp), scaleOnFocus = 1.02f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = strings.close, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
