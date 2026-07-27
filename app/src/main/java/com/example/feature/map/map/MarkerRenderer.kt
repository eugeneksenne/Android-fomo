package com.example.feature.map.map

import android.webkit.WebView
import com.example.core.data.ExploreVenue
import com.example.feature.map.util.MarkerGenerator
import com.example.feature.map.util.getVenueCoordinates

/**
 * The Map screen's "rendering pipeline": translates high-level map intents
 * (center camera, draw route, toggle heatmap, filter by category, add a
 * marker) into the exact `evaluateJavascript(...)` calls the pre-refactor
 * `MapScreen.kt` issued inline against `webViewRef`.
 *
 * Every function here is a null-safe wrapper around `WebView.evaluateJavascript`
 * so call sites (top bar, chips, cards, floating buttons, dialogs) don't need
 * to know the underlying JS function names or string-templating details -
 * they just call `MarkerRenderer.centerOnVenue(webView, venue)` etc.
 *
 * No JS behaviour changes here versus the pre-refactor inline calls; this is
 * purely "move the same evaluateJavascript(...) strings into one file".
 */
object MarkerRenderer {

    /** Default city camera framing used by the Recenter floating button. */
    val CITY_CENTER = getVenueCoordinatesForCityDefault()
    const val CITY_DEFAULT_ZOOM = 13
    const val VENUE_FOCUS_ZOOM = 15

    private fun getVenueCoordinatesForCityDefault() = com.example.feature.map.util.MapCoordinates(-26.115, 28.055)

    fun centerOn(webView: WebView?, latitude: Double, longitude: Double, zoom: Int = VENUE_FOCUS_ZOOM) {
        webView?.evaluateJavascript("centerOn($latitude, $longitude, $zoom);", null)
    }

    fun centerOnVenue(webView: WebView?, venue: ExploreVenue, zoom: Int = VENUE_FOCUS_ZOOM) {
        val coords = getVenueCoordinates(venue.id)
        centerOn(webView, coords.latitude, coords.longitude, zoom)
    }

    fun recenterToCityDefault(webView: WebView?) {
        centerOn(webView, CITY_CENTER.latitude, CITY_CENTER.longitude, CITY_DEFAULT_ZOOM)
    }

    fun drawRouteTo(webView: WebView?, latitude: Double, longitude: Double) {
        webView?.evaluateJavascript("drawRoute($latitude, $longitude);", null)
    }

    fun drawRouteToVenue(webView: WebView?, venue: ExploreVenue) {
        val coords = getVenueCoordinates(venue.id)
        drawRouteTo(webView, coords.latitude, coords.longitude)
    }

    /** Combined "jump to and draw a route to" used by search results and notification deep-links. */
    fun centerAndDrawRouteTo(webView: WebView?, latitude: Double, longitude: Double, zoom: Int = VENUE_FOCUS_ZOOM) {
        webView?.evaluateJavascript("centerOn($latitude, $longitude, $zoom); drawRoute($latitude, $longitude);", null)
    }

    fun clearRoute(webView: WebView?) {
        webView?.evaluateJavascript("clearRoute();", null)
    }

    fun toggleHeatmap(webView: WebView?, enabled: Boolean) {
        webView?.evaluateJavascript("toggleHeatmap($enabled);", null)
    }

    fun filterCategory(webView: WebView?, categoryLabel: String) {
        webView?.evaluateJavascript("filterCategory('$categoryLabel');", null)
    }

    fun fetchOverpassPOIs(webView: WebView?, categoryLabel: String) {
        webView?.evaluateJavascript("fetchOverpassPOIs('$categoryLabel');", null)
    }

    /** Category chip selection triggers both the local filter and a live Overpass POI refresh. */
    fun filterCategoryAndFetchOverpass(webView: WebView?, categoryLabel: String) {
        webView?.evaluateJavascript("filterCategory('$categoryLabel'); fetchOverpassPOIs('$categoryLabel');", null)
    }

    /** Appends a freshly-added (Add Place dialog) venue marker and flies the camera to it. */
    fun addCustomVenueMarker(webView: WebView?, venue: ExploreVenue) {
        webView?.evaluateJavascript(MarkerGenerator.buildAddCustomVenueScript(venue), null)
    }
}
