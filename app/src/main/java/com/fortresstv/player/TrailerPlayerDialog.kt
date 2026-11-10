package com.fortresstv.player

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fortresstv.player.ui.LocalAppStrings
import com.fortresstv.player.ui.LocalFormFactor
import com.fortresstv.player.ui.tvFocusHighlight
import kotlin.math.roundToInt

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
    val parentComposition = rememberCompositionContext()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentTitle by rememberUpdatedState(title)
    val dismiss by rememberUpdatedState(onDismiss)

    DisposableEffect(context, lifecycle, trailerUrl) {
        val videoId = extractYouTubeVideoId(trailerUrl)
        val externalUrl = videoId?.let { "https://www.youtube.com/watch?v=$it" }
            ?: trailerWebUri(trailerUrl)?.toASCIIString()
        val dialog = ComponentDialog(context, android.R.style.Theme_Material_Dialog_NoActionBar)
        val window = requireNotNull(dialog.window)
        window.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
        window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(false)

        val panel = LinearLayout(dialog.context).apply { orientation = LinearLayout.VERTICAL }
        val webView = videoId?.let { WebView(dialog.context) }
        // Put the video and Chromium's fullscreen surface directly in the native window.
        val root = object : FrameLayout(dialog.context) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                val density = resources.displayMetrics.density
                val width = MeasureSpec.getSize(widthMeasureSpec)
                val height = MeasureSpec.getSize(heightMeasureSpec)
                val videoSpace = (height - (72 * density).roundToInt()).coerceAtLeast(1)
                val panelWidth = minOf(width - (16 * density).roundToInt(), videoSpace * 16 / 9)
                    .coerceAtLeast(1)
                panel.layoutParams.width = panelWidth
                webView?.layoutParams?.height = (panelWidth * 9 / 16).coerceIn(
                    minOf((200 * density).roundToInt(), videoSpace), videoSpace
                )
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            }
        }
        root.addView(panel, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.CENTER))
        panel.addView(
            ComposeView(dialog.context).apply {
                setParentCompositionContext(parentComposition)
                setContent {
                    TrailerDialogHeader(currentTitle, externalUrl, videoId != null) { dialog.dismiss() }
                }
            },
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        )

        var fullscreen: FullscreenTrailer? = null
        fun exitFullscreen() {
            val active = fullscreen ?: return
            fullscreen = null
            root.removeView(active.view)
            panel.visibility = View.VISIBLE
            active.callback.onCustomViewHidden()
        }

        if (webView != null && videoId != null) {
            panel.addView(webView, LinearLayout.LayoutParams(MATCH_PARENT, 1))
            webView.apply {
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
                        if (fullscreen != null || view.parent != null) {
                            callback.onCustomViewHidden()
                            return
                        }
                        fullscreen = FullscreenTrailer(view, callback)
                        panel.visibility = View.INVISIBLE
                        root.addView(view, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
                    }

                    override fun onHideCustomView() {
                        exitFullscreen()
                    }
                }
            }
        }

        val backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (fullscreen != null) exitFullscreen() else dialog.dismiss()
            }
        }
        dialog.onBackPressedDispatcher.addCallback(backCallback)
        dialog.setOnDismissListener { dismiss() }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                webView?.onPause()
                dialog.dismiss()
            }
        }
        lifecycle.addObserver(observer)
        dialog.setContentView(root)
        dialog.show()
        window.setLayout(MATCH_PARENT, MATCH_PARENT)
        if (webView != null && videoId != null) {
            // YouTube uses the host app ID as the embedding client's referrer.
            val appOrigin = "https://${context.packageName}/"
            val html = """
                <!doctype html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <style>
                        html, body, iframe { width: 100%; height: 100%; margin: 0; border: 0; }
                        body { background: #000; overflow: hidden; }
                        iframe { display: block; }
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
            webView.loadDataWithBaseURL(appOrigin, html, "text/html", "UTF-8", null)
        }

        onDispose {
            lifecycle.removeObserver(observer)
            backCallback.remove()
            dialog.setOnDismissListener(null)
            exitFullscreen()
            dialog.dismiss()
            webView?.let {
                panel.removeView(it)
                it.stopLoading()
                it.destroy()
            }
        }
    }
}

@Composable
private fun TrailerDialogHeader(
    title: String,
    externalUrl: String?,
    hasEmbeddedVideo: Boolean,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val isTv = LocalFormFactor.current.isTelevision
    val closeFocusRequester = remember { FocusRequester() }
    Surface(
        shape = if (hasEmbeddedVideo) RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        else RoundedCornerShape(16.dp),
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
                                onClose()
                            } catch (_: ActivityNotFoundException) {
                                Toast.makeText(context, strings.trailerLinkOpenFailed, Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = if (hasEmbeddedVideo) strings.openInYouTube
                            else strings.openTrailerExternally
                        )
                    }
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .focusRequester(closeFocusRequester)
                        .tvFocusHighlight(shape = CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = strings.close)
                }
            }
            if (!hasEmbeddedVideo) {
                Text(strings.trailerUnavailableInApp, modifier = Modifier.padding(16.dp))
            }
        }
    }

    LaunchedEffect(isTv) {
        if (isTv) closeFocusRequester.requestFocus()
    }
}
