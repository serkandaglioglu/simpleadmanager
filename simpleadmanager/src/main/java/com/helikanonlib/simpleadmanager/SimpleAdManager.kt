package com.helikanonlib.simpleadmanager

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.ViewCompat
import java.util.*
import java.util.concurrent.TimeUnit

fun CharSequence.splitIgnoreEmpty(vararg delimiters: String): List<String> {
    return this.split(*delimiters).filter {
        it.isNotEmpty()
    }
}

class SimpleAdManager {

    var testMode: Boolean = false
    var deviceId: String = ""
    var showAds: Boolean = true
    var autoLoadForInterstitial: Boolean = false
    var autoLoadForRewarded: Boolean = true
    var autoLoadDelay: Long = 10
    var randomInterval: Int = 40
    var interstitialMinElapsedSecondsToNextShow: Int = 40
    var rewardedMinElapsedSecondsToNextShow: Int = 40
    var isEnabledLoadAndShowIfNotExistsAdsOnAutoloadMode = false

    var globalInterstitialShowListener: AdPlatformShowListener? = null
    var globalRewardedShowListener: AdPlatformShowListener? = null
    var globalInterstitialLoadListener: AdPlatformLoadListener? = null
    var globalRewardedLoadListener: AdPlatformLoadListener? = null

    var adPlatforms: MutableList<AdPlatformWrapper> = mutableListOf<AdPlatformWrapper>()
    var adPlatformSortByAdFormat: MutableMap<String, List<AdPlatformTypeEnum>> = mutableMapOf()
    var lastShowDateByAdFormat = mutableMapOf<AdFormatEnum, Date>()

    // PLACEMENT GROUPS
    var defaultPlacementGroupCode = "default"
    var placementGroups: MutableList<AdPlacementGroup> = mutableListOf()
    fun getPlacementGroupByCodeAndPlatform(placementGroupCode: String, platformTypeEnum: AdPlatformTypeEnum): AdPlacementGroup? {
        var pg: AdPlacementGroup? = null

        run breaker@{
            placementGroups.forEach {
                if (it.groupCode == placementGroupCode && it.platformType == platformTypeEnum) {
                    pg = it
                    return@breaker
                }

                Log.e("SimpleAdManager", it.groupCode)
            }
        }

        return pg
    }


    constructor() {
        initHandlers()
    }

    fun initializePlatformsWithActivity(activity: Activity) {
        if (!showAds) return

        adPlatforms.forEach forEach@{ platform ->
            platform.initialize(activity, testMode)

            if (testMode) {
                platform.enableTestMode(activity.applicationContext, deviceId)
            }
        }
    }

    fun initializePlatforms(context: Context) {
        if (!showAds) return

        adPlatforms.forEach forEach@{ platform ->
            platform.initialize(context, testMode)

            if (testMode) {
                platform.enableTestMode(context, deviceId)
            }
        }
    }

    /*fun start(activity: Activity) {
        if (autoLoadForInterstitial) {
            placementGroups.forEachIndexed { index, pgName ->
                if (index > 0) return@forEachIndexed
                loadInterstitial(activity, listener = null, platform = null, parallel = false, placementGroupIndex = getPlacementGroupIndexByName(pgName))
            }

        }

        if (autoLoadForRewarded) {
            placementGroups.forEachIndexed { index, pgName ->
                if (index > 0) return@forEachIndexed
                loadRewarded(activity, null, null, false, getPlacementGroupIndexByName(pgName))
            }
        }
    }

    fun enableTestMode(activity: Activity, deviceId: String) {

        this.testMode = true
        this.deviceId = deviceId

        adPlatforms.forEach forEach@{ platform ->
            if (testMode) {
                platform.platformInstance.enableTestMode(activity.applicationContext, deviceId)
            }
        }
    }*/


    fun enableTestMode(activity: Activity, deviceId: String) {

        this.testMode = true
        this.deviceId = deviceId

        placementGroups.forEach {
            if (it.platformType == AdPlatformTypeEnum.ADMOB) {
                it.interstitial = "ca-app-pub-3940256099942544/1033173712"
                it.banner = "ca-app-pub-3940256099942544/6300978111"
                it.rewarded = "ca-app-pub-3940256099942544/5224354917"
                it.mrec = "ca-app-pub-3940256099942544/6300978111"
                it.nativeSmall = "ca-app-pub-3940256099942544/2247696110"
                it.nativeMedium = "ca-app-pub-3940256099942544/2247696110"
                it.appOpenAd = "ca-app-pub-3940256099942544/3419835294"
            }
        }


    }

    fun addAdPlatform(adPlatform: AdPlatformWrapper) = apply { this.adPlatforms.add(adPlatform) }
    fun getAdPlatformByType(platformType: AdPlatformTypeEnum): AdPlatformWrapper? {
        val filteredPlatforms = adPlatforms.filter { it -> it.platformType == platformType }
        return if (filteredPlatforms.size > 0) filteredPlatforms[0] else null
    }

    /**
     * example :
     * adManager.setAdPlatformSortByAdFormatStr("interstitial", "ironsource,mopub,admob,facebook")
     * adManager.setAdPlatformSortByAdFormatStr("banner", "ironsource,facebook,admob,startapp,mopub")
     */
    fun setAdPlatformSortByAdFormatStr(placementGroupCode: String, adFormatName: String, adPlatformsStr: String) {
        try {
            val _afPlatformsArr = adPlatformsStr.splitIgnoreEmpty(",").map {
                if (!it.isNullOrEmpty()) {
                    AdPlatformTypeEnum.valueOf(it.trim().uppercase(Locale.ENGLISH))
                } else {
                    AdPlatformTypeEnum.valueOf(it.trim().uppercase(Locale.ENGLISH))
                }
            }

            adPlatformSortByAdFormat.put(
                placementGroupCode + "__" + adFormatName.uppercase(Locale.ENGLISH), _afPlatformsArr
            )
        } catch (e: Exception) {
            Log.e("AdManager", "setAdPlatformSortByAdFormatStr >> ${e.message}")
        }
    }


