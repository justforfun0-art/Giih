package com.example.gigwork.di

import android.content.Context
import android.util.Log
import com.example.gigwork.data.config.ConfigProvider
import com.example.gigwork.data.config.ConfigProviderImpl
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ConfigModule {

    @Binds
    @Singleton
    abstract fun bindConfigProvider(
        configProviderImpl: ConfigProviderImpl
    ): ConfigProvider

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseCrashlytics(): FirebaseCrashlytics {
            return FirebaseCrashlytics.getInstance()
        }

        @Provides
        @Singleton
        fun provideDebugMode(): Boolean {
            return try {
                Class.forName("com.example.gigwork.DebugInitializer")
                true
            } catch (e: ClassNotFoundException) {
                false
            }
        }

        @Provides
        @Singleton
        fun provideLoggerConfig(
            @ApplicationContext context: Context,
            isDebugMode: Boolean
        ): LoggerConfig {
            return LoggerConfig(
                isDebugMode = isDebugMode,
                packageName = context.packageName,
                maxTagLength = 23,
                maxLogLength = 4000,
                minLogLevel = if (isDebugMode) Log.VERBOSE else Log.INFO
            )
        }
    }
}

data class LoggerConfig(
    val isDebugMode: Boolean,
    val packageName: String,
    val maxTagLength: Int,
    val maxLogLength: Int,
    val minLogLevel: Int
)