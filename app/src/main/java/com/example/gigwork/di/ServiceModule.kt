package com.example.gigwork.di

import com.example.gigwork.data.api.SupabaseClient
import com.example.gigwork.data.api.UserApi
import com.example.gigwork.data.api.UserApiImpl
import com.example.gigwork.data.auth.AuthService
import com.example.gigwork.data.auth.AuthServiceImpl
import com.example.gigwork.data.auth.PreferenceManager
import com.example.gigwork.util.Logger
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    @Provides
    @Singleton
    fun provideAuthService(
        firebaseAuth: FirebaseAuth,
        supabaseClient: SupabaseClient,
        preferenceManager: PreferenceManager,
        logger: Logger
    ): AuthService {
        return AuthServiceImpl(firebaseAuth, supabaseClient, preferenceManager, logger)
    }
    @Provides
    @Singleton
    fun provideUserApi(supabaseClient: SupabaseClient, authService: AuthService, logger: Logger): UserApi {
        return UserApiImpl(supabaseClient, authService, logger)
    }
}