    private fun _getAdPlatformsWithSortedByAdFormat(placementGroupCode: String, adFormatEnum: AdFormatEnum): MutableList<AdPlatformWrapper> {
        val filteredAdPlatforms = mutableListOf<AdPlatformWrapper>()
        val adFormatSort = adPlatformSortByAdFormat[placementGroupCode + "__" + adFormatEnum.name.uppercase(Locale.ENGLISH)]

        if (adFormatSort == null) {
            throw Exception("SimpleAdManager >> empty adFormatSort var. [" + placementGroupCode + "__" + adFormatEnum.name.uppercase(Locale.ENGLISH) + "]")
        }

        adFormatSort.forEach { adPlatformType ->
            getAdPlatformByType(adPlatformType)?.let {
                filteredAdPlatforms.add(it)
            }
        }

        return filteredAdPlatforms
    }


    // ################################################################################
    // ################################### INTERSTITIAL ###############################
    // ################################################################################
    @JvmOverloads
    fun loadInterstitial(activity: Activity, placementGroupCode: String = defaultPlacementGroupCode, listener: AdPlatformLoadListener? = null, platformType: AdPlatformTypeEnum? = null, parallel: Boolean = false) {
        if (!showAds) return

        if (platformType == null) {
            if (parallel) {
                // send true parallel when first load after init ads. Else you dont need parallel load
                val interstitialAdPlatforms = _getAdPlatformsWithSortedByAdFormat(placementGroupCode, AdFormatEnum.INTERSTITIAL)
                interstitialAdPlatforms.forEach forEach@{ _platform ->
                    val placementId = getPlacementGroupByCodeAndPlatform(placementGroupCode, _platform.platformType)?.interstitial
                    if (placementId == null) return@forEach

                    if (!_platform.isInterstitialLoaded(placementId)) {
                        _loadInterstitial(activity, _platform.platformType, placementId, listener)
                    }
                }
            } else {
                _loadInterstitialFromFirstAvailable(activity, placementGroupCode, listener, 0)
            }
        } else {
            val platform = getAdPlatformByType(platformType)
            platform?.let {
                val placementId = getPlacementGroupByCodeAndPlatform(placementGroupCode, platform.platformType)?.interstitial
                if (placementId == null) {
                    throw Exception("_loadInterstitial >> placementId null error")
                }
                _loadInterstitial(activity, platform.platformType, placementId, listener = listener)

            }

        }
    }

