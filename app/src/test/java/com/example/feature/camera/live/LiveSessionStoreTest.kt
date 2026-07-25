package com.example.feature.camera.live

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for local-first Live session tracking and crash recovery.
 *
 * The spec promises "Users never lose their broadcast". Before this existed,
 * ending a Live only flipped a boolean, so a process death mid-broadcast left
 * the recording orphaned with no record it had happened.
 */
@RunWith(RobolectricTestRunner::class)
class LiveSessionStoreTest {

    private lateinit var store: LiveSessionStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        // Clear persisted state between tests.
        context.getSharedPreferences("fomo_live_sessions", android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()
        store = LiveSessionStore.getInstance(context)
        store.sessions.value.forEach { store.delete(it.id) }
    }

    @Test
    fun `a started session is immediately recorded as in-progress`() {
        store.startSession("s1", "Truth Nightclub")

        val session = store.sessions.value.single()
        assertEquals(LiveSessionStore.Status.RECORDING, session.status)
        assertTrue("an in-flight session must be flagged unfinished", session.isUnfinished)
    }

    @Test
    fun `an interrupted session is offered for recovery`() {
        store.startSession("s1", "Truth Nightclub")

        // Simulates relaunching after a crash: the session is still RECORDING.
        val recoverable = store.findRecoverable()
        assertNotNull("a crashed session must be recoverable", recoverable)
        assertEquals("s1", recoverable!!.id)
    }

    @Test
    fun `a cleanly ended session is not treated as a crash`() {
        store.startSession("s1", "Truth Nightclub")
        store.markEnded("s1", localUri = "content://media/1", peakViewers = 0)

        assertNull(store.findRecoverable())
        assertEquals(LiveSessionStore.Status.QUEUED, store.sessions.value.single().status)
    }

    @Test
    fun `recovery moves an interrupted session into the upload queue`() {
        store.startSession("s1", "Truth Nightclub")
        store.recover("s1", localUri = "content://media/1")

        val session = store.sessions.value.single()
        assertEquals(LiveSessionStore.Status.QUEUED, session.status)
        assertEquals("content://media/1", session.localUri)
        assertNull(store.findRecoverable())
    }

    @Test
    fun `recovery without a file is marked failed rather than silently queued`() {
        store.startSession("s1", "Truth Nightclub")
        store.recover("s1", localUri = null)

        val session = store.sessions.value.single()
        assertEquals(LiveSessionStore.Status.FAILED, session.status)
        assertNotNull(session.failureReason)
    }

    @Test
    fun `a recording that produces no file is a failure not a success`() {
        store.startSession("s1", "Truth Nightclub")
        store.markEnded("s1", localUri = null, peakViewers = 0)

        assertEquals(LiveSessionStore.Status.FAILED, store.sessions.value.single().status)
    }

    @Test
    fun `full publish lifecycle transitions correctly`() {
        store.startSession("s1", "Truth Nightclub")
        store.markEnded("s1", "content://media/1", peakViewers = 0)
        store.markUploading("s1")
        assertEquals(LiveSessionStore.Status.UPLOADING, store.sessions.value.single().status)

        store.markPublished("s1", "https://cdn.example/replay.mp4")
        val session = store.sessions.value.single()
        assertEquals(LiveSessionStore.Status.PUBLISHED, session.status)
        assertEquals("https://cdn.example/replay.mp4", session.remoteUrl)
    }

    @Test
    fun `sessions survive a process restart`() {
        store.startSession("s1", "Truth Nightclub")
        store.markEnded("s1", "content://media/1", peakViewers = 0)

        // A brand new instance reads back from disk.
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val reloaded = LiveSessionStore.getInstance(context)
        assertEquals(1, reloaded.sessions.value.size)
        assertEquals("content://media/1", reloaded.sessions.value.single().localUri)
    }

    @Test
    fun `deleting removes the session entirely`() {
        store.startSession("s1", "Truth Nightclub")
        store.delete("s1")
        assertTrue(store.sessions.value.isEmpty())
        assertNull(store.findRecoverable())
    }

    @Test
    fun `sessions are ordered newest first`() {
        store.startSession("old", "Venue A")
        Thread.sleep(5)
        store.startSession("new", "Venue B")

        assertEquals("new", store.sessions.value.first().id)
    }
}
