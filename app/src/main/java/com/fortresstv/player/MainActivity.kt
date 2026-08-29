package com.fortresstv.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortresstv.player.ui.theme.FortressTVTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { FortressTVTheme { FortressWelcomeScreen() } }
    }
}

@Composable
fun FortressWelcomeScreen(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.widthIn(max = 680.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ShieldMark()
                Spacer(Modifier.height(28.dp))
                Text("FortressTV", fontSize = 42.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Your channels. Your fortress.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(48.dp))
                Text(
                    "Add your playlist to start watching",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { },
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) { Text("Open M3U file") }
                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) { Text("Enter playlist URL") }
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    "Playlist details stay on this device.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ShieldMark() {
    Box(
        modifier = Modifier.size(104.dp).clip(RoundedCornerShape(30.dp))
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Text("F", color = Color(0xFF061B19), fontSize = 52.sp, fontWeight = FontWeight.Black)
    }
}

@Preview(showBackground = true, widthDp = 1024, heightDp = 600)
@Composable
private fun FortressWelcomeScreenPreview() {
    FortressTVTheme { FortressWelcomeScreen() }
}
