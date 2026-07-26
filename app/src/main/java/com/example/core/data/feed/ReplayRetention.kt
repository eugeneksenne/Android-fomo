package com.example.core.data.feed

import java.util.concurrent.TimeUnit

/**
 * Replay retention policy.
 *
 * Spec:
 * ```
 * Temporary Lives              7 days
 * Standard Lives               30 days
 * High-Momentum Lives          90 days
 * Verified Venue/Creator       180 days
 * Pinned Replays               max 10 per creator, until manually removed
 * ```
 *
 * None of this existed — replays had no expiry concept at all, so storage would
 * have grown without bound and the tiering the spec describes was unenforced.
 *
 * Pure functions so the policy is testable and can be applied identically on
 * the client (for display) and by a server-side cleanup job (for deletion).
 */
object ReplayRetention {

    enum class Tier(val days: Int) {
        TEMPORARY(7),
        STANDARD(30),
        HIGH_MOMENTUM(90),
        VERIFIED(180),
        /** Pinned replays never expire on a timer. */
        PINNED(Int.MAX_VALUE),
    }

    /** Spec: "Maximum of 10 per creator". */
    const val MAX_PINNED_PER_CREATOR = 10

    /** Velocity at or above which a Live counts as high-momentum. */
    private const val HIGH_MOMENTUM_VELOCITY = 8f

    /** A Live shorter than this is treated as temporary. */
    private const val TEMPORARY_MAX_DURATION_MINUTES = 5

    /**
     * Resolves the retention tier.
     *
     * Order matters: pinning wins outright, then author verification, then
     * measured momentum, then duration. This means a creator can never lose a
     * replay they explicitly pinned, and a verified venue's content is retained
     * for the full window even if it was quiet.
     */
    fun tierFor(
        isPinned: Boolean,
        isVerifiedAuthor: Boolean,
        peakVelocity: Float,
        durationMinutes: Int,
    ): Tier = when {
        isPinned -> Tier.PINNED
        isVerifiedAuthor -> Tier.VERIFIED
        peakVelocity >= HIGH_MOMENTUM_VELOCITY -> Tier.HIGH_MOMENTUM
        durationMinutes < TEMPORARY_MAX_DURATION_MINUTES -> Tier.TEMPORARY
        else -> Tier.STANDARD
    }

    /** Absolute expiry, or null for pinned replays. */
    fun expiresAtMs(tier: Tier, publishedAtMs: Long): Long? =
        if (tier == Tier.PINNED) null
        else publishedAtMs + TimeUnit.DAYS.toMillis(tier.days.toLong())

    fun isExpired(
        tier: Tier,
        publishedAtMs: Long,
        nowMs: Long = System.currentTimeMillis(),
    ): Boolean {
        val expiry = expiresAtMs(tier, publishedAtMs) ?: return false
        return nowMs >= expiry
    }

    /** Whole days left, or null when the replay never expires. */
    fun daysRemaining(
        tier: Tier,
        publishedAtMs: Long,
        nowMs: Long = System.currentTimeMillis(),
    ): Int? {
        val expiry = expiresAtMs(tier, publishedAtMs) ?: return null
        val remaining = expiry - nowMs
        if (remaining <= 0) return 0
        return TimeUnit.MILLISECONDS.toDays(remaining).toInt()
    }

    /**
     * Whether another replay can be pinned.
     * Enforced client-side for immediate feedback; a backend must enforce it
     * too, since a client check alone is not a guarantee.
     */
    fun canPin(currentPinnedCount: Int): Boolean =
        currentPinnedCount < MAX_PINNED_PER_CREATOR

    /** Human-readable summary for the creator's replay library. */
    fun describe(
        tier: Tier,
        publishedAtMs: Long,
        nowMs: Long = System.currentTimeMillis(),
    ): String = when (tier) {
        Tier.PINNED -> "Pinned • kept until you remove it"
        else -> when (val days = daysRemaining(tier, publishedAtMs, nowMs)) {
            null -> "Kept indefinitely"
            0 -> "Expires today"
            1 -> "Expires tomorrow"
            else -> "Expires in $days days"
        }
    }
}
