package com.fortresstv.player.subtitles

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortresstv.player.ui.LocalAppStrings
import kotlinx.coroutines.launch

@Composable
fun OpenSubtitlesSearchDialog(
    initialQuery: String,
    season: Int? = null,
    episode: Int? = null,
    preferredLanguage: String,
    settings: OpenSubtitlesSettings,
    onSubtitleSelected: (OpenSubtitlesItem, String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf(initialQuery) }
    var searchSeason by remember { mutableStateOf(season?.toString().orEmpty()) }
    var searchEpisode by remember { mutableStateOf(episode?.toString().orEmpty()) }

    var isLoading by remember { mutableStateOf(true) }
    var downloadingFileId by remember { mutableStateOf<Int?>(null) }
    var results by remember { mutableStateOf<List<OpenSubtitlesItem>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun performSearch() {
        if (searchQuery.isBlank()) return
        isLoading = true
        errorMessage = null
        scope.launch {
            val client = OpenSubtitlesClient(
                apiKey = settings.apiKey,
                username = settings.username.takeIf { it.isNotBlank() },
                password = settings.password.takeIf { it.isNotBlank() }
            )
            val lang = openSubtitlesLanguageQuery(preferredLanguage, settings.preferPtPt)
            val res = client.searchSubtitles(
                query = searchQuery,
                season = searchSeason.toIntOrNull(),
                episode = searchEpisode.toIntOrNull(),
                languages = lang
            )
            isLoading = false
            if (res.isSuccess) {
                val list = res.getOrDefault(emptyList())
                results = if (settings.preferPtPt && preferredLanguage == "pt") {
                    list.sortedWith(
                        compareByDescending<OpenSubtitlesItem> { it.language == "pt-pt" }
                            .thenByDescending { it.downloads }
                    )
                } else {
                    list.sortedByDescending { it.downloads }
                }
                if (results.isEmpty()) {
                    errorMessage = strings.noSubtitlesFound
                }
            } else {
                errorMessage = res.exceptionOrNull()?.message ?: strings.noSubtitlesFound
            }
        }
    }

    LaunchedEffect(Unit) {
        performSearch()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Subtitles, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(strings.openSubtitles, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    if (season != null || episode != null) {
                        OutlinedTextField(
                            value = searchSeason,
                            onValueChange = { searchSeason = it },
                            label = { Text("S") },
                            singleLine = true,
                            modifier = Modifier.width(55.dp)
                        )
                        OutlinedTextField(
                            value = searchEpisode,
                            onValueChange = { searchEpisode = it },
                            label = { Text("E") },
                            singleLine = true,
                            modifier = Modifier.width(55.dp)
                        )
                    }
                    IconButton(
                        onClick = { performSearch() },
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                if (isLoading) {
                    Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(strokeWidth = 2.dp)
                            Spacer(Modifier.height(8.dp))
                            Text(strings.searchingSubtitles, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else if (errorMessage != null && results.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text(errorMessage ?: strings.noSubtitlesFound, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    errorMessage?.let {
                        Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                    LazyColumn(
                        Modifier.fillMaxWidth().weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(results, key = { it.fileId }) { item ->
                            val isDownloading = downloadingFileId == item.fileId
                            val isPtPt = item.language == "pt-pt"
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isDownloading) {
                                        downloadingFileId = item.fileId
                                        scope.launch {
                                            val client = OpenSubtitlesClient(
                                                apiKey = settings.apiKey,
                                                username = settings.username.takeIf { it.isNotBlank() },
                                                password = settings.password.takeIf { it.isNotBlank() }
                                            )
                                            val dlRes = client.downloadSubtitle(item.fileId)
                                            downloadingFileId = null
                                            if (dlRes.isSuccess) {
                                                onSubtitleSelected(item, dlRes.getOrThrow())
                                            } else {
                                                errorMessage = dlRes.exceptionOrNull()?.message ?: strings.openSubtitlesConnectionFailed
                                            }
                                        }
                                    }
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val badgeText = when (item.language) {
                                                "pt-pt" -> "PT-PT 🇵🇹"
                                                "pt-br" -> "PT-BR 🇧🇷"
                                                else -> item.language.uppercase()
                                            }
                                            val badgeBg = if (isPtPt) Color(0xFF1B5E20) else Color(0xFF0D47A1)
                                            Box(
                                                Modifier
                                                    .background(badgeBg, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(badgeText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                item.release,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (item.rating > 0.0) {
                                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(12.dp))
                                                Spacer(Modifier.width(2.dp))
                                                Text(String.format("%.1f", item.rating), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Spacer(Modifier.width(8.dp))
                                            }
                                            Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                                            Spacer(Modifier.width(2.dp))
                                            Text("${item.downloads}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            if (item.hearingImpaired) {
                                                Spacer(Modifier.width(8.dp))
                                                Text("HI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                            }
                                        }
                                    }

                                    if (isDownloading) {
                                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Download, contentDescription = "Download", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.close)
            }
        }
    )
}
