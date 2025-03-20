// presentation/navigation/di/NavigationModule.kt
package com.example.gigwork.di

import com.example.gigwork.presentation.navigation.NavigationCommands
import com.example.gigwork.presentation.navigation.NavigationManager
import com.example.gigwork.presentation.NavigationScope
import com.example.gigwork.presentation.navigation.analytics.NavigationEventTracker
import com.example.gigwork.presentation.navigation.analytics.NavigationEventTrackerImpl
import com.example.gigwork.presentation.navigation.deeplink.DeepLinkHandler
import com.example.gigwork.presentation.navigation.menu.MenuInteractionHandler
import com.example.gigwork.presentation.navigation.permissions.NavigationPermissionManager
import com.example.gigwork.presentation.navigation.state.NavigationState
import com.example.gigwork.presentation.navigation.transitions.ScreenTransitionManager
import com.example.gigwork.util.Logger
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NavigationModule {

    @Provides
    @Singleton
    fun provideNavigationCommands(navigationState: NavigationState): NavigationCommands {
        return navigationState
    }

    @Provides
    @Singleton
    @NavigationScope
    fun provideNavigationScope(
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): CoroutineScope {
        return CoroutineScope(SupervisorJob() + ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideDeepLinkHandler(
        logger: Logger,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @NavigationScope navigationScope: CoroutineScope,
        navigationCommands: NavigationCommands
    ): DeepLinkHandler {
        return DeepLinkHandler(logger, ioDispatcher, navigationScope, navigationCommands)
    }

    @Provides
    @Singleton
    fun provideNavigationPermissionManager(
        logger: Logger,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @NavigationScope navigationScope: CoroutineScope
    ): NavigationPermissionManager {
        return NavigationPermissionManager(logger, ioDispatcher, navigationScope)
    }

    // First provide NavigationManager without MenuInteractionHandler to break circular dependency
    @Provides
    @Singleton
    fun provideNavigationManager(
        navigationState: NavigationState,
        transitionManager: ScreenTransitionManager,
        permissionManager: NavigationPermissionManager,
        eventTracker: NavigationEventTracker,
        logger: Logger,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): NavigationManager {
        return NavigationManager(
            navigationState,
            transitionManager,
            permissionManager,
            eventTracker,
            logger,
            ioDispatcher
        )
    }

    // Then provide MenuInteractionHandler with NavigationCommands instead of NavigationManager
    @Provides
    @Singleton
    fun provideMenuInteractionHandler(
        logger: Logger,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @NavigationScope navigationScope: CoroutineScope,
        navigationCommands: NavigationCommands
    ): MenuInteractionHandler {
        return MenuInteractionHandler(logger, ioDispatcher, navigationScope, navigationCommands)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NavigationBindingModule {
    @Binds
    @Singleton
    abstract fun bindNavigationEventTracker(
        impl: NavigationEventTrackerImpl
    ): NavigationEventTracker
}