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
import com.applovin.sdk.AppLovinMediationProvider
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkConfiguration
import com.applovin.sdk.AppLovinSdkInitializationConfiguration
import com.applovin.sdk.AppLovinSdkUtils


/**
 * appId is equal with applovin SDK-key
 */
class ApplovinAds(override var appId: String) : AdPlatformWrapper(appId) {

    override var platformType = AdPlatformTypeEnum.APPLOVIN
    var adIntances: MutableMap<String, PlatformAdInstance?> = mutableMapOf()

    override fun initialize(activity: Activity, onInitializeComplete: ((Boolean) -> Unit)?, testMode: Boolean) {
    }

    override fun initialize(context: Context, onInitializeComplete: ((Boolean) -> Unit)?, testMode: Boolean) {
        if (!beginInitialization(onInitializeComplete)) return

        val initConfig = AppLovinSdkInitializationConfiguration.builder(appId)
            .setMediationProvider(AppLovinMediationProvider.MAX)
            .build()

        AppLovinSdk.getInstance(context).initialize(initConfig) { completeInitialization() }
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


        val applovinInterstitialIns = MaxInterstitialAd(placementId)
        applovinInterstitialIns.setListener(object : MaxAdListener {
            override fun onAdLoaded(ad: MaxAd) {
                updateLastLoadInterstitialDateByAdPlatform(platformType)
                listener?.onLoaded(platformType)
            }


            override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                adIntances[placementId] = null
                listener?.onPlatformError(platformError(AdFormatEnum.INTERSTITIAL, placementId, "${platformType.name} interstitial >> error code=${error.code} / ${error.message}"))
            }

            override fun onAdDisplayed(ad: MaxAd) {

            }

            override fun onAdHidden(ad: MaxAd) {

            }

            override fun onAdClicked(ad: MaxAd) {

            }

            override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {

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
            listener?.onPlatformError(platformError(AdFormatEnum.INTERSTITIAL, placementId, "${platformType.name} interstitial >> noads loaded"))
            return
        }

        val interstitial: MaxInterstitialAd? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as MaxInterstitialAd? else null

        interstitial?.setListener(object : MaxAdListener {
            override fun onAdLoaded(ad: MaxAd) {

            }

            override fun onAdDisplayed(ad: MaxAd) {
                listener?.onDisplayed(platformType)
            }

            override fun onAdHidden(ad: MaxAd) {
                adIntances[placementId] = null
                listener?.onClosed(platformType)
            }

            override fun onAdClicked(ad: MaxAd) {
                listener?.onClicked(platformType)
            }

            override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                adIntances[placementId] = null
                listener?.onPlatformError(platformError(AdFormatEnum.INTERSTITIAL, placementId, "${platformType.name} interstitial show >> error code=${error.code} / ${error.message}"))
            }

            override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
                adIntances[placementId] = null
                listener?.onPlatformError(platformError(AdFormatEnum.INTERSTITIAL, placementId, "${platformType.name} interstitial show >> error code=${error.code} / ${error.message}"))
            }

        })
        interstitial?.showAd(shownWhere, activity)
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

    // MaxRewardedAd is one shared instance per ad unit with a single listener slot. Callbacks are routed per placement
    // so a load started while an ad is on screen can't replace (and swallow) that ad's reward/close callbacks.
    private val rewardedLoadListeners = mutableMapOf<String, MutableList<AdPlatformLoadListener>>()
    private val rewardedShowListeners = mutableMapOf<String, AdPlatformShowListener>()

    private fun rewardedListener(placementId: String) = object : MaxRewardedAdListener {
        override fun onAdLoaded(ad: MaxAd) {
            updateLastLoadRewardedDateByAdPlatform(platformType)
            rewardedLoadListeners.remove(placementId)?.forEach { it.onLoaded(platformType) }
        }

        override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
            adIntances[placementId] = null
            rewardedLoadListeners.remove(placementId)?.forEach {
                it.onPlatformError(platformError(AdFormatEnum.REWARDED, placementId, "${platformType.name} rewarded load >> error code=${error.code} / ${error.message}"))
            }
        }

        override fun onAdDisplayed(ad: MaxAd) {
            rewardedShowListeners[placementId]?.onDisplayed(platformType)
        }

        override fun onAdHidden(ad: MaxAd) {
            rewardedShowListeners.remove(placementId)?.onClosed(platformType)
        }

        override fun onAdClicked(ad: MaxAd) {}

