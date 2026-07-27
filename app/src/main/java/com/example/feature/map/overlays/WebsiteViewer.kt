package com.example.feature.map.overlays

import androidx.compose.runtime.Composable
import com.example.feature.website.FomoWebsiteViewerSheet

/**
 * Thin Map-screen wrapper around the shared
 * [com.example.feature.website.FomoWebsiteViewerSheet] universal in-app
 * website viewer.
 *
 * Extracted from the inline `activeWebsiteUrl?.let { ... }` block in
 * `MapScreen.kt`. Renders nothing when [url] is null.
 */
@Composable
fun WebsiteViewer(
    url: String?,
    title: String?,
    onDismiss: () -> Unit
) {
    if (url == null) return
    FomoWebsiteViewerSheet(
        url = url,
        initialTitle = title,
        onDismiss = onDismiss
    )
}
