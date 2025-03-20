package com.example.gigwork.di

import android.content.Context
import com.google.firebase.FirebaseApp
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GoogleServicesConfigModule {
    @Provides
    @Singleton
    fun provideFirebaseApp(@ApplicationContext context: Context): FirebaseApp {
        return try {
            FirebaseApp.initializeApp(context) ?: FirebaseApp.getInstance()
        } catch (e: Exception) {
            FirebaseApp.getInstance() ?: throw IllegalStateException("Firebase could not be initialized")
        }
    }
}