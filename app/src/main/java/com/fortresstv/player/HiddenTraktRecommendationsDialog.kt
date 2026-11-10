package com.fortresstv.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fortresstv.player.trakt.TraktHiddenRecommendationsPage
import com.fortresstv.player.trakt.TraktRecommendationItem
import com.fortresstv.player.ui.LocalAppStrings
import kotlinx.coroutines.launch

@Composable
internal fun HiddenTraktRecommendationsDialog(
    loadPage: suspend (Int) -> Result<TraktHiddenRecommendationsPage>,
    restore: suspend (TraktRecommendationItem) -> Boolean,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    var hidden by remember { mutableStateOf(emptyList<TraktRecommendationItem>()) }
    var nextPage by remember { mutableStateOf(1) }
    var hasMore by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var restoring by remember { mutableStateOf(false) }
    var loadFailed by remember { mutableStateOf(false) }
    var restoreFailed by remember { mutableStateOf(false) }

    suspend fun fetchNextPage() {
        if (loading) return
        loading = true
        loadFailed = false
        try {
            loadPage(nextPage).onSuccess { page ->
                hidden = (hidden + page.items).distinct()
                hasMore = page.hasMore
                nextPage++
            }.onFailure { loadFailed = true }
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { fetchNextPage() }
    AlertDialog(
        onDismissRequest = { if (!restoring) onDismiss() },
        title = { Text(strings.traktHiddenRecommendations) },
        text = {
            Column {
                LazyColumn(
                    Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(hidden) { item ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                item.year?.let { "${item.title} ($it)" } ?: item.title,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                enabled = !loading && !restoring,
                                onClick = {
                                    restoring = true
                                    restoreFailed = false
                                    scope.launch {
                                        try {
                                            if (restore(item)) onDismiss() else restoreFailed = true
                                        } finally {
                                            restoring = false
                                        }
                                    }
                                }
                            ) { Text(strings.traktRestoreRecommendation) }
                        }
                    }
                    if (hasMore && !loading && !loadFailed && nextPage > 1) {
                        item {
                            TextButton(enabled = !restoring, onClick = { scope.launch { fetchNextPage() } }) {
                                Text(strings.traktLoadMoreHiddenRecommendations)
                            }
                        }
                    }
                }
                if (loading || restoring) {
                    CircularProgressIndicator(Modifier.padding(top = 8.dp))
                } else if (loadFailed) {
                    Text(strings.traktHiddenRecommendationsFailed, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { scope.launch { fetchNextPage() } }) { Text(strings.retry) }
                } else if (restoreFailed) {
                    Text(strings.traktRecommendationActionFailed, color = MaterialTheme.colorScheme.error)
                } else if (hidden.isEmpty() && !hasMore) {
                    Text(strings.traktNoHiddenRecommendations)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !restoring, onClick = onDismiss) { Text(strings.close) }
        }
    )
}
