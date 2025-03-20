package com.example.gigwork.di

import android.content.Context
import com.example.gigwork.data.api.SupabaseClient
import com.example.gigwork.data.auth.AuthBridgeService
import com.example.gigwork.data.auth.AuthService
import com.example.gigwork.data.auth.AuthServiceImpl
import com.example.gigwork.data.auth.HybridSessionManager
import com.example.gigwork.data.auth.PreferenceManager
import com.example.gigwork.data.repository.AuthRepositoryImpl
import com.example.gigwork.data.security.EncryptedPreferences
import com.example.gigwork.domain.repository.AuthRepository
import com.example.gigwork.presentation.NavigationScope
import com.example.gigwork.presentation.navigation.NavigationCommands
import com.example.gigwork.presentation.navigation.NavigationManager
import com.example.gigwork.presentation.navigation.auth.AuthNavigationManager
import com.example.gigwork.util.Logger
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideAuthBridgeService(
        supabaseClient: SupabaseClient,
        preferenceManager: PreferenceManager
    ): AuthBridgeService {
        return AuthBridgeService(supabaseClient.client, preferenceManager)
    }
    @Provides
    @Singleton
    fun providePreferenceManager(
        @ApplicationContext context: Context
    ): PreferenceManager {
        return PreferenceManager(context)
    }

    @Provides
    @Singleton
    fun provideHybridSessionManager(
        supabaseClient: SupabaseClient,
        preferenceManager: PreferenceManager
    ): HybridSessionManager {
        return HybridSessionManager(supabaseClient.client, preferenceManager)
    }
    @Provides
    @Singleton
    fun provideAuthRepository(
        encryptedPreferences: EncryptedPreferences,
        firebaseAuth: FirebaseAuth,
        supabaseClient: SupabaseClient,
        authBridgeService: AuthBridgeService,
        logger: Logger
    ): AuthRepository {
        return AuthRepositoryImpl(
            encryptedPreferences,
            firebaseAuth,
            supabaseClient,
            authBridgeService,
            logger
        )
    }
    @Provides
    @Singleton
    fun provideAuthNavigationManager(
        navigationManager: NavigationManager,
        navigationCommands: NavigationCommands,
        logger: Logger,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @NavigationScope navigationScope: CoroutineScope
    ): AuthNavigationManager {
        return AuthNavigationManager(
            navigationManager = navigationManager,
            navigationCommands = navigationCommands,
            logger = logger,
            ioDispatcher = ioDispatcher,
            navigationScope = navigationScope
        )
    }
/*
    @Provides
    @Singleton
    fun provideAuthService(
        firebaseAuth: FirebaseAuth,
        supabaseClient: SupabaseClient,
        preferenceManager: PreferenceManager,
        logger: Logger
    ): AuthService {
        return AuthServiceImpl(
            auth = firebaseAuth,
            supabaseClient = supabaseClient,
            preferenceManager = preferenceManager,
            logger = logger
        )
    }*/
}