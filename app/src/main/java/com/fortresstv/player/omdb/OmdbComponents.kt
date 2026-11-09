package com.fortresstv.player.omdb

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OmdbRatingsRow(
    metadata: OmdbMetadata?,
    fallbackText: String,
    modifier: Modifier = Modifier
) {
    if (metadata != null && metadata.hasRatings) {
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // IMDb Rating Pill
            val imdb = metadata.imdbRating
            if (!imdb.isNullOrBlank() && imdb != "N/A") {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF1E1A08),
                    border = BorderStroke(1.dp, Color(0xFFF5C518).copy(alpha = 0.75f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "IMDb",
                            color = Color(0xFFF5C518),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFF5C518),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = imdb,
                            color = Color(0xFFFFFAEB),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        val votes = metadata.imdbVotes
                        if (!votes.isNullOrBlank() && votes != "N/A") {
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "(${formatVotesCompact(votes)})",
                                color = Color(0xFFC7B97A),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Rotten Tomatoes Pill
            val rt = metadata.rottenTomatoes
            if (!rt.isNullOrBlank() && rt != "N/A") {
                val pct = rt.replace("%", "").trim().toIntOrNull() ?: 0
                val isFresh = pct >= 60
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isFresh) Color(0xFF260D08) else Color(0xFF121E0F),
                    border = BorderStroke(
                        1.dp,
                        if (isFresh) Color(0xFFFA320A).copy(alpha = 0.8f) else Color(0xFF468C32).copy(alpha = 0.8f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isFresh) "🍅" else "🟢",
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = rt,
                            color = if (isFresh) Color(0xFFFFEBE8) else Color(0xFFEAF5E8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Metascore Pill
            val meta = metadata.metascore
            if (!meta.isNullOrBlank() && meta != "N/A") {
                val score = meta.trim().toIntOrNull() ?: 0
                val scoreColor = when {
                    score >= 61 -> Color(0xFF66CC33)
                    score >= 40 -> Color(0xFFFFCC33)
                    else -> Color(0xFFFF4D4D)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = scoreColor.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, scoreColor.copy(alpha = 0.8f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Metascore",
                            color = scoreColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = meta,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Age Certification Pill (if available)
            val rated = metadata.rated
            if (!rated.isNullOrBlank() && rated != "N/A") {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = rated,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
    } else if (fallbackText.isNotBlank()) {
        Text(
            text = fallbackText,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = modifier
        )
    }
}

@Composable
fun OmdbAwardsRow(
    awards: String?,
    modifier: Modifier = Modifier
) {
    if (awards.isNullOrBlank() || awards.equals("N/A", ignoreCase = true)) return

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF26200A).copy(alpha = 0.7f),
        border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = awards,
                color = Color(0xFFFFF6D1),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 16.sp
            )
        }
    }
}

private fun formatVotesCompact(rawVotes: String): String {
    val clean = rawVotes.filter { it.isDigit() }
    val num = clean.toLongOrNull() ?: return rawVotes
    return when {
        num >= 1_000_000 -> "%.1fM".format(num / 1_000_000.0).replace(".0M", "M")
        num >= 1_000 -> "%.0fk".format(num / 1_000.0)
        else -> num.toString()
    }
}
