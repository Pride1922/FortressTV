package com.fortresstv.player

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fortresstv.player.ui.LocalAppStrings
import com.fortresstv.player.ui.LocalFormFactor
import com.fortresstv.player.ui.tvFocusHighlight

private data class FullscreenTrailer(
    val view: View,
    val callback: WebChromeClient.CustomViewCallback
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun TrailerPlayerDialog(
    title: String,
    trailerUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val isTv = LocalFormFactor.current.isTelevision
    val closeFocusRequester = remember { FocusRequester() }
    val videoId = remember(trailerUrl) { extractYouTubeVideoId(trailerUrl) }
    val externalUrl = remember(trailerUrl, videoId) {
        videoId?.let { "https://www.youtube.com/watch?v=$it" }
            ?: trailerWebUri(trailerUrl)?.toASCIIString()
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val dismiss by rememberUpdatedState(onDismiss)
    var trailerWebView by remember { mutableStateOf<WebView?>(null) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                // Stop playback even if backgrounding delays disposal of the dialog.
                trailerWebView?.loadUrl("about:blank")
                dismiss()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    var fullscreen by remember { mutableStateOf<FullscreenTrailer?>(null) }
    val exitFullscreen = {
        val active = fullscreen
        fullscreen = null
        active?.callback?.onCustomViewHidden()
    }

    Dialog(
        onDismissRequest = {
            if (fullscreen != null) exitFullscreen() else onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val availableHeight = (maxHeight - 72.dp).coerceAtLeast(1.dp)
            val playerWidth = minOf(maxWidth - 16.dp, availableHeight * (16f / 9f))
                .coerceAtLeast(1.dp)
            // Keep the controls usable on narrow screens where 16:9 is below YouTube's minimum.
            val playerHeight = (playerWidth * (9f / 16f)).coerceIn(
                minOf(200.dp, availableHeight), availableHeight
            )

            Surface(
                modifier = Modifier.width(playerWidth),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (externalUrl != null) {
                            IconButton(
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(externalUrl)))
                                        onDismiss()
                                    } catch (_: ActivityNotFoundException) {
                                        Toast.makeText(context, strings.trailerLinkOpenFailed, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = if (videoId != null) strings.openInYouTube
                                    else strings.openTrailerExternally
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .focusRequester(closeFocusRequester)
                                .tvFocusHighlight(shape = CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = strings.close)
                        }
                    }

                    if (videoId != null) {
                        AndroidView(
                            factory = { viewContext ->
                                WebView(viewContext).apply {
                                    setBackgroundColor(android.graphics.Color.BLACK)
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.mediaPlaybackRequiresUserGesture = false
                                    settings.allowFileAccess = false
                                    settings.allowContentAccess = false
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            view: WebView,
                                            request: WebResourceRequest
                                        ): Boolean = request.isForMainFrame
                                    }
                                    webChromeClient = object : WebChromeClient() {
                                        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                                            if (fullscreen == null) fullscreen = FullscreenTrailer(view, callback)
                                            else callback.onCustomViewHidden()
                                        }

                                        override fun onHideCustomView() {
                                            exitFullscreen()
                                        }
                                    }

                                    // YouTube uses the host app ID as the embedding client's referrer.
                                    val appOrigin = "https://${viewContext.packageName}"
                                    val html = """
                                        <!doctype html>
                                        <html>
                                        <head>
                                            <meta name="viewport" content="width=device-width, initial-scale=1">
                                            <style>
                                                html, body, iframe { width: 100%; height: 100%; margin: 0; border: 0; }
                                                body { background: #000; overflow: hidden; }
                                            </style>
                                        </head>
                                        <body>
                                            <iframe title="YouTube" allow="autoplay; encrypted-media; fullscreen"
                                                allowfullscreen referrerpolicy="strict-origin-when-cross-origin"
                                                src="https://www.youtube.com/embed/$videoId?autoplay=1&amp;playsinline=1&amp;rel=0">
                                            </iframe>
                                        </body>
                                        </html>
                                    """.trimIndent()
                                    loadDataWithBaseURL("$appOrigin/", html, "text/html", "UTF-8", null)
                                    trailerWebView = this
                                }
                            },
                            onRelease = { webView ->
                                trailerWebView = null
                                exitFullscreen()
                                webView.stopLoading()
                                webView.destroy()
                            },
                            modifier = Modifier.fillMaxWidth().height(playerHeight)
                        )
                    } else {
                        Text(strings.trailerUnavailableInApp, modifier = Modifier.padding(16.dp))
                    }
                }
            }

            fullscreen?.let { active ->
                key(active.view) {
                    AndroidView(
                        factory = { viewContext ->
                            FrameLayout(viewContext).apply {
                                addView(active.view, FrameLayout.LayoutParams(-1, -1))
                            }
                        },
                        onRelease = { it.removeAllViews() },
                        modifier = Modifier.fillMaxSize().background(Color.Black)
                    )
                }
            }

            LaunchedEffect(isTv) {
                if (isTv) closeFocusRequester.requestFocus()
            }
        }
    }
}
