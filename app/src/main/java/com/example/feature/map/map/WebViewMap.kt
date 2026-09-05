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
                setBackgroundColor(android.graphics.Color.parseColor("#0B0F19"))
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36 FomoOSM/1.0"
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
                webChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onConsoleMessage(cm: android.webkit.ConsoleMessage?): Boolean {
                        android.util.Log.d("FomoOSMMap", "${cm?.message()} -- line ${cm?.lineNumber()}")
                        return true
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onWebViewReady(this@apply)
                    }
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
