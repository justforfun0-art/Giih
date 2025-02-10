// File: app/src/main/java/com/example/gigwork/di/security/SecurityModule.kt
package com.example.gigwork.di

import android.content.Context
import com.example.gigwork.data.security.EncryptedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideEncryptedPreferences(
        @ApplicationContext context: Context
    ): EncryptedPreferences = EncryptedPreferences(context)
}