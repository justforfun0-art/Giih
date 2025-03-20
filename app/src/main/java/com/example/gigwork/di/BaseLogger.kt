package com.example.gigwork.di

import com.example.gigwork.presentation.base.Logger
import com.example.gigwork.util.LoggerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

// Qualifier annotation for base logger
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseLogger

// Logger Module for dependency injection
@Module
@InstallIn(SingletonComponent::class)
object LoggerModule {

    @Provides
    @Singleton
    fun provideLoggerImpl(): LoggerImpl {
        return LoggerImpl()
    }

    @BaseLogger
    @Provides
    @Singleton
    fun provideBaseLogger(loggerImpl: LoggerImpl): Logger {
        return object : Logger {
            override fun debug(tag: String, message: String) {
                loggerImpl.d(tag, message, emptyMap())
            }

            override fun error(tag: String, message: String, throwable: Throwable?) {
                loggerImpl.e(tag, message, throwable, emptyMap())
            }
        }
    }
}

// Error Handler Qualifier
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ErrorHandlerQualifier