package com.sudokuscout

import android.app.Application
import android.content.Context

class SudokuScoutApplication : Application() {
    
    override fun attachBaseContext(base: Context) {
        // Use default context to avoid SharedPreferences access during Application creation
        super.attachBaseContext(base)
    }
    
    override fun onCreate() {
        super.onCreate()
        // Initialize any global settings here if needed
    }
}