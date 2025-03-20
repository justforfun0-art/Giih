package com.example.gigwork.di

import android.content.Context
import androidx.room.Room
import com.example.gigwork.data.database.AppDatabase
import com.example.gigwork.data.database.ApplicationDao
import com.example.gigwork.data.database.BookmarkDao
import com.example.gigwork.data.database.FavoriteLocationDao
import com.example.gigwork.data.database.JobDraftDao
import com.example.gigwork.data.database.LocationDao
import com.example.gigwork.data.database.RatingDao
import com.example.gigwork.data.database.StatisticsDao
import com.example.gigwork.data.database.UserDao
import com.example.gigwork.data.database.dao.JobDao
import com.example.gigwork.data.repository.BookmarkRepositoryImpl
import com.example.gigwork.data.repository.EmployerRatingRepositoryImpl
import com.example.gigwork.data.repository.FavoriteLocationRepositoryImpl
import com.example.gigwork.data.repository.FileRepositoryImpl
import com.example.gigwork.data.repository.JobApplicationRepositoryImpl
import com.example.gigwork.data.repository.JobDraftRepositoryImpl
import com.example.gigwork.domain.repository.UserRepository
import com.example.gigwork.domain.repository.FileRepository
import com.example.gigwork.data.repository.JobStatisticsRepositoryImpl
import com.example.gigwork.data.repository.LocationManagerImpl
import com.example.gigwork.data.repository.LocationRepositoryImpl
import com.example.gigwork.data.repository.UserLocationRepositoryImpl
import com.example.gigwork.domain.repository.BookmarkRepository
import com.example.gigwork.domain.repository.EmployerRatingRepository
import com.example.gigwork.domain.repository.FavoriteLocationRepository
import com.example.gigwork.domain.repository.JobApplicationRepository
import com.example.gigwork.domain.repository.JobRepository
import com.example.gigwork.domain.repository.JobStatisticsRepository
import com.example.gigwork.domain.repository.LocationManager
import com.example.gigwork.domain.repository.LocationRepository
import com.example.gigwork.domain.repository.UserLocationRepository
import com.example.gigwork.data.repository.JobRepositoryImpl
import com.example.gigwork.data.repository.UserRepositoryImpl
import com.example.gigwork.domain.repository.JobDraftRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBookmarkRepository(impl: BookmarkRepositoryImpl): BookmarkRepository

    @Binds
    @Singleton
    abstract fun bindEmployerRatingRepository(impl: EmployerRatingRepositoryImpl): EmployerRatingRepository

    @Binds
    @Singleton
    abstract fun bindFavoriteLocationRepository(impl: FavoriteLocationRepositoryImpl): FavoriteLocationRepository

    @Binds
    @Singleton
    abstract fun bindJobApplicationRepository(impl: JobApplicationRepositoryImpl): JobApplicationRepository

    @Binds
    @Singleton
    abstract fun bindJobStatisticsRepository(impl: JobStatisticsRepositoryImpl): JobStatisticsRepository

    @Binds
    @Singleton
    abstract fun bindLocationManager(impl: LocationManagerImpl): LocationManager

    @Binds
    @Singleton
    abstract fun bindUserLocationRepository(impl: UserLocationRepositoryImpl): UserLocationRepository

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

    // REMOVED AuthRepository binding since it's already provided in AuthModule

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindFileRepository(impl: FileRepositoryImpl): FileRepository

    @Binds
    @Singleton
    abstract fun bindJobDraftRepository(impl: JobDraftRepositoryImpl): JobDraftRepository
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
    fun provideJobDao(db: AppDatabase): JobDao {
        return db.jobDao()
    }

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao {
        return db.userDao()
    }
    @Provides
    fun provideBookmarkDao(db: AppDatabase): BookmarkDao {
        return db.bookmarkDao()
    }

    @Provides
    fun provideLocationDao(db: AppDatabase): LocationDao {
        return db.locationDao()
    }

    @Provides
    fun provideApplicationDao(db: AppDatabase): ApplicationDao {
        return db.applicationDao()
    }

    @Provides
    fun provideRatingDao(db: AppDatabase): RatingDao {
        return db.ratingDao()
    }

    @Provides
    fun provideStatisticsDao(db: AppDatabase): StatisticsDao {
        return db.statisticsDao()
    }

    @Provides
    fun provideFavoriteLocationDao(db: AppDatabase): FavoriteLocationDao {
        return db.favoriteLocationDao()
    }

    @Provides
    fun provideJobDraftDao(db: AppDatabase): JobDraftDao {
        return db.jobDraftDao()
    }
}