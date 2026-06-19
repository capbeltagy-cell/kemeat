package com.example.ui

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdManager {
    private const val TAG = "KhemetAdManager"

    // Set TEST_MODE to false to use real AdMob production ads
    var TEST_MODE = false

    // ==========================================
    // Real AdMob Production IDs
    // ==========================================
    private const val PROD_BANNER_ID = "ca-app-pub-4693639798724853/7700814042"
    private const val PROD_INTERSTITIAL_ID = "ca-app-pub-4693639798724853/9126566030"
    private const val PROD_REWARDED_ID = "ca-app-pub-4693639798724853/7568686177"

    // ==========================================
    // Official Google Test Ad Unit IDs
    // ==========================================
    private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

    // getters for active units based on current mode
    val bannerAdUnitId: String
        get() = if (TEST_MODE) TEST_BANNER_ID else PROD_BANNER_ID

    val interstitialAdUnitId: String
        get() = if (TEST_MODE) TEST_INTERSTITIAL_ID else PROD_INTERSTITIAL_ID

    val rewardedAdUnitId: String
        get() = if (TEST_MODE) TEST_REWARDED_ID else PROD_REWARDED_ID

    // Ad Instances
    private var mInterstitialAd: InterstitialAd? = null
    private var mRewardedAd: RewardedAd? = null

    // Loading/No-Fill States
    var isInterstitialLoading = false
    var isRewardedLoading = false

    // Retry counts for exponential backoff logic (no-fill retry safe limits)
    private var interstitialRetryCount = 0
    private var rewardedRetryCount = 0

    /**
     * Pre-loads an Interstitial Ad asynchronously.
     */
    fun loadInterstitial(context: Context) {
        if (mInterstitialAd != null || isInterstitialLoading) return
        isInterstitialLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            interstitialAdUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "Interstitial Ad successfully loaded.")
                    mInterstitialAd = interstitialAd
                    isInterstitialLoading = false
                    interstitialRetryCount = 0 // reset
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Interstitial failed to load: ${loadAdError.message}")
                    mInterstitialAd = null
                    isInterstitialLoading = false
                    
                    // Exponential backoff retry (max sleep 60s)
                    interstitialRetryCount++
                    val retryDelay = (2 * interstitialRetryCount).coerceAtMost(30) * 1000L
                    Log.d(TAG, "Retrying loading Interstitial in ${retryDelay / 1000}s")
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        loadInterstitial(context)
                    }, retryDelay)
                }
            }
        )
    }

    /**
     * Shows loaded Interstitial Ad.
     */
    fun showInterstitial(activity: Activity, onAdClosed: () -> Unit) {
        val ad = mInterstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad was dismissed.")
                    mInterstitialAd = null
                    loadInterstitial(activity) // pre-fetch next
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "Interstitial failed to show: ${adError.message}")
                    mInterstitialAd = null
                    onAdClosed() // do not block user
                }
            }
            ad.show(activity)
        } else {
            Log.d(TAG, "Interstitial ad not ready.")
            loadInterstitial(activity) // try loading
            onAdClosed() // trigger callback so gameplay flow continues immediately
        }
    }

    /**
     * Pre-loads a Rewarded Ad asynchronously.
     */
    fun loadRewarded(context: Context) {
        if (mRewardedAd != null || isRewardedLoading) return
        isRewardedLoading = true

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            rewardedAdUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    Log.d(TAG, "Rewarded Ad successfully loaded.")
                    mRewardedAd = rewardedAd
                    isRewardedLoading = false
                    rewardedRetryCount = 0
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Rewarded failed to load: ${loadAdError.message}")
                    mRewardedAd = null
                    isRewardedLoading = false

                    // Simple retry backoff
                    rewardedRetryCount++
                    val retryDelay = (2 * rewardedRetryCount).coerceAtMost(30) * 1000L
                    Log.d(TAG, "Retrying loading Rewarded in ${retryDelay / 1000}s")
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        loadRewarded(context)
                    }, retryDelay)
                }
            }
        )
    }

    /**
     * Shows loaded Rewarded Ad and invokes callbacks.
     */
    fun showRewarded(
        activity: Activity,
        onRewardEarned: (amount: Int) -> Unit,
        onAdClosed: () -> Unit,
        onAdFailed: (String) -> Unit
    ) {
        val ad = mRewardedAd
        if (ad != null) {
            var awardTriggered = false
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Rewarded ad was dismissed.")
                    mRewardedAd = null
                    loadRewarded(activity) // reload next
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "Rewarded failed to show: ${adError.message}")
                    mRewardedAd = null
                    onAdFailed(adError.message)
                }
            }
            
            // Show with explicit reward callback
            ad.show(activity) { rewardItem ->
                Log.d(TAG, "onUserEarnedReward: verified success! Reward amount: ${rewardItem.amount}")
                awardTriggered = true
                onRewardEarned(rewardItem.amount)
            }
        } else {
            Log.d(TAG, "Rewarded ad not filled or loaded.")
            loadRewarded(activity)
            onAdFailed("Ad not loaded yet. Sandbox loading...")
        }
    }

    /**
     * Helper to verify if Rewarded Ad is ready.
     */
    fun isRewardedAdReady(): Boolean {
        return mRewardedAd != null
    }

    /**
     * Helper to verify if Interstitial Ad is ready.
     */
    fun isInterstitialAdReady(): Boolean {
        return mInterstitialAd != null
    }
}
