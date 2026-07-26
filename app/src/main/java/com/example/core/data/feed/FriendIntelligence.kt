package com.example.core.data.feed

import com.example.core.data.MyCircleRepository

/**
 * Friend Intelligence line shown in the Moment metadata stack.
 *
 * Spec examples:
 *   👥 8 friends rippled this
 *   🔥 Your circle is pulling up
 *   ✨ 3 friends routed here tonight
 *
 * and, importantly, *"Displayed only when relevant."*
 *
 * Previously `friendActivityText` was a fixed string baked into each seed
 * Moment ("👥 8 friends rippled this"), shown identically to every user
 * regardless of whether they had any friends at all — a fabricated social
 * signal, the same class of problem as the invented live viewer counts.
 *
 * This derives the line from the user's actual circle, and returns null when
 * there is nothing genuine to say.
 */
object FriendIntelligence {

    /** How many of the user's circle are currently at this venue. */
    fun friendsAtVenue(venueName: String): List<String> {
        if (venueName.isBlank()) return emptyList()
        return runCatching {
            MyCircleRepository.friendsState.value
                .filter { it.venueName?.equals(venueName, ignoreCase = true) == true }
                .map { it.name }
        }.getOrDefault(emptyList())
    }

    /**
     * Number of the user's friends who engaged with this Moment.
     *
     * Engagement is attributed by matching commenters against the circle.
     * A real backend would carry explicit engagement records; until then this
     * only counts what can actually be observed, rather than inventing a
     * number.
     */
    fun friendsEngaged(moment: Moment): Int {
        val circle = runCatching {
            MyCircleRepository.friendsState.value.map { it.name.lowercase() }.toSet()
        }.getOrDefault(emptySet())
        if (circle.isEmpty()) return 0

        return moment.comments
            .map { it.author.lowercase() }
            .filter { it in circle }
            .distinct()
            .size
    }

    /**
     * Builds the display line, or null when nothing relevant applies.
     *
     * Ordering is deliberate: presence at the venue is the most actionable
     * signal (it can pull someone out tonight), then engagement.
     */
    fun lineFor(moment: Moment): String? {
        val atVenue = friendsAtVenue(moment.locationName)
        if (atVenue.isNotEmpty()) {
            return when (atVenue.size) {
                1 -> "✨ ${atVenue.first()} is here tonight"
                2 -> "✨ ${atVenue[0]} and ${atVenue[1]} are here tonight"
                else -> "🔥 Your circle is pulling up • ${atVenue.size} here"
            }
        }

        val engaged = friendsEngaged(moment)
        if (engaged > 0) {
            return if (engaged == 1) "👥 1 friend rippled this"
            else "👥 $engaged friends rippled this"
        }

        // Nothing genuine to report - the spec says show nothing.
        return null
    }
}
