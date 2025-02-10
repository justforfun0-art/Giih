// di/RepositoryModule.kt
package com.example.gigwork.di

import com.example.gigwork.data.repository.JobRepositoryImpl
import com.example.gigwork.domain.repository.JobRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindJobRepository(
        impl: JobRepositoryImpl
    ): JobRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        impl: LocationRepositoryImpl
    ): LocationRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "gigwork.db"
    ).build()

    @Provides
    fun provideJobDao(db: AppDatabase): JobDao = db.jobDao()
}