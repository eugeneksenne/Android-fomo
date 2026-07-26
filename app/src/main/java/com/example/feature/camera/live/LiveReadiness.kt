package com.example.feature.camera.live

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

/**
 * Live Readiness Check.
 *
 * The spec requires eight checks before every broadcast: camera, microphone,
 * internet, GPS, venue detection, available storage, battery level and device
 * temperature.
 *
 * The previous screen listed five items hardcoded `to true` — including
 * "Local storage safe (24.2 GB / Est. 6h 15m)" and "Thermal temperature status
 * (Cool)" — so it displayed all-green on a device with a full disk, no network
 * or no permissions, and then failed at recording time.
 *
 * Every check here queries the real device.
 */
object LiveReadiness {

    /** Bitrate we assume for local 1080p recording, used for time estimates. */
    private const val RECORDING_BITRATE_BYTES_PER_SEC = 1_500_000L // ~12 Mbps

    /** Refuse to start a broadcast below this much free space. */
    private const val MIN_FREE_BYTES = 500L * 1024 * 1024 // 500 MB

    /** Warn below this battery percentage. */
    private const val LOW_BATTERY_PERCENT = 15

    /** Warn at or above this battery temperature (°C). */
    private const val HOT_BATTERY_CELSIUS = 42

    enum class Severity {
        /** Requirement met. */
        PASS,

        /** Broadcast can proceed but quality or duration may suffer. */
        WARN,

        /** Broadcast must not start. */
        BLOCK,
    }

    data class Check(
        val label: String,
        val severity: Severity,
        val detail: String,
    )

    data class Report(
        val checks: List<Check>,
        val estimatedRecordingSeconds: Long,
        val freeBytes: Long,
    ) {
        /** True when nothing is blocking. Warnings do not prevent going live. */
        val canGoLive: Boolean get() = checks.none { it.severity == Severity.BLOCK }

        val blockingReasons: List<Check> get() = checks.filter { it.severity == Severity.BLOCK }
    }

    /**
     * Runs all eight checks against the real device state.
     *
     * @param venueIdentified whether Venue Intelligence resolved a venue; the
     *   spec treats venue detection as a readiness item, but a missing venue
     *   only warns since a user may deliberately broadcast without one.
     */
    fun evaluate(context: Context, venueIdentified: Boolean): Report {
        val freeBytes = freeStorageBytes()
        val estimatedSeconds = freeBytes / RECORDING_BITRATE_BYTES_PER_SEC

        val checks = listOf(
            checkPermission(
                context,
                Manifest.permission.CAMERA,
                label = "Camera",
                grantedDetail = "Ready",
                deniedDetail = "Camera permission is required to broadcast",
                blockIfMissing = true
            ),
            checkPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
                label = "Microphone",
                grantedDetail = "Ready",
                deniedDetail = "Without mic access your broadcast will be silent",
                blockIfMissing = false
            ),
            checkInternet(context),
            checkLocation(context),
            checkVenue(venueIdentified),
            checkStorage(freeBytes, estimatedSeconds),
            checkBattery(context),
            checkTemperature(context),
        )

