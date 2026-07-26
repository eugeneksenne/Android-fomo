package com.example.core.data.media

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for background upload de-duplication.
 *
 * Uploads previously ran inline in a Composable's scope, so backgrounding the
 * app or losing connectivity abandoned the moment permanently. Routing through
 * WorkManager fixes that, but makes duplicate enqueues a real risk — hence the
 * stable unique-name contract covered here.
 */
@RunWith(RobolectricTestRunner::class)
class MediaUploadWorkerTest {

    @Test
    fun `the same live session always maps to one work name`() {
        val uri = Uri.parse("content://media/external/video/media/42")
        val a = MediaUploadWorker.uniqueNameFor("live_123", uri)
        val b = MediaUploadWorker.uniqueNameFor("live_123", uri)
        assertEquals("re-enqueueing must not create a second upload", a, b)
    }

    @Test
    fun `different sessions map to different work names`() {
        val uri = Uri.parse("content://media/external/video/media/42")
        assertNotEquals(
            MediaUploadWorker.uniqueNameFor("live_1", uri),
            MediaUploadWorker.uniqueNameFor("live_2", uri)
        )
    }

    @Test
    fun `without a session the uri identifies the work`() {
        val uri = Uri.parse("content://media/external/images/media/7")
        val a = MediaUploadWorker.uniqueNameFor(null, uri)
        val b = MediaUploadWorker.uniqueNameFor(null, uri)
        assertEquals("the same file must not upload twice", a, b)
    }

    @Test
    fun `different files map to different work names`() {
        assertNotEquals(
            MediaUploadWorker.uniqueNameFor(null, Uri.parse("content://media/1")),
            MediaUploadWorker.uniqueNameFor(null, Uri.parse("content://media/2"))
        )
    }

    @Test
    fun `a session id takes precedence over the uri`() {
        // The graded file's Uri changes after processing; keying on the session
        // keeps the retry pointed at one logical upload.
        val original = Uri.parse("content://media/original")
        val graded = Uri.parse("file:///cache/look_999.mp4")
        assertEquals(
            MediaUploadWorker.uniqueNameFor("live_9", original),
            MediaUploadWorker.uniqueNameFor("live_9", graded)
        )
    }

    @Test
    fun `work names are non blank and prefixed`() {
        val name = MediaUploadWorker.uniqueNameFor("live_1", Uri.parse("content://media/1"))
        assertTrue(name.isNotBlank())
        assertTrue(name.startsWith("upload_"))
    }

    @Test
    fun `every upload policy is representable`() {
        // Mirrors the spec's Upload Preferences.
        val policies = MediaUploadWorker.Companion.UploadPolicy.entries
        assertEquals(3, policies.size)
        assertTrue(policies.contains(MediaUploadWorker.Companion.UploadPolicy.IMMEDIATE))
        assertTrue(policies.contains(MediaUploadWorker.Companion.UploadPolicy.WIFI_ONLY))
        assertTrue(policies.contains(MediaUploadWorker.Companion.UploadPolicy.CHARGING_ONLY))
    }
}
