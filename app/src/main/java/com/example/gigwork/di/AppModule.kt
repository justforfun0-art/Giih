// app/src/main/java/com/example/gigwork/di/AppModule.kt
package com.example.gigwork.di

import android.content.Context
import android.net.ConnectivityManager
import androidx.room.Room
import com.example.gigwork.data.api.SupabaseClient
import com.example.gigwork.data.config.ConfigProvider
import com.example.gigwork.data.config.ConfigProviderImpl
import com.example.gigwork.data.database.AppDatabase
import com.example.gigwork.data.database.dao.JobDao
import com.example.gigwork.data.security.EncryptedPreferences
import com.example.gigwork.util.Logger
import com.example.gigwork.util.LoggerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

   /* @Provides
    @Singleton
    fun provideConfigProvider(@ApplicationContext context: Context): ConfigProvider {
        return ConfigProviderImpl(context)
    }

   @Provides
   @Singleton
   @ApplicationContext
   fun provideContext(@ApplicationContext context: Context): Context {
       return context
   }

    */
   @Provides
   fun provideConnectivityManager(@ApplicationContext context: Context): ConnectivityManager {
       return context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
   }
    @Provides
    @Singleton
    fun provideSupabaseClient(configProvider: ConfigProvider): SupabaseClient {
        return SupabaseClient(configProvider)
    }
    /*
        @Singleton
        @Provides
        fun provideEncryptedPreferences(@ApplicationContext context: Context): EncryptedPreferences {
            return EncryptedPreferences(context)
        }


        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "gigwork_database"
            ).build()
        }

        @Provides
        @Singleton
        fun provideJobDao(database: AppDatabase): JobDao {
            return database.jobDao()
        }*/

    @Provides
    @Singleton
    fun provideLogger(): Logger {
        return LoggerImpl()
    }
}