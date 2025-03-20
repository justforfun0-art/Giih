package com.example.gigwork.di

import com.example.gigwork.core.error.handler.GlobalErrorHandler
import com.example.gigwork.core.error.model.ErrorMessage
import com.example.gigwork.presentation.base.AppError
import com.example.gigwork.presentation.base.ErrorHandler
import com.example.gigwork.presentation.base.Logger
import com.example.gigwork.util.LoggerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ErrorHandlingModule {

    @Provides
    @Singleton
    @ErrorHandlerQualifier
    fun provideQualifiedErrorHandler(errorHandler: GlobalErrorHandler): ErrorHandler {
        return object : ErrorHandler {
            override fun handle(error: AppError): ErrorMessage {
                return errorHandler.handle(error)
            }
        }
    }
    @Provides
    @Singleton
    fun provideErrorHandler(errorHandler: GlobalErrorHandler): ErrorHandler {
        return object : ErrorHandler {
            override fun handle(error: AppError): ErrorMessage {
                return errorHandler.handle(error)
            }
        }
    }

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