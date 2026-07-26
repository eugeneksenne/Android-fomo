package com.example.core.data.feed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the FOMO Score ranking engine.
 *
 * Before this existed the feed tabs applied a boolean filter and rendered
 * whatever order the list happened to be in — there was no ranking at all.
 */
class FomoScoreTest {

    private val forYou = FomoScore.Weights.FOR_YOU

    // ---- the spec's central property ---------------------------------------

    @Test
    fun `momentum outweighs historical popularity`() {
        // Spec: "Momentum always outweighs historical popularity."
        val freshViral = FomoScore.Signals(
            velocityRipplesPerMin = 25f,
            watchCompletion = 0.85f,
            likes = 120,
            ripples = 90,
            ageMinutes = 10,
            distanceMetres = 500.0,
        )
        val oldPopular = FomoScore.Signals(
            velocityRipplesPerMin = 0.2f,
            watchCompletion = 0.4f,
            likes = 100_000,
            ripples = 9_000,
            ageMinutes = 60 * 24 * 3,
            distanceMetres = 500.0,
        )
        assertTrue(
            "a Moment going viral now must outrank a stale one with more lifetime likes",
            FomoScore.score(freshViral, forYou) > FomoScore.score(oldPopular, forYou)
        )
    }

    @Test
    fun `huge counts cannot swamp every other signal`() {
        // Logarithmic compression stops one metric dominating and ossifying
        // the feed around a handful of posts.
        val big = FomoScore.Signals(likes = 1_000_000, ageMinutes = 60 * 24 * 7)
        val modest = FomoScore.Signals(likes = 1_000, ageMinutes = 60 * 24 * 7)
        val ratio = FomoScore.score(big, forYou) / FomoScore.score(modest, forYou).coerceAtLeast(0.001f)
        assertTrue("1000x the likes must not mean 1000x the score, got ${ratio}x", ratio < 3f)
    }

    // ---- recency -----------------------------------------------------------

    @Test
    fun `recency halves every six hours`() {
        assertEquals(1.0, FomoScore.recencyScore(0), 0.001)
        assertEquals(0.5, FomoScore.recencyScore(360), 0.001)
        assertEquals(0.25, FomoScore.recencyScore(720), 0.001)
    }

    @Test
    fun `newer content ranks above otherwise identical older content`() {
        val base = FomoScore.Signals(velocityRipplesPerMin = 5f, likes = 50)
        val newer = base.copy(ageMinutes = 5)
        val older = base.copy(ageMinutes = 60 * 12)
        assertTrue(FomoScore.score(newer, forYou) > FomoScore.score(older, forYou))
    }

    // ---- proximity ---------------------------------------------------------

    @Test
    fun `proximity decays with distance`() {
        assertEquals(1.0, FomoScore.proximityScore(0.0), 0.001)
        assertTrue(FomoScore.proximityScore(1_000.0) > FomoScore.proximityScore(10_000.0))
        assertEquals(0.0, FomoScore.proximityScore(25_000.0), 0.001)
        assertEquals(0.0, FomoScore.proximityScore(999_999.0), 0.001)
    }

    @Test
    fun `unknown distance contributes nothing rather than counting as zero metres`() {
        // Treating unknown as 0m would rank distance-less content as if the
        // user were standing in the venue.
        assertEquals(0.0, FomoScore.proximityScore(null), 0.001)
    }

    @Test
    fun `Nearby weights proximity far above For You`() {
        val near = FomoScore.Signals(distanceMetres = 200.0, ageMinutes = 30)
        val far = FomoScore.Signals(distanceMetres = 20_000.0, ageMinutes = 30)

        val nearbyGap = FomoScore.score(near, FomoScore.Weights.NEARBY) -
            FomoScore.score(far, FomoScore.Weights.NEARBY)
        val forYouGap = FomoScore.score(near, forYou) - FomoScore.score(far, forYou)

        assertTrue("Nearby must be more distance-sensitive", nearbyGap > forYouGap)
    }

    // ---- tab weighting -----------------------------------------------------

    @Test
    fun `Following is dominated by recency as the spec requires`() {
        // Spec: Following is "sorted by newest".
        val newerQuiet = FomoScore.Signals(ageMinutes = 5, velocityRipplesPerMin = 0.1f)
        val olderViral = FomoScore.Signals(ageMinutes = 60 * 8, velocityRipplesPerMin = 30f)
        assertTrue(
            FomoScore.score(newerQuiet, FomoScore.Weights.FOLLOWING) >
                FomoScore.score(olderViral, FomoScore.Weights.FOLLOWING)
        )
    }

