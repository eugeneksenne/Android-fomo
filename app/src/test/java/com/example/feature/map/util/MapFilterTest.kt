package com.example.feature.map.util

import com.example.core.data.ExploreVenue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the Map screen's filtering, ranking, and marker-generation
 * pipelines: [VenueFilter], [VenueRanking] and [MarkerGenerator].
 */
class MapFilterTest {

    private fun venue(
        id: String,
        category: String,
        rating: Float = 4.0f
    ) = ExploreVenue(
        id = id,
        name = "Venue $id",
        category = category,
        subcategory = "Test",
        imageUrl = "",
        rating = rating,
        reviewCount = 5,
        address = "Test Address",
        area = "Test Area",
        distanceText = "1km away",
        attributes = emptyList(),
        openDays = "Open Now",
        startHour = 18,
        endHour = 2
    )

    // ---------------------------------------------------------------------
    // Category normalisation
    // ---------------------------------------------------------------------

    @Test
    fun normalizeCategory_stripsEmojiAndPunctuationAndLowercases() {
        assertEquals("nightlife", VenueFilter.normalizeCategory("🌙 Nightlife"))
        assertEquals("all", VenueFilter.normalizeCategory("All"))
        assertEquals("wellness", VenueFilter.normalizeCategory("☕ Wellness"))
    }

    // ---------------------------------------------------------------------
    // Category filtering
    // ---------------------------------------------------------------------

    @Test
    fun filterByCategory_all_returnsEveryVenue() {
        val venues = listOf(venue("v1", "Nightlife"), venue("v2", "Food"))
        val result = VenueFilter.filterByCategory(venues, "All")
        assertEquals(venues, result)
    }

    @Test
    fun filterByCategory_wellness_matchesBothRecoverAndWellnessCategories() {
        val venues = listOf(
            venue("v1", "Recover"),
            venue("v2", "Wellness"),
            venue("v3", "Nightlife")
        )
        val result = VenueFilter.filterByCategory(venues, "☕ Wellness")
        assertEquals(setOf("v1", "v2"), result.map { it.id }.toSet())
    }

    @Test
    fun filterByCategory_events_matchesCuratedIdsAndEvtPrefixedVenues() {
        val venues = listOf(
            venue("fomo_club", "Nightlife"),
            venue("d48_midrand", "Nightlife"),
            venue("konka_soweto", "Nightlife"),
            venue("evt_1234", "Nightlife"),
            venue("taboo_sandton", "Nightlife")
        )
        val result = VenueFilter.filterByCategory(venues, "🎫 Events")
        assertEquals(
            setOf("fomo_club", "d48_midrand", "konka_soweto", "evt_1234"),
            result.map { it.id }.toSet()
        )
    }

    @Test
    fun filterByCategory_specificCategory_matchesOnlyThatCategory() {
        val venues = listOf(
            venue("v1", "Nightlife"),
            venue("v2", "Food"),
            venue("v3", "Nightlife")
        )
        val result = VenueFilter.filterByCategory(venues, "🌙 Nightlife")
        assertEquals(setOf("v1", "v3"), result.map { it.id }.toSet())
    }

    @Test
    fun filterByCategory_noMatches_returnsEmptyList() {
        val venues = listOf(venue("v1", "Nightlife"))
        val result = VenueFilter.filterByCategory(venues, "✈ Travel")
        assertTrue(result.isEmpty())
    }

    @Test
    fun hasEvent_matchesCuratedIdsAndEvtPrefix() {
        assertTrue(VenueFilter.hasEvent(venue("fomo_club", "Nightlife")))
        assertTrue(VenueFilter.hasEvent(venue("d48_midrand", "Nightlife")))
        assertTrue(VenueFilter.hasEvent(venue("konka_soweto", "Nightlife")))
        assertTrue(VenueFilter.hasEvent(venue("evt_999", "Nightlife")))
        assertFalse(VenueFilter.hasEvent(venue("taboo_sandton", "Nightlife")))
    }

    // ---------------------------------------------------------------------
    // Nearest venue ranking
    // ---------------------------------------------------------------------

    @Test
    fun nearestVenue_picksHighestRatedVenue() {
        val venues = listOf(
            venue("low", "Nightlife", rating = 3.5f),
            venue("high", "Nightlife", rating = 4.9f),
            venue("mid", "Nightlife", rating = 4.2f)
        )
        val nearest = VenueRanking.nearestVenue(venues)
        assertEquals("high", nearest.id)
    }

    @Test
    fun nearestVenue_emptyList_fallsBackToDefaultFomoClubVenue() {
        val nearest = VenueRanking.nearestVenue(emptyList())
        assertEquals(defaultFomoClubVenue.id, nearest.id)
    }

    @Test
    fun vibeScore_scalesRatingToPercentage() {
        assertEquals(90, VenueRanking.vibeScore(venue("v1", "Nightlife", rating = 4.5f)))
        assertEquals(100, VenueRanking.vibeScore(venue("v2", "Nightlife", rating = 5.0f)))
    }

