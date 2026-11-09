package com.helikanonlibsample.simple_admanager

import android.app.Application
import android.util.Log
import com.helikanonlib.simpleadmanager.*

class MyApplication : Application() {

    private lateinit var appOpenLifecycleController: AppOpenLifecycleController

    companion object {
        lateinit var adManager: SimpleAdManager
        var admobAppOpenAdManager: AppOpenAdManager? = null
    }

    override fun onCreate() {
        super.onCreate()

        initAdManager()
        initAppOpenAds()

        adManager.initializePlatforms(
            applicationContext,
            onInitializeComplete = { result ->
                Log.d("adManager", "initialization ${result.status} failed=${result.failedPlatforms}")
                if (result.status == AdInitializationStatus.SUCCESS ||
                    result.status == AdInitializationStatus.PARTIAL_SUCCESS
                ) {
                    admobAppOpenAdManager?.load()
                }
            },
            onPlatformInitializeComplete = { result ->
                Log.d("adManager", "${result.platform.name} initialization success=${result.isSuccessful}")
            }
        )
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


            setPlacementGroups(
                listOf(
                    AdPlacementGroup(
                        groupCode = SimpleAdManager.DEFAULT_PLACEMENT_GROUPCODE,
                        platformType = AdPlatformTypeEnum.ADMOB,
                        interstitial = "ca-app-pub-3940256099942544/1033173712",
                        rewarded = "ca-app-pub-3940256099942544/5224354917",
                        banner = "ca-app-pub-3940256099942544/9214589741",
                        mrec = "ca-app-pub-3940256099942544/6300978111",
                        appOpenAd = "ca-app-pub-3940256099942544/9257395921",
                    ),
                    AdPlacementGroup(
                        groupCode = SimpleAdManager.DEFAULT_PLACEMENT_GROUPCODE,
                        platformType = AdPlatformTypeEnum.APPLOVIN,
                        interstitial = "7c4a01242eaee289",
                        rewarded = "f2f5534658a6b4ab",
                        banner = "609a039e1d803bea",
                        mrec = "851a6927fdae17d5",
                        appOpenAd = "dd9249369deec4ec"
                    ),

                    AdPlacementGroup(
                        groupCode = "second",
                        platformType = AdPlatformTypeEnum.ADMOB,
                        interstitial = "ca-app-pub-3940256099942544/1033173712",
                        rewarded = "ca-app-pub-3940256099942544/5224354917",
                        banner = "ca-app-pub-3940256099942544/9214589741",
                        mrec = "ca-app-pub-3940256099942544/6300978111",
                        appOpenAd = "ca-app-pub-3940256099942544/9257395921",
                    )
                )
            )

            setAdPlatforms(
                listOf(
                    AdmobAds("ca-app-pub-3940256099942544~3347511713"),
                    ApplovinAds("pHS9IvJQm4f3s9fPw3xpJLFIu6lLDfukm72lv_OqABUrmgxiCnDKCx4vAPNHI9lOv3oDcJZngd-ek0cHus5pBP")
                )
            )
        }

        // default placement group (returns a result; isSuccessful=false means the order was rejected)
        adManager.setAdPlatformSortByAdFormatStr(SimpleAdManager.DEFAULT_PLACEMENT_GROUPCODE, AdFormatEnum.INTERSTITIAL.name, "admob,applovin")
        adManager.setAdPlatformSortByAdFormatStr(SimpleAdManager.DEFAULT_PLACEMENT_GROUPCODE, AdFormatEnum.REWARDED.name, "applovin,admob")
        adManager.setAdPlatformSortByAdFormatStr(SimpleAdManager.DEFAULT_PLACEMENT_GROUPCODE, AdFormatEnum.BANNER.name, "admob,applovin")
        adManager.setAdPlatformSortByAdFormatStr(SimpleAdManager.DEFAULT_PLACEMENT_GROUPCODE, AdFormatEnum.MREC.name, "applovin,admob")


        adManager.isEnableShowLoadingViewForInterstitial = false
        val secondGroupOrder = adManager.setAdPlatformSortByAdFormatStr("second", AdFormatEnum.INTERSTITIAL.name, "admob")
        if (!secondGroupOrder.isSuccessful) {
            Log.e("adManager", "second group order rejected: ${secondGroupOrder.message}")
        }


        adManager.globalInterstitialLoadListener = object : AdPlatformLoadListener() {
            override fun onPlatformError(error: AdPlatformError) {
                Log.d("adManager", "[LOAD][INTERSTITIAL] platform error ${error.platform.name} > ${error.message}")
            }

            override fun onError(error: AdManagerError) {
                Log.d("adManager", "[LOAD][INTERSTITIAL] manager error > ${error.message}")
            }
        }
        adManager.globalRewardedLoadListener = object : AdPlatformLoadListener() {
            override fun onPlatformError(error: AdPlatformError) {
                Log.d("adManager", "[LOAD][REWARDED] platform error ${error.platform.name} > ${error.message}")
            }

            override fun onError(error: AdManagerError) {
                Log.d("adManager", "[LOAD][REWARDED] manager error > ${error.message}")
            }
        }

        adManager.globalInterstitialShowListener = object : AdPlatformShowListener() {
            override fun onPlatformError(error: AdPlatformError) {
                Log.d("adManager", "[SHOW][INTERSTITIAL] platform error ${error.platform.name} > ${error.message}")
            }

            override fun onError(error: AdManagerError) {
                Log.d("adManager", "[SHOW][INTERSTITIAL] manager error > ${error.message}")
            }
        }

        adManager.globalRewardedShowListener = object : AdPlatformShowListener() {
            override fun onPlatformError(error: AdPlatformError) {
                Log.d("adManager", "[SHOW][REWARDED] platform error ${error.platform.name} > ${error.message}")
            }

            override fun onError(error: AdManagerError) {
                // terminal: the manager tried every platform (or nothing was loaded)
                Log.d("adManager", "[SHOW][REWARDED] manager error > ${error.message}")
            }
        }
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
                override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum) {
                    Log.e("adManager", "AppOpenAdManager >>> success display")
                }

                override fun onPlatformError(error: AdPlatformError) {
                    Log.e("adManager", "AppOpenAdManager platform show error >>> ${error.message}")
                }

                override fun onError(error: AdManagerError) {
                    Log.e("adManager", "AppOpenAdManager show error >>> ${error.message}")
                }

            },
            object : AdPlatformLoadListener() {
                override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum) {
                    Log.e("adManager", "AppOpenAdManager >>> success load")
                }

                override fun onPlatformError(error: AdPlatformError) {
                    Log.e("adManager", "AppOpenAdManager platform load error >>> ${error.message}")
                }

                override fun onError(error: AdManagerError) {
                    Log.e("adManager", "AppOpenAdManager load error >>> ${error.message}")
                }

            }
        )

        // admobAppOpenAdManager?.excludedActivities?.add(JavaSampleActivity::class.java.simpleName)
        admobAppOpenAdManager?.minElapsedSecondsToNextShow = 10
        // Interstitial veya Rewarded kapandıktan sonra App Open göstermeden önce beklenecek süre.
        admobAppOpenAdManager?.minElapsedSecondsAfterFullScreenAd = 30

        // shows app open ads when the app comes back to foreground (not on cold start)
        appOpenLifecycleController = AppOpenLifecycleController(this) {
            admobAppOpenAdManager
        }.also { it.register() }
    }

}