    private fun _loadInterstitialFromFirstAvailable(activity: Activity, placementGroupCode: String, listener: AdPlatformLoadListener? = null, index: Int = 0) {
        val interstitialAdPlatforms = _getAdPlatformsWithSortedByAdFormat(placementGroupCode, AdFormatEnum.INTERSTITIAL)
        if (index >= interstitialAdPlatforms.size) {
            return
        }
        val platform = interstitialAdPlatforms[index]
        val placementId = getPlacementGroupByCodeAndPlatform(placementGroupCode, platform.platformType)?.interstitial

        val _listener = object : AdPlatformLoadListener() {
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                if ((index + 1) < interstitialAdPlatforms.size) {
                    activity.runOnUiThread { _loadInterstitialFromFirstAvailable(activity, placementGroupCode, listener, index + 1) }
                } else {
                    globalInterstitialLoadListener?.onError(AdErrorMode.MANAGER, "No interstitial found in all platforms", adPlatformEnum)
                    listener?.onError(AdErrorMode.MANAGER, "No interstitial found in all platforms", adPlatformEnum)
                }
                globalInterstitialLoadListener?.onError(errorMode, errorMessage, adPlatformEnum)
                listener?.onError(errorMode, errorMessage, adPlatformEnum)
            }

            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum?) {
                globalInterstitialLoadListener?.onLoaded(adPlatformEnum)
                listener?.onLoaded(adPlatformEnum)
            }
        }

        if (placementId == null) {
            throw Exception("_loadInterstitialFromFirstAvailable >> placementId null error")
        }
        platform.loadInterstitial(activity, placementId, _listener)
    }

    private fun _loadInterstitial(activity: Activity, platformType: AdPlatformTypeEnum, placementId: String, listener: AdPlatformLoadListener? = null) {
        val _listener = object : AdPlatformLoadListener() {
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                globalInterstitialLoadListener?.onError(errorMode, errorMessage, adPlatformEnum)
                listener?.onError(errorMode, errorMessage, adPlatformEnum)

                globalInterstitialLoadListener?.onError(AdErrorMode.MANAGER, errorMessage, adPlatformEnum)
                listener?.onError(AdErrorMode.MANAGER, errorMessage, adPlatformEnum)
            }

            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum?) {

                globalInterstitialLoadListener?.onLoaded(adPlatformEnum)
                listener?.onLoaded(adPlatformEnum)
            }
        }

        val platform = getAdPlatformByType(platformType)
        platform?.loadInterstitial(activity, placementId, _listener)
    }


    @JvmOverloads
    fun loadAndShowInterstitial(
        activity: Activity, shownWhere: String = "defaultPlace", placementGroupCode: String = defaultPlacementGroupCode,
        listener: AdPlatformShowListener? = null, platformType: AdPlatformTypeEnum? = null
    ) {

        if (isEnableShowLoadingViewForInterstitial) {
            activity.runOnUiThread {
                addLoadingViewToActivity(activity)
            }
        }

        val loadListener: AdPlatformLoadListener = object : AdPlatformLoadListener() {
            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum?) {
                // this listener will trigger just one time after firt load any platform
                _showInterstitial(activity, shownWhere, placementGroupCode, listener, platformType, false)
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                // it will come here for each ad platforms, so we wants only call _showInterstitial
                // after try all platforms
                // _showInterstitial will trigger user listener
                if (errorMode == AdErrorMode.MANAGER) {
                    _showInterstitial(activity, shownWhere, placementGroupCode, listener, platformType, false)
                }


            }
        }
        loadInterstitial(activity, placementGroupCode, loadListener, platformType, false)
    }

    @JvmOverloads
    fun showInterstitialForTimeStrategy(
        activity: Activity, shownWhere: String = "defaultPlace", placementGroupCode: String = defaultPlacementGroupCode,
        listener: AdPlatformShowListener? = null,
        platformType: AdPlatformTypeEnum? = null, loadAndShowIfNotExistsAdsOnAutoloadMode: Boolean = true
    ) {
        if (!showAds) return

        var isAvailableToShow = true
        val lastShowDate = lastShowDateByAdFormat.get(AdFormatEnum.INTERSTITIAL)

        if (lastShowDate != null) {
            val now = Date()
            val elapsedSeconds = (now.time - lastShowDate.time) / 1000
            val requiredElapsedTime = randInt(0, randomInterval) + interstitialMinElapsedSecondsToNextShow
            isAvailableToShow = elapsedSeconds > requiredElapsedTime
        }

        if (isAvailableToShow) {
            showInterstitial(activity, shownWhere, placementGroupCode, listener, platformType, loadAndShowIfNotExistsAdsOnAutoloadMode)
        }
    }

    @JvmOverloads
    fun showInterstitial(
        activity: Activity, shownWhere: String = "defaultPlace", placementGroupCode: String = defaultPlacementGroupCode,
        listener: AdPlatformShowListener? = null,
        platformType: AdPlatformTypeEnum? = null, loadAndShowIfNotExistsAdsOnAutoloadMode: Boolean = true
    ) {
        if (!showAds) return

        if (autoLoadForInterstitial) {
            val isShowed = _showInterstitial(activity, shownWhere, placementGroupCode, listener, platformType, loadAndShowIfNotExistsAdsOnAutoloadMode)
        } else {

            activity.runOnUiThread {
                addLoadingViewToActivity(activity)
            }
            loadAndShowInterstitial(activity, shownWhere, placementGroupCode, listener, platformType)
        }
    }

    // load edilmiş reklamı göstermeye çalışır.
    private fun _showInterstitial(
        activity: Activity, shownWhere: String = "defaultPlace", placementGroupCode: String = defaultPlacementGroupCode,
        listener: AdPlatformShowListener? = null,
        platformType: AdPlatformTypeEnum? = null, loadAndShowIfNotExistsAdsOnAutoloadMode: Boolean = false
    ): Boolean {
        if (!showAds) return true

        val interstitialAdPlatforms = _getAdPlatformsWithSortedByAdFormat(placementGroupCode, AdFormatEnum.INTERSTITIAL)

        val _listener = object : AdPlatformShowListener() {
            override fun onClosed(adPlatformEnum: AdPlatformTypeEnum?) {

                activity.runOnUiThread {
                    removeLoadingViewFromActivity(activity)
                }

                globalInterstitialShowListener?.onClosed(adPlatformEnum)
                listener?.onClosed(adPlatformEnum)
                saveLastShowDate(AdFormatEnum.INTERSTITIAL)

                // on close load new one for next show
                if (autoLoadForInterstitial) {
                    _autoloadInterstitialByHandler(activity, placementGroupCode, null, platformType)
                }
            }

            override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum?) {
                if (adPlatformEnum != AdPlatformTypeEnum.STARTAPP) {
                    stopAutoloadInterstitialHandler()
                }

                globalInterstitialShowListener?.onDisplayed(adPlatformEnum)
                listener?.onDisplayed(adPlatformEnum)

                activity.runOnUiThread {
                    removeLoadingViewFromActivity(activity)
                }
            }

            override fun onClicked(adPlatformEnum: AdPlatformTypeEnum?) {
                globalInterstitialShowListener?.onClicked(adPlatformEnum)
                listener?.onClicked(adPlatformEnum)
            }

            override fun onRewarded(type: String?, amount: Int?, adPlatformEnum: AdPlatformTypeEnum?) {
                globalInterstitialShowListener?.onRewarded(type, amount, adPlatformEnum)
                listener?.onRewarded(type, amount, adPlatformEnum)
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                activity.runOnUiThread {
                    removeLoadingViewFromActivity(activity)
                }

                if (autoLoadForInterstitial) {
                    if (isEnabledLoadAndShowIfNotExistsAdsOnAutoloadMode && loadAndShowIfNotExistsAdsOnAutoloadMode) {
                        stopAutoloadInterstitialHandler()
                        loadAndShowInterstitial(activity, shownWhere, placementGroupCode, listener, platformType)
                    } else {
                        _autoloadInterstitialByHandler(activity, placementGroupCode, null, platformType)

                        globalInterstitialShowListener?.onError(errorMode, errorMessage, adPlatformEnum)
                        // listener?.onError(errorMode, errorMessage, adPlatformEnum) // call for adplatform
                        listener?.onError(AdErrorMode.MANAGER, errorMessage, adPlatformEnum) // call for manager

                    }
                } else {
                    globalInterstitialShowListener?.onError(errorMode, errorMessage, adPlatformEnum)
                    // listener?.onError(errorMode, errorMessage, adPlatformEnum) // call for adplatform
                    listener?.onError(AdErrorMode.MANAGER, errorMessage, adPlatformEnum) // call for manager
                }

            }
        }

        var hasLoadedInterstitial = false
        if (platformType != null) {

            val platform = getAdPlatformByType(platformType)
            if (platform != null) {
                val placementId = getPlacementGroupByCodeAndPlatform(placementGroupCode, platform.platformType)?.interstitial
                placementId?.let {
                    if (platform.isInterstitialLoaded(placementId) == true) {
                        platform.showInterstitial(activity, placementId, shownWhere, _listener)
                        hasLoadedInterstitial = true
                    }
                }
            }

        } else {
            run breaker@{
                interstitialAdPlatforms.forEach forEach@{ platform ->
                    val placementId = getPlacementGroupByCodeAndPlatform(placementGroupCode, platform.platformType)?.interstitial
                    if (placementId != null) {
                        if (platform.isInterstitialLoaded(placementId)) {
                            platform.showInterstitial(activity, placementId, shownWhere, _listener)
                            hasLoadedInterstitial = true
                            return@breaker
                            //return@forEach
                        }
                    }

                }
            }
        }

        if (!hasLoadedInterstitial) {
            // globalInterstitialShowListener?.onError(AdErrorMode.MANAGER, "there is no loaded interstitial for show. All platforms is not loaded", null)
            _listener?.onError(AdErrorMode.MANAGER, "there is no loaded interstitial for show. All platforms is not loaded", null)

        }

        return hasLoadedInterstitial
    }

    fun hasLoadedInterstitial(placementGroupCode: String = defaultPlacementGroupCode, platformType: AdPlatformTypeEnum? = null): Boolean {
        var hasLoaded = false

        val interstitialAdPlatforms = _getAdPlatformsWithSortedByAdFormat(placementGroupCode, AdFormatEnum.INTERSTITIAL)
        run breaker@{
            interstitialAdPlatforms.forEach forEach@{ _platform ->
                if (platformType != null && _platform.platformType != platformType) {
                    return@breaker
                }

                val placementId = getPlacementGroupByCodeAndPlatform(placementGroupCode, _platform.platformType)?.interstitial
                placementId?.let {
                    if (_platform.isInterstitialLoaded(placementId)) {
                        hasLoaded = true
                        return@breaker
                    }
                }

            }
        }

        return hasLoaded
    }


    // ################################################################################
    // ################################### REWARDED ###################################
    // ################################################################################
    fun hasLoadedRewarded(placementGroupCode: String = defaultPlacementGroupCode, platformType: AdPlatformTypeEnum?): Boolean {
        var hasLoaded = false

        val rewardedAdPlatforms = _getAdPlatformsWithSortedByAdFormat(placementGroupCode, AdFormatEnum.REWARDED)
        run breaker@{
            rewardedAdPlatforms.forEach forEach@{ _platform ->
                if (platformType != null && _platform.platformType != platformType) {
                    return@forEach
                }

                val placementId = getPlacementGroupByCodeAndPlatform(placementGroupCode, _platform.platformType)?.rewarded
                placementId?.let {
                    if (_platform.isRewardedLoaded(placementId)) {
                        hasLoaded = true
                        return@breaker
                    }
                }
            }
        }

        return hasLoaded
    }

    @JvmOverloads
    fun loadRewarded(
        activity: Activity, placementGroupCode: String = defaultPlacementGroupCode, listener: AdPlatformLoadListener? = null,
        platformType: AdPlatformTypeEnum? = null, parallel: Boolean = false
    ) {
        if (!showAds) return

        if (platformType == null) {
            if (parallel) {
                // send true parallel when first load after init ads. Else you dont need parallel load
                val rewardedAdPlatforms = _getAdPlatformsWithSortedByAdFormat(placementGroupCode, AdFormatEnum.REWARDED)
                rewardedAdPlatforms.forEach forEach@{ _platform ->
                    val placementId = getPlacementGroupByCodeAndPlatform(placementGroupCode, _platform.platformType)?.rewarded
                    if (placementId == null) return@forEach

                    if (!_platform.isRewardedLoaded(placementId)) {
                        _loadRewarded(activity, _platform.platformType, placementId, listener)
                    }
                }
            } else {
                _loadRewardedFromFirstAvailable(activity, placementGroupCode, listener)
            }

        } else {
            val platform = getAdPlatformByType(platformType)
            platform?.let {
                val placementId = getPlacementGroupByCodeAndPlatform(placementGroupCode, platform.platformType)?.rewarded
                if (placementId == null) {
                    throw Exception("_loadRewarded >> placementId null error")
                }
                _loadRewarded(activity, platform.platformType, placementId, listener)
            }
        }

    }


    private fun _loadRewardedFromFirstAvailable(activity: Activity, placementGroupCode: String = defaultPlacementGroupCode, listener: AdPlatformLoadListener? = null, index: Int = 0) {

        val rewardedAdPlatforms = _getAdPlatformsWithSortedByAdFormat(placementGroupCode, AdFormatEnum.REWARDED)
        if (index >= rewardedAdPlatforms.size) {
            return
        }
        val platform = rewardedAdPlatforms[index]
        val placementId = getPlacementGroupByCodeAndPlatform(placementGroupCode, platform.platformType)?.rewarded

        val _listener = object : AdPlatformLoadListener() {
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                if ((index + 1) < rewardedAdPlatforms.size) {
                    activity.runOnUiThread { _loadRewardedFromFirstAvailable(activity, placementGroupCode, listener, index + 1) }
                } else {
                    globalRewardedLoadListener?.onError(AdErrorMode.MANAGER, "No rewarded found in all platforms", null)
                    listener?.onError(AdErrorMode.MANAGER, "No rewarded found in all platforms", adPlatformEnum)

                }
                globalRewardedLoadListener?.onError(errorMode, errorMessage, adPlatformEnum)
                listener?.onError(errorMode, errorMessage, adPlatformEnum)

            }

            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum?) {
                globalRewardedLoadListener?.onLoaded(adPlatformEnum)
                listener?.onLoaded(adPlatformEnum)

            }
        }
        if (placementId == null) {
            throw Exception("_loadRewardedFromFirstAvailable >> placementId null error")
        }
        platform.loadRewarded(activity, placementId, _listener)
    }

    private fun _loadRewarded(activity: Activity, platformType: AdPlatformTypeEnum, placementId: String, listener: AdPlatformLoadListener? = null) {
        val _listener = object : AdPlatformLoadListener() {
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                globalRewardedLoadListener?.onError(errorMode, errorMessage, adPlatformEnum)
                listener?.onError(errorMode, errorMessage, adPlatformEnum)

                globalRewardedLoadListener?.onError(AdErrorMode.MANAGER, errorMessage, adPlatformEnum)
                listener?.onError(AdErrorMode.MANAGER, errorMessage, adPlatformEnum)
            }

            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum?) {
                globalRewardedLoadListener?.onLoaded(adPlatformEnum)
                listener?.onLoaded(adPlatformEnum)
            }

        }

        val platform = getAdPlatformByType(platformType)
        platform?.loadRewarded(activity, placementId, _listener)
    }


    @JvmOverloads
    fun loadAndShowRewarded(
        activity: Activity, shownWhere: String = "defaultPlace", placementGroupCode: String = defaultPlacementGroupCode,
        listener: AdPlatformShowListener? = null, platformType: AdPlatformTypeEnum? = null
    ) {
        /*
        call _showRewarded(listener, platform) in onLoaded and onError because of
        we wants call listener?.onError by _showRewarded
         */
        val loadListener: AdPlatformLoadListener = object : AdPlatformLoadListener() {
            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum?) {
                _showRewarded(activity, shownWhere, placementGroupCode, listener, platformType)
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                if (errorMode == AdErrorMode.MANAGER) {
                    _showRewarded(activity, shownWhere, placementGroupCode, listener, platformType)
                }
            }
        }
        loadRewarded(activity, placementGroupCode, loadListener, platformType, false)
    }

    @JvmOverloads
    fun showRewarded(
        activity: Activity, shownWhere: String = "defaultPlace", placementGroupCode: String = defaultPlacementGroupCode,
        listener: AdPlatformShowListener? = null,
        platformType: AdPlatformTypeEnum? = null, loadAndShowIfNotExistsAdsOnAutoloadMode: Boolean = true
    ) {
        if (!showAds) return

        if (autoLoadForRewarded) {
            val isShowed = _showRewarded(activity, shownWhere, placementGroupCode, listener, platformType)

            if (!isShowed) {
                if (isEnabledLoadAndShowIfNotExistsAdsOnAutoloadMode && loadAndShowIfNotExistsAdsOnAutoloadMode) {
                    stopAutoloadRewardedHandler()
                    loadAndShowRewarded(activity, shownWhere, placementGroupCode, listener, platformType)
                }
            }
        } else {
            loadAndShowRewarded(activity, shownWhere, placementGroupCode, listener, platformType)
        }
    }

    fun _showRewarded(
        activity: Activity, shownWhere: String = "defaultPlace", placementGroupCode: String,
        listener: AdPlatformShowListener? = null,
        platformType: AdPlatformTypeEnum? = null
    ): Boolean {
        if (!showAds) return true

        val rewardedAdPlatforms = _getAdPlatformsWithSortedByAdFormat(placementGroupCode, AdFormatEnum.REWARDED)
        val _listener = object : AdPlatformShowListener() {
            override fun onClosed(adPlatformEnum: AdPlatformTypeEnum?) {
                // on close load new one for next show
                globalRewardedShowListener?.onClosed(adPlatformEnum)
                listener?.onClosed(adPlatformEnum)
                saveLastShowDate(AdFormatEnum.REWARDED)

                if (autoLoadForRewarded) {
                    _autoloadRewardedByHandler(activity, placementGroupCode, null, platformType)
                }
            }

            override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum?) {
                if (adPlatformEnum != AdPlatformTypeEnum.STARTAPP) {
                    stopAutoloadRewardedHandler()
                }

                globalRewardedShowListener?.onDisplayed(adPlatformEnum)
                listener?.onDisplayed(adPlatformEnum)
            }

            override fun onClicked(adPlatformEnum: AdPlatformTypeEnum?) {
                globalRewardedShowListener?.onClicked(adPlatformEnum)
                listener?.onClicked(adPlatformEnum)
            }

            override fun onRewarded(type: String?, amount: Int?, adPlatformEnum: AdPlatformTypeEnum?) {
                globalRewardedShowListener?.onRewarded(type, amount, adPlatformEnum)
                listener?.onRewarded(type, amount, adPlatformEnum)
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {

                globalRewardedShowListener?.onError(errorMode, errorMessage, adPlatformEnum)
                listener?.onError(AdErrorMode.MANAGER, errorMessage, adPlatformEnum)
                // listener?.onError(errorMode, errorMessage, adPlatformEnum)

                if (autoLoadForRewarded) {
                    _autoloadRewardedByHandler(activity, placementGroupCode, null, platformType)
                }
            }
        }

        var hasLoadedRewarded = false

        if (platformType != null) {
            val platform = getAdPlatformByType(platformType)

            if (platform != null) {
                val placementId = getPlacementGroupByCodeAndPlatform(placementGroupCode, platform.platformType)?.rewarded
                placementId?.let {
                    if (platform.isRewardedLoaded(placementId)) {
                        platform.showRewarded(activity, placementId, shownWhere, _listener)
                        hasLoadedRewarded = true
                    }
                }
            }

        } else {
            run breaker@{
                rewardedAdPlatforms.forEach forEach@{ platform ->
                    val placementId = getPlacementGroupByCodeAndPlatform(placementGroupCode, platform.platformType)?.rewarded
                    if (placementId != null) {
                        if (platform.isRewardedLoaded(placementId)) {
                            platform.showRewarded(activity, placementId, shownWhere, _listener)
                            hasLoadedRewarded = true
                            return@breaker
                        }
                    }

                }
            }

        }

        if (!hasLoadedRewarded) {
            _listener.onError(AdErrorMode.MANAGER, "There is no loaded rewarded. Tried in all platforms", null)
        }

        return hasLoadedRewarded
    }


    // ################################################################################
    // ################################### BANNER ######################################
    // ################################################################################
    @JvmOverloads
    fun showBanner(
        activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener? = null,
        placementGroupCode: String = defaultPlacementGroupCode, platformType: AdPlatformTypeEnum? = null
    ) {
        if (!showAds) return


        if (platformType == null) {
            var startFrom = 0

            // if already banner loaded, start from this platform
            val bannerAdPlatforms = _getAdPlatformsWithSortedByAdFormat(placementGroupCode, AdFormatEnum.BANNER)
            if (bannerAdPlatforms.size > 0) {
                run breaker@{
                    bannerAdPlatforms.forEachIndexed forEachIndexed@{ i, _platform ->

                        val placementId = getPlacementGroupByCodeAndPlatform(placementGroupCode, _platform.platformType)?.banner
                        placementId?.let {
                            if (_platform.isBannerLoaded(placementId)) {
                                startFrom = i
                                return@breaker
                            }
                        }

                    }
                }
            }
            _showBannerFromFirstAvailable(activity, placementGroupCode, containerView, listener, startFrom)
        } else {

            val platform = getAdPlatformByType(platformType)
            platform?.let {
                val placementId = getPlacementGroupByCodeAndPlatform(placementGroupCode, platform.platformType)?.banner
                if (placementId != null) {
                    _showBanner(activity, platformType, placementId, containerView, listener)
                }
            }

        }
    }

    private fun _showBannerFromFirstAvailable(
        activity: Activity, placementGroupCode: String = defaultPlacementGroupCode, containerView: RelativeLayout,
        listener: AdPlatformShowListener? = null, platformIndex: Int = 0
    ) {
        val bannerAdPlatforms = _getAdPlatformsWithSortedByAdFormat(placementGroupCode, AdFormatEnum.BANNER)
        if (platformIndex >= bannerAdPlatforms.size) {
            return
        }

        val _listener = object : AdPlatformShowListener() {
            override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum?) {
                saveLastShowDate(AdFormatEnum.BANNER)
                listener?.onDisplayed(adPlatformEnum)
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                listener?.onError(errorMode, errorMessage, adPlatformEnum)

                if ((platformIndex + 1) < bannerAdPlatforms.size) {
                    activity.runOnUiThread { _showBannerFromFirstAvailable(activity, placementGroupCode, containerView, listener, platformIndex + 1) }
                } else {
                    listener?.onError(AdErrorMode.MANAGER, errorMessage, adPlatformEnum) // there is no banner ads. Tried on all platforms
                }
            }
        }

        val platform = bannerAdPlatforms[platformIndex]
        val placementId = getPlacementGroupByCodeAndPlatform(placementGroupCode, platform.platformType)?.banner
        platform.showBanner(activity, placementId ?: "wrong_placement", containerView, _listener)
    }

    private fun _showBanner(
        activity: Activity, platformType: AdPlatformTypeEnum, placementId: String,
        containerView: RelativeLayout, listener: AdPlatformShowListener? = null
    ) {
        val platform = getAdPlatformByType(platformType)

        platform?.showBanner(activity, placementId, containerView, object : AdPlatformShowListener() {
            override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum?) {
                saveLastShowDate(AdFormatEnum.BANNER)
                listener?.onDisplayed(adPlatformEnum)
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                listener?.onError(errorMode, errorMessage, adPlatformEnum)
                listener?.onError(AdErrorMode.MANAGER, errorMessage, adPlatformEnum)
            }
        })
    }

    // ################################################################################
    // ################################### MREC ######################################
    // ################################################################################
    @JvmOverloads
    fun showMrec(
        activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener? = null,
        placementGroupCode: String = defaultPlacementGroupCode, platformType: AdPlatformTypeEnum? = null
    ) {
        if (!showAds) return


        if (platformType == null) {
            var startFrom = 0

            // if already mrec loaded, start from this platform
            val mrecAdPlatforms = _getAdPlatformsWithSortedByAdFormat(placementGroupCode, AdFormatEnum.MREC)
            if (mrecAdPlatforms.size > 0) {
                run breaker@{
                    mrecAdPlatforms.forEachIndexed forEachIndexed@{ i, _platform ->

                        val placementId = getPlacementGroupByCodeAndPlatform(placementGroupCode, _platform.platformType)?.mrec
                        placementId?.let {
                            if (_platform.isMrecLoaded(placementId)) {
                                startFrom = i
                                return@breaker
                            }
                        }

                    }
                }
            }
            _showMrecFromFirstAvailable(activity, placementGroupCode, containerView, listener, startFrom)
        } else {

            val platform = getAdPlatformByType(platformType)
            platform?.let {
                val placementId = getPlacementGroupByCodeAndPlatform(placementGroupCode, platform.platformType)?.mrec
                if (placementId != null) {
                    _showMrec(activity, platformType, placementId, containerView, listener)
                }
            }

        }
    }

    private fun _showMrecFromFirstAvailable(
        activity: Activity, placementGroupCode: String = defaultPlacementGroupCode, containerView: RelativeLayout,
        listener: AdPlatformShowListener? = null, platformIndex: Int = 0
    ) {
        val mrecAdPlatforms = _getAdPlatformsWithSortedByAdFormat(placementGroupCode, AdFormatEnum.MREC)
        if (platformIndex >= mrecAdPlatforms.size) {
            return
        }

        val _listener = object : AdPlatformShowListener() {
            override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum?) {
                saveLastShowDate(AdFormatEnum.MREC)
                listener?.onDisplayed(adPlatformEnum)
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                if ((platformIndex + 1) < mrecAdPlatforms.size) {
                    activity.runOnUiThread { _showMrecFromFirstAvailable(activity, placementGroupCode, containerView, listener, platformIndex + 1) }
                } else {
                    listener?.onError(AdErrorMode.MANAGER, errorMessage, adPlatformEnum) // there is no mrec ads. Tried on all platforms
                }
                listener?.onError(errorMode, errorMessage, adPlatformEnum)
            }
        }

        val platform = mrecAdPlatforms[platformIndex]
        val placementId = getPlacementGroupByCodeAndPlatform(placementGroupCode, platform.platformType)?.mrec
        platform.showMrec(activity, placementId ?: "wrong_placement", containerView, _listener)
    }

    private fun _showMrec(
        activity: Activity, platformType: AdPlatformTypeEnum, placementId: String,
        containerView: RelativeLayout, listener: AdPlatformShowListener? = null
    ) {
        val platform = getAdPlatformByType(platformType)

        platform?.showMrec(activity, placementId, containerView, object : AdPlatformShowListener() {
            override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum?) {
                saveLastShowDate(AdFormatEnum.MREC)
                listener?.onDisplayed(adPlatformEnum)
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                listener?.onError(errorMode, errorMessage, adPlatformEnum)
                listener?.onError(AdErrorMode.MANAGER, errorMessage, adPlatformEnum)
            }
        })
    }

    // ################################################################################
    // ################################### OTHER ######################################
    // ################################################################################

    fun onCreate(activity: Activity) {
        adPlatforms.forEach forEach@{ platform ->
            platform.onCreate(activity)
        }
    }

    fun onResume(activity: Activity) {
        if (!showAds) return

        adPlatforms.forEach forEach@{ platform ->
            platform.onResume(activity)
        }
    }

    fun onPause(activity: Activity) {
        if (!showAds) return

        adPlatforms.forEach forEach@{ platform ->
            platform.onPause(activity)
        }
    }

    fun onStop(activity: Activity) {
        if (!showAds) return

        adPlatforms.forEach forEach@{ platform ->
            platform.onStop(activity)
        }
    }


    fun saveLastShowDate(adFormatEnum: AdFormatEnum) {
        lastShowDateByAdFormat.put(adFormatEnum, Date())
    }

    fun randInt(min: Int, max: Int): Int {
        return Random().nextInt(max - min + 1) + min
    }

    var lastPostDelayedSetTimeForInterstitialLoad: Date? = null
    private fun _autoloadInterstitialByHandler(activity: Activity, placementGroupCode: String = defaultPlacementGroupCode, listener: AdPlatformLoadListener? = null, platformType: AdPlatformTypeEnum? = null) {

        if (!isHandlerAvailableForLoads) {
            try {
                activity.runOnUiThread {
                    placementGroups.forEachIndexed { index, pgName ->
                        if (index < 1) {
                            loadInterstitial(activity, placementGroupCode, listener, platformType, false)
                        }
                    }

                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return
        }

        if (hasWorkingAutoloadInterstitialHandler) {
            if (lastPostDelayedSetTimeForInterstitialLoad != null) {
                // val diffInMillies = Date().time - lastPostDelayedSetTime!!.time
                val diffSeconds = TimeUnit.SECONDS.convert(Date().time - lastPostDelayedSetTimeForInterstitialLoad!!.time, TimeUnit.MILLISECONDS)
                if (diffSeconds < (autoLoadDelay + 2)) {
                    return
                }
            }
        }

        initHandlers()

        //stopAutoloadInterstitialHandler()

        hasWorkingAutoloadInterstitialHandler = true
        lastPostDelayedSetTimeForInterstitialLoad = Date()

        autoloadInterstitialHandler?.postDelayed({
            try {
                activity.runOnUiThread {
                    placementGroups.forEachIndexed { index, pgName ->
                        if (index < 1) {
                            loadInterstitial(activity, placementGroupCode, listener, platformType, false)
                        }
                    }

                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            hasWorkingAutoloadInterstitialHandler = false
        }, autoLoadDelay * 1000)
    }

    var lastPostDelayedSetTimeForRewardedLoad: Date? = null
    private fun _autoloadRewardedByHandler(activity: Activity, placementGroupCode: String = defaultPlacementGroupCode, listener: AdPlatformLoadListener? = null, platformType: AdPlatformTypeEnum? = null) {

        if (!isHandlerAvailableForLoads) {
            try {
                activity.runOnUiThread {
                    placementGroups.forEachIndexed { index, pgName ->
                        if (index > 0) return@forEachIndexed
                        loadRewarded(activity, placementGroupCode, listener, platformType, false)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }
        if (hasWorkingAutoloadRewardedHandler) {
            if (lastPostDelayedSetTimeForRewardedLoad != null) {
                val diffSeconds = TimeUnit.SECONDS.convert(Date().time - lastPostDelayedSetTimeForRewardedLoad!!.time, TimeUnit.MILLISECONDS)
                if (diffSeconds < (autoLoadDelay + 2)) {
                    return
                }
            }
        }

        initHandlers()
        //stopAutoloadRewardedHandler()

        hasWorkingAutoloadRewardedHandler = true
        lastPostDelayedSetTimeForRewardedLoad = Date()

        autoloadRewardedHandler?.postDelayed({
            try {
                activity.runOnUiThread {
                    placementGroups.forEachIndexed { index, pgName ->
                        if (index > 0) return@forEachIndexed
                        loadRewarded(activity, placementGroupCode, listener, platformType, false)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            hasWorkingAutoloadRewardedHandler = false
        }, autoLoadDelay * 1000)
    }


    fun stopAutoloadInterstitialHandler() {
        try {
            autoloadInterstitialHandler?.removeCallbacksAndMessages(null)
            hasWorkingAutoloadInterstitialHandler = false
        } catch (e: Exception) {
        }

    }

    fun stopAutoloadRewardedHandler() {
        try {
            autoloadRewardedHandler?.removeCallbacksAndMessages(null)
            hasWorkingAutoloadRewardedHandler = false
        } catch (e: Exception) {
        }
    }

    fun destroy(activity: Activity) {
        adPlatforms.forEach {
            it.destroy(activity)
        }

        stopAutoloadInterstitialHandler()
        stopAutoloadRewardedHandler()
        handlerThread?.quit()
        handlerThread = null
    }

    // AUTOLOAD HANDLERS
    // handlers
    private var isHandlerAvailableForLoads = false
    private var handlerThread: HandlerThread? = null
    private var autoloadInterstitialHandler: Handler? = null
    private var autoloadRewardedHandler: Handler? = null
    private var hasWorkingAutoloadInterstitialHandler = false
    private var hasWorkingAutoloadRewardedHandler = false

    private fun initHandlers() {
        if (handlerThread == null) {
            handlerThread = HandlerThread("admanager-bg-thread")
            handlerThread?.start()

            hasWorkingAutoloadInterstitialHandler = false
            hasWorkingAutoloadRewardedHandler = false
            autoloadInterstitialHandler = Handler(handlerThread!!.looper)
            autoloadRewardedHandler = Handler(handlerThread!!.looper)
        }
    }


    // AD SHOWING VIEW
    var isEnableShowLoadingViewForInterstitial:Boolean = true
    var loadingView: AdLoadingCustomView? = null
    fun initLoadingView(activity: Activity, rootView: ViewGroup? = null) {
        if (loadingView != null) {
            rootView?.let {
                setLoadingLayoutParams(rootView)
            }

            if (loadingView?.parent != null) {
                (loadingView?.parent as ViewGroup).removeView(loadingView)
            }
            return
        }
        loadingView = AdLoadingCustomView(activity, null)

        rootView?.let {
            setLoadingLayoutParams(rootView)
        }

        loadingView?.bringToFront()
    }

    fun setLoadingLayoutParams(rootView: ViewGroup) {
        var className = rootView::class.simpleName

        if (rootView::class.simpleName == "NestedScrollView") {
            className = rootView.getChildAt(0)::class.simpleName
        }

        when (className) {
            "RelativeLayout" -> {
                val layoutParams: RelativeLayout.LayoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)

                loadingView?.layoutParams = layoutParams
            }
            "ConstraintLayout" -> {
                val layoutParams: ConstraintLayout.LayoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams.bottomToBottom = ConstraintSet.PARENT_ID
                layoutParams.endToEnd = ConstraintSet.PARENT_ID
                layoutParams.startToStart = ConstraintSet.PARENT_ID
                layoutParams.topToTop = ConstraintSet.PARENT_ID

                loadingView?.layoutParams = layoutParams
            }
            "LinearLayout" -> {
                val layoutParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams.gravity = Gravity.CENTER

                loadingView?.layoutParams = layoutParams
            }
        }
    }

    fun addLoadingViewToActivity(activity: Activity) {
        if (!isEnableShowLoadingViewForInterstitial) {
            return
        }

        try {
            val activityRootView = activity.findViewById<ViewGroup>(android.R.id.content)
                .getChildAt(0) as ViewGroup

            initLoadingView(activity, activityRootView)

            val className = activityRootView::class.simpleName

            if (className == "NestedScrollView") {
                (activityRootView.getChildAt(0) as ViewGroup).addView(loadingView)
            } else {
                activityRootView.addView(loadingView)
            }

            ViewCompat.setTranslationZ(loadingView as View, 100f);

            loadingView?.bringToFront()
            //loadingView?.invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun removeLoadingViewFromActivity(activity: Activity) {
        try {
            if (loadingView == null) return
            val activityRootView = activity.findViewById<ViewGroup>(android.R.id.content)
                .getChildAt(0) as ViewGroup

            val className = activityRootView::class.simpleName

            if (className == "NestedScrollView") {
                (activityRootView.getChildAt(0) as ViewGroup).removeView(loadingView)
            } else {
                activityRootView.removeView(loadingView)
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    // ################################################################################
    // ################################### NATIVE ######################################
    // ################################################################################
    @JvmOverloads
    fun loadSmallNativeAds(activity: Activity, count: Int, listener: AdPlatformLoadListener? = null, placementGroupCode: String = defaultPlacementGroupCode) {
        if (!showAds) return

        _loadNativeFromAllNetworks(activity, AdFormatEnum.NATIVE_SMALL, count, listener, placementGroupCode)
    }

    @JvmOverloads
    fun loadMediumNativeAds(activity: Activity, count: Int, listener: AdPlatformLoadListener? = null, placementGroupCode: String = defaultPlacementGroupCode) {
        if (!showAds) return

        _loadNativeFromAllNetworks(activity, AdFormatEnum.NATIVE_MEDIUM, count, listener, placementGroupCode)
    }

    @JvmOverloads
    fun loadNativeAds(activity: Activity, nativeAdFormat: AdFormatEnum, count: Int, listener: AdPlatformLoadListener? = null, placementGroupCode: String = defaultPlacementGroupCode) {
        if (!showAds) return

        _loadNativeFromAllNetworks(activity, nativeAdFormat, count, listener, placementGroupCode)
    }

    private fun _loadNativeFromAllNetworks(activity: Activity, nativeAdFormat: AdFormatEnum, count: Int, listener: AdPlatformLoadListener? = null, placementGroupCode: String = defaultPlacementGroupCode) {
        val nativeAdPlatforms = _getAdPlatformsWithSortedByAdFormat(placementGroupCode, nativeAdFormat)
        if (nativeAdPlatforms.size == 0) {
            return
        }

        val _listener = object : AdPlatformLoadListener() {
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                listener?.onError(errorMode, errorMessage, adPlatformEnum)
            }

            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum?) {
                listener?.onLoaded(adPlatformEnum)
            }
        }

        nativeAdPlatforms.forEach { platform ->
            val placementId = if (nativeAdFormat == AdFormatEnum.NATIVE_SMALL) {
                getPlacementGroupByCodeAndPlatform(placementGroupCode, platform.platformType)?.nativeSmall
            } else {
                getPlacementGroupByCodeAndPlatform(placementGroupCode, platform.platformType)?.nativeMedium
            }

            placementId?.let {
                platform.loadNativeAds(activity, placementId, nativeAdFormat, count, _listener)
            }
        }

    }

    var lastShowedAdNetworkForNativeAds: MutableMap<String, AdPlatformWrapper> = mutableMapOf()
    fun showNative(activity: Activity, nativeAdFormat: AdFormatEnum, containerView: ViewGroup, listener: AdPlatformShowListener? = null, placementGroupCode: String = defaultPlacementGroupCode): Boolean {
        val nativeAdPlatforms = _getAdPlatformsWithSortedByAdFormat(placementGroupCode, nativeAdFormat)
        if (nativeAdPlatforms.size == 0) {
            return false
        }


        // set current show ad network
        var currentShowAdNetwork: AdPlatformWrapper? = null
        try {
            val nativeAdPlatformsHasLoadedNativeAds: List<AdPlatformWrapper> = nativeAdPlatforms.filter {

                val placementId = if (nativeAdFormat == AdFormatEnum.NATIVE_SMALL) {
                    getPlacementGroupByCodeAndPlatform(placementGroupCode, it.platformType)?.nativeSmall
                } else {
                    getPlacementGroupByCodeAndPlatform(placementGroupCode, it.platformType)?.nativeMedium
                }

                if (placementId == null) {
                    false
                } else {
                    it.hasLoadedNative(nativeAdFormat, placementId)
                }
            }
            if (nativeAdPlatformsHasLoadedNativeAds.size > 1) {
                if (lastShowedAdNetworkForNativeAds[nativeAdFormat.name] != null) {
                    val lastShowedIndex = nativeAdPlatformsHasLoadedNativeAds.indexOf(lastShowedAdNetworkForNativeAds[nativeAdFormat.name])

                    // eğer son öğeyse
                    if (lastShowedIndex == (nativeAdPlatformsHasLoadedNativeAds.size - 1)) {
                        currentShowAdNetwork = nativeAdPlatformsHasLoadedNativeAds[0]
                    } else {
                        currentShowAdNetwork = nativeAdPlatformsHasLoadedNativeAds[lastShowedIndex + 1]
                    }

                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }


        var showed = false
        for (i in 0 until nativeAdPlatforms.size) {
            val platform = nativeAdPlatforms[i]

            val placementId = if (nativeAdFormat == AdFormatEnum.NATIVE_SMALL) {
                getPlacementGroupByCodeAndPlatform(placementGroupCode, platform.platformType)?.nativeSmall
            } else {
                getPlacementGroupByCodeAndPlatform(placementGroupCode, platform.platformType)?.nativeMedium
            }
            if (placementId == null) continue

            val hasLoadedNative = platform.hasLoadedNative(nativeAdFormat, placementId)

            if (currentShowAdNetwork != null) {
                if (currentShowAdNetwork != platform) {
                    continue
                }
            }

            if (hasLoadedNative) {
                platform.showNative(activity, placementId, nativeAdFormat, containerView, listener)
                lastShowedAdNetworkForNativeAds[nativeAdFormat.name] = platform
                showed = true
            }

            if (showed) break

        }

        if (!showed) {
            listener?.onError(AdErrorMode.MANAGER, "no loaded native ad", null)
        }


        return showed
    }


    fun hasLoadedNativeAds(activity: Activity, nativeAdFormat: AdFormatEnum, placementGroupCode: String = defaultPlacementGroupCode): Boolean {
        return getLoadedNativeAdsCount(activity, nativeAdFormat) > 0
    }

    fun getLoadedNativeAdsCount(activity: Activity, nativeAdFormat: AdFormatEnum, placementGroupCode: String = defaultPlacementGroupCode): Int {
        var count = 0
        val nativeAdPlatforms = _getAdPlatformsWithSortedByAdFormat(placementGroupCode, nativeAdFormat)
        if (nativeAdPlatforms.size == 0) {
            return count
        }

        nativeAdPlatforms.forEach { platform ->

            val placementId = if (nativeAdFormat == AdFormatEnum.NATIVE_SMALL) {
                getPlacementGroupByCodeAndPlatform(placementGroupCode, platform.platformType)?.nativeSmall
            } else {
                getPlacementGroupByCodeAndPlatform(placementGroupCode, platform.platformType)?.nativeMedium
            }

            placementId?.let {
                count += platform.getNativeAds(activity, placementId, nativeAdFormat).size
            }
        }

        return count

    }

}