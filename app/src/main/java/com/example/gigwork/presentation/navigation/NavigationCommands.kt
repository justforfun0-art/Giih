package com.example.gigwork.presentation.navigation

import androidx.navigation.NavOptionsBuilder

interface NavigationCommands {
    suspend fun navigateTo(
        route: String,
        options: NavOptionsBuilder.() -> Unit = {}
    )
}