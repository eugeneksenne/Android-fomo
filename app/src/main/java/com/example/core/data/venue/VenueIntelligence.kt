package com.example.core.data.venue

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToInt

/**
 * Venue Intelligence Engine.
 *
 * Spec pipeline:
 *   GPS → Offline Venue Pack → Cloud Verification → Confidence Score → Venue Identified
 *
 * This replaces the previous hardcoded `"Truth Nightclub"` / `"Confidence: 99%"`
 * label, which displayed the same venue and the same fake confidence figure
 * regardless of where the user actually was.
 *
 * Offline-first: matching runs against the locally bundled venue pack, so the
 * camera identifies a venue with no network. Cloud verification is a later
 * refinement step and is intentionally not required for a result.
 */
object VenueIntelligence {

    private const val TAG = "VenueIntelligence"

    /** Beyond this distance we don't claim a venue match at all. */
    private const val MAX_MATCH_METRES = 400.0

    /** At or below this distance we're confident enough to auto-attach the venue. */
    private const val HIGH_CONFIDENCE_METRES = 60.0

    /** Below this score the UI must offer "Nearby / Search / Skip" per the spec. */
    const val LOW_CONFIDENCE_THRESHOLD = 0.65f

    /**
     * A venue candidate with a real, computed confidence score.
     *
     * @param confidence 0f..1f derived from GPS distance and accuracy — not a
     *   decorative constant.
     */
    data class VenueMatch(
        val venueId: String,
        val venueName: String,
        val distanceMetres: Double,
        val confidence: Float,
    ) {
        val confidencePercent: Int get() = (confidence * 100).roundToInt()
        val isHighConfidence: Boolean get() = confidence >= LOW_CONFIDENCE_THRESHOLD
    }

    sealed interface VenueState {
        /** Detection in progress. */
        data object Detecting : VenueState

        /** Location permission not granted — the user must pick manually. */
        data object PermissionDenied : VenueState

        /** No GPS fix available (indoors, airplane mode, disabled). */
        data object NoLocation : VenueState

        /** A confident match. */
        data class Identified(val match: VenueMatch) : VenueState

        /**
         * Matches exist but none are confident. Per the spec the UI shows
         * Nearby Venues / Search Venue / Skip.
         */
        data class LowConfidence(val candidates: List<VenueMatch>) : VenueState

        /** Nothing nearby at all. */
        data object NoVenueNearby : VenueState
    }

    /** Bundled offline venue pack: id, display name and coordinates. */
    private data class PackedVenue(
        val id: String,
        val name: String,
        val latitude: Double,
        val longitude: Double,
    )

    // Offline Venue Pack. Coordinates are the same set the Map screen uses.
    private val offlinePack = listOf(
        PackedVenue("fomo_club", "FOMO Club Rosebank", -26.1452, 28.0472),
        PackedVenue("d48_midrand", "D48 Midrand", -25.9981, 28.1263),
        PackedVenue("konka_soweto", "Konka Soweto", -26.2561, 27.8542),
        PackedVenue("taboo_sandton", "Taboo Sandton", -26.1044, 28.0581),
        PackedVenue("marble_rosebank", "Marble Rosebank", -26.1461, 28.0432),
        PackedVenue("proud_mary", "Proud Mary", -26.1445, 28.0454),
        PackedVenue("legend_barber", "Legends Barber", -26.1456, 28.0421),
        PackedVenue("sorbet_salon", "Sorbet Salon", -26.1072, 28.0524),
        PackedVenue("sanctuary_spa", "Sanctuary Spa", -26.1085, 28.0551),
        PackedVenue("four_seasons_westcliff", "Four Seasons Westcliff", -26.1643, 28.0285),
    )

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Runs the full detection pipeline. Safe to call on every camera open;
     * returns a state rather than throwing.
     */
    @SuppressLint("MissingPermission") // guarded by hasLocationPermission()
    suspend fun detect(context: Context): VenueState {
        if (!hasLocationPermission(context)) return VenueState.PermissionDenied

        val location = try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .setMaxUpdateAgeMillis(60_000L)
                .build()
            client.getCurrentLocation(request, null).await()
        } catch (e: Exception) {
            Log.w(TAG, "Unable to obtain a location fix", e)
            null
        } ?: return VenueState.NoLocation

        return matchAgainstPack(location)
    }

    /** Pure matching step — separated so it can be unit tested without GPS. */
    internal fun matchAgainstPack(location: Location): VenueState {
        val candidates = offlinePack
            .map { venue ->
                val results = FloatArray(1)
                Location.distanceBetween(
                    location.latitude, location.longitude,
                    venue.latitude, venue.longitude,
                    results
                )
                val metres = results[0].toDouble()
                VenueMatch(
                    venueId = venue.id,
                    venueName = venue.name,
                    distanceMetres = metres,
                    confidence = confidenceFor(metres, location.accuracy)
                )
            }
            .filter { it.distanceMetres <= MAX_MATCH_METRES }
            .sortedByDescending { it.confidence }

        if (candidates.isEmpty()) return VenueState.NoVenueNearby

        val best = candidates.first()
        return if (best.isHighConfidence) {
            VenueState.Identified(best)
        } else {
            VenueState.LowConfidence(candidates.take(5))
        }
    }

    /**
     * Confidence combines proximity with GPS accuracy.
     *
     * A fix reported as accurate to ±100 m cannot justify a high-confidence
     * claim even if the nearest venue happens to be 10 m away, so accuracy
     * damps the score.
     */
    internal fun confidenceFor(distanceMetres: Double, accuracyMetres: Float): Float {
        val proximity = when {
            distanceMetres <= HIGH_CONFIDENCE_METRES -> 1.0
            distanceMetres >= MAX_MATCH_METRES -> 0.0
            else -> 1.0 - ((distanceMetres - HIGH_CONFIDENCE_METRES) /
                (MAX_MATCH_METRES - HIGH_CONFIDENCE_METRES))
        }

        val accuracyFactor = when {
            accuracyMetres <= 0f -> 0.8          // unknown accuracy
            accuracyMetres <= 20f -> 1.0
            accuracyMetres >= 150f -> 0.4
            else -> 1.0 - ((accuracyMetres - 20f) / 130f) * 0.6
        }

        return (proximity * accuracyFactor).coerceIn(0.0, 1.0).toFloat()
    }
}
