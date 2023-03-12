package com.helikanonlib.simpleadmanager

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import android.widget.RelativeLayout
import java.util.*


abstract class AdPlatformWrapper(open var appId: String) {

    abstract val platformType: AdPlatformTypeEnum

    abstract fun initialize(activity: Activity, testMode: Boolean = false)
    abstract fun initialize(context: Context, testMode: Boolean = false)
    abstract fun enableTestMode(context: Context, deviceId: String? = null)

    abstract fun loadInterstitial(activity: Activity, placementId: String, listener: AdPlatformLoadListener?)
    abstract fun showInterstitial(activity: Activity, placementId: String, listener: AdPlatformShowListener? = null)
    abstract fun isInterstitialLoaded(placementId: String): Boolean

    abstract fun isBannerLoaded(placementId: String): Boolean
    abstract fun showBanner(activity: Activity, placementId: String, containerView: RelativeLayout, listener: AdPlatformShowListener? = null)

    abstract fun isMrecLoaded(placementId: String): Boolean
    abstract fun showMrec(activity: Activity, placementId: String, containerView: RelativeLayout, listener: AdPlatformShowListener? = null)

    abstract fun loadRewarded(activity: Activity, placementId: String, listener: AdPlatformLoadListener? = null)
    abstract fun showRewarded(activity: Activity, placementId: String, listener: AdPlatformShowListener? = null)
    abstract fun isRewardedLoaded(placementId: String): Boolean


    // val nativeAds: ArrayList<Any> = arrayListOf()
    abstract fun hasLoadedNative(nativeAdFormat: AdFormatEnum, placementId: String): Boolean
    abstract fun loadNativeAds(activity: Activity, placementId: String, nativeAdFormat: AdFormatEnum, count: Int, listener: AdPlatformLoadListener? = null)

    // adSize >> [small,medium]
    abstract fun showNative(activity: Activity, placementId: String, nativeAdFormat: AdFormatEnum, containerView: ViewGroup, listener: AdPlatformShowListener? = null): Boolean
    abstract fun getNativeAds(activity: Activity, placementId: String, nativeAdFormat: AdFormatEnum): ArrayList<Any>

    abstract fun destroy(activity: Activity)
    abstract fun destroyBanner(activity: Activity, placementId: String)
    abstract fun destroyMrec(activity: Activity, placementId: String)

    open fun onCreate(activity: Activity) {}
    open fun onPause(activity: Activity) {}
    open fun onStop(activity: Activity) {}
    open fun onResume(activity: Activity) {}

    protected fun _isBannerLoaded(bannerAdView: ViewGroup?): Boolean {
        return bannerAdView != null && bannerAdView.parent != null
    }

    protected fun _removeBannerViewIfExists(bannerAdView: ViewGroup?, containerView: ViewGroup? = null): Boolean {
        if (_isBannerLoaded(bannerAdView)) {
            (bannerAdView?.parent as ViewGroup).removeView(bannerAdView)
            return true
        }

        containerView?.let {
            try {
                containerView.removeAllViews()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return false
    }

    protected fun _removeViewIfExists(view: ViewGroup?, containerView: ViewGroup? = null) {

        try {
            if (view?.parent != null) {
                (view.parent as ViewGroup).removeView(view)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        containerView?.let {
            try {
                containerView.removeAllViews()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    var lastLoadedInterstitialsDateByAdPlatform = mutableMapOf<String, Date>()
    var lastLoadedRewardedDateByAdPlatform = mutableMapOf<String, Date>()
    var lastLoadedBannerDateByAdPlatform = mutableMapOf<String, Date>()
    var lastLoadedMrecDateByAdPlatform = mutableMapOf<String, Date>()
    var loadedInterstitialAvailableDuration = 60 * 2
    var loadedRewardedAvailableDuration = 60 * 6
    var loadedBannerAvailableDuration = 60 * 2
    var loadedMrecAvailableDuration = 60 * 2

    fun updateLastLoadInterstitialDateByAdPlatform(adPlatformEnum: AdPlatformTypeEnum) {
        lastLoadedInterstitialsDateByAdPlatform[adPlatformEnum.name] = Date()
    }

    fun isValidLoadedInterstitial(adPlatformEnum: AdPlatformTypeEnum): Boolean {
        var isValid = true

        val lastLoadedDate = lastLoadedInterstitialsDateByAdPlatform.get(adPlatformEnum.name)
        if (lastLoadedDate != null) {
            val now = Date()
            val elapsedSeconds = (now.time - lastLoadedDate.time) / 1000
            isValid = elapsedSeconds < loadedInterstitialAvailableDuration
        }

        return isValid
    }

    fun updateLastLoadRewardedDateByAdPlatform(adPlatformEnum: AdPlatformTypeEnum) {
        lastLoadedRewardedDateByAdPlatform[adPlatformEnum.name] = Date()
    }

    fun isValidLoadedRewarded(adPlatformEnum: AdPlatformTypeEnum): Boolean {
        var isValid = true

        val lastLoadedDate = lastLoadedRewardedDateByAdPlatform.get(adPlatformEnum.name)
        if (lastLoadedDate != null) {
            val now = Date()
            val elapsedSeconds = (now.time - lastLoadedDate.time) / 1000
            isValid = elapsedSeconds < loadedRewardedAvailableDuration
        }

        return isValid
    }


    fun updateLastLoadBannerDateByAdPlatform(adPlatformEnum: AdPlatformTypeEnum) {
        lastLoadedBannerDateByAdPlatform[adPlatformEnum.name] = Date()
    }

    fun isValidLoadedBanner(adPlatformEnum: AdPlatformTypeEnum): Boolean {
        var isValid = true

        val lastLoadedDate = lastLoadedBannerDateByAdPlatform.get(adPlatformEnum.name)
        if (lastLoadedDate != null) {
            val now = Date()
            val elapsedSeconds = (now.time - lastLoadedDate.time) / 1000
            isValid = elapsedSeconds < loadedBannerAvailableDuration
        }

        return isValid
    }


    fun updateLastLoadMrecDateByAdPlatform(adPlatformEnum: AdPlatformTypeEnum) {
        lastLoadedMrecDateByAdPlatform[adPlatformEnum.name] = Date()
    }

    fun isValidLoadedMrec(adPlatformEnum: AdPlatformTypeEnum): Boolean {
        var isValid = true

        val lastLoadedDate = lastLoadedMrecDateByAdPlatform.get(adPlatformEnum.name)
        if (lastLoadedDate != null) {
            val now = Date()
            val elapsedSeconds = (now.time - lastLoadedDate.time) / 1000
            isValid = elapsedSeconds < loadedMrecAvailableDuration
        }

        return isValid
    }

}