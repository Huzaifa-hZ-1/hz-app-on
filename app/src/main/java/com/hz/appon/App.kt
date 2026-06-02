package com.hz.appon

import android.app.Application
import com.hz.appon.shared.AppContainer
import timber.log.Timber

/**
 * Application entry point.
 * Initialises Timber logging and the dependency container.
 */
class App : Application() {

    /** Dependency container — access via `(application as App).container` in Activities. */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        container = AppContainer(this)
        container.networkMonitor.register()
        Timber.d("App initialised")
    }
}
