package com.helikanonlibsample.simple_admanager

import android.app.Application
import android.util.Log
import com.helikanonlib.simpleadmanager.*

class MyApplication : Application() {


    companion object {
        lateinit var adManager: SimpleAdManager
        var admobAppOpenAdManager: AppOpenAdManager? = null
    }

    override fun onCreate() {
        super.onCreate()

        initAdManager()
        initAppOpenAds()
    }

    fun initAdManager() {
        adManager = SimpleAdManager().apply {

            showAds = true
            autoLoadForInterstitial = false
            isEnabledLoadAndShowIfNotExistsAdsOnAutoloadMode = true
            autoLoadDelay = 11 // seconds
            autoLoadForRewarded = true

            randomInterval = 30 // random seconds for showing interstitial. Interstitial will show after previous showing passed seconds between 60-90
            interstitialMinElapsedSecondsToNextShow = 30
            rewardedMinElapsedSecondsToNextShow = 30

            testMode = BuildConfig.DEBUG
            deviceId = "47088e48-5195-4757-90b2-0da94116befd" // necessary if testmode enabled


            placementGroups = mutableListOf(
                AdPlacementGroup(
                    groupCode = SimpleAdManager.DEFAULT_PLACEMENT_GROUPCODE,
                    platformType = AdPlatformTypeEnum.ADMOB,
                    interstitial = "ca-app-pub-3940256099942544/1033173712",
                    rewarded = "ca-app-pub-3940256099942544/5224354917",
                    banner = "ca-app-pub-3940256099942544/6300978112",
                    mrec = "ca-app-pub-3940256099942544/6300978111",
                    nativeSmall = "ca-app-pub-3940256099942544/2247696110",
                    nativeMedium = "",
                    appOpenAd = "ca-app-pub-3940256099942544/9257395921",
                ),
                AdPlacementGroup(
                    groupCode = SimpleAdManager.DEFAULT_PLACEMENT_GROUPCODE,
                    platformType = AdPlatformTypeEnum.APPLOVIN,
                    interstitial = "7c4a01242eaee289",
                    rewarded = "f2f5534658a6b4ab",
                    banner = "609a039e1d803bea",
                    mrec = "851a6927fdae17d5",
                    nativeSmall = "2b1686bf9db060d3",
                    nativeMedium = "96454048eeffaad2",
                    appOpenAd = "dd9249369deec4ec"
                ),
                AdPlacementGroup(
                    groupCode = SimpleAdManager.DEFAULT_PLACEMENT_GROUPCODE,
                    platformType = AdPlatformTypeEnum.IRONSOURCE,
                    interstitial = "DefaultInterstitial",
                    rewarded = "DefaultRewardedVideo",
                    banner = "DefaultBanner",
                    mrec = "MREC_BANNER",
                    nativeSmall = "nosupportSmall",
                    nativeMedium = "nosupportMedium",
                    appOpenAd = "nosupport"
                ),

                AdPlacementGroup(
                    groupCode = "second",
                    platformType = AdPlatformTypeEnum.ADMOB,
                    interstitial = "ca-app-pub-3940256099942544/1033173712",
                    rewarded = "ca-app-pub-3940256099942544/5224354917",
                    banner = "ca-app-pub-3940256099942544/6300978111",
                    mrec = "ca-app-pub-3940256099942544/6300978111",
                    nativeSmall = "ca-app-pub-3940256099942544/2247696110",
                    nativeMedium = "",
                    appOpenAd = "ca-app-pub-3940256099942544/3419835294",
                )
            )

            adPlatforms = mutableListOf<AdPlatformWrapper>(
                AdmobAds("ca-app-pub-3940256099942544~3347511713"),
                ApplovinAds("noneed")
            )
        }

        // default placement group
        adManager.setAdPlatformSortByAdFormatStr(SimpleAdManager.DEFAULT_PLACEMENT_GROUPCODE, AdFormatEnum.INTERSTITIAL.name, "admob,applovin")
        adManager.setAdPlatformSortByAdFormatStr(SimpleAdManager.DEFAULT_PLACEMENT_GROUPCODE, AdFormatEnum.REWARDED.name, "applovin,admob")
        adManager.setAdPlatformSortByAdFormatStr(SimpleAdManager.DEFAULT_PLACEMENT_GROUPCODE, AdFormatEnum.BANNER.name, "applovin,admob")
        adManager.setAdPlatformSortByAdFormatStr(SimpleAdManager.DEFAULT_PLACEMENT_GROUPCODE, AdFormatEnum.MREC.name, "applovin,admob")
        adManager.setAdPlatformSortByAdFormatStr(SimpleAdManager.DEFAULT_PLACEMENT_GROUPCODE, AdFormatEnum.NATIVE_SMALL.name, "applovin,admob")
        adManager.setAdPlatformSortByAdFormatStr(SimpleAdManager.DEFAULT_PLACEMENT_GROUPCODE, AdFormatEnum.NATIVE_MEDIUM.name, "applovin,admob")
        adManager.setAdPlatformSortByAdFormatStr(SimpleAdManager.DEFAULT_PLACEMENT_GROUPCODE, AdFormatEnum.APP_OPEN.name, "applovin,admob")


        adManager.isEnableShowLoadingViewForInterstitial = false
        adManager.setAdPlatformSortByAdFormatStr("second", AdFormatEnum.INTERSTITIAL.name, "admob")



        adManager.globalInterstitialLoadListener = object : AdPlatformLoadListener() {
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                if (errorMode == AdErrorMode.MANAGER) {
                    Log.d("adManager", "[LOAD][INTERSTITIAL] AdErrorMode.MANAGER globalInterstitialLoadListener > $errorMessage")
                } else {
                    Log.d("adManager", "[LOAD][INTERSTITIAL] AdErrorMode.PLATFORM globalInterstitialLoadListener > $errorMessage ${adPlatformEnum?.name}")
                }
            }
        }
        adManager.globalRewardedLoadListener = object : AdPlatformLoadListener() {
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                if (errorMode == AdErrorMode.MANAGER) {
                    Log.d("adManager", "[LOAD][REWARDED] AdErrorMode.MANAGER globalRewardedLoadListener > $errorMessage")
                } else {
                    Log.d("adManager", "[LOAD][REWARDED] AdErrorMode.PLATFORM globalRewardedLoadListener > $errorMessage ${adPlatformEnum?.name}")
                }
            }
        }

        adManager.globalInterstitialShowListener = object : AdPlatformShowListener() {
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                if (errorMode == AdErrorMode.MANAGER) {
                    Log.d("adManager", "[SHOW][INTERSTITIAL] AdErrorMode.MANAGER globalInterstitialShowListener > $errorMessage")
                } else {
                    Log.d("adManager", "[SHOW][INTERSTITIAL] AdErrorMode.PLATFORM globalInterstitialShowListener > $errorMessage ${adPlatformEnum?.name}")
                }
            }
        }

        adManager.globalRewardedShowListener = object : AdPlatformShowListener() {
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                // AdErrorMode.MANAGER >> it means . We tried to load in all platforms but no one load interstitial
                if (errorMode == AdErrorMode.MANAGER) {
                    Log.d("adManager", "[SHOW][REWARDED] AdErrorMode.MANAGER globalRewardedShowListener > $errorMessage")
                } else {
                    Log.d("adManager", "[SHOW][REWARDED] AdErrorMode.PLATFORM globalRewardedShowListener > $errorMessage ${adPlatformEnum?.name}")
                }

            }
        }
        adManager.initializePlatforms(applicationContext)
    }

    fun initAppOpenAds() {
        admobAppOpenAdManager = AppOpenAdManager(
            this,
            mutableMapOf(
                AdPlatformTypeEnum.ADMOB to "ca-app-pub-3940256099942544/9257395921",
                AdPlatformTypeEnum.APPLOVIN to "dd9249369deec4ec",
            ),
            "admob,applovin",

            object : AdPlatformShowListener() {
                override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum?) {
                    Log.e("adManager", "AdmobAppOpenAdManager >>> success display")
                }

                override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {

                    Log.e("adManager", "AdmobAppOpenAdManager show error >>> $errorMessage")
                }

            },
            object : AdPlatformLoadListener() {
                override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum?) {
                    Log.e("adManager", "AdmobAppOpenAdManager >>> success load")
                }

                override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                    Log.e("adManager", "AdmobAppOpenAdManager load error >>> $errorMessage")
                }

            }
        )

        // admobAppOpenAdManager?.excludedActivities?.add(JavaSampleActivity::class.java.simpleName)
        admobAppOpenAdManager?.minElapsedSecondsToNextShow = 10
        admobAppOpenAdManager?.disable()
    }

}