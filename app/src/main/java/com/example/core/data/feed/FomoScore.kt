package com.example.core.data.feed

import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow

/**
 * The FOMO Score — the Feed's ranking engine.
 *
 * Spec: *"Every Moment receives a live FOMO Score"* from Ripple velocity, watch
 * completion, likes, comments, shares, saves, friend activity, venue
 * popularity, distance, recency and trust score — and, critically,
 * *"Momentum always outweighs historical popularity."*
 *
 * None of this existed. The tabs applied a boolean filter and rendered whatever
 * order the list happened to be in, so a viral Moment from ten minutes ago sat
 * below a stale one with more lifetime likes.
 *
 * Design notes:
 *  - **Momentum dominates.** Velocity carries the largest weight and recency
 *    applies an exponential decay, so a fast-rising new Moment outranks an old
 *    popular one. That is the single most important property here.
 *  - Raw counts are compressed with `ln(1 + x)`. Without it a Moment with
 *    100k likes would swamp every other signal and the feed would ossify around
 *    a handful of posts.
 *  - Each tab reweights the same signals rather than using a different
 *    algorithm, matching the spec's "same Moment component, only the ranking
 *    changes".
 *  - Pure functions with no Android dependencies, so ranking is unit-testable.
 */
object FomoScore {

    /** Half-life of freshness, in hours. */
    private const val RECENCY_HALF_LIFE_HOURS = 6.0

    /** Distance beyond which proximity contributes nothing. */
    private const val MAX_RELEVANT_METRES = 25_000.0

    /** Signal weights per tab. They need not sum to 1; scores are relative. */
    data class Weights(
        val velocity: Float,
        val watchCompletion: Float,
        val engagement: Float,
        val friendActivity: Float,
        val proximity: Float,
        val recency: Float,
        val liveViewers: Float,
        val trust: Float,
    ) {
        companion object {
            /** Balanced, momentum-led personalised ranking. */
            val FOR_YOU = Weights(
                velocity = 3.0f, watchCompletion = 2.0f, engagement = 1.2f,
                friendActivity = 1.5f, proximity = 0.8f, recency = 2.5f,
                liveViewers = 0.5f, trust = 0.6f,
            )

            /** Spec: Following is "sorted by newest", so recency dominates. */
            val FOLLOWING = Weights(
                velocity = 0.5f, watchCompletion = 0.3f, engagement = 0.3f,
                friendActivity = 0.4f, proximity = 0.2f, recency = 8.0f,
                liveViewers = 0.2f, trust = 0.2f,
            )

            /** Proximity-led. */
            val NEARBY = Weights(
                velocity = 1.2f, watchCompletion = 0.8f, engagement = 0.6f,
                friendActivity = 1.0f, proximity = 6.0f, recency = 2.0f,
                liveViewers = 0.4f, trust = 0.4f,
            )

            /** Spec: live viewers, ripple velocity, friend activity, distance. */
            val LIVE = Weights(
                velocity = 2.0f, watchCompletion = 0.2f, engagement = 0.5f,
                friendActivity = 1.5f, proximity = 1.5f, recency = 1.0f,
                liveViewers = 4.0f, trust = 0.5f,
            )

            fun forTab(tab: String): Weights = when (tab) {
                "Following" -> FOLLOWING
                "Nearby" -> NEARBY
                "Live" -> LIVE
                else -> FOR_YOU
            }
        }
    }

    /** Everything the scorer needs, decoupled from the UI model. */
    data class Signals(
        val velocityRipplesPerMin: Float = 0f,
        val watchCompletion: Float = 0f,
        val likes: Int = 0,
        val comments: Int = 0,
        val shares: Int = 0,
        val saves: Int = 0,
        val ripples: Int = 0,
        val friendsEngaged: Int = 0,
        val distanceMetres: Double? = null,
        val ageMinutes: Long = 0,
        val liveViewers: Int = 0,
        val isVerifiedAuthor: Boolean = false,
        val isFollowing: Boolean = false,
        val isLiveNow: Boolean = false,
    )

    /**
     * Computes the score. Higher ranks first.
     *
     * @return a non-negative score; 0 for content with no signal at all.
     */
    fun score(signals: Signals, weights: Weights): Float {
        val velocity = compress(signals.velocityRipplesPerMin.toDouble())
        val watch = signals.watchCompletion.coerceIn(0f, 1f).toDouble()

        // Engagement is a weighted blend: a save or share signals far stronger
        // intent than a like, so they count for more.
        val engagement = compress(
            signals.likes * 1.0 +
                signals.comments * 2.0 +
                signals.saves * 3.0 +
                signals.shares * 4.0 +
                signals.ripples * 2.5
        )

        val friends = compress(signals.friendsEngaged.toDouble() * 2.0)
        val proximity = proximityScore(signals.distanceMetres)
        val recency = recencyScore(signals.ageMinutes)
        val live = if (signals.isLiveNow) compress(signals.liveViewers.toDouble()) else 0.0
        val trust = trustScore(signals)

        return (
            weights.velocity * velocity +
                weights.watchCompletion * watch +
                weights.engagement * engagement +
                weights.friendActivity * friends +
                weights.proximity * proximity +
                weights.recency * recency +
                weights.liveViewers * live +
                weights.trust * trust
            ).toFloat().coerceAtLeast(0f)
    }

