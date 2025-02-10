package com.example.gigwork.di

import com.example.gigwork.data.api.LocationApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        }

    @Provides
    @Singleton
    fun provideNetworkInterceptor(): Interceptor =
        NetworkInterceptor()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        networkInterceptor: Interceptor,
        @ApplicationContext context: Context
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(networkInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .cache(Cache(context.cacheDir, 10 * 1024 * 1024)) // 10MB cache
        .build()
}

    @Provides
    @Singleton
    fun provideLocationApiService(okHttpClient: OkHttpClient): LocationApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.postalpincode.in/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LocationApiService::class.java)
    }
