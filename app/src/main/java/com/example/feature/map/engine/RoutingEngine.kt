package com.example.feature.map.engine

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLngPoint(val latitude: Double, val longitude: Double)

data class RouteStep(
    val instruction: String,
    val distanceKm: Double
)

data class Route(
    val destinationName: String,
    val totalDistanceKm: Double,
    val estimatedMinutes: Int,
    val steps: List<RouteStep>,
    val waypoints: List<LatLngPoint>
)

class RoutingEngine {

    fun calculateRoute(
        startLat: Double,
        startLng: Double,
        destination: FomoVenue
    ): Route {
        val distKm = calculateDistanceKm(startLat, startLng, destination.latitude, destination.longitude)
        val mins = (distKm * 2.5 + 2).toInt().coerceAtLeast(3)

        val waypoints = listOf(
            LatLngPoint(startLat, startLng),
            LatLngPoint(startLat + (destination.latitude - startLat) * 0.4, startLng + (destination.longitude - startLng) * 0.3),
            LatLngPoint(startLat + (destination.latitude - startLat) * 0.75, startLng + (destination.longitude - startLng) * 0.8),
            LatLngPoint(destination.latitude, destination.longitude)
        )

        val steps = listOf(
            RouteStep("Head towards ${destination.address ?: "venue area"} along OSM main road", distKm * 0.4),
            RouteStep("Turn right towards ${destination.name} entrance", distKm * 0.5),
            RouteStep("Arrive at ${destination.name}", distKm * 0.1)
        )

        return Route(
            destinationName = destination.name,
            totalDistanceKm = distKm,
            estimatedMinutes = mins,
            steps = steps,
            waypoints = waypoints
        )
    }

    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Radius of the earth in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
