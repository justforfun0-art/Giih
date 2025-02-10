// GigWorkApplication.kt
package com.example.gigwork

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GigWorkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize any application-level dependencies here
    }
}