    // ---------------------------------------------------------------------
    // Coordinates lookup
    // ---------------------------------------------------------------------

    @Test
    fun getVenueCoordinates_returnsCuratedCoordinatesForKnownVenues() {
        val coords = getVenueCoordinates("fomo_club")
        assertEquals(-26.1452, coords.latitude, 0.0001)
        assertEquals(28.0472, coords.longitude, 0.0001)
    }

    @Test
    fun getVenueCoordinates_unknownVenue_returnsCoordinatesNearCityCenter() {
        val coords = getVenueCoordinates("some_unknown_venue_id")
        // Fallback jitters within +/-0.01 of the FOMO Club coordinates.
        assertTrue(coords.latitude in -26.1552..-26.1352)
        assertTrue(coords.longitude in 28.0372..28.0572)
    }

    // ---------------------------------------------------------------------
    // Marker generation
    // ---------------------------------------------------------------------

    @Test
    fun buildVenueMarkersScript_emitsOneAddVenueMarkerCallPerVenue() {
        val venues = listOf(venue("v1", "Nightlife"), venue("v2", "Food"))
        val script = MarkerGenerator.buildVenueMarkersScript(venues)
        assertEquals(2, Regex("addVenueMarker\\(").findAll(script).count())
        assertTrue(script.contains("'v1'"))
        assertTrue(script.contains("'v2'"))
    }

    @Test
    fun buildVenueMarkersScript_flagsCuratedHotAndTrendingVenues() {
        val script = MarkerGenerator.buildVenueMarkersScript(listOf(venue("fomo_club", "Nightlife")))
        // fomo_club is curated as hasFlashDrop, isLive, hasEvent, sponsored,
        // trending and hot all at once (see MarkerGenerator kdoc).
        assertTrue(script.contains("true"))
    }

    @Test
    fun buildFriendMarkersScript_emitsOneAddFriendMarkerCallPerFriend() {
        val friends = listOf(
            com.example.core.data.CircleFriend(
                id = "f1",
                name = "Test Friend",
                username = "testfriend",
                avatarUrl = "",
                mutualFriendsCount = 2,
                currentActivity = "At the club",
                status = "Online",
                latitude = -26.1,
                longitude = 28.0
            )
        )
        val script = MarkerGenerator.buildFriendMarkersScript(friends)
        assertEquals(1, Regex("addFriendMarker\\(").findAll(script).count())
        assertTrue(script.contains("'f1'"))
    }

    @Test
    fun buildAddCustomVenueScript_includesCenterOnCall() {
        val script = MarkerGenerator.buildAddCustomVenueScript(venue("custom_1", "Nightlife"))
        assertTrue(script.contains("addVenueMarker("))
        assertTrue(script.contains("centerOn("))
        assertTrue(script.contains("'custom_1'"))
    }

    // ---------------------------------------------------------------------
    // JS-injection escaping (Add Place dialog fields are free-text/user input)
    // ---------------------------------------------------------------------

    @Test
    fun buildAddCustomVenueScript_escapesSingleQuotesInUserSuppliedFields() {
        // A user typing this into the Add Place "Subcategory" field must not
        // be able to break out of the JS string literal and inject a call.
        val malicious = venue("custom_2", "Nightlife").copy(
            subcategory = "VIP' ); alert(document.cookie); //"
        )
        val script = MarkerGenerator.buildAddCustomVenueScript(malicious)
        assertFalse(
            "Unescaped single quote would let injected JS run as a sibling statement",
            script.contains("VIP' ); alert(document.cookie); //")
        )
        assertTrue(script.contains("VIP\\' ); alert(document.cookie); //"))
    }

    @Test
    fun buildVenueMarkersScript_escapesSingleQuotesInNameAndSubcategory() {
        val malicious = venue("custom_3", "Nightlife").copy(
            name = "O'Malley's</script><script>alert(1)</script>",
            subcategory = "Bar' + window.location"
        )
        val script = MarkerGenerator.buildVenueMarkersScript(listOf(malicious))
        assertTrue(script.contains("O\\'Malley\\'s"))
        assertTrue(script.contains("Bar\\' + window.location"))
    }

    @Test
    fun buildFriendMarkersScript_escapesSingleQuotesInFriendFields() {
        val friend = com.example.core.data.CircleFriend(
            id = "f1",
            name = "Mal'icious",
            username = "testfriend",
            avatarUrl = "",
            mutualFriendsCount = 2,
            currentActivity = "at the club' ); alert(1); //",
            status = "Online",
            latitude = -26.1,
            longitude = 28.0
        )
        val script = MarkerGenerator.buildFriendMarkersScript(listOf(friend))
        assertFalse(script.contains("at the club' ); alert(1); //"))
        assertTrue(script.contains("Mal\\'icious"))
    }
}
