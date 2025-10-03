package com.helikanonlib.simpleadmanager

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.google.android.gms.ads.*
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.helikanonlib.simpleadmanager.admobnativetemplates.NativeTemplateStyle
import com.helikanonlib.simpleadmanager.admobnativetemplates.TemplateView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdmobAds(override var appId: String) : AdPlatformWrapper(appId) {

    override var platformType = AdPlatformTypeEnum.ADMOB
    var adIntances: MutableMap<String, PlatformAdInstance?> = mutableMapOf()

    companion object {
        var isInitialized = false
    }

    override fun initialize(activity: Activity, testMode: Boolean) {
    }

    override fun initialize(context: Context, testMode: Boolean) {
        if (isInitialized) return

        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(context, object: OnInitializationCompleteListener{
                override fun onInitializationComplete(initializationStatus: InitializationStatus) {


                    /*for ((adapterClass, status) in initializationStatus.adapterStatusMap) {
                        Log.d(
                            "AdmobAds",
                            "Adapter: $adapterClass, Status: ${status.description}, Latency: ${status.latency}ms",
                        )
                    }*/
                }

            })
        }
        isInitialized = true

        if (testMode) {
            enableTestMode(context, null)
        }

        /*val x = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        val y = x.metaData.getString("com.google.android.gms.ads.APPLICATION_ID")
        val aasdfa = 4+4*/
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
                    listener?.onError(AdErrorMode.PLATFORM, "${platformType.name} interstitial >> error code=${adError.code} / ${adError.message}", platformType)
                }
            })
    }

    override fun showInterstitial(activity: Activity, placementId: String, shownWhere: String, listener: AdPlatformShowListener?) {
        if (!isInterstitialLoaded(placementId)) {
            listener?.onError(AdErrorMode.PLATFORM, "${platformType.name} interstitial >> noads loaded", platformType)
            return
        }

        val interstitial: InterstitialAd? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as InterstitialAd? else null

        interstitial?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                adIntances[placementId] = null
                listener?.onError(AdErrorMode.PLATFORM, "${platformType.name} interstitial show >> error code=${adError.code} / ${adError.message}", platformType)
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
                    listener?.onError(AdErrorMode.PLATFORM, "${platformType.name} rewarded load >> error code=${adError.code} / ${adError.message}", platformType)
                }
            })

    }

    override fun showRewarded(activity: Activity, placementId: String, shownWhere: String, listener: AdPlatformShowListener?) {
        if (!isRewardedLoaded(placementId)) {
            listener?.onError(AdErrorMode.PLATFORM, "${platformType.name} rewarded >> noadsloaded", platformType)
            return
        }

        val rewardedAd: RewardedAd? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as RewardedAd? else null

        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                listener?.onError(AdErrorMode.PLATFORM, "${platformType.name} rewarded show >> error code=${adError.code} / ${adError.message}", platformType)
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
        if (isLoaded && !isValidLoadedBanner(platformType)) {
            _removeBannerViewIfExists(bannerAdView)
            adIntances[placementId] = null
            isLoaded = false
        }

        return isLoaded
    }

    override fun showBanner(activity: Activity, placementId: String, containerView: RelativeLayout, listener: AdPlatformShowListener?) {

        var bannerAdView: AdView? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as AdView? else null

        val lp = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            .apply {
                addRule(RelativeLayout.CENTER_HORIZONTAL)
            }

        if (_isBannerLoaded(bannerAdView)) {
            try {
                _removeBannerViewIfExists(bannerAdView)
                containerView.addView(bannerAdView, lp)
                listener?.onDisplayed(platformType)
            } catch (e: Exception) {
                listener?.onError(AdErrorMode.PLATFORM, "${platformType.name} banner >> isbannerloaded", platformType)
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
                adIntances[placementId] = null
                activity.runOnUiThread {
                    _removeBannerViewIfExists(bannerAdView, containerView)
                }

                listener?.onError(AdErrorMode.PLATFORM, "${platformType.name} banner >> error code=${error.code} / ${error.message}", platformType)
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
        val display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val density = outMetrics.density

        var adWidthPixels = containerView.width.toFloat()
        if (adWidthPixels == 0f) {
            adWidthPixels = outMetrics.widthPixels.toFloat()
        }

        val adWidth = (adWidthPixels / density).toInt()

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }


    // ################################################################################
    // ################################### MREC #######################################
    // ################################################################################
    override fun isMrecLoaded(placementId: String): Boolean {
        val mrecAdView: AdView? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as AdView? else null

        var isLoaded = _isBannerLoaded(mrecAdView)
        if (isLoaded && !isValidLoadedMrec(platformType)) {
            _removeBannerViewIfExists(mrecAdView)
            adIntances[placementId] = null
            isLoaded = false
        }

        return isLoaded
    }

    override fun showMrec(activity: Activity, placementId: String, containerView: RelativeLayout, listener: AdPlatformShowListener?) {

        var mrecAdView: AdView? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as AdView? else null

        val lp = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            .apply {
                addRule(RelativeLayout.CENTER_HORIZONTAL)
            }

        if (_isBannerLoaded(mrecAdView)) {
            try {
                _removeBannerViewIfExists(mrecAdView)
                containerView.addView(mrecAdView, lp)
                listener?.onDisplayed(platformType)
            } catch (e: Exception) {
                listener?.onError(AdErrorMode.PLATFORM, "${platformType.name} mrec >> ismrecloaded", platformType)
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
                adIntances[placementId] = null
                activity.runOnUiThread {
                    _removeBannerViewIfExists(mrecAdView, containerView)
                }

                listener?.onError(AdErrorMode.PLATFORM, "${platformType.name} mrec >> error code=${error.code} / ${error.message}", platformType)
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
        val display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val density = outMetrics.density

        var adWidthPixels = containerView.width.toFloat()
        if (adWidthPixels == 0f) {
            adWidthPixels = outMetrics.widthPixels.toFloat()
        }

        val adWidth = (adWidthPixels / density).toInt() - 24

        return AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(activity, adWidth)
    }

    // ################################################################################
    // ################################### NATIVE #####################################
    // ################################################################################
    override fun hasLoadedNative(nativeAdFormat: AdFormatEnum, placementId: String): Boolean {
        val nativeAds: ArrayList<Any> = if (adIntances.containsKey(placementId) && adIntances[placementId] != null) adIntances.get(placementId)?.instance as ArrayList<Any> else ArrayList<Any>()
        return nativeAds.size > 0
    }

    @JvmOverloads
    override fun loadNativeAds(activity: Activity, placementId: String, nativeAdFormat: AdFormatEnum, count: Int, listener: AdPlatformLoadListener?) {

        var nativeAds: ArrayList<Any> = if (adIntances.containsKey(placementId) && adIntances[placementId] != null) adIntances.get(placementId)?.instance as ArrayList<Any> else ArrayList<Any>()


        // destroy olds
        try {
            nativeAds.forEach {
                (it as NativeAd).destroy()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            nativeAds.clear()

            adIntances[placementId] = PlatformAdInstance(
                nativeAdFormat,
                placementId,
                nativeAds
            )
        }

        lateinit var adLoader: AdLoader
        adLoader = AdLoader.Builder(activity, placementId)
            .forNativeAd { nativeAd ->

                nativeAds.add(nativeAd)

                adIntances[placementId] = PlatformAdInstance(
                    nativeAdFormat,
                    placementId,
                    nativeAds
                )

                if (!adLoader.isLoading) {
                    listener?.onLoaded(platformType)
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdLoaded() {
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    if (!adLoader.isLoading) {
                        if (nativeAds.size > 0) {
                            listener?.onLoaded(platformType)
                        } else {
                            listener?.onError(AdErrorMode.PLATFORM, adError.message, platformType)
                        }
                    }
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .build()
            )
            .build()
        // adLoader.loadAd(AdRequest.Builder().build())
        adLoader.loadAds(AdRequest.Builder().build(), count)
    }

    private var lastLoadedNativeAdPositions: MutableMap<String, Int> = mutableMapOf()

    override fun showNative(activity: Activity, placementId: String, nativeAdFormat: AdFormatEnum, containerView: ViewGroup, listener: AdPlatformShowListener?): Boolean {

        val nativeAds: ArrayList<Any> = if (adIntances.containsKey(placementId) && adIntances[placementId] != null) adIntances.get(placementId)?.instance as ArrayList<Any> else ArrayList<Any>()

        val lastLoadedNativeAdPosition: Int = if (lastLoadedNativeAdPositions.containsKey(placementId)) lastLoadedNativeAdPositions[placementId]!! else -1
        var showPositionAt = if (lastLoadedNativeAdPosition == -1) 0 else lastLoadedNativeAdPosition + 1

        if (showPositionAt >= nativeAds.size) {
            showPositionAt = 0
        }

        if (nativeAds.size == 0) {
            return false
        }
        lastLoadedNativeAdPositions[placementId] = showPositionAt

        val nativeAd = nativeAds[showPositionAt] as NativeAd

        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val nativeSMALLView = inflater.inflate(
            if (nativeAdFormat == AdFormatEnum.NATIVE_SMALL) {
                R.layout.admob_native_small
            } else {
                R.layout.admob_native_medium
            }, null
        )


        val template = nativeSMALLView.findViewById<TemplateView>(
            if (nativeAdFormat == AdFormatEnum.NATIVE_SMALL) {
                R.id.admanager_native_small
            } else {
                R.id.admanager_native_medium
            }
        )

        val styles = NativeTemplateStyle.Builder().build()
        template.setStyles(styles)
        template.setNativeAd(nativeAd)

        _removeViewIfExists(template.parent as ViewGroup?, containerView)
        containerView.addView(template.parent as ViewGroup)

        return true
    }

    override fun getNativeAds(activity: Activity, placementId: String, nativeAdFormat: AdFormatEnum): ArrayList<Any> {
        return if (adIntances.containsKey(placementId) && adIntances[placementId] != null) adIntances.get(placementId)?.instance as ArrayList<Any> else ArrayList<Any>()
    }

    // ################################################################################
    // ################################### NATIVE #####################################
    // ################################################################################

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
                } else if (adIns.format == AdFormatEnum.NATIVE_SMALL) {
                    _destroyAllNativeAds(activity, adIns.placementId)
                } else if (adIns.format == AdFormatEnum.NATIVE_MEDIUM) {
                    _destroyAllNativeAds(activity, adIns.placementId)
                }
            }
        } catch (e: Exception) {
            Log.e("AdmobAds", e.message ?: "")
        }

    }

    private fun _destroyAllNativeAds(activity: Activity, placementId: String) {
        try {
            // destroy NATIVE ads
            val adIns = if (adIntances.containsKey(placementId) && adIntances[placementId] != null) adIntances.get(placementId) else null
            if (adIns == null) return

            var nativeAds: ArrayList<Any> = adIns as ArrayList<Any>
            try {
                nativeAds.forEach {
                    (it as NativeAd).destroy()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                nativeAds.clear()
                adIntances[placementId] = PlatformAdInstance(
                    adIns.format,
                    adIns.placementId,
                    nativeAds
                )
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