package com.example.core.di

import com.example.core.data.realtime.FomoSocketManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFomoSocketManager(): FomoSocketManager {
        return FomoSocketManager.getInstance()
    }
}