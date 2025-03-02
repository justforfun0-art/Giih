package com.example.gigwork.di

import android.content.Context
import com.example.gigwork.data.api.SupabaseClient
import com.example.gigwork.data.database.AppDatabase
import com.example.gigwork.data.database.dao.JobDao
import com.example.gigwork.util.Logger
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.mockk
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AppModule::class]
)
object TestAppModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(@ApplicationContext context: Context): SupabaseClient {
        return mockk(relaxed = true)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return mockk(relaxed = true)
    }

    @Provides
    @Singleton
    fun provideJobDao(database: AppDatabase): JobDao {
        return mockk(relaxed = true)
    }

    @Provides
    @Singleton
    fun provideLogger(): Logger {
        return mockk(relaxed = true)
    }
}