        return Report(
            checks = checks,
            estimatedRecordingSeconds = estimatedSeconds,
            freeBytes = freeBytes
        )
    }

    // ---- individual checks -------------------------------------------------

    private fun checkPermission(
        context: Context,
        permission: String,
        label: String,
        grantedDetail: String,
        deniedDetail: String,
        blockIfMissing: Boolean,
    ): Check {
        val granted = ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
        return Check(
            label = label,
            severity = when {
                granted -> Severity.PASS
                blockIfMissing -> Severity.BLOCK
                else -> Severity.WARN
            },
            detail = if (granted) grantedDetail else deniedDetail
        )
    }

    private fun checkInternet(context: Context): Check {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val caps = cm?.activeNetwork?.let { cm.getNetworkCapabilities(it) }

        val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val unmetered = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

        return Check(
            label = "Internet",
            severity = when {
                !hasInternet -> Severity.WARN // local recording still works
                else -> Severity.PASS
            },
            detail = when {
                !hasInternet ->
                    "Offline — your Live will record locally and upload later"
                isWifi || unmetered -> "Connected (Wi-Fi)"
                else -> "Connected (mobile data — may use your allowance)"
            }
        )
    }

    private fun checkLocation(context: Context): Check {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return Check(
            label = "GPS",
            severity = if (fine || coarse) Severity.PASS else Severity.WARN,
            detail = when {
                fine -> "Precise location available"
                coarse -> "Approximate location only"
                else -> "Location off — your Live won't be anchored to a venue"
            }
        )
    }

    private fun checkVenue(identified: Boolean) = Check(
        label = "Venue detection",
        severity = if (identified) Severity.PASS else Severity.WARN,
        detail = if (identified) "Venue identified" else "No venue detected — you can pick one manually"
    )

    private fun checkStorage(freeBytes: Long, estimatedSeconds: Long): Check {
        val gb = freeBytes / 1_000_000_000.0
        return Check(
            label = "Storage",
            severity = when {
                freeBytes < MIN_FREE_BYTES -> Severity.BLOCK
                estimatedSeconds < 600 -> Severity.WARN // under 10 minutes
                else -> Severity.PASS
            },
            detail = when {
                freeBytes < MIN_FREE_BYTES ->
                    "Not enough free space to record (${format(gb)} free)"
                else ->
                    "${format(gb)} free • about ${formatDuration(estimatedSeconds)} of recording"
            }
        )
    }

    private fun checkBattery(context: Context): Check {
        val percent = batteryPercent(context)
        val charging = isCharging(context)
        return Check(
            label = "Battery",
            severity = when {
                percent < 0 -> Severity.PASS // unknown; don't block
                charging -> Severity.PASS
                percent <= 5 -> Severity.BLOCK
                percent < LOW_BATTERY_PERCENT -> Severity.WARN
                else -> Severity.PASS
            },
            detail = when {
                percent < 0 -> "Battery level unavailable"
                charging -> "$percent% (charging)"
                percent <= 5 -> "$percent% — too low to broadcast safely"
                percent < LOW_BATTERY_PERCENT -> "$percent% — consider plugging in"
                else -> "$percent%"
            }
        )
    }

    private fun checkTemperature(context: Context): Check {
        val celsius = batteryTemperatureCelsius(context)
        return Check(
            label = "Device temperature",
            severity = when {
                celsius == null -> Severity.PASS
                celsius >= HOT_BATTERY_CELSIUS + 6 -> Severity.BLOCK
                celsius >= HOT_BATTERY_CELSIUS -> Severity.WARN
                else -> Severity.PASS
            },
            detail = when {
                celsius == null -> "Temperature unavailable"
                celsius >= HOT_BATTERY_CELSIUS + 6 -> "$celsius°C — device too hot to record"
                celsius >= HOT_BATTERY_CELSIUS -> "$celsius°C — running warm, quality may drop"
                else -> "$celsius°C (normal)"
            }
        )
    }

    // ---- device queries ----------------------------------------------------

    private fun freeStorageBytes(): Long = try {
        val stat = StatFs(Environment.getDataDirectory().path)
        stat.availableBlocksLong * stat.blockSizeLong
    } catch (e: Exception) {
        0L
    }

    private fun batteryStatus(context: Context): Intent? =
        try {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (e: Exception) {
            null
        }

    internal fun batteryPercent(context: Context): Int {
        val intent = batteryStatus(context) ?: return -1
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return -1
        return ((level / scale.toFloat()) * 100).roundToInt()
    }

    internal fun isCharging(context: Context): Boolean {
        val intent = batteryStatus(context) ?: return false
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }

    /** Battery temperature is reported in tenths of a degree Celsius. */
    internal fun batteryTemperatureCelsius(context: Context): Int? {
        val intent = batteryStatus(context) ?: return null
        val tenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        if (tenths == Int.MIN_VALUE) return null
        return (tenths / 10.0).roundToInt()
    }

    // ---- formatting --------------------------------------------------------

    internal fun format(gb: Double): String =
        if (gb >= 10) "${gb.roundToInt()} GB" else String.format("%.1f GB", gb)

    internal fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }
}
