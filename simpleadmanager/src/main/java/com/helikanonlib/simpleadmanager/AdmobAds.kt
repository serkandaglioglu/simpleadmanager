package com.helikanonlib.simpleadmanager

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowMetrics
import android.widget.RelativeLayout
import com.google.android.gms.ads.*
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdmobAds(override var appId: String) : AdPlatformWrapper(appId) {

    override var platformType = AdPlatformTypeEnum.ADMOB
    var adIntances: MutableMap<String, PlatformAdInstance?> = mutableMapOf()

    override fun initialize(context: Context, onInitializeComplete: ((Boolean) -> Unit)?, testMode: Boolean) {
        if (!beginInitialization(onInitializeComplete)) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                MobileAds.initialize(context, object : OnInitializationCompleteListener {
                    override fun onInitializationComplete(initializationStatus: InitializationStatus) {

                        if (BuildConfig.DEBUG) {
                            for ((adapterClass, status) in initializationStatus.adapterStatusMap) {
                                Log.d(
                                    "AdmobAds",
                                    "Adapter: $adapterClass, Status: ${status.description}, Latency: ${status.latency}ms",
                                )
                            }
                        }
                        completeInitialization()
                    }
                })
            } catch (error: Exception) {
                failInitialization()
            }
        }

        if (testMode) {
            enableTestMode(context, null)
        }

    }

    override fun enableTestMode(context: Context, deviceId: String?) {

        /*for (group in placementGroups) {
            group.interstitial = "ca-app-pub-3940256099942544/1033173712"
            group.banner = "ca-app-pub-3940256099942544/6300978111"
            group.rewarded = "ca-app-pub-3940256099942544/5224354917"
            group.mrec = "ca-app-pub-3940256099942544/6300978111"
            group.native = "ca-app-pub-3940256099942544/2247696110"
            group.appOpenAd = "ca-app-pub-3940256099942544/3419835294"
        }*/

    }


    // ################################################################################
    // ################################### INTERSTITIAL ###############################
    // ################################################################################
    override fun isInterstitialLoaded(placementId: String): Boolean {

        val interstitial: InterstitialAd? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as InterstitialAd? else null

        var isLoaded = interstitial != null
        if (isLoaded && !isValidLoadedInterstitial(platformType)) {
            adIntances[placementId] = null
            isLoaded = false
        }

        return isLoaded
    }


    override fun loadInterstitial(activity: Activity, placementId: String, listener: AdPlatformLoadListener?) {
        if (isInterstitialLoaded(placementId)) {
            listener?.onLoaded(platformType)
            return
        }

        adIntances[placementId] = null

        InterstitialAd.load(
            activity, placementId, AdRequest.Builder()
                .build(), object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    adIntances[placementId] = PlatformAdInstance(
                        AdFormatEnum.INTERSTITIAL,
                        placementId,
                        interstitialAd
                    )

                    updateLastLoadInterstitialDateByAdPlatform(platformType)

                    listener?.onLoaded(platformType)
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    adIntances[placementId] = null
                    listener?.onPlatformError(platformError(AdFormatEnum.INTERSTITIAL, placementId, "${platformType.name} interstitial >> error code=${adError.code} / ${adError.message}"))
                }
            })
    }

    override fun showInterstitial(activity: Activity, placementId: String, shownWhere: String, listener: AdPlatformShowListener?) {
        if (!isInterstitialLoaded(placementId)) {
            listener?.onPlatformError(platformError(AdFormatEnum.INTERSTITIAL, placementId, "${platformType.name} interstitial >> noads loaded"))
            return
        }

        val interstitial: InterstitialAd? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as InterstitialAd? else null

        interstitial?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                adIntances[placementId] = null
                listener?.onPlatformError(platformError(AdFormatEnum.INTERSTITIAL, placementId, "${platformType.name} interstitial show >> error code=${adError.code} / ${adError.message}"))
            }

            override fun onAdShowedFullScreenContent() {
                listener?.onDisplayed(platformType)
            }

            override fun onAdDismissedFullScreenContent() {
                adIntances[placementId] = null
                listener?.onClosed(platformType)
            }

        }

        adIntances[placementId] = null
        interstitial?.show(activity)

    }

    // ################################################################################
    // ################################### REWARDED ###################################
    // ################################################################################
    override fun isRewardedLoaded(placementId: String): Boolean {
        val rewardedAd: RewardedAd? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as RewardedAd? else null

        var isLoaded = rewardedAd != null
        if (isLoaded && !isValidLoadedRewarded(platformType)) {
            adIntances[placementId] = null
            isLoaded = false
        }

        return isLoaded
    }


    override fun loadRewarded(activity: Activity, placementId: String, listener: AdPlatformLoadListener?) {
        if (isRewardedLoaded(placementId)) {
            listener?.onLoaded(platformType)
            return
        }

        adIntances[placementId] = null

        RewardedAd.load(
            activity, placementId, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(p0: RewardedAd) {
                    adIntances[placementId] = PlatformAdInstance(
                        AdFormatEnum.REWARDED,
                        placementId,
                        p0
                    )

                    updateLastLoadRewardedDateByAdPlatform(platformType)

                    listener?.onLoaded(platformType)
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    adIntances[placementId] = null
                    listener?.onPlatformError(platformError(AdFormatEnum.REWARDED, placementId, "${platformType.name} rewarded load >> error code=${adError.code} / ${adError.message}"))
                }
            })

    }

    override fun showRewarded(activity: Activity, placementId: String, shownWhere: String, listener: AdPlatformShowListener?) {
        if (!isRewardedLoaded(placementId)) {
            listener?.onPlatformError(platformError(AdFormatEnum.REWARDED, placementId, "${platformType.name} rewarded >> noadsloaded"))
            return
        }

        val rewardedAd: RewardedAd? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as RewardedAd? else null

        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                listener?.onPlatformError(platformError(AdFormatEnum.REWARDED, placementId, "${platformType.name} rewarded show >> error code=${adError.code} / ${adError.message}"))
                adIntances[placementId] = null
            }

            override fun onAdShowedFullScreenContent() {
                listener?.onDisplayed(platformType)
            }

            override fun onAdDismissedFullScreenContent() {
                adIntances[placementId] = null
                listener?.onClosed(platformType)
            }

        }

        adIntances[placementId] = null
        rewardedAd?.show(activity, OnUserEarnedRewardListener { rewardItem ->
            listener?.onRewarded(rewardItem.type, rewardItem.amount, platformType)
        })
    }

    // ################################################################################
    // ################################### BANNER #####################################
    // ################################################################################
    override fun isBannerLoaded(placementId: String): Boolean {
        val bannerAdView: AdView? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as AdView? else null

        var isLoaded = _isBannerLoaded(bannerAdView)
        /*if (isLoaded && !isValidLoadedBanner(platformType)) {
            _removeBannerViewIfExists(bannerAdView)
            adIntances[placementId] = null
            isLoaded = false
        }*/

        return isLoaded
    }

    override fun showBanner(activity: Activity, placementId: String, containerView: RelativeLayout, listener: AdPlatformShowListener?) {

        var bannerAdView: AdView? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as AdView? else null

        val lp = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            .apply {
                addRule(RelativeLayout.CENTER_HORIZONTAL)
            }

        if (isBannerLoaded(placementId)) {
            try {
                _removeBannerViewIfExists(bannerAdView)
                containerView.addView(bannerAdView, lp)
                listener?.onDisplayed(platformType)
            } catch (e: Exception) {
                listener?.onPlatformError(platformError(AdFormatEnum.BANNER, placementId, "${platformType.name} banner >> isbannerloaded"))
            }
            return
        }

        bannerAdView = AdView(activity.applicationContext)
        // bannerAdView?.adSize = AdSize.SMART_BANNER

        bannerAdView.setAdSize(getBannerAdaptiveSize(activity, containerView))
        bannerAdView.adUnitId = placementId
        bannerAdView.adListener = object : AdListener() {
            override fun onAdFailedToLoad(error: LoadAdError) {
                super.onAdFailedToLoad(error)

                if (!isBannerLoaded(placementId)){

                    adIntances[placementId] = null
                    activity.runOnUiThread {
                        _removeBannerViewIfExists(bannerAdView, containerView)
                        bannerAdView.destroy() // a failed view is never reused
                    }

                    listener?.onPlatformError(platformError(AdFormatEnum.BANNER, placementId, "${platformType.name} banner >> error code=${error.code} / ${error.message}"))
                }
            }

            override fun onAdLoaded() {
                super.onAdLoaded()

                adIntances[placementId] = PlatformAdInstance(
                    AdFormatEnum.BANNER,
                    placementId,
                    bannerAdView
                )
            }

            override fun onAdImpression() {
                super.onAdImpression()

                listener?.onDisplayed(platformType)
            }

            override fun onAdClicked() {
                listener?.onClicked(platformType)
            }
        }
        _removeBannerViewIfExists(bannerAdView, containerView)
        containerView.addView(bannerAdView, lp)

        bannerAdView?.loadAd(AdRequest.Builder().build())
    }

    fun getBannerAdaptiveSize(activity: Activity, containerView: RelativeLayout): AdSize {
        val displayMetrics = activity.resources.displayMetrics
        val adWidthPixels =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowMetrics: WindowMetrics = activity.windowManager.currentWindowMetrics
                windowMetrics.bounds.width()
            } else {
                displayMetrics.widthPixels
            }
        val density = displayMetrics.density

        val adWidth = (adWidthPixels / density).toInt()

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }


    // ################################################################################
    // ################################### MREC #######################################
    // ################################################################################
    override fun isMrecLoaded(placementId: String): Boolean {
        val mrecAdView: AdView? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as AdView? else null

        var isLoaded = _isBannerLoaded(mrecAdView)
        /*if (isLoaded && !isValidLoadedMrec(platformType)) {
            _removeBannerViewIfExists(mrecAdView)
            adIntances[placementId] = null
            isLoaded = false
        }*/

        return isLoaded
    }

    override fun showMrec(activity: Activity, placementId: String, containerView: RelativeLayout, listener: AdPlatformShowListener?) {

        var mrecAdView: AdView? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as AdView? else null

        val lp = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            .apply {
                addRule(RelativeLayout.CENTER_HORIZONTAL)
            }

        if (isMrecLoaded(placementId)) {
            try {
                _removeBannerViewIfExists(mrecAdView)
                containerView.addView(mrecAdView, lp)
                listener?.onDisplayed(platformType)
            } catch (e: Exception) {
                listener?.onPlatformError(platformError(AdFormatEnum.MREC, placementId, "${platformType.name} mrec >> ismrecloaded"))
            }
            return
        }

        mrecAdView = AdView(activity.applicationContext)
        // mrecAdView?.adSize = AdSize.SMART_MREC

        mrecAdView.setAdSize(getMrecBannerAdaptiveSize(activity, containerView))
        mrecAdView.adUnitId = placementId
        mrecAdView.adListener = object : AdListener() {
            override fun onAdFailedToLoad(error: LoadAdError) {
                super.onAdFailedToLoad(error)

                if (!isMrecLoaded(placementId)){
                    adIntances[placementId] = null

                    activity.runOnUiThread {
                        _removeBannerViewIfExists(mrecAdView, containerView)
                        mrecAdView.destroy() // a failed view is never reused
                    }

                    listener?.onPlatformError(platformError(AdFormatEnum.MREC, placementId, "${platformType.name} mrec >> error code=${error.code} / ${error.message}"))
                }
            }

            override fun onAdLoaded() {
                super.onAdLoaded()

                adIntances[placementId] = PlatformAdInstance(
                    AdFormatEnum.MREC,
                    placementId,
                    mrecAdView
                )
            }

            override fun onAdImpression() {
                super.onAdImpression()

                listener?.onDisplayed(platformType)
            }

            override fun onAdClicked() {
                listener?.onClicked(platformType)
            }
        }
        _removeBannerViewIfExists(mrecAdView, containerView)
        containerView.addView(mrecAdView, lp)

        mrecAdView?.loadAd(AdRequest.Builder().build())
    }

    fun getMrecBannerAdaptiveSize(activity: Activity, containerView: RelativeLayout): AdSize {
        val displayMetrics = activity.resources.displayMetrics
        val adWidthPixels =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowMetrics: WindowMetrics = activity.windowManager.currentWindowMetrics
                windowMetrics.bounds.width()
            } else {
                displayMetrics.widthPixels
            }
        val density = displayMetrics.density
        val adWidth = (adWidthPixels / density).toInt() - 24

        return AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(activity, adWidth)
    }

    override fun destroy(activity: Activity) {

        try {
            for (entry in adIntances.entries.iterator()) {
                val adIns = entry.value
                if (adIns == null) continue

                if (adIns.format == AdFormatEnum.INTERSTITIAL) {
                    adIntances[entry.key] = null
                } else if (adIns.format == AdFormatEnum.REWARDED) {
                    adIntances[entry.key] = null
                } else if (adIns.format == AdFormatEnum.BANNER) {
                    destroyBanner(activity, adIns.placementId)
                } else if (adIns.format == AdFormatEnum.MREC) {
                    destroyMrec(activity, adIns.placementId)
                }
            }
        } catch (e: Exception) {
            Log.e("AdmobAds", e.message ?: "")
        }

    }

    override fun destroyBanner(activity: Activity, placementId: String) {
        try {
            var bannerAdView: AdView? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as AdView? else null
            if (_isBannerLoaded(bannerAdView)) {
                try {
                    _removeBannerViewIfExists(bannerAdView)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            bannerAdView?.destroy()
            bannerAdView = null
            adIntances[placementId] = null

        } catch (e: Exception) {
            Log.e("AdmobAds", e.message ?: "")
        }

    }

    override fun destroyMrec(activity: Activity, placementId: String) {
        try {
            var mrecAdView: AdView? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as AdView? else null
            if (_isBannerLoaded(mrecAdView)) {
                try {
                    _removeBannerViewIfExists(mrecAdView)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            mrecAdView?.destroy()
            mrecAdView = null
            adIntances[placementId] = null

        } catch (e: Exception) {
            Log.e("AdmobAds", e.message ?: "")
        }
    }


    override fun onPause(activity: Activity) {}
    override fun onStop(activity: Activity) {}
    override fun onResume(activity: Activity) {}

}