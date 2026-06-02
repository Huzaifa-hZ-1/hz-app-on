package com.hz.appon

import android.app.Application
import timber.log.Timber

/**
 * Application entry point.
 *
 * Initialises global tooling (Timber logging) once at process start.
 * Debug builds get a full logging tree; release builds are silent.
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            // Timber.DebugTree prints class name, line number, and thread automatically.
            // Removed in release builds — no logs leak to production.
            Timber.plant(Timber.DebugTree())
        }
    }
}
