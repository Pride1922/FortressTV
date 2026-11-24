package com.fortresstv.player

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fortresstv.player.ui.LocalAppStrings
import com.fortresstv.player.ui.tvFocusHighlight
import java.util.Locale

@Composable
internal fun SubtitleTimingControls(offsetMs: Int, onChange: (Int) -> Unit) {
    val strings = LocalAppStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(strings.subtitleTiming, style = MaterialTheme.typography.titleSmall)
        Text(strings.subtitleTimingHint, style = MaterialTheme.typography.bodySmall)
        Text(String.format(Locale.ROOT, "%+.1f s", offsetMs / 1_000f), style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { onChange(offsetMs - 100) }, enabled = offsetMs > -5_000,
                modifier = Modifier.weight(1f).tvFocusHighlight()
            ) { Text("${strings.subtitlesEarlier}\n−100 ms") }
            OutlinedButton(
                onClick = { onChange(offsetMs + 100) }, enabled = offsetMs < 5_000,
                modifier = Modifier.weight(1f).tvFocusHighlight()
            ) { Text("${strings.subtitlesLater}\n+100 ms") }
        }
        TextButton(onClick = { onChange(0) }, enabled = offsetMs != 0, modifier = Modifier.tvFocusHighlight()) { Text(strings.resetSubtitleTiming) }
    }
}
