package com.helikanonlib.simpleadmanager

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.view.ViewCompat
import com.applovin.mediation.*
import com.applovin.mediation.ads.MaxAdView
import com.applovin.mediation.ads.MaxInterstitialAd
import com.applovin.mediation.ads.MaxRewardedAd
import com.applovin.mediation.nativeAds.MaxNativeAdListener
import com.applovin.mediation.nativeAds.MaxNativeAdLoader
import com.applovin.mediation.nativeAds.MaxNativeAdView
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkConfiguration
import com.applovin.sdk.AppLovinSdkUtils

class ApplovinAds(override var appId: String) : AdPlatformWrapper(appId) {

    override var platformType = AdPlatformTypeEnum.APPLOVIN
    var adIntances: MutableMap<String, PlatformAdInstance?> = mutableMapOf()

    companion object {
        var isInitialized = false
    }

    override fun initialize(activity: Activity, testMode: Boolean) {
    }

    override fun initialize(context: Context, testMode: Boolean) {
        if (isInitialized) return

        AppLovinSdk.getInstance(context).mediationProvider = "max"
        AppLovinSdk.getInstance(context).initializeSdk { configuration: AppLovinSdkConfiguration ->

        }
        isInitialized = true
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

        val interstitial: MaxInterstitialAd? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as MaxInterstitialAd? else null

        var isLoaded = interstitial?.isReady ?: false
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


        val applovinInterstitialIns = MaxInterstitialAd(placementId, activity)
        applovinInterstitialIns.setListener(object : MaxAdListener {
            override fun onAdLoaded(ad: MaxAd?) {
                updateLastLoadInterstitialDateByAdPlatform(platformType)
                listener?.onLoaded(platformType)
            }


            override fun onAdLoadFailed(adUnitId: String?, error: MaxError?) {
                adIntances[placementId] = null
                listener?.onError(AdErrorMode.PLATFORM, "${platformType.name} interstitial >> error code=${error?.code} / ${error?.message}", platformType)
            }

            override fun onAdDisplayed(ad: MaxAd?) {

            }

            override fun onAdHidden(ad: MaxAd?) {

            }

            override fun onAdClicked(ad: MaxAd?) {

            }

            override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {

            }

        })

        adIntances[placementId] = PlatformAdInstance(
            AdFormatEnum.INTERSTITIAL,
            placementId,
            applovinInterstitialIns
        )
        applovinInterstitialIns.loadAd()

    }

    override fun showInterstitial(activity: Activity, placementId: String, shownWhere: String, listener: AdPlatformShowListener?) {
        if (!isInterstitialLoaded(placementId)) {
            listener?.onError(AdErrorMode.PLATFORM, "${platformType.name} interstitial >> noads loaded", platformType)
            return
        }

        val interstitial: MaxInterstitialAd? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as MaxInterstitialAd? else null

        interstitial?.setListener(object : MaxAdListener {
            override fun onAdLoaded(ad: MaxAd?) {

            }

            override fun onAdDisplayed(ad: MaxAd?) {
                listener?.onDisplayed(platformType)
            }

            override fun onAdHidden(ad: MaxAd?) {
                adIntances[placementId] = null
                listener?.onClosed(platformType)
            }

            override fun onAdClicked(ad: MaxAd?) {
                listener?.onClicked(platformType)
            }

            override fun onAdLoadFailed(adUnitId: String?, error: MaxError?) {
                adIntances[placementId] = null
                listener?.onError(AdErrorMode.PLATFORM, "${platformType.name} interstitial show >> error code=${error?.code} / ${error?.message}", platformType)
            }

            override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {
                adIntances[placementId] = null
                listener?.onError(AdErrorMode.PLATFORM, "${platformType.name} interstitial show >> error code=${error?.code} / ${error?.message}", platformType)
            }

        })
        interstitial?.showAd(shownWhere)
        adIntances[placementId] = null

    }

    // ################################################################################
    // ################################### REWARDED ###################################
    // ################################################################################
    override fun isRewardedLoaded(placementId: String): Boolean {
        val rewardedAd: MaxRewardedAd? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as MaxRewardedAd? else null

        var isLoaded = rewardedAd?.isReady ?: false
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

        val rewardedAd = MaxRewardedAd.getInstance(placementId, activity)

        rewardedAd?.setListener(object : MaxRewardedAdListener {
            override fun onAdLoaded(ad: MaxAd?) {
                updateLastLoadRewardedDateByAdPlatform(platformType)

                listener?.onLoaded(platformType)
            }

            override fun onAdLoadFailed(adUnitId: String?, error: MaxError?) {
                adIntances[placementId] = null
                listener?.onError(AdErrorMode.PLATFORM, "${platformType.name} rewarded load >> error code=${error?.code} / ${error?.message}", platformType)
            }


            override fun onAdDisplayed(ad: MaxAd?) {

            }

            override fun onAdHidden(ad: MaxAd?) {

            }

            override fun onAdClicked(ad: MaxAd?) {

            }

            override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {

            }

            override fun onRewardedVideoStarted(ad: MaxAd?) {

            }

            override fun onRewardedVideoCompleted(ad: MaxAd?) {

            }

            override fun onUserRewarded(ad: MaxAd?, reward: MaxReward?) {

            }

        })
        rewardedAd?.loadAd()
        adIntances[placementId] = PlatformAdInstance(
            AdFormatEnum.REWARDED,
            placementId,
            rewardedAd
        )

    }

