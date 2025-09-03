package com.sudokuscout

import android.app.Application
import android.content.Context

class SudokuScoutApplication : Application() {
    
    override fun attachBaseContext(base: Context) {
        try {
            // Apply locale setting during application startup
            super.attachBaseContext(LocaleHelper.attachBaseContext(base))
        } catch (e: Exception) {
            // Fall back to default if there's any issue with SharedPreferences
            super.attachBaseContext(base)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        // Initialize any global settings here if needed
    }
}