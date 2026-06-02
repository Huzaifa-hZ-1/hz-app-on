package com.hz.appon

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.hz.appon.home.HomeActivity
import com.hz.appon.onboarding.OnboardingActivity
import timber.log.Timber

/**
 * Entry point of the app. Routes to Onboarding on first launch, Home on subsequent launches.
 *
 * In Android, an Activity whose intent-filter has ACTION_MAIN + CATEGORY_LAUNCHER is the
 * process entry point. Splash does its work in onCreate and immediately finishes so it
 * never appears in the back stack.
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = (application as App).container.userPreferences

        val destination = if (prefs.hasCompletedOnboarding) {
            Timber.d("Onboarding done — routing to Home")
            Intent(this, HomeActivity::class.java)
        } else {
            Timber.d("First launch — routing to Onboarding")
            Intent(this, OnboardingActivity::class.java)
        }

        startActivity(destination)
        // finish() removes Splash from the back stack — pressing Back from Home exits the app
        finish()
    }
}
