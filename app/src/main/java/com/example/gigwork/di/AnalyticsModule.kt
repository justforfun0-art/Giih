package com.example.gigwork.di

import com.example.gigwork.util.analytics.Analytics
import com.example.gigwork.util.analytics.DebugAnalyticsImpl
import com.example.gigwork.util.analytics.FirebaseAnalyticsImpl
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Binds
    @Singleton
    abstract fun bindAnalytics(impl: FirebaseAnalyticsImpl): Analytics

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseAnalytics(app: android.app.Application): FirebaseAnalytics {
            return FirebaseAnalytics.getInstance(app)
        }
    }
}