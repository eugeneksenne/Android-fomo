package com.example

import android.app.Application
import com.example.core.config.FirebaseRuntimeConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FomoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseRuntimeConfig.ensureFirebaseApp(
                context = this,
                apiKey = BuildConfig.FIREBASE_API_KEY,
                applicationId = BuildConfig.FIREBASE_APPLICATION_ID,
                projectId = BuildConfig.FIREBASE_PROJECT_ID,
                storageBucket = BuildConfig.FIREBASE_STORAGE_BUCKET
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}