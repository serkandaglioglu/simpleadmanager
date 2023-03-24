package com.helikanonlib.simpleadmanager

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.IronSourceBannerLayout
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.model.Placement
import com.ironsource.mediationsdk.sdk.BannerListener
import com.ironsource.mediationsdk.sdk.InterstitialListener
import com.ironsource.mediationsdk.sdk.RewardedVideoListener

class IronSourceAds(override var appId: String) : AdPlatformWrapper(appId) {

    override var platformType = AdPlatformTypeEnum.IRONSOURCE
    var adIntances: MutableMap<String, PlatformAdInstance?> = mutableMapOf()

    companion object {
        var isInitialized = false
    }

    override fun initialize(activity: Activity, testMode: Boolean) {
        if (isInitialized) return

        IronSource.init(activity, appId, IronSource.AD_UNIT.INTERSTITIAL, IronSource.AD_UNIT.BANNER, IronSource.AD_UNIT.REWARDED_VIDEO)
        isInitialized = true
    }

    override fun initialize(context: Context, testMode: Boolean) {

    }

    override fun enableTestMode(context: Context, deviceId: String?) {

    }


    // ################################################################################
    // ################################### INTERSTITIAL ###############################
    // ################################################################################
    override fun isInterstitialLoaded(placementId: String): Boolean {
        return IronSource.isInterstitialReady()
    }