    @Test
    fun `Live weights viewer count heavily`() {
        val busy = FomoScore.Signals(isLiveNow = true, liveViewers = 5_000, ageMinutes = 10)
        val quiet = FomoScore.Signals(isLiveNow = true, liveViewers = 5, ageMinutes = 10)
        assertTrue(
            FomoScore.score(busy, FomoScore.Weights.LIVE) >
                FomoScore.score(quiet, FomoScore.Weights.LIVE)
        )
    }

    @Test
    fun `viewer count only counts for content that is actually live`() {
        val notLive = FomoScore.Signals(isLiveNow = false, liveViewers = 5_000)
        val zeroViewers = FomoScore.Signals(isLiveNow = false, liveViewers = 0)
        assertEquals(
            FomoScore.score(notLive, FomoScore.Weights.LIVE),
            FomoScore.score(zeroViewers, FomoScore.Weights.LIVE),
            0.001f
        )
    }

    @Test
    fun `every tab resolves to a weight set`() {
        listOf("For You", "Following", "Nearby", "Live", "Unknown").forEach {
            FomoScore.Weights.forTab(it)
        }
        assertEquals(FomoScore.Weights.FOR_YOU, FomoScore.Weights.forTab("anything else"))
    }

    // ---- robustness --------------------------------------------------------

    @Test
    fun `an empty signal set is dominated by its recency term`() {
        // A brand-new Moment with no engagement still scores, because age 0
        // gives full recency. That is intended: fresh content must be
        // discoverable before it has accumulated any signal.
        val score = FomoScore.score(FomoScore.Signals(), FomoScore.Weights.FOLLOWING)
        assertEquals(FomoScore.Weights.FOLLOWING.recency, score, 0.01f)
    }

    @Test
    fun `a stale empty Moment scores near zero`() {
        val ancient = FomoScore.Signals(ageMinutes = 60 * 24 * 30)
        assertTrue(FomoScore.score(ancient, FomoScore.Weights.FOLLOWING) < 0.1f)
    }

    @Test
    fun `scores are never negative`() {
        val odd = FomoScore.Signals(
            velocityRipplesPerMin = -5f,
            watchCompletion = -1f,
            likes = -10,
            ageMinutes = -100,
        )
        assertTrue(FomoScore.score(odd, forYou) >= 0f)
    }

    // ---- parsing -----------------------------------------------------------

    @Test
    fun `distance strings parse into metres`() {
        assertEquals(280.0, FomoScore.parseDistanceMetres("280m away")!!, 0.1)
        assertEquals(1_400.0, FomoScore.parseDistanceMetres("1.4km away")!!, 0.1)
        assertEquals(0.0, FomoScore.parseDistanceMetres("Right here")!!, 0.1)
        assertEquals(12_000.0, FomoScore.parseDistanceMetres("12 km away")!!, 0.1)
    }

    @Test
    fun `unparseable distances return null`() {
        assertNull(FomoScore.parseDistanceMetres(""))
        assertNull(FomoScore.parseDistanceMetres("somewhere in town"))
        assertNull(FomoScore.parseDistanceMetres("18 min away"))
    }

    @Test
    fun `relative timestamps parse into minutes`() {
        assertEquals(0L, FomoScore.parseAgeMinutes("Just now"))
        assertEquals(5L, FomoScore.parseAgeMinutes("5m ago"))
        assertEquals(120L, FomoScore.parseAgeMinutes("2h ago"))
        assertEquals(60L * 24 * 3, FomoScore.parseAgeMinutes("3d ago"))
    }

    @Test
    fun `unknown timestamps are treated as fresh not ancient`() {
        // Failing to parse must never silently bury content.
        assertEquals(0L, FomoScore.parseAgeMinutes("whenever"))
        assertEquals(0L, FomoScore.parseAgeMinutes(""))
    }

    // ---- momentum labels ---------------------------------------------------

    @Test
    fun `momentum labels match the spec's five states`() {
        assertEquals("Quiet", FomoScore.momentumLabel(0.1f))
        assertEquals("Active", FomoScore.momentumLabel(1f))
        assertEquals("Heating", FomoScore.momentumLabel(5f))
        assertEquals("Hot", FomoScore.momentumLabel(10f))
        assertEquals("Viral", FomoScore.momentumLabel(30f))
    }

    // ---- ordering ----------------------------------------------------------

    @Test
    fun `rank returns highest scoring first`() {
        val quiet = "quiet" to FomoScore.Signals(ageMinutes = 60 * 48)
        val hot = "hot" to FomoScore.Signals(velocityRipplesPerMin = 20f, ageMinutes = 5)
        assertEquals(listOf("hot", "quiet"), FomoScore.rank(listOf(quiet, hot), forYou))
    }
}
