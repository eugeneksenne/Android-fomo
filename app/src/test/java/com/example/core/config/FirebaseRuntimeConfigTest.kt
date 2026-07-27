package com.example.core.config

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseRuntimeConfigTest {
    @Test
    fun usableFirebaseConfigRejectsBlankAndPlaceholderValues() {
        assertFalse(FirebaseRuntimeConfig.hasUsableFirebaseConfig("", "app", "project"))
        assertFalse(FirebaseRuntimeConfig.hasUsableFirebaseConfig("your_api_key", "app", "project"))
        assertFalse(FirebaseRuntimeConfig.hasUsableFirebaseConfig("api", "default", "project"))
        assertFalse(FirebaseRuntimeConfig.hasUsableFirebaseConfig("api", "app", "change_me_project"))
    }

    @Test
    fun usableFirebaseConfigAcceptsConcreteValues() {
        assertTrue(
            FirebaseRuntimeConfig.hasUsableFirebaseConfig(
                apiKey = "AIzaSyExamplePublicClientKey",
                applicationId = "1:123456789:android:abcdef",
                projectId = "findlyts-prod"
            )
        )
    }

    @Test
    fun oauthClientIdValidationRejectsMissingDefaults() {
        assertFalse(FirebaseRuntimeConfig.hasUsableOAuthClientId(null))
        assertFalse(FirebaseRuntimeConfig.hasUsableOAuthClientId("default_web_client_id"))
        assertFalse(FirebaseRuntimeConfig.hasUsableOAuthClientId("placeholder-client"))
        assertTrue(FirebaseRuntimeConfig.hasUsableOAuthClientId("123-example.apps.googleusercontent.com"))
    }
}