    override fun loadInterstitial(activity: Activity, placementId: String, listener: AdPlatformLoadListener?) {
        if (isInterstitialLoaded(placementId)) {
            listener?.onLoaded(platformType)
            return
        }

        IronSource.setInterstitialListener(object : InterstitialListener {
            override fun onInterstitialAdLoadFailed(p0: IronSourceError?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platformType.name} interstitial >>${p0?.errorCode ?: ""} - ${p0?.errorMessage ?: ""}", platformType)
            }

            override fun onInterstitialAdClosed() {

            }

            override fun onInterstitialAdShowFailed(p0: IronSourceError?) {

            }

            override fun onInterstitialAdClicked() {

            }

            override fun onInterstitialAdReady() {
                updateLastLoadInterstitialDateByAdPlatform(platformType)
                listener?.onLoaded(platformType)
            }

            override fun onInterstitialAdOpened() {

            }

            override fun onInterstitialAdShowSucceeded() {

            }
        })
        IronSource.loadInterstitial()
    }

    override fun showInterstitial(activity: Activity, placementId: String, shownWhere: String, listener: AdPlatformShowListener?) {
        if (!isInterstitialLoaded(placementId)) {
            listener?.onError(AdErrorMode.PLATFORM, "${platformType.name} interstitial >> noads loaded", platformType)
            return
        }

        IronSource.setInterstitialListener(object : InterstitialListener {
            override fun onInterstitialAdLoadFailed(p0: IronSourceError?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platformType.name} interstitial >> ${p0?.errorCode ?: ""} - ${p0?.errorMessage ?: ""}", platformType)
            }

            override fun onInterstitialAdClosed() {
                listener?.onClosed(platformType)
            }

            override fun onInterstitialAdShowFailed(p0: IronSourceError?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platformType.name} interstitial >> ${p0?.errorCode ?: ""} - ${p0?.errorMessage ?: ""}", platformType)
            }

            override fun onInterstitialAdClicked() {
                listener?.onClicked(platformType)
            }

            override fun onInterstitialAdReady() {

            }

            override fun onInterstitialAdOpened() {

            }

            override fun onInterstitialAdShowSucceeded() {
                listener?.onDisplayed(platformType)
            }
        })
        IronSource.showInterstitial(placementId)

    }

    // ################################################################################
    // ################################### REWARDED ###################################
    // ################################################################################
    override fun isRewardedLoaded(placementId: String): Boolean {
        return IronSource.isRewardedVideoAvailable()
    }


    override fun loadRewarded(activity: Activity, placementId: String, listener: AdPlatformLoadListener?) {
        if (isRewardedLoaded(placementId)) {
            listener?.onLoaded(platformType)
        } else {
            listener?.onError(AdErrorMode.PLATFORM, "${platformType.name} rewarded >> load error", platformType)
        }

    }

    override fun showRewarded(activity: Activity, placementId: String, shownWhere: String, listener: AdPlatformShowListener?) {
        if (!isRewardedLoaded(placementId)) {
            listener?.onError(AdErrorMode.PLATFORM, "${platformType.name} rewarded >> noadsloaded", platformType)
            return
        }

        IronSource.setRewardedVideoListener(object : RewardedVideoListener {
            override fun onRewardedVideoAdClosed() {
                listener?.onClosed(platformType)
            }

            override fun onRewardedVideoAdRewarded(p0: Placement?) {
                listener?.onRewarded(p0?.rewardName, p0?.rewardAmount, platformType)
            }

            override fun onRewardedVideoAdClicked(p0: Placement?) {
                listener?.onClicked(platformType)
            }

            override fun onRewardedVideoAdOpened() {
                listener?.onDisplayed(platformType)
            }

            override fun onRewardedVideoAdShowFailed(p0: IronSourceError?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platformType.name} rewarded >> ${p0?.errorCode ?: ""} - ${p0?.errorMessage ?: ""}", platformType)
            }

            // TODO debug this method
            /**
             * Invoked when there is a change in the ad availability status.
             *
             * @param - available - value will change to true when rewarded videos are *available.
             *          You can then show the video by calling showRewardedVideo().
             *          Value will change to false when no videos are available.
             */
            // call this after video close
            override fun onRewardedVideoAvailabilityChanged(available: Boolean) {

            }

            override fun onRewardedVideoAdEnded() {

            }

            override fun onRewardedVideoAdStarted() {

            }
        })

        IronSource.showRewardedVideo(placementId)
    }

    // ################################################################################
    // ################################### BANNER #####################################
    // ################################################################################
    override fun isBannerLoaded(placementId: String): Boolean {
        val bannerAdView: IronSourceBannerLayout? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as IronSourceBannerLayout? else null

        var isLoaded = _isBannerLoaded(bannerAdView)
        if (isLoaded && !isValidLoadedBanner(platformType)) {
            _removeBannerViewIfExists(bannerAdView)
            adIntances[placementId] = null
            isLoaded = false
        }

        return isLoaded
    }

    override fun showBanner(activity: Activity, placementId: String, containerView: RelativeLayout, listener: AdPlatformShowListener?) {

        var bannerAdView: IronSourceBannerLayout? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as IronSourceBannerLayout? else null

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

        bannerAdView = IronSource.createBanner(activity, ISBannerSize.BANNER)
        bannerAdView?.bannerListener = object : BannerListener {
            override fun onBannerAdClicked() {
                listener?.onClicked(platformType)
            }

            override fun onBannerAdLoadFailed(p0: IronSourceError?) {
                adIntances[placementId] = null
                activity.runOnUiThread {
                    _removeBannerViewIfExists(bannerAdView, containerView)
                }

                listener?.onError(AdErrorMode.PLATFORM, "${platformType.name} banner >> ${p0?.errorCode ?: ""} - ${p0?.errorMessage ?: ""}", platformType)
            }

            override fun onBannerAdLoaded() {
                bannerAdView?.visibility = View.VISIBLE
            }

            override fun onBannerAdLeftApplication() {

            }

            override fun onBannerAdScreenDismissed() {

            }

            override fun onBannerAdScreenPresented() {
                listener?.onDisplayed(platformType)
            }
        }

        adIntances[placementId] = PlatformAdInstance(
            AdFormatEnum.BANNER,
            placementId,
            bannerAdView
        )
        _removeBannerViewIfExists(bannerAdView, containerView)
        containerView.addView(bannerAdView, lp)


        IronSource.loadBanner(bannerAdView, placementId)
    }

    // ################################################################################
    // ################################### MREC #######################################
    // ################################################################################
    override fun isMrecLoaded(placementId: String): Boolean {
        val mrecAdView: IronSourceBannerLayout? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as IronSourceBannerLayout? else null

        var isLoaded = _isBannerLoaded(mrecAdView)
        if (isLoaded && !isValidLoadedMrec(platformType)) {
            _removeBannerViewIfExists(mrecAdView)
            adIntances[placementId] = null
            isLoaded = false
        }

        return isLoaded
    }

    override fun showMrec(activity: Activity, placementId: String, containerView: RelativeLayout, listener: AdPlatformShowListener?) {

        var mrecAdView: IronSourceBannerLayout? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as IronSourceBannerLayout? else null

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

        mrecAdView = IronSource.createBanner(activity, ISBannerSize.RECTANGLE)
        mrecAdView?.bannerListener = object : BannerListener {
            override fun onBannerAdClicked() {
                listener?.onClicked(platformType)
            }

            override fun onBannerAdLoadFailed(p0: IronSourceError?) {
                adIntances[placementId] = null
                activity.runOnUiThread {
                    _removeBannerViewIfExists(mrecAdView, containerView)
                }

                listener?.onError(AdErrorMode.PLATFORM, "${platformType.name} mrec >> ${p0?.errorCode ?: ""} - ${p0?.errorMessage ?: ""}", platformType)
            }

            override fun onBannerAdLoaded() {
                mrecAdView?.visibility = View.VISIBLE

            }

            override fun onBannerAdLeftApplication() {

            }

            override fun onBannerAdScreenDismissed() {
                listener?.onDisplayed(platformType)
            }

            override fun onBannerAdScreenPresented() {

            }
        }

        adIntances[placementId] = PlatformAdInstance(
            AdFormatEnum.MREC,
            placementId,
            mrecAdView
        )
        _removeBannerViewIfExists(mrecAdView, containerView)
        containerView.addView(mrecAdView, lp)

        IronSource.loadBanner(mrecAdView, placementId)
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

        listener?.onError(AdErrorMode.PLATFORM, "not supported native ad >> ${platformType.name}", platformType)
    }

    private var lastLoadedNativeAdPositions: MutableMap<String, Int> = mutableMapOf()

    override fun showNative(activity: Activity, placementId: String, nativeAdFormat: AdFormatEnum, containerView: ViewGroup, listener: AdPlatformShowListener?): Boolean {
        return false
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

                } else if (adIns.format == AdFormatEnum.NATIVE_MEDIUM) {

                }
            }


        } catch (e: Exception) {
            Log.e("IronsourceAds", e.message ?: "")
        }
    }

    override fun destroyBanner(activity: Activity, placementId: String) {
        try {
            var bannerAdView: IronSourceBannerLayout? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as IronSourceBannerLayout? else null
            if (_isBannerLoaded(bannerAdView)) {
                try {
                    _removeBannerViewIfExists(bannerAdView)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            bannerAdView?.let {
                IronSource.destroyBanner(bannerAdView)
                bannerAdView = null
            }
            adIntances[placementId] = null

        } catch (e: Exception) {
            Log.e("IronsourceAds", e.message ?: "")
        }

    }

    override fun destroyMrec(activity: Activity, placementId: String) {
        try {
            var mrecAdView: IronSourceBannerLayout? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as IronSourceBannerLayout? else null
            if (_isBannerLoaded(mrecAdView)) {
                try {
                    _removeBannerViewIfExists(mrecAdView)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            mrecAdView?.let {
                IronSource.destroyBanner(mrecAdView)
                mrecAdView = null
            }
            adIntances[placementId] = null

        } catch (e: Exception) {
            Log.e("IronsourceAds", e.message ?: "")
        }
    }


    override fun onPause(activity: Activity) {}
    override fun onStop(activity: Activity) {}
    override fun onResume(activity: Activity) {}

}