    override fun showRewarded(activity: Activity, placementId: String, shownWhere: String, listener: AdPlatformShowListener?) {
        if (!isRewardedLoaded(placementId)) {
            listener?.onError(AdErrorMode.PLATFORM, "${platformType.name} rewarded >> noadsloaded", platformType)
            return
        }

        val rewardedAd: MaxRewardedAd? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as MaxRewardedAd? else null

        rewardedAd?.setListener(object : MaxRewardedAdListener {
            override fun onAdLoaded(ad: MaxAd?) {

            }

            override fun onAdDisplayed(ad: MaxAd?) {
                listener?.onDisplayed(platformType)
            }

            override fun onAdHidden(ad: MaxAd?) {
                adIntances[placementId] = null
                listener?.onClosed(platformType)
            }

            override fun onAdClicked(ad: MaxAd?) {
            }

            override fun onAdLoadFailed(adUnitId: String?, error: MaxError?) {

            }

            override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platformType.name} rewarded show >> error code=${error?.code} / ${error?.message}", platformType)
                adIntances[placementId] = null
            }

            override fun onRewardedVideoStarted(ad: MaxAd?) {

            }

            override fun onRewardedVideoCompleted(ad: MaxAd?) {

            }

            override fun onUserRewarded(ad: MaxAd?, reward: MaxReward?) {
                listener?.onRewarded(reward?.label, reward?.amount, platformType)
            }
        })

        rewardedAd?.showAd(shownWhere)
        adIntances[placementId] = null

    }


    // ################################################################################
    // ################################### BANNER #####################################
    // ################################################################################
    override fun isBannerLoaded(placementId: String): Boolean {
        val bannerAdView: MaxAdView? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as MaxAdView? else null

        var isLoaded = _isBannerLoaded(bannerAdView)
        if (isLoaded && !isValidLoadedBanner(platformType)) {
            _removeBannerViewIfExists(bannerAdView)
            adIntances[placementId] = null
            isLoaded = false
        }

        return isLoaded
    }

    override fun showBanner(activity: Activity, placementId: String, containerView: RelativeLayout, listener: AdPlatformShowListener?) {

        var bannerAdView: MaxAdView? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as MaxAdView? else null

        /*val lp = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            .apply {
                addRule(RelativeLayout.CENTER_HORIZONTAL)
            }*/

        if (_isBannerLoaded(bannerAdView)) {
            try {
                _removeBannerViewIfExists(bannerAdView)
                containerView.addView(bannerAdView)
                listener?.onDisplayed(platformType)
            } catch (e: Exception) {
                listener?.onError(AdErrorMode.PLATFORM, "${platformType.name} banner >> isbannerloaded", platformType)
            }
            return
        }

        bannerAdView = MaxAdView(placementId, activity)
        // bannerAdView.id = ViewCompat.generateViewId()

        val isTablet = AppLovinSdkUtils.isTablet(activity)
        val heightPx = AppLovinSdkUtils.dpToPx(activity, if (isTablet) 90 else 50)
        // val heightDp = MaxAdFormat.BANNER.getAdaptiveSize(activity).height
        // val heightPx = AppLovinSdkUtils.dpToPx(activity, heightDp)

        bannerAdView.layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx)
            .apply {
                addRule(RelativeLayout.CENTER_HORIZONTAL)
            }
        // bannerAdView.setExtraParameter("adaptive_banner", "true")
        // applovinBannerIns?.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, activity.resources.getDimensionPixelSize(R.dimen.applovinBannerHeight))
        //applovinBannerIns?.layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, activity.resources.getDimensionPixelSize(R.dimen.applovinBannerHeight))


        bannerAdView.setListener(object : MaxAdViewAdListener {
            override fun onAdLoaded(ad: MaxAd?) {
                adIntances[placementId] = PlatformAdInstance(
                    AdFormatEnum.BANNER,
                    placementId,
                    bannerAdView
                )
            }

            override fun onAdLoadFailed(adUnitId: String?, error: MaxError?) {
                adIntances[placementId] = null
                activity.runOnUiThread {
                    _removeBannerViewIfExists(bannerAdView, containerView)
                }

                listener?.onError(AdErrorMode.PLATFORM, "${platformType.name} banner >> error code=${error?.code} / ${error?.message}", platformType)
            }

            override fun onAdDisplayed(ad: MaxAd?) {
                listener?.onDisplayed(platformType)
            }

            override fun onAdHidden(ad: MaxAd?) {

            }

            override fun onAdClicked(ad: MaxAd?) {
                listener?.onClicked(platformType)
            }

            override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {

            }

            override fun onAdExpanded(ad: MaxAd?) {

            }

            override fun onAdCollapsed(ad: MaxAd?) {

            }

        })
        _removeBannerViewIfExists(bannerAdView, containerView)
        containerView.addView(bannerAdView)
        bannerAdView.loadAd()
    }

    // ################################################################################
    // ################################### MREC #######################################
    // ################################################################################
    override fun isMrecLoaded(placementId: String): Boolean {
        val mrecAdView: MaxAdView? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as MaxAdView? else null

        var isLoaded = _isBannerLoaded(mrecAdView)
        if (isLoaded && !isValidLoadedMrec(platformType)) {
            _removeBannerViewIfExists(mrecAdView)
            adIntances[placementId] = null
            isLoaded = false
        }

        return isLoaded
    }

    override fun showMrec(activity: Activity, placementId: String, containerView: RelativeLayout, listener: AdPlatformShowListener?) {

        var mrecAdView: MaxAdView? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as MaxAdView? else null

        /*val lp = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            .apply {
                addRule(RelativeLayout.CENTER_HORIZONTAL)
            }*/

        if (_isBannerLoaded(mrecAdView)) {
            try {
                _removeBannerViewIfExists(mrecAdView)
                containerView.addView(mrecAdView)
                listener?.onDisplayed(platformType)
            } catch (e: Exception) {
                listener?.onError(AdErrorMode.PLATFORM, "${platformType.name} mrec >> ismrecloaded", platformType)
            }
            return
        }

        mrecAdView = MaxAdView(placementId, activity)
        // bannerAdView.id = ViewCompat.generateViewId()

        val widthPx = AppLovinSdkUtils.dpToPx(activity, 300)
        val heightPx = AppLovinSdkUtils.dpToPx(activity, 250)

        mrecAdView?.layoutParams = RelativeLayout.LayoutParams(widthPx, heightPx).apply {
            addRule(RelativeLayout.CENTER_HORIZONTAL)
        }


        mrecAdView.setListener(object : MaxAdViewAdListener {
            override fun onAdLoaded(ad: MaxAd?) {
                adIntances[placementId] = PlatformAdInstance(
                    AdFormatEnum.MREC,
                    placementId,
                    mrecAdView
                )
            }

            override fun onAdLoadFailed(adUnitId: String?, error: MaxError?) {
                adIntances[placementId] = null
                activity.runOnUiThread {
                    _removeBannerViewIfExists(mrecAdView, containerView)
                }

                listener?.onError(AdErrorMode.PLATFORM, "${platformType.name} mrec >> error code=${error?.code} / ${error?.message}", platformType)
            }


            override fun onAdDisplayed(ad: MaxAd?) {
                listener?.onDisplayed(platformType)
            }

            override fun onAdHidden(ad: MaxAd?) {

            }

            override fun onAdClicked(ad: MaxAd?) {
                listener?.onClicked(platformType)
            }

            override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {

            }

            override fun onAdExpanded(ad: MaxAd?) {

            }

            override fun onAdCollapsed(ad: MaxAd?) {

            }

        })
        _removeBannerViewIfExists(mrecAdView, containerView)
        containerView.addView(mrecAdView)

        mrecAdView.loadAd()
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
                (it as MaxNativeAdView).recycle()
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

        val nativeAdLoader = MaxNativeAdLoader(placementId, activity)
        nativeAdLoader.setNativeAdListener(object : MaxNativeAdListener() {
            override fun onNativeAdLoaded(nativeAdView: MaxNativeAdView?, ad: MaxAd?) {
                super.onNativeAdLoaded(nativeAdView, ad)

                nativeAdView?.let {
                    nativeAds.add(it)
                    adIntances[placementId] = PlatformAdInstance(
                        nativeAdFormat,
                        placementId,
                        nativeAds
                    )
                }

                listener?.onLoaded(platformType)
            }

            override fun onNativeAdLoadFailed(adUnitId: String?, error: MaxError?) {
                super.onNativeAdLoadFailed(adUnitId, error)

                listener?.onError(AdErrorMode.PLATFORM, error?.message ?: "applovin native ad load error", platformType)
            }

            override fun onNativeAdClicked(ad: MaxAd?) {
                super.onNativeAdClicked(ad)
            }
        })
        nativeAdLoader.loadAd()
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

        val nativeAd = nativeAds[showPositionAt] as MaxNativeAdView?

        nativeAd?.let {
            _removeViewIfExists(nativeAd, containerView)
            // containerView.removeAllViews()
            containerView.addView(nativeAd)

        }
        return nativeAd != null
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
            Log.e("ApplovinAds", e.message ?: "")
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
                    (it as MaxNativeAdView).recycle()
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
            Log.e("ApplovinAds", e.message ?: "")
        }
    }

    override fun destroyBanner(activity: Activity, placementId: String) {
        try {
            var bannerAdView: MaxAdView? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as MaxAdView? else null
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
            Log.e("ApplovinAds", e.message ?: "")
        }

    }

    override fun destroyMrec(activity: Activity, placementId: String) {
        try {
            var mrecAdView: MaxAdView? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as MaxAdView? else null
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
            Log.e("ApplovinAds", e.message ?: "")
        }
    }


    override fun onPause(activity: Activity) {}
    override fun onStop(activity: Activity) {}
    override fun onResume(activity: Activity) {}

}