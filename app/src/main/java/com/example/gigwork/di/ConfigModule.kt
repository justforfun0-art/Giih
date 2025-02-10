// di/ConfigModule.kt
package com.example.gigwork.di

import com.example.gigwork.data.config.ConfigProvider
import com.example.gigwork.data.config.ProductionConfigProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ConfigModule {
    @Binds
    @Singleton
    abstract fun bindConfigProvider(
        productionConfigProvider: ProductionConfigProvider
    ): ConfigProvider
}