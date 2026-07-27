package com.example.feature.discover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import coil.compose.LocalImageLoader
import coil.request.ImageRequest

/**
 * Prefetches hero and first-viewport imagery so the Discover feed paints
 * instantly instead of popping in while the user scrolls.
 *
 * Static showcase imagery (hero, live moments, trending, closing soon and
 * the smart places fallbacks) is prefetched unconditionally; data-driven
 * imagery (venues, events, circle stories) is prefetched from the current
 * repository state for the first few cards only.
 *
 * Prefetch requests share Coil's memory/disk cache with the AsyncImage
 * calls, so a warmed request renders synchronously in the card.
 */
@Composable
fun DiscoverImagePrefetcher(
    venues: List<com.example.core.data.ExploreVenue> = emptyList(),
    events: List<com.example.core.data.Event> = emptyList(),
    stories: List<com.example.core.data.CircleStory> = emptyList()
) {
    val context = LocalContext.current
    val imageLoader = LocalImageLoader.current

    LaunchedEffect(venues, events, stories) {
        val urls = STATIC_FIRST_VIEWPORT_URLS +
            venues.take(5).map { it.imageUrl } +
            events.take(3).map { it.posterUrl } +
            stories.take(8).map { it.userAvatar }

        urls.distinct().forEach { url ->
            if (url.isNotBlank()) {
                imageLoader.enqueue(
                    ImageRequest.Builder(context)
                        .data(url)
                        // No draw target: warm the caches only.
                        .target(onSuccess = {})
                        .build()
                )
            }
        }
    }
}

/** First-viewport static imagery used by HeroSection and the static sections. */
private val STATIC_FIRST_VIEWPORT_URLS = listOf(
    // HeroSection
    "https://images.unsplash.com/photo-1577546684742-df290b201464?q=80&w=1000&auto=format&fit=crop",
    // MyCircleSection "Your Story"
    "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?q=80&w=200&auto=format&fit=crop",
    // ClosingSoonCard venue thumb
    "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?q=80&w=200&auto=format&fit=crop",
    // LiveMomentCard
    "https://images.unsplash.com/photo-1545128485-c400e7702796?q=80&w=400&auto=format&fit=crop",
    // SmartPlacesSection fallback cards
    "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?q=80&w=600&auto=format&fit=crop",
    "https://images.unsplash.com/photo-1514933651103-005eec06c04b?q=80&w=600&auto=format&fit=crop",
    // TrendingCard thumb
    "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=80&w=200&auto=format&fit=crop"
)
