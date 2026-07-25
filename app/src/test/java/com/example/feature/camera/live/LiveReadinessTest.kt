package com.example.feature.camera.live

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for the Live readiness gate.
 *
 * The previous dialog hardcoded every row `to true`, so it reported
 * "Local storage safe (24.2 GB)" and "Thermal status (Cool)" on any device in
 * any condition, then failed at record time.
 */
@RunWith(RobolectricTestRunner::class)
class LiveReadinessTest {

    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun `evaluates all eight checks required by the spec`() {
        val report = LiveReadiness.evaluate(context, venueIdentified = true)
        val labels = report.checks.map { it.label }
        assertEquals(8, report.checks.size)
        listOf(
            "Camera", "Microphone", "Internet", "GPS",
            "Venue detection", "Storage", "Battery", "Device temperature"
        ).forEach { expected ->
            assertTrue("missing required check: $expected", labels.contains(expected))
        }
    }

    @Test
    fun `missing camera permission blocks the broadcast`() {
        // Robolectric denies permissions by default.
        val report = LiveReadiness.evaluate(context, venueIdentified = true)
        val camera = report.checks.single { it.label == "Camera" }
        assertEquals(LiveReadiness.Severity.BLOCK, camera.severity)
        assertFalse("must not be able to go live without a camera", report.canGoLive)
        assertTrue(report.blockingReasons.isNotEmpty())
    }

    @Test
    fun `missing microphone warns but does not block`() {
        val report = LiveReadiness.evaluate(context, venueIdentified = true)
        val mic = report.checks.single { it.label == "Microphone" }
        assertEquals(
            "a silent broadcast is degraded, not impossible",
            LiveReadiness.Severity.WARN,
            mic.severity
        )
    }

    @Test
    fun `an undetected venue warns but does not block`() {
        val report = LiveReadiness.evaluate(context, venueIdentified = false)
        val venue = report.checks.single { it.label == "Venue detection" }
        assertEquals(LiveReadiness.Severity.WARN, venue.severity)
    }

    @Test
    fun `an identified venue passes`() {
        val report = LiveReadiness.evaluate(context, venueIdentified = true)
        assertEquals(
            LiveReadiness.Severity.PASS,
            report.checks.single { it.label == "Venue detection" }.severity
        )
    }

    @Test
    fun `offline only warns because recording is local-first`() {
        val report = LiveReadiness.evaluate(context, venueIdentified = true)
        val internet = report.checks.single { it.label == "Internet" }
        assertTrue(
            "local-first recording means no network must never block",
            internet.severity != LiveReadiness.Severity.BLOCK
        )
    }

    @Test
    fun `storage estimate is reported and non-negative`() {
        val report = LiveReadiness.evaluate(context, venueIdentified = true)
        assertTrue(report.freeBytes >= 0)
        assertTrue(report.estimatedRecordingSeconds >= 0)
    }

    @Test
    fun `duration formatting is human readable`() {
        assertEquals("45s", LiveReadiness.formatDuration(45))
        assertEquals("5m", LiveReadiness.formatDuration(300))
        assertEquals("1h 30m", LiveReadiness.formatDuration(5400))
        assertEquals("6h 12m", LiveReadiness.formatDuration(22_320))
    }

    @Test
    fun `storage formatting switches precision sensibly`() {
        assertEquals("1.5 GB", LiveReadiness.format(1.5))
        assertEquals("22 GB", LiveReadiness.format(21.8))
    }

    @Test
    fun `every check carries an explanatory detail`() {
        val report = LiveReadiness.evaluate(context, venueIdentified = false)
        report.checks.forEach { check ->
            assertTrue(
                "check '${check.label}' must explain itself to the user",
                check.detail.isNotBlank()
            )
        }
    }
}