    /**
     * Exponential freshness decay. At one half-life the contribution is 0.5, at
     * two 0.25, and so on — this is what makes momentum beat lifetime totals.
     */
    internal fun recencyScore(ageMinutes: Long): Double {
        if (ageMinutes <= 0) return 1.0
        val hours = ageMinutes / 60.0
        return 0.5.pow(hours / RECENCY_HALF_LIFE_HOURS)
    }

    /** 1.0 at the venue, decaying linearly to 0 at [MAX_RELEVANT_METRES]. */
    internal fun proximityScore(distanceMetres: Double?): Double {
        if (distanceMetres == null) return 0.0
        if (distanceMetres <= 0) return 1.0
        if (distanceMetres >= MAX_RELEVANT_METRES) return 0.0
        return 1.0 - (distanceMetres / MAX_RELEVANT_METRES)
    }

    /** Logarithmic compression so large counts cannot dominate. */
    internal fun compress(value: Double): Double =
        if (value <= 0) 0.0 else ln(1.0 + value)

    /** Small, bounded credibility bonus. */
    internal fun trustScore(signals: Signals): Double {
        var t = 0.0
        if (signals.isVerifiedAuthor) t += 0.6
        if (signals.isFollowing) t += 0.4
        return t
    }

    /**
     * Parses the human distance strings the models carry ("280m away",
     * "1.4km away", "Right here") into metres.
     *
     * Returns null when no distance is expressed, so proximity contributes
     * nothing rather than being wrongly treated as zero metres (which would
     * rank unknown-distance content as if it were right next to the user).
     */
    fun parseDistanceMetres(text: String): Double? {
        val t = text.trim().lowercase()
        if (t.isEmpty()) return null
        if (t.contains("right here") || t.contains("here now")) return 0.0

        val match = Regex("([0-9]+(?:[.,][0-9]+)?)\\s*(km|m)\\b").find(t) ?: return null
        val value = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        return when (match.groupValues[2]) {
            "km" -> value * 1000.0
            else -> value
        }
    }

    /**
     * Parses relative timestamps ("Just now", "5m ago", "2h ago", "3d ago")
     * into minutes. Unknown formats are treated as fresh rather than ancient,
     * so a parsing gap never silently buries content.
     */
    fun parseAgeMinutes(timeAgo: String): Long {
        val t = timeAgo.trim().lowercase()
        if (t.isEmpty() || t.contains("just now") || t.contains("now")) return 0

        val match = Regex("([0-9]+)\\s*(s|m|h|d|w)").find(t) ?: return 0
        val value = match.groupValues[1].toLongOrNull() ?: return 0
        return when (match.groupValues[2]) {
            "s" -> 0
            "m" -> value
            "h" -> value * 60
            "d" -> value * 60 * 24
            "w" -> value * 60 * 24 * 7
            else -> 0
        }
    }

    /** Maps velocity onto the spec's momentum labels. */
    fun momentumLabel(velocityRipplesPerMin: Float): String = when {
        velocityRipplesPerMin >= 20f -> "Viral"
        velocityRipplesPerMin >= 8f -> "Hot"
        velocityRipplesPerMin >= 3f -> "Heating"
        velocityRipplesPerMin >= 0.5f -> "Active"
        else -> "Quiet"
    }

    /** Convenience: rank a list of pre-built signal pairs. */
    fun <T> rank(items: List<Pair<T, Signals>>, weights: Weights): List<T> =
        items.sortedByDescending { score(it.second, weights) }.map { it.first }

    /** Highest score first; used by the repository. */
    fun bestFirst(a: Float, b: Float): Int = b.compareTo(a)

    /** Exposed so callers can show why something ranked highly. */
    fun explain(signals: Signals, weights: Weights): Map<String, Float> = mapOf(
        "velocity" to (weights.velocity * compress(signals.velocityRipplesPerMin.toDouble())).toFloat(),
        "watch" to (weights.watchCompletion * signals.watchCompletion).toFloat(),
        "recency" to (weights.recency * recencyScore(signals.ageMinutes)).toFloat(),
        "proximity" to (weights.proximity * proximityScore(signals.distanceMetres)).toFloat(),
        "friends" to (weights.friendActivity * compress(signals.friendsEngaged * 2.0)).toFloat(),
        "live" to (
            weights.liveViewers *
                (if (signals.isLiveNow) compress(signals.liveViewers.toDouble()) else 0.0)
            ).toFloat(),
        "max" to max(1f, 1f),
    )
}
