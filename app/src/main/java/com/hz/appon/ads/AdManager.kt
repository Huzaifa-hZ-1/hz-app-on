package com.hz.appon.ads

import android.app.Activity
import android.content.Context
import android.widget.FrameLayout
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import timber.log.Timber

/**
 * Central manager for all AdMob ad types: banner, interstitial, and rewarded.
 *
 * Uses Google's public test ad unit IDs — replace with real IDs from admob.google.com
 * before publishing. Real IDs belong in a build config or remote config, not hardcoded here.
 *
 * Android note: AdMob initialisation is asynchronous. Ads should be pre-loaded
 * before they're needed — done here in [init] and [preloadInterstitial].
 */
class AdManager(context: Context) {

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    init {
        MobileAds.initialize(context) {
            Timber.d("AdMob initialised")
            preloadInterstitial(context)
            preloadRewarded(context)
        }
    }

    /**
     * Inflates a banner ad into [container].
     * Call from Activity.onCreate — the banner remains for the Activity's lifetime.
     */
    fun loadBanner(container: FrameLayout) {
        val adView = AdView(container.context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = TEST_BANNER_ID
        }
        container.addView(adView)
        adView.loadAd(AdRequest.Builder().build())
        Timber.d("Banner ad loaded")
    }

    /**
     * Shows an interstitial ad if one is loaded. Pre-loads the next one after display.
     * Safe to call even if no ad is ready — it silently skips.
     */
    fun showInterstitial(activity: Activity) {
        val ad = interstitialAd
        if (ad == null) {
            Timber.w("Interstitial not ready — skipping")
            preloadInterstitial(activity)
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                preloadInterstitial(activity)
            }
        }
        ad.show(activity)
        Timber.d("Interstitial shown")
    }

    /**
     * Shows a rewarded ad. Calls [onRewarded] only if the user earns the reward.
     * Safe to call if no ad is ready — it silently skips and pre-loads.
     */
    fun showRewarded(activity: Activity, onRewarded: () -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            Timber.w("Rewarded ad not ready — skipping")
            preloadRewarded(activity)
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                preloadRewarded(activity)
            }
        }
        ad.show(activity) { _ ->
            Timber.d("Rewarded ad — reward earned")
            onRewarded()
        }
    }

    private fun preloadInterstitial(context: Context) {
        InterstitialAd.load(context, TEST_INTERSTITIAL_ID, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Timber.d("Interstitial pre-loaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Timber.w("Interstitial failed to load: ${error.message}")
                }
            })
    }

    private fun preloadRewarded(context: Context) {
        RewardedAd.load(context, TEST_REWARDED_ID, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Timber.d("Rewarded ad pre-loaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Timber.w("Rewarded ad failed to load: ${error.message}")
                }
            })
    }

    companion object {
        // Google's public test IDs — safe to use during development, never earn real revenue
        private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
        private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
        private const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
    }
}
