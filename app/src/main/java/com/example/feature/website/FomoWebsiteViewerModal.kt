package com.example.feature.website

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.webkit.GeolocationPermissions
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Universal Utility function to launch any website across FOMO.
 * Uses Chrome Custom Tabs as a fast default, or can route into the full-stack In-App WebView Engine.
 */
fun openFomoWebsite(
    context: Context,
    rawUrl: String,
    title: String? = null,
    onShowInAppViewer: ((url: String, title: String?) -> Unit)? = null
) {
    if (rawUrl.isBlank()) {
        Toast.makeText(context, "Invalid website address", Toast.LENGTH_SHORT).show()
        return
    }

    val formattedUrl = when {
        rawUrl.startsWith("http://") || rawUrl.startsWith("https://") -> rawUrl
        else -> "https://$rawUrl"
    }

    if (onShowInAppViewer != null) {
        onShowInAppViewer(formattedUrl, title)
        return
    }

    // High-performance Chrome Custom Tabs implementation
    try {
        val colorSchemeParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(0xFF0B0E14.toInt())
            .setNavigationBarColor(0xFF0B0E14.toInt())
            .build()

        val customTabsIntent = CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(colorSchemeParams)
            .setShowTitle(true)
            .setUrlBarHidingEnabled(false)
            .build()

        customTabsIntent.launchUrl(context, Uri.parse(formattedUrl))
    } catch (e: Exception) {
        // Fallback to standard system browser
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl))
            context.startActivity(browserIntent)
        } catch (err: Exception) {
            Toast.makeText(context, "Could not open website: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}

/**
 * Full-stack In-App Universal Website Viewer Sheet
 * Supporting responsive views, payment gateways (Stripe, PayPal, PayFast, Ozow), SSL validation,
 * file uploads, deep links, and desktop mode toggle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FomoWebsiteViewerSheet(
    url: String,
    initialTitle: String? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var currentUrl by remember { mutableStateOf(if (url.startsWith("http")) url else "https://$url") }
    var pageTitle by remember { mutableStateOf(initialTitle ?: getDomainFromUrl(currentUrl)) }
    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0.05f) }
    var isDesktopView by remember { mutableStateOf(false) }
    var isSecureSsl by remember { mutableStateOf(currentUrl.startsWith("https://")) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    // File Upload Callback for WebChromeClient (e.g., uploading receipts, ID verification, ticket documents)
    var fileUploadCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (fileUploadCallback != null) {
            val data = result.data?.data
            val results = if (data != null) arrayOf(data) else null
            fileUploadCallback?.onReceiveValue(results)
            fileUploadCallback = null
        }
    }

    // Payment Gateway & Secure Merchant Detection
    val isPaymentPage = remember(currentUrl) {
        val lower = currentUrl.lowercase()
        lower.contains("stripe") || lower.contains("paypal") || lower.contains("payfast") ||
                lower.contains("ozow") || lower.contains("peachpayments") || lower.contains("yoco") ||
                lower.contains("checkout") || lower.contains("pay") || lower.contains("booking")
    }

    BackHandler(enabled = canGoBack) {
        webViewRef?.goBack()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF090B10),
        contentColor = Color.White,
        tonalElevation = 8.dp,
        scrimColor = Color.Black.copy(alpha = 0.75f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090B10))
        ) {
            // 1. Top Control Bar & Security Lock Header
            Surface(
                color = Color(0xFF0F1522),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Close Button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White.copy(alpha = 0.08f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close Viewer", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        // Domain & Security Badge Capsule
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF182032),
                            border = BorderStroke(
                                1.dp,
                                if (isPaymentPage) Color(0xFF00E676) else if (isSecureSsl) Color(0xFF29B6F6) else Color(0xFFFFB74D)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isPaymentPage) Icons.Default.Shield else if (isSecureSsl) Icons.Default.Lock else Icons.Default.Warning,
                                    contentDescription = "SSL Security Status",
                                    tint = if (isPaymentPage) Color(0xFF00E676) else if (isSecureSsl) Color(0xFF29B6F6) else Color(0xFFFFB74D),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = pageTitle,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = getDomainFromUrl(currentUrl),
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Action Buttons: Reload, Desktop/Mobile Toggle, Share, Open External
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Reload
                            IconButton(
                                onClick = {
                                    hasError = false
                                    webViewRef?.reload()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reload Page", tint = Color.White, modifier = Modifier.size(18.dp))
                            }

                            // Desktop/Mobile User-Agent Toggle
                            IconButton(
                                onClick = {
                                    isDesktopView = !isDesktopView
                                    webViewRef?.let { webView ->
                                        val settings = webView.settings
                                        if (isDesktopView) {
                                            settings.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                                        } else {
                                            settings.userAgentString = null // Default mobile UA
                                        }
                                        webView.reload()
                                    }
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        if (isDesktopView) Color(0xFF76FF03).copy(alpha = 0.2f) else Color.Transparent,
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = if (isDesktopView) Icons.Default.DesktopWindows else Icons.Default.PhoneAndroid,
                                    contentDescription = "Toggle Desktop View",
                                    tint = if (isDesktopView) Color(0xFF76FF03) else Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Share Link
                            IconButton(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, currentUrl)
                                        putExtra(Intent.EXTRA_SUBJECT, pageTitle)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Website"))
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share Link", tint = Color.White, modifier = Modifier.size(18.dp))
                            }

                            // Open in System Browser
                            IconButton(
                                onClick = {
                                    openFomoWebsite(context, currentUrl)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = "Open in Chrome Browser", tint = Color(0xFF29B6F6), modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Loading Progress Indicator
                    if (isLoading) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = if (isPaymentPage) Color(0xFF00E676) else Color(0xFF76FF03),
                            trackColor = Color(0xFF182032)
                        )
                    }

                    // Payment / Merchant Session Banner
                    if (isPaymentPage) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF00E676).copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "🔒 Secure Merchant Payment Flow Active (Stripe / Gateway Direct)",
                                    color = Color(0xFF00E676),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // 2. Main WebView Body & Error Fallback
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (hasError) {
                    // Friendly Connection Error Card
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFF5252).copy(alpha = 0.15f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(36.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Unable to load website",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "Check your network connection or try opening in your system browser.",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    hasError = false
                                    webViewRef?.reload()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76FF03), contentColor = Color.Black)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    openFomoWebsite(context, currentUrl)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E283C), contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open in Browser")
                            }
                        }
                    }
                } else {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    javaScriptCanOpenWindowsAutomatically = false
                                    setSupportMultipleWindows(false)
                                    // SECURITY: deny the remote page access to the
                                    // app's private file storage and content providers.
                                    allowFileAccess = false
                                    allowContentAccess = false
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    // SECURITY: never silently downgrade to http on an
                                    // https page. NEVER_ALLOW blocks mixed content.
                                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        progress = newProgress / 100f
                                        isLoading = newProgress < 100
                                    }

                                    override fun onReceivedTitle(view: WebView?, title: String?) {
                                        if (!title.isNullOrEmpty()) {
                                            pageTitle = title
                                        }
                                    }

                                    override fun onGeolocationPermissionsShowPrompt(
                                        origin: String?,
                                        callback: GeolocationPermissions.Callback?
                                    ) {
                                        // SECURITY/PRIVACY: do not auto-grant precise
                                        // location to arbitrary third-party web content.
                                        // Granting silently (the previous behaviour) leaks
                                        // the user's location to any page they open and
                                        // breaches the Play Store user-data policy.
                                        // Deny, and do not retain the decision.
                                        callback?.invoke(origin, false, false)
                                    }

                                    override fun onShowFileChooser(
                                        webView: WebView?,
                                        filePathCallback: ValueCallback<Array<Uri>>?,
                                        fileChooserParams: FileChooserParams?
                                    ): Boolean {
                                        fileUploadCallback?.onReceiveValue(null)
                                        fileUploadCallback = filePathCallback
                                        try {
                                            val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                                type = "*/*"
                                            }
                                            filePickerLauncher.launch(intent)
                                        } catch (e: Exception) {
                                            fileUploadCallback?.onReceiveValue(null)
                                            fileUploadCallback = null
                                            return false
                                        }
                                        return true
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                        if (url == null) return false

                                        // Handle non-http schemes (bank apps, intent://, whatsapp:, tel:, mailto:)
                                        if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                            try {
                                                val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                                                ctx.startActivity(intent)
                                                return true
                                            } catch (e: Exception) {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                    ctx.startActivity(intent)
                                                    return true
                                                } catch (err: Exception) {
                                                    Toast.makeText(ctx, "App for link not found", Toast.LENGTH_SHORT).show()
                                                    return true
                                                }
                                            }
                                        }

                                        currentUrl = url
                                        isSecureSsl = url.startsWith("https://")
                                        return false
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                        isLoading = true
                                        hasError = false
                                        if (url != null) {
                                            currentUrl = url
                                            isSecureSsl = url.startsWith("https://")
                                        }
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isLoading = false
                                        canGoBack = view?.canGoBack() == true
                                        canGoForward = view?.canGoForward() == true
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        errorCode: Int,
                                        description: String?,
                                        failingUrl: String?
                                    ) {
                                        if (errorCode != ERROR_HOST_LOOKUP && errorCode != ERROR_CONNECT) {
                                            // Soft error
                                        } else {
                                            hasError = true
                                            errorMessage = description ?: "Failed to connect to host."
                                        }
                                    }

                                    override fun onReceivedSslError(
                                        view: WebView?,
                                        handler: SslErrorHandler?,
                                        error: SslError?
                                    ) {
                                        // SECURITY: never call handler.proceed() on an SSL error.
                                        // This viewer is documented as supporting payment
                                        // gateways (Stripe/PayPal/PayFast/Ozow); silently
                                        // accepting an invalid certificate makes every session
                                        // trivially man-in-the-middleable and is an automatic
                                        // Google Play rejection under the Device and Network
                                        // Abuse policy.
                                        handler?.cancel()
                                        isLoading = false
                                        isSecureSsl = false
                                        hasError = true
                                        errorMessage = when (error?.primaryError) {
                                            SslError.SSL_EXPIRED ->
                                                "This site's security certificate has expired."
                                            SslError.SSL_IDMISMATCH ->
                                                "This site's security certificate does not match its address."
                                            SslError.SSL_UNTRUSTED ->
                                                "This site's security certificate is not trusted."
                                            SslError.SSL_DATE_INVALID ->
                                                "This site's security certificate is not yet valid."
                                            else ->
                                                "A secure connection to this site could not be established."
                                        } + " For your safety, the page was blocked."
                                    }
                                }

                                loadUrl(currentUrl)
                                webViewRef = this
                            }
                        },
                        update = { webView ->
                            webViewRef = webView
                            canGoBack = webView.canGoBack()
                            canGoForward = webView.canGoForward()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 3. Bottom Web Navigation Bar
            Surface(
                color = Color(0xFF0D121D),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { webViewRef?.goBack() },
                            enabled = canGoBack,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = if (canGoBack) Color.White else Color.White.copy(alpha = 0.25f)
                            )
                        }

                        IconButton(
                            onClick = { webViewRef?.goForward() },
                            enabled = canGoForward,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Default.ArrowForward,
                                contentDescription = "Forward",
                                tint = if (canGoForward) Color.White else Color.White.copy(alpha = 0.25f)
                            )
                        }
                    }

                    Text(
                        text = "FOMO Universal Web Engine",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    IconButton(
                        onClick = {
                            openFomoWebsite(context, currentUrl)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Public, contentDescription = "Launch Browser", tint = Color(0xFF76FF03))
                    }
                }
            }
        }
    }
}

private fun getDomainFromUrl(url: String): String {
    return try {
        val uri = Uri.parse(if (url.startsWith("http")) url else "https://$url")
        uri.host?.removePrefix("www.") ?: url
    } catch (e: Exception) {
        url
    }
}
