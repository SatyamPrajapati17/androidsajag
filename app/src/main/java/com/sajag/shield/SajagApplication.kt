package com.sajag.shield

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SajagApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i("SajagApplication", "🛡️ Sajag Mobile Shield initialized. Real-time scam interception active.")
    }
}
