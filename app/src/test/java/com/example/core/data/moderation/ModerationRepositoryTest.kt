package com.example.core.data.moderation

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for the UGC moderation controls.
 *
 * These exist because Google Play's User Generated Content policy requires an
 * in-app way to report content and block users. The app previously had neither,
 * which is a standard cause of Play rejection for social apps.
 */
@RunWith(RobolectricTestRunner::class)
class ModerationRepositoryTest {

    private lateinit var repo: ModerationRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("fomo_moderation", Context.MODE_PRIVATE)
            .edit().clear().commit()
        repo = ModerationRepository.getInstance(context)
        // The singleton survives between tests; reset its in-memory state.
        repo.state.value.blockedUsers.forEach { repo.unblockUser(it) }
        repo.state.value.hiddenMoments.forEach { repo.unhideMoment(it) }
    }

    // ---- blocking ----------------------------------------------------------

    @Test
    fun `a blocked user's content is not visible`() {
        repo.blockUser("SpamAccount")
        assertTrue(repo.isBlocked("SpamAccount"))
        assertFalse(repo.isVisible(momentId = "m1", authorId = "SpamAccount"))
    }

    @Test
    fun `blocking is case insensitive`() {
        // Legacy seed content keys on display name, so casing must not matter.
        repo.blockUser("SpamAccount")
        assertTrue(repo.isBlocked("spamaccount"))
        assertTrue(repo.isBlocked("SPAMACCOUNT"))
    }

    @Test
    fun `blocking one user does not affect others`() {
        repo.blockUser("SpamAccount")
        assertFalse(repo.isBlocked("Sarah"))
        assertTrue(repo.isVisible(momentId = "m2", authorId = "Sarah"))
    }

    @Test
    fun `unblocking restores visibility`() {
        repo.blockUser("SpamAccount")
        repo.unblockUser("SpamAccount")
        assertFalse(repo.isBlocked("SpamAccount"))
        assertTrue(repo.isVisible(momentId = "m1", authorId = "SpamAccount"))
    }

    @Test
    fun `blank identifiers are ignored`() {
        repo.blockUser("")
        assertTrue(repo.state.value.blockedUsers.isEmpty())
    }

    // ---- hiding ------------------------------------------------------------

    @Test
    fun `hiding removes a single post without blocking its author`() {
        repo.hideMoment("m1")
        assertFalse(repo.isVisible(momentId = "m1", authorId = "Sarah"))
        // Their other posts remain visible.
        assertTrue(repo.isVisible(momentId = "m2", authorId = "Sarah"))
        assertFalse(repo.isBlocked("Sarah"))
    }

    @Test
    fun `unhiding restores a post`() {
        repo.hideMoment("m1")
        repo.unhideMoment("m1")
        assertTrue(repo.isVisible(momentId = "m1", authorId = "Sarah"))
    }

    // ---- reporting ---------------------------------------------------------

    @Test
    fun `reporting hides the post immediately`() {
        // Play expects reported content to disappear for the reporter without
        // waiting for a moderation decision.
        repo.reportMoment(
            momentId = "m1",
            authorId = "Sarah",
            reason = ModerationRepository.ReportReason.SPAM,
        )
        assertTrue(repo.isReported("m1"))
        assertFalse(repo.isVisible(momentId = "m1", authorId = "Sarah"))
    }

    @Test
    fun `reporting alone does not block the author`() {
        repo.reportMoment(
            momentId = "m1",
            authorId = "Sarah",
            reason = ModerationRepository.ReportReason.SPAM,
        )
        assertFalse(repo.isBlocked("Sarah"))
    }

    @Test
    fun `reporting can block the author when requested`() {
        repo.reportMoment(
            momentId = "m1",
            authorId = "Harasser",
            reason = ModerationRepository.ReportReason.HARASSMENT,
            alsoBlockAuthor = true,
        )
        assertTrue(repo.isBlocked("Harasser"))
    }

    @Test
    fun `the report taxonomy covers the categories Play expects`() {
        val labels = ModerationRepository.ReportReason.entries.map { it.label }
        listOf("Spam", "Harassment", "Hate speech", "Violence", "Nudity", "self-harm")
            .forEach { needle ->
                assertTrue(
                    "missing a report category matching '$needle'",
                    labels.any { it.contains(needle, ignoreCase = true) }
                )
            }
    }

    @Test
    fun `state persists across instances`() {
        repo.blockUser("SpamAccount")
        repo.hideMoment("m9")

        val context = ApplicationProvider.getApplicationContext<Context>()
        val reloaded = ModerationRepository.getInstance(context)
        assertTrue(reloaded.isBlocked("SpamAccount"))
        assertTrue(reloaded.isHidden("m9"))
    }
}
