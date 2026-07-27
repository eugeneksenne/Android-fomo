package com.example.feature.map.map

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Thin Compose wrapper around the raw Android [WebView] used to host the
 * Leaflet map page.
 *
 * This is exactly the `AndroidView { WebView(ctx).apply { ... } }` block that
 * used to live inline inside `MapScreen`'s body - same `WebViewClient`, same
 * settings (JS enabled, DOM storage enabled, mixed content always allowed),
 * same `AndroidBridge` Javascript interface shape
 * (`onVenueClick`/`onFriendClick`/`onOverpassResult`), same
 * `loadDataWithBaseURL` call. Only the callback bodies moved from being
 * inlined against `MapScreen`'s local state to plain callback parameters, so
 * this file has no knowledge of `MapScreenState`, filtering, or ranking.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewMap(
    initialHtml: String,
    onVenueMarkerClick: (venueId: String) -> Unit,
    onFriendMarkerClick: (friendId: String) -> Unit,
    onOverpassResult: (count: Int, category: String) -> Unit,
    onWebViewReady: (WebView) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                webViewClient = WebViewClient()
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onVenueClick(venueId: String) {
                            post { onVenueMarkerClick(venueId) }
                        }

                        @JavascriptInterface
                        fun onFriendClick(friendId: String) {
                            post { onFriendMarkerClick(friendId) }
                        }

                        @JavascriptInterface
                        fun onOverpassResult(count: Int, category: String) {
                            post { onOverpassResult(count, category) }
                        }
                    },
                    "AndroidBridge"
                )
                loadDataWithBaseURL("https://openstreetmap.org", initialHtml, "text/html", "UTF-8", null)
                onWebViewReady(this)
            }
        },
        modifier = modifier
    )
}
