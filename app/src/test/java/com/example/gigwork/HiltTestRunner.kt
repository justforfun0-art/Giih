package com.example.gigwork

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        loader: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(loader, HiltTestApplication::class.java.name, context)
    }
}