        override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
            rewardedShowListeners.remove(placementId)
                ?.onPlatformError(platformError(AdFormatEnum.REWARDED, placementId, "${platformType.name} rewarded show >> error code=${error.code} / ${error.message}"))
        }

        override fun onUserRewarded(ad: MaxAd, reward: MaxReward) {
            rewardedShowListeners[placementId]?.onRewarded(reward.label, reward.amount, platformType)
        }
    }

    override fun loadRewarded(activity: Activity, placementId: String, listener: AdPlatformLoadListener?) {
        if (isRewardedLoaded(placementId)) {
            listener?.onLoaded(platformType)
            return
        }

        adIntances[placementId] = null

        val rewardedAd = MaxRewardedAd.getInstance(placementId)
        listener?.let { rewardedLoadListeners.getOrPut(placementId) { mutableListOf() }.add(it) }
        rewardedAd.setListener(rewardedListener(placementId))
        rewardedAd.loadAd()
        adIntances[placementId] = PlatformAdInstance(
            AdFormatEnum.REWARDED,
            placementId,
            rewardedAd
        )

    }

    override fun showRewarded(activity: Activity, placementId: String, shownWhere: String, listener: AdPlatformShowListener?) {
        if (!isRewardedLoaded(placementId)) {
            listener?.onPlatformError(platformError(AdFormatEnum.REWARDED, placementId, "${platformType.name} rewarded >> noadsloaded"))
            return
        }

        val rewardedAd = adIntances[placementId]?.instance as MaxRewardedAd
        if (listener != null) rewardedShowListeners[placementId] = listener else rewardedShowListeners.remove(placementId)
        rewardedAd.setListener(rewardedListener(placementId))
        rewardedAd.showAd(shownWhere, activity)
        adIntances[placementId] = null

    }


    // ################################################################################
    // ################################### BANNER #####################################
    // ################################################################################
    override fun isBannerLoaded(placementId: String): Boolean {
        val bannerAdView: MaxAdView? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as MaxAdView? else null

        var isLoaded = _isBannerLoaded(bannerAdView)
        /*if (isLoaded && !isValidLoadedBanner(platformType)) {
            _removeBannerViewIfExists(bannerAdView)
            adIntances[placementId] = null
            isLoaded = false
        }*/

        return isLoaded
    }

    override fun showBanner(activity: Activity, placementId: String, containerView: RelativeLayout, listener: AdPlatformShowListener?) {

        var bannerAdView: MaxAdView? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as MaxAdView? else null

        /*val lp = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            .apply {
                addRule(RelativeLayout.CENTER_HORIZONTAL)
            }*/

        if (isBannerLoaded(placementId)) {
            try {
                _removeBannerViewIfExists(bannerAdView)
                containerView.addView(bannerAdView)
                listener?.onDisplayed(platformType)
            } catch (e: Exception) {
                listener?.onPlatformError(platformError(AdFormatEnum.BANNER, placementId, "${platformType.name} banner >> isbannerloaded"))
            }
            return
        }

        bannerAdView = MaxAdView(placementId)
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
            override fun onAdLoaded(ad: MaxAd) {
                adIntances[placementId] = PlatformAdInstance(
                    AdFormatEnum.BANNER,
                    placementId,
                    bannerAdView
                )
            }

            override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                if (!isBannerLoaded(placementId)){
                    adIntances[placementId] = null
                    activity.runOnUiThread {
                        _removeBannerViewIfExists(bannerAdView, containerView)
                    }

                    listener?.onPlatformError(platformError(AdFormatEnum.BANNER, placementId, "${platformType.name} banner >> error code=${error.code} / ${error.message}"))
                }
            }

            override fun onAdDisplayed(ad: MaxAd) {
                listener?.onDisplayed(platformType)
            }

            override fun onAdHidden(ad: MaxAd) {

            }

            override fun onAdClicked(ad: MaxAd) {
                listener?.onClicked(platformType)
            }

            override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {

            }

            override fun onAdExpanded(ad: MaxAd) {

            }

            override fun onAdCollapsed(ad: MaxAd) {

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
        /*if (isLoaded && !isValidLoadedMrec(platformType)) {
            _removeBannerViewIfExists(mrecAdView)
            adIntances[placementId] = null
            isLoaded = false
        }*/

        return isLoaded
    }

    override fun showMrec(activity: Activity, placementId: String, containerView: RelativeLayout, listener: AdPlatformShowListener?) {

        var mrecAdView: MaxAdView? = if (adIntances.containsKey(placementId)) adIntances.get(placementId)?.instance as MaxAdView? else null

        /*val lp = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            .apply {
                addRule(RelativeLayout.CENTER_HORIZONTAL)
            }*/

        if (isMrecLoaded(placementId)) {
            try {
                _removeBannerViewIfExists(mrecAdView)
                containerView.addView(mrecAdView)
                listener?.onDisplayed(platformType)
            } catch (e: Exception) {
                listener?.onPlatformError(platformError(AdFormatEnum.MREC, placementId, "${platformType.name} mrec >> ismrecloaded"))
            }
            return
        }

        mrecAdView = MaxAdView(placementId)
        // bannerAdView.id = ViewCompat.generateViewId()

        val widthPx = AppLovinSdkUtils.dpToPx(activity, 300)
        val heightPx = AppLovinSdkUtils.dpToPx(activity, 250)

        mrecAdView?.layoutParams = RelativeLayout.LayoutParams(widthPx, heightPx).apply {
            addRule(RelativeLayout.CENTER_HORIZONTAL)
        }


        mrecAdView.setListener(object : MaxAdViewAdListener {
            override fun onAdLoaded(ad: MaxAd) {
                adIntances[placementId] = PlatformAdInstance(
                    AdFormatEnum.MREC,
                    placementId,
                    mrecAdView
                )
            }

            override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                if (!isMrecLoaded(placementId)){
                    adIntances[placementId] = null
                    activity.runOnUiThread {
                        _removeBannerViewIfExists(mrecAdView, containerView)
                    }

                    listener?.onPlatformError(platformError(AdFormatEnum.MREC, placementId, "${platformType.name} mrec >> error code=${error.code} / ${error.message}"))
                }
            }


            override fun onAdDisplayed(ad: MaxAd) {
                listener?.onDisplayed(platformType)
            }

            override fun onAdHidden(ad: MaxAd) {

            }

            override fun onAdClicked(ad: MaxAd) {
                listener?.onClicked(platformType)
            }

            override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {

            }

            override fun onAdExpanded(ad: MaxAd) {

            }

            override fun onAdCollapsed(ad: MaxAd) {

            }

        })
        _removeBannerViewIfExists(mrecAdView, containerView)
        containerView.addView(mrecAdView)

        mrecAdView.loadAd()
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