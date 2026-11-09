package com.helikanonlib.simpleadmanager

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
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

    companion object {
        var DEFAULT_PLACEMENT_GROUPCODE = "default"
        var DEFAULT_INTERSTITIAL_SHOWN_WHERE_NAME = "defaultPlace"
    }

    var testMode: Boolean = false
    var deviceId: String = ""
    var showAds: Boolean = true
    var autoLoadForInterstitial: Boolean = false
    var autoLoadForRewarded: Boolean = true
    var autoLoadDelay: Long = 10
        set(value) {
            require(value >= 0) { "autoLoadDelay cannot be negative" }
            field = value
        }
    var randomInterval: Int = 40
        set(value) {
            require(value >= 0) { "randomInterval cannot be negative" }
            field = value
        }
    var interstitialMinElapsedSecondsToNextShow: Int = 40
        set(value) {
            require(value >= 0) { "interstitialMinElapsedSecondsToNextShow cannot be negative" }
            field = value
        }
    var rewardedMinElapsedSecondsToNextShow: Int = 40
        set(value) {
            require(value >= 0) { "rewardedMinElapsedSecondsToNextShow cannot be negative" }
            field = value
        }
    var isEnabledLoadAndShowIfNotExistsAdsOnAutoloadMode = false

    var globalInterstitialShowListener: AdPlatformShowListener? = null
    var globalRewardedShowListener: AdPlatformShowListener? = null
    var globalInterstitialLoadListener: AdPlatformLoadListener? = null
    var globalRewardedLoadListener: AdPlatformLoadListener? = null

    private val adPlatforms = mutableListOf<AdPlatformWrapper>()
    private val adPlatformSortByAdFormat = mutableMapOf<String, List<AdPlatformTypeEnum>>()
    private val lastShowDateByAdFormat = mutableMapOf<AdFormatEnum, Date>()
    private val placementGroups = mutableListOf<AdPlacementGroup>()

    // handlers
    private var handlerThread: HandlerThread? = null
    private var autoloadInterstitialHandler: Handler? = null
    private var autoloadRewardedHandler: Handler? = null
    private var hasWorkingAutoloadInterstitialHandler = false
    private var hasWorkingAutoloadRewardedHandler = false

    // Keep leases reachable outside SDK callbacks so Activity/manager destruction can unblock App Open ads.
    private val displayLeaseLock = Any()
    private val displayLeasesByActivity = IdentityHashMap<Activity, MutableSet<AppOpenAdDisplayGate.Lease>>()

    /** A platform with its placement id for one ad format in one placement group. */
    private class PlatformPlacement(val platform: AdPlatformWrapper, val placementId: String) {
        val type: AdPlatformTypeEnum get() = platform.platformType
    }

    constructor() {
        initHandlers()
    }

    private inline fun notifyLoadListeners(
        globalListener: AdPlatformLoadListener?,
        localListener: AdPlatformLoadListener?,
        notify: (AdPlatformLoadListener) -> Unit
    ) {
        globalListener?.let(notify)
        if (localListener !== globalListener) localListener?.let(notify)
    }

    private inline fun notifyShowListeners(
        globalListener: AdPlatformShowListener?,
        localListener: AdPlatformShowListener?,
        notify: (AdPlatformShowListener) -> Unit
    ) {
        globalListener?.let(notify)
        if (localListener !== globalListener) localListener?.let(notify)
    }

    private fun adsDisabledError(adFormat: AdFormatEnum, placementGroupCode: String) =
        AdManagerError(
            format = adFormat,
            placementGroupCode = placementGroupCode,
            message = "Ads are disabled because showAds is false"
        )

    private fun noPlatformError(adFormat: AdFormatEnum, placementGroupCode: String) =
        AdManagerError(
            format = adFormat,
            placementGroupCode = placementGroupCode,
            message = "No ${adFormat.name.lowercase(Locale.ENGLISH)} ad platform is configured for placement group '$placementGroupCode'"
        )

    private fun notifyLoadDisabled(
        adFormat: AdFormatEnum,
        placementGroupCode: String,
        globalListener: AdPlatformLoadListener?,
        localListener: AdPlatformLoadListener?
    ) {
        val error = adsDisabledError(adFormat, placementGroupCode)
        notifyLoadListeners(globalListener, localListener) { it.onError(error) }
    }

    private fun notifyShowDisabled(
        adFormat: AdFormatEnum,
        placementGroupCode: String,
        globalListener: AdPlatformShowListener?,
        localListener: AdPlatformShowListener?
    ) {
        val error = adsDisabledError(adFormat, placementGroupCode)
        notifyShowListeners(globalListener, localListener) { it.onError(error) }
    }

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

    private fun acquireFullScreenDisplayLease(activity: Activity): AppOpenAdDisplayGate.Lease {
        return synchronized(displayLeaseLock) {
            AppOpenAdDisplayGate.acquire().also { lease ->
                displayLeasesByActivity.getOrPut(activity, ::mutableSetOf).add(lease)
            }
        }
    }

    private fun releaseFullScreenDisplayLease(activity: Activity, lease: AppOpenAdDisplayGate.Lease) {
        synchronized(displayLeaseLock) {
            displayLeasesByActivity[activity]?.let { activityLeases ->
                activityLeases.remove(lease)
                if (activityLeases.isEmpty()) displayLeasesByActivity.remove(activity)
            }
        }
        lease.release()
    }

    private fun releaseFullScreenDisplayLeases(activity: Activity) {
        val leases = synchronized(displayLeaseLock) {
            displayLeasesByActivity.remove(activity)?.toList().orEmpty()
        }
        leases.forEach { it.release() }
    }

    private fun releaseAllFullScreenDisplayLeases() {
        val leases = synchronized(displayLeaseLock) {
            displayLeasesByActivity.values.flatten().also { displayLeasesByActivity.clear() }
        }
        leases.forEach { it.release() }
    }

    // ################################################################################
    // ################################### SETUP ######################################
    // ################################################################################
    fun initializePlatformsWithActivity(activity: Activity) {
        if (!showAds) return

        adPlatforms.forEach forEach@{ platform ->
            platform.initialize(activity, null, testMode)

            if (testMode) {
                platform.enableTestMode(activity.applicationContext, deviceId)
            }
        }
    }

    @JvmOverloads
    fun initializePlatforms(
        context: Context,
        onInitializeComplete: (AdInitializationResult) -> Unit = {},
        onPlatformInitializeComplete: (AdPlatformInitializationResult) -> Unit = {}
    ) {
        if (!showAds) {
            onInitializeComplete(
                AdInitializationResult(
                    AdInitializationStatus.DISABLED,
                    message = "Ad initialization is disabled because showAds is false"
                )
            )
            return
        }

        val platforms = adPlatforms.toList()
        if (platforms.isEmpty()) {
            onInitializeComplete(
                AdInitializationResult(
                    AdInitializationStatus.FAILURE,
                    message = "No ad platform is configured"
                )
            )
            return
        }

        val resultLock = Any()
        val platformResults = mutableListOf<AdPlatformInitializationResult>()
        var completedCount = 0
        var successCount = 0

        platforms.forEach { platform ->
            val onPlatformComplete: (Boolean) -> Unit = { isSuccessful ->
                val platformResult = AdPlatformInitializationResult(platform.platformType, isSuccessful)

                synchronized(resultLock) {
                    completedCount++
                    if (isSuccessful) successCount++
                    platformResults.add(platformResult)
                    onPlatformInitializeComplete(platformResult)

                    if (completedCount == platforms.size) {
                        val status = when (successCount) {
                            platforms.size -> AdInitializationStatus.SUCCESS
                            0 -> AdInitializationStatus.FAILURE
                            else -> AdInitializationStatus.PARTIAL_SUCCESS
                        }
                        onInitializeComplete(AdInitializationResult(status, platformResults.toList()))
                    }
                }
            }

            try {
                platform.initialize(context, onPlatformComplete, testMode)
            } catch (error: Exception) {
                onPlatformComplete(false)
            }

            if (testMode) {
                platform.enableTestMode(context, deviceId)
            }
        }
    }

    fun isAllPlatformsSdksInitialized(): Boolean =
        adPlatforms.isNotEmpty() && adPlatforms.all { it.isInitialized }

    fun isPlatformSdkInitialized(platformType: AdPlatformTypeEnum): Boolean =
        getAdPlatformByType(platformType)?.isInitialized == true

    fun enableTestMode(activity: Activity, deviceId: String) {

        this.testMode = true
        this.deviceId = deviceId

        placementGroups.forEach {
            if (it.platformType == AdPlatformTypeEnum.ADMOB) {
                it.interstitial = "ca-app-pub-3940256099942544/1033173712"
                // every format needs its own unit id: platforms cache ad instances by placement id
                it.banner = "ca-app-pub-3940256099942544/9214589741"
                it.rewarded = "ca-app-pub-3940256099942544/5224354917"
                it.mrec = "ca-app-pub-3940256099942544/6300978111"
                it.appOpenAd = "ca-app-pub-3940256099942544/9257395921"
            }
        }


    }

    fun setAdPlatforms(adPlatforms: List<AdPlatformWrapper>) = apply {
        this.adPlatforms.clear()
        this.adPlatforms.addAll(adPlatforms)
    }

    fun addAdPlatform(adPlatform: AdPlatformWrapper) = apply { this.adPlatforms.add(adPlatform) }
    fun getAdPlatformByType(platformType: AdPlatformTypeEnum): AdPlatformWrapper? =
        adPlatforms.firstOrNull { it.platformType == platformType }

    fun setPlacementGroups(placementGroups: List<AdPlacementGroup>) = apply {
        require(placementGroups.isNotEmpty()) { "At least one placement group must be configured" }
        require(placementGroups.none { it.groupCode.isBlank() }) { "Placement group codes cannot be blank" }
        val duplicates = placementGroups.groupingBy { it.groupCode to it.platformType }.eachCount().filterValues { it > 1 }.keys
        require(duplicates.isEmpty()) { "A placement group code can be configured only once per platform: $duplicates" }

        this.placementGroups.clear()
        this.placementGroups.addAll(placementGroups)
    }

    fun getPlacementGroupByCodeAndPlatform(placementGroupCode: String, platformTypeEnum: AdPlatformTypeEnum): AdPlacementGroup? =
        placementGroups.firstOrNull { it.groupCode == placementGroupCode && it.platformType == platformTypeEnum }

    private fun validatePlacementGroup(placementGroupCode: String) {
        require(placementGroups.any { it.groupCode == placementGroupCode }) {
            "Unknown placement group '$placementGroupCode'. Configured groups: ${placementGroups.map { it.groupCode }.distinct()}"
        }
    }

    private fun AdPlacementGroup.placementId(adFormat: AdFormatEnum): String = when (adFormat) {
        AdFormatEnum.INTERSTITIAL -> interstitial
        AdFormatEnum.BANNER -> banner
        AdFormatEnum.REWARDED -> rewarded
        AdFormatEnum.MREC -> mrec
        AdFormatEnum.APP_OPEN -> appOpenAd
    }

    private fun placementOf(placementGroupCode: String, platformType: AdPlatformTypeEnum, adFormat: AdFormatEnum): PlatformPlacement? {
        val platform = getAdPlatformByType(platformType) ?: return null
        val placementId = getPlacementGroupByCodeAndPlatform(placementGroupCode, platformType)?.placementId(adFormat)
        return if (placementId.isNullOrBlank()) null else PlatformPlacement(platform, placementId)
    }

    fun setAdPlatformOrder(
        placementGroupCode: String,
        adFormat: AdFormatEnum,
        vararg platforms: AdPlatformTypeEnum
    ): AdPlatformOrderResult {
        if (adFormat == AdFormatEnum.APP_OPEN) {
            return AdPlatformOrderResult(false, "Platform order is not supported for ${adFormat.name}; AppOpenAdManager has its own show order")
        }
        if (platforms.isEmpty()) {
            return AdPlatformOrderResult(false, "Platform order cannot be empty")
        }
        if (platforms.distinct().size != platforms.size) {
            return AdPlatformOrderResult(false, "Platform order contains duplicate platforms: ${platforms.toList()}")
        }

        try {
            validatePlacementGroup(placementGroupCode)
        } catch (error: IllegalArgumentException) {
            return AdPlatformOrderResult(false, error.message)
        }

        platforms.forEach { platformType ->
            if (getAdPlatformByType(platformType) == null) {
                return AdPlatformOrderResult(false, "$platformType is not configured in SimpleAdManager")
            }
            if (placementOf(placementGroupCode, platformType, adFormat) == null) {
                return AdPlatformOrderResult(false, "$platformType has no ${adFormat.name} placement in group '$placementGroupCode'")
            }
        }

        adPlatformSortByAdFormat[sortKey(placementGroupCode, adFormat)] = platforms.toList()
        return AdPlatformOrderResult(true)
    }

    /**
     * example :
     * adManager.setAdPlatformSortByAdFormatStr("default", "interstitial", "admob,applovin")
     * adManager.setAdPlatformSortByAdFormatStr("default", "banner", "applovin,admob")
     */
    fun setAdPlatformSortByAdFormatStr(placementGroupCode: String, adFormatName: String, adPlatformsStr: String): AdPlatformOrderResult {
        val adFormat = AdFormatEnum.entries.firstOrNull {
            it.name.equals(adFormatName.trim(), ignoreCase = true)
        } ?: return AdPlatformOrderResult(false, "Unknown ad format '$adFormatName'")

        val platformNames = adPlatformsStr.split(',').map { it.trim() }
        if (platformNames.any { it.isEmpty() }) {
            return AdPlatformOrderResult(false, "Platform order contains an empty platform name")
        }

        val platforms = platformNames.map { platformName ->
            AdPlatformTypeEnum.entries.firstOrNull {
                it.name.equals(platformName, ignoreCase = true)
            } ?: return AdPlatformOrderResult(false, "Unknown ad platform '$platformName'")
        }

        return setAdPlatformOrder(placementGroupCode, adFormat, *platforms.toTypedArray())
    }

    private fun sortKey(placementGroupCode: String, adFormat: AdFormatEnum) = placementGroupCode + "__" + adFormat.name

    // Configured order, or every platform when no order is set. Platforms without a placement for the format are skipped.
    private fun _getAdPlatformsWithSortedByAdFormat(placementGroupCode: String, adFormatEnum: AdFormatEnum): List<PlatformPlacement> {
        validatePlacementGroup(placementGroupCode)
        val order = adPlatformSortByAdFormat[sortKey(placementGroupCode, adFormatEnum)] ?: adPlatforms.map { it.platformType }
        return order.mapNotNull { placementOf(placementGroupCode, it, adFormatEnum) }
    }

    private fun missingPlacementError(adFormat: AdFormatEnum, placementGroupCode: String, platformType: AdPlatformTypeEnum) =
        AdManagerError(
            format = adFormat,
            placementGroupCode = placementGroupCode,
            attemptedPlatforms = listOf(platformType),
            message = "$platformType has no ${adFormat.name.lowercase(Locale.ENGLISH)} placement in group '$placementGroupCode'"
        )


    // ################################################################################
    // ################################### INTERSTITIAL ###############################
    // ################################################################################
    @JvmOverloads
    fun loadInterstitial(
        activity: Activity, placementGroupCode: String = DEFAULT_PLACEMENT_GROUPCODE, listener: AdPlatformLoadListener? = null,
        platformType: AdPlatformTypeEnum? = null, parallel: Boolean = false
    ) {
        if (!showAds) return notifyLoadDisabled(AdFormatEnum.INTERSTITIAL, placementGroupCode, globalInterstitialLoadListener, listener)
        validatePlacementGroup(placementGroupCode)

        if (platformType != null) {
            val placement = placementOf(placementGroupCode, platformType, AdFormatEnum.INTERSTITIAL)
            if (placement == null) {
                val error = missingPlacementError(AdFormatEnum.INTERSTITIAL, placementGroupCode, platformType)
                notifyLoadListeners(globalInterstitialLoadListener, listener) { it.onError(error) }
                return
            }
            _loadInterstitial(activity, placementGroupCode, placement, listener)
            return
        }

        if (!parallel) {
            _loadInterstitialFromFirstAvailable(activity, placementGroupCode, listener)
            return
        }

        // send true parallel when first load after init ads. Else you dont need parallel load
        val interstitialAdPlatforms = _getAdPlatformsWithSortedByAdFormat(placementGroupCode, AdFormatEnum.INTERSTITIAL)
        if (interstitialAdPlatforms.isEmpty()) {
            val error = noPlatformError(AdFormatEnum.INTERSTITIAL, placementGroupCode)
            notifyLoadListeners(globalInterstitialLoadListener, listener) { it.onError(error) }
            return
        }
        val platformsToLoad = interstitialAdPlatforms.filterNot { it.platform.isInterstitialLoaded(it.placementId) }
        if (platformsToLoad.isEmpty()) {
            notifyLoadListeners(globalInterstitialLoadListener, listener) { it.onLoaded(interstitialAdPlatforms.first().type) }
            return
        }

        val platformErrors = mutableListOf<AdPlatformError>()
        var completedCount = 0
        var hasLoaded = false
        platformsToLoad.forEach { candidate ->
            candidate.platform.loadInterstitial(activity, candidate.placementId, object : AdPlatformLoadListener() {
                override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum) {
                    hasLoaded = true
                    completedCount++
                    notifyLoadListeners(globalInterstitialLoadListener, listener) { it.onLoaded(adPlatformEnum) }
                }

                override fun onPlatformError(error: AdPlatformError) {
                    platformErrors.add(error)
                    completedCount++
                    notifyLoadListeners(globalInterstitialLoadListener, listener) { it.onPlatformError(error) }
                    if (completedCount == platformsToLoad.size && !hasLoaded) {
                        val managerError = AdManagerError(
                            AdFormatEnum.INTERSTITIAL,
                            placementGroupCode,
                            platformsToLoad.map { it.type },
                            platformErrors.toList(),
                            "No interstitial found in all platforms"
                        )
                        notifyLoadListeners(globalInterstitialLoadListener, listener) { it.onError(managerError) }
                    }
                }
            })
        }
    }

    private fun _loadInterstitialFromFirstAvailable(
        activity: Activity,
        placementGroupCode: String,
        listener: AdPlatformLoadListener? = null,
        index: Int = 0,
        platformErrors: MutableList<AdPlatformError> = mutableListOf()
    ) {
        val interstitialAdPlatforms = _getAdPlatformsWithSortedByAdFormat(placementGroupCode, AdFormatEnum.INTERSTITIAL)
        if (interstitialAdPlatforms.isEmpty()) {
            val error = noPlatformError(AdFormatEnum.INTERSTITIAL, placementGroupCode)
            notifyLoadListeners(globalInterstitialLoadListener, listener) { it.onError(error) }
            return
        }
        if (index >= interstitialAdPlatforms.size) {
            return
        }
        val candidate = interstitialAdPlatforms[index]

        val _listener = object : AdPlatformLoadListener() {
            override fun onPlatformError(error: AdPlatformError) {
                platformErrors.add(error)
                notifyLoadListeners(globalInterstitialLoadListener, listener) { it.onPlatformError(error) }

                if ((index + 1) < interstitialAdPlatforms.size) {
                    activity.runOnUiThread {
                        _loadInterstitialFromFirstAvailable(activity, placementGroupCode, listener, index + 1, platformErrors)
                    }
                } else {
                    val managerError = AdManagerError(
                        AdFormatEnum.INTERSTITIAL,
                        placementGroupCode,
                        interstitialAdPlatforms.map { it.type },
                        platformErrors.toList(),
                        "No interstitial found in all platforms"
                    )
                    notifyLoadListeners(globalInterstitialLoadListener, listener) { it.onError(managerError) }
                }
            }

            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum) {
                notifyLoadListeners(globalInterstitialLoadListener, listener) { it.onLoaded(adPlatformEnum) }
            }
        }

        candidate.platform.loadInterstitial(activity, candidate.placementId, _listener)
    }

    private fun _loadInterstitial(activity: Activity, placementGroupCode: String, placement: PlatformPlacement, listener: AdPlatformLoadListener? = null) {
        val _listener = object : AdPlatformLoadListener() {
            override fun onPlatformError(error: AdPlatformError) {
                notifyLoadListeners(globalInterstitialLoadListener, listener) { it.onPlatformError(error) }
                val managerError = AdManagerError(
                    AdFormatEnum.INTERSTITIAL,
                    placementGroupCode,
                    listOf(placement.type),
                    listOf(error),
                    "Interstitial load failed for ${placement.type.name}"
                )
                notifyLoadListeners(globalInterstitialLoadListener, listener) { it.onError(managerError) }
            }

            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum) {
                notifyLoadListeners(globalInterstitialLoadListener, listener) { it.onLoaded(adPlatformEnum) }
            }
        }

        placement.platform.loadInterstitial(activity, placement.placementId, _listener)
    }


    /*
    call _showInterstitial in onLoaded and onError because of
    we wants call listener?.onError by _showInterstitial
     */
    @JvmOverloads
    fun loadAndShowInterstitial(
        activity: Activity, shownWhere: String = DEFAULT_INTERSTITIAL_SHOWN_WHERE_NAME, placementGroupCode: String = DEFAULT_PLACEMENT_GROUPCODE,
        listener: AdPlatformShowListener? = null, platformType: AdPlatformTypeEnum? = null
    ) {
        if (!showAds) return notifyShowDisabled(AdFormatEnum.INTERSTITIAL, placementGroupCode, globalInterstitialShowListener, listener)
        validatePlacementGroup(placementGroupCode)

        activity.runOnUiThread {
            addLoadingViewToActivity(activity)
        }

        val loadListener: AdPlatformLoadListener = object : AdPlatformLoadListener() {
            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum) {
                // this listener will trigger just one time after firt load any platform
                _showInterstitial(activity, shownWhere, placementGroupCode, listener, platformType, false)
            }

            override fun onError(error: AdManagerError) {
                // all platforms are tried; _showInterstitial reports the terminal result to the user listener
                _showInterstitial(activity, shownWhere, placementGroupCode, listener, platformType, false)
            }
        }
        loadInterstitial(activity, placementGroupCode, loadListener, platformType, false)
    }

    @JvmOverloads
    fun showInterstitialForTimeStrategy(
        activity: Activity, shownWhere: String = DEFAULT_INTERSTITIAL_SHOWN_WHERE_NAME, placementGroupCode: String = DEFAULT_PLACEMENT_GROUPCODE,
        listener: AdPlatformShowListener? = null,
        platformType: AdPlatformTypeEnum? = null, loadAndShowIfNotExistsAdsOnAutoloadMode: Boolean = true
    ): AdShowRequestResult {
        if (!showAds) {
            notifyShowDisabled(AdFormatEnum.INTERSTITIAL, placementGroupCode, globalInterstitialShowListener, listener)
            return AdShowRequestResult.DISABLED
        }
        validatePlacementGroup(placementGroupCode)

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
            return AdShowRequestResult.SHOW_REQUESTED
        }

        return AdShowRequestResult.THROTTLED
    }

    @JvmOverloads
    fun showInterstitial(
        activity: Activity, shownWhere: String = DEFAULT_INTERSTITIAL_SHOWN_WHERE_NAME, placementGroupCode: String = DEFAULT_PLACEMENT_GROUPCODE,
        listener: AdPlatformShowListener? = null,
        platformType: AdPlatformTypeEnum? = null, loadAndShowIfNotExistsAdsOnAutoloadMode: Boolean = true
    ) {
        if (!showAds) return notifyShowDisabled(AdFormatEnum.INTERSTITIAL, placementGroupCode, globalInterstitialShowListener, listener)
        validatePlacementGroup(placementGroupCode)

        if (autoLoadForInterstitial) {
            _showInterstitial(activity, shownWhere, placementGroupCode, listener, platformType, loadAndShowIfNotExistsAdsOnAutoloadMode)
        } else {
            loadAndShowInterstitial(activity, shownWhere, placementGroupCode, listener, platformType)
        }
    }

    // load edilmiş reklamı göstermeye çalışır.
    private fun _showInterstitial(
        activity: Activity, shownWhere: String = DEFAULT_INTERSTITIAL_SHOWN_WHERE_NAME, placementGroupCode: String = DEFAULT_PLACEMENT_GROUPCODE,
        listener: AdPlatformShowListener? = null,
        platformType: AdPlatformTypeEnum? = null, loadAndShowIfNotExistsAdsOnAutoloadMode: Boolean = false
    ): Boolean {
        if (!showAds) {
            notifyShowDisabled(AdFormatEnum.INTERSTITIAL, placementGroupCode, globalInterstitialShowListener, listener)
            return false
        }

        val interstitialAdPlatforms = _getAdPlatformsWithSortedByAdFormat(placementGroupCode, AdFormatEnum.INTERSTITIAL)
        var displayLease: AppOpenAdDisplayGate.Lease? = null

        fun releaseDisplayLease() {
            displayLease?.let { releaseFullScreenDisplayLease(activity, it) }
            displayLease = null
        }

        // shared by "nothing loaded" and "platform failed to show"
        fun failShow(error: AdManagerError) {
            activity.runOnUiThread {
                removeLoadingViewFromActivity(activity)
            }

            if (autoLoadForInterstitial && isEnabledLoadAndShowIfNotExistsAdsOnAutoloadMode && loadAndShowIfNotExistsAdsOnAutoloadMode) {
                stopAutoloadInterstitialHandler()
                loadAndShowInterstitial(activity, shownWhere, placementGroupCode, listener, platformType)
                return
            }

            if (autoLoadForInterstitial) {
                _autoloadInterstitialByHandler(activity, placementGroupCode, null, null)
            }
            notifyShowListeners(globalInterstitialShowListener, listener) { it.onError(error) }
        }

        val _listener = object : AdPlatformShowListener() {
            override fun onClosed(adPlatformEnum: AdPlatformTypeEnum) {
                AppOpenAdDisplayGate.recordFullScreenAdClosed()
                releaseDisplayLease()

                activity.runOnUiThread {
                    removeLoadingViewFromActivity(activity)
                }

                notifyShowListeners(globalInterstitialShowListener, listener) { it.onClosed(adPlatformEnum) }
                saveLastShowDate(AdFormatEnum.INTERSTITIAL)

                // on close load new one for next show
                if (autoLoadForInterstitial) {
                    _autoloadInterstitialByHandler(activity, placementGroupCode, null, null)
                }
            }

            override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum) {
                stopAutoloadInterstitialHandler()

                notifyShowListeners(globalInterstitialShowListener, listener) { it.onDisplayed(adPlatformEnum) }

                activity.runOnUiThread {
                    removeLoadingViewFromActivity(activity)
                }
            }

            override fun onClicked(adPlatformEnum: AdPlatformTypeEnum) {
                notifyShowListeners(globalInterstitialShowListener, listener) { it.onClicked(adPlatformEnum) }
            }

            override fun onRewarded(type: String?, amount: Int?, adPlatformEnum: AdPlatformTypeEnum) {
                notifyShowListeners(globalInterstitialShowListener, listener) { it.onRewarded(type, amount, adPlatformEnum) }
            }

            override fun onPlatformError(error: AdPlatformError) {
                releaseDisplayLease()
                notifyShowListeners(globalInterstitialShowListener, listener) { it.onPlatformError(error) }
                failShow(
                    AdManagerError(
                        AdFormatEnum.INTERSTITIAL,
                        placementGroupCode,
                        listOf(error.platform),
                        listOf(error),
                        "Interstitial show failed for ${error.platform.name}"
                    )
                )
            }
        }

        fun showLoadedInterstitial(placement: PlatformPlacement): Boolean {
            displayLease = acquireFullScreenDisplayLease(activity)
            try {
                placement.platform.showInterstitial(activity, placement.placementId, shownWhere, _listener)
            } catch (error: Exception) {
                _listener.onPlatformError(
                    AdPlatformError(
                        AdFormatEnum.INTERSTITIAL,
                        placement.type,
                        placement.placementId,
                        "${placement.type.name} interstitial show failed: ${error.message.orEmpty()}",
                        error
                    )
                )
            }
            return true
        }

        val placementToShow = if (platformType != null) {
            placementOf(placementGroupCode, platformType, AdFormatEnum.INTERSTITIAL)
                ?.takeIf { it.platform.isInterstitialLoaded(it.placementId) }
        } else {
            interstitialAdPlatforms.firstOrNull { it.platform.isInterstitialLoaded(it.placementId) }
        }

        if (placementToShow != null) {
            return showLoadedInterstitial(placementToShow)
        }

        failShow(
            AdManagerError(
                AdFormatEnum.INTERSTITIAL,
                placementGroupCode,
                platformType?.let(::listOf) ?: interstitialAdPlatforms.map { it.type },
                message = "There is no loaded interstitial to show"
            )
        )
        return false
    }

    @JvmOverloads
    fun hasLoadedInterstitial(placementGroupCode: String = DEFAULT_PLACEMENT_GROUPCODE, platformType: AdPlatformTypeEnum? = null): Boolean {
        if (!showAds) return false

        return _getAdPlatformsWithSortedByAdFormat(placementGroupCode, AdFormatEnum.INTERSTITIAL).any {
            (platformType == null || it.type == platformType) && it.platform.isInterstitialLoaded(it.placementId)
        }
    }


    // ################################################################################
    // ################################### REWARDED ###################################
    // ################################################################################
    @JvmOverloads
    fun hasLoadedRewarded(placementGroupCode: String = DEFAULT_PLACEMENT_GROUPCODE, platformType: AdPlatformTypeEnum? = null): Boolean {
        if (!showAds) return false

        return _getAdPlatformsWithSortedByAdFormat(placementGroupCode, AdFormatEnum.REWARDED).any {
            (platformType == null || it.type == platformType) && it.platform.isRewardedLoaded(it.placementId)
        }
    }

    @JvmOverloads
    fun loadRewarded(
        activity: Activity, placementGroupCode: String = DEFAULT_PLACEMENT_GROUPCODE, listener: AdPlatformLoadListener? = null,
        platformType: AdPlatformTypeEnum? = null, parallel: Boolean = false
    ) {
        if (!showAds) return notifyLoadDisabled(AdFormatEnum.REWARDED, placementGroupCode, globalRewardedLoadListener, listener)
        validatePlacementGroup(placementGroupCode)

        if (platformType != null) {
            val placement = placementOf(placementGroupCode, platformType, AdFormatEnum.REWARDED)
            if (placement == null) {
                val error = missingPlacementError(AdFormatEnum.REWARDED, placementGroupCode, platformType)
                notifyLoadListeners(globalRewardedLoadListener, listener) { it.onError(error) }
                return
            }
            _loadRewarded(activity, placementGroupCode, placement, listener)
            return
        }

        if (!parallel) {
            _loadRewardedFromFirstAvailable(activity, placementGroupCode, listener)
            return
        }

        // send true parallel when first load after init ads. Else you dont need parallel load
        val rewardedAdPlatforms = _getAdPlatformsWithSortedByAdFormat(placementGroupCode, AdFormatEnum.REWARDED)
        if (rewardedAdPlatforms.isEmpty()) {
            val error = noPlatformError(AdFormatEnum.REWARDED, placementGroupCode)
            notifyLoadListeners(globalRewardedLoadListener, listener) { it.onError(error) }
            return
        }
        val platformsToLoad = rewardedAdPlatforms.filterNot { it.platform.isRewardedLoaded(it.placementId) }
        if (platformsToLoad.isEmpty()) {
            notifyLoadListeners(globalRewardedLoadListener, listener) { it.onLoaded(rewardedAdPlatforms.first().type) }
            return
        }

        val platformErrors = mutableListOf<AdPlatformError>()
        var completedCount = 0
        var hasLoaded = false
        platformsToLoad.forEach { candidate ->
            candidate.platform.loadRewarded(activity, candidate.placementId, object : AdPlatformLoadListener() {
                override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum) {
                    hasLoaded = true
                    completedCount++
                    notifyLoadListeners(globalRewardedLoadListener, listener) { it.onLoaded(adPlatformEnum) }
                }

                override fun onPlatformError(error: AdPlatformError) {
                    platformErrors.add(error)
                    completedCount++
                    notifyLoadListeners(globalRewardedLoadListener, listener) { it.onPlatformError(error) }
                    if (completedCount == platformsToLoad.size && !hasLoaded) {
                        val managerError = AdManagerError(
                            AdFormatEnum.REWARDED,
                            placementGroupCode,
                            platformsToLoad.map { it.type },
                            platformErrors.toList(),
                            "No rewarded found in all platforms"
                        )
                        notifyLoadListeners(globalRewardedLoadListener, listener) { it.onError(managerError) }
                    }
                }
            })
        }
    }


    private fun _loadRewardedFromFirstAvailable(
        activity: Activity,
        placementGroupCode: String = DEFAULT_PLACEMENT_GROUPCODE,
        listener: AdPlatformLoadListener? = null,
        index: Int = 0,
        platformErrors: MutableList<AdPlatformError> = mutableListOf()
    ) {
        val rewardedAdPlatforms = _getAdPlatformsWithSortedByAdFormat(placementGroupCode, AdFormatEnum.REWARDED)
        if (rewardedAdPlatforms.isEmpty()) {
            val error = noPlatformError(AdFormatEnum.REWARDED, placementGroupCode)
            notifyLoadListeners(globalRewardedLoadListener, listener) { it.onError(error) }
            return
        }
        if (index >= rewardedAdPlatforms.size) {
            return
        }
        val candidate = rewardedAdPlatforms[index]

        val _listener = object : AdPlatformLoadListener() {
            override fun onPlatformError(error: AdPlatformError) {
                platformErrors.add(error)
                notifyLoadListeners(globalRewardedLoadListener, listener) { it.onPlatformError(error) }

                if ((index + 1) < rewardedAdPlatforms.size) {
                    activity.runOnUiThread {
                        _loadRewardedFromFirstAvailable(activity, placementGroupCode, listener, index + 1, platformErrors)
                    }
                } else {
                    val managerError = AdManagerError(
                        AdFormatEnum.REWARDED,
                        placementGroupCode,
                        rewardedAdPlatforms.map { it.type },
                        platformErrors.toList(),
                        "No rewarded found in all platforms"
                    )
                    notifyLoadListeners(globalRewardedLoadListener, listener) { it.onError(managerError) }
                }
            }

            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum) {
                notifyLoadListeners(globalRewardedLoadListener, listener) { it.onLoaded(adPlatformEnum) }
            }
        }

        candidate.platform.loadRewarded(activity, candidate.placementId, _listener)
    }

    private fun _loadRewarded(activity: Activity, placementGroupCode: String, placement: PlatformPlacement, listener: AdPlatformLoadListener? = null) {
        val _listener = object : AdPlatformLoadListener() {
            override fun onPlatformError(error: AdPlatformError) {
                notifyLoadListeners(globalRewardedLoadListener, listener) { it.onPlatformError(error) }
                val managerError = AdManagerError(
                    AdFormatEnum.REWARDED,
                    placementGroupCode,
                    listOf(placement.type),
                    listOf(error),
                    "Rewarded load failed for ${placement.type.name}"
                )
                notifyLoadListeners(globalRewardedLoadListener, listener) { it.onError(managerError) }
            }

            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum) {
                notifyLoadListeners(globalRewardedLoadListener, listener) { it.onLoaded(adPlatformEnum) }
            }
        }

        placement.platform.loadRewarded(activity, placement.placementId, _listener)
    }


    @JvmOverloads
    fun loadAndShowRewarded(
        activity: Activity, shownWhere: String = DEFAULT_INTERSTITIAL_SHOWN_WHERE_NAME, placementGroupCode: String = DEFAULT_PLACEMENT_GROUPCODE,
        listener: AdPlatformShowListener? = null, platformType: AdPlatformTypeEnum? = null
    ) {
        if (!showAds) return notifyShowDisabled(AdFormatEnum.REWARDED, placementGroupCode, globalRewardedShowListener, listener)
        validatePlacementGroup(placementGroupCode)
        /*
        call _showRewarded(listener, platform) in onLoaded and onError because of
        we wants call listener?.onError by _showRewarded
         */
        val loadListener: AdPlatformLoadListener = object : AdPlatformLoadListener() {
            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum) {
                _showRewarded(activity, shownWhere, placementGroupCode, listener, platformType)
            }

            override fun onError(error: AdManagerError) {
                _showRewarded(activity, shownWhere, placementGroupCode, listener, platformType)
            }
        }
        loadRewarded(activity, placementGroupCode, loadListener, platformType, false)
    }

    @JvmOverloads
    fun showRewarded(
        activity: Activity, shownWhere: String = DEFAULT_INTERSTITIAL_SHOWN_WHERE_NAME, placementGroupCode: String = DEFAULT_PLACEMENT_GROUPCODE,
        listener: AdPlatformShowListener? = null,
        platformType: AdPlatformTypeEnum? = null, loadAndShowIfNotExistsAdsOnAutoloadMode: Boolean = true
    ) {
        if (!showAds) return notifyShowDisabled(AdFormatEnum.REWARDED, placementGroupCode, globalRewardedShowListener, listener)
        validatePlacementGroup(placementGroupCode)

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
        activity: Activity, shownWhere: String = DEFAULT_INTERSTITIAL_SHOWN_WHERE_NAME, placementGroupCode: String,
        listener: AdPlatformShowListener? = null,
        platformType: AdPlatformTypeEnum? = null
    ): Boolean {
        if (!showAds) {
            notifyShowDisabled(AdFormatEnum.REWARDED, placementGroupCode, globalRewardedShowListener, listener)
            return false
        }

        val rewardedAdPlatforms = _getAdPlatformsWithSortedByAdFormat(placementGroupCode, AdFormatEnum.REWARDED)
        var displayLease: AppOpenAdDisplayGate.Lease? = null

        fun releaseDisplayLease() {
            displayLease?.let { releaseFullScreenDisplayLease(activity, it) }
            displayLease = null
        }

        // shared by "nothing loaded" and "platform failed to show"
        fun failShow(error: AdManagerError) {
            notifyShowListeners(globalRewardedShowListener, listener) { it.onError(error) }

            if (autoLoadForRewarded) {
                _autoloadRewardedByHandler(activity, placementGroupCode, null, platformType)
            }
        }

        val _listener = object : AdPlatformShowListener() {
            override fun onClosed(adPlatformEnum: AdPlatformTypeEnum) {
                AppOpenAdDisplayGate.recordFullScreenAdClosed()
                releaseDisplayLease()
                // on close load new one for next show
                notifyShowListeners(globalRewardedShowListener, listener) { it.onClosed(adPlatformEnum) }
                saveLastShowDate(AdFormatEnum.REWARDED)

                if (autoLoadForRewarded) {
                    _autoloadRewardedByHandler(activity, placementGroupCode, null, platformType)
                }
            }

            override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum) {
                stopAutoloadRewardedHandler()

                notifyShowListeners(globalRewardedShowListener, listener) { it.onDisplayed(adPlatformEnum) }
            }

            override fun onClicked(adPlatformEnum: AdPlatformTypeEnum) {
                notifyShowListeners(globalRewardedShowListener, listener) { it.onClicked(adPlatformEnum) }
            }

            override fun onRewarded(type: String?, amount: Int?, adPlatformEnum: AdPlatformTypeEnum) {
                notifyShowListeners(globalRewardedShowListener, listener) { it.onRewarded(type, amount, adPlatformEnum) }
            }

            override fun onPlatformError(error: AdPlatformError) {
                releaseDisplayLease()
                notifyShowListeners(globalRewardedShowListener, listener) { it.onPlatformError(error) }
                failShow(
                    AdManagerError(
                        AdFormatEnum.REWARDED,
                        placementGroupCode,
                        listOf(error.platform),
                        listOf(error),
                        "Rewarded show failed for ${error.platform.name}"
                    )
                )
            }
        }

        fun showLoadedRewarded(placement: PlatformPlacement): Boolean {
            displayLease = acquireFullScreenDisplayLease(activity)
            try {
                placement.platform.showRewarded(activity, placement.placementId, shownWhere, _listener)
            } catch (error: Exception) {
                _listener.onPlatformError(
                    AdPlatformError(
                        AdFormatEnum.REWARDED,
                        placement.type,
                        placement.placementId,
                        "${placement.type.name} rewarded show failed: ${error.message.orEmpty()}",
                        error
                    )
                )
            }
            return true
        }

        val placementToShow = if (platformType != null) {
            placementOf(placementGroupCode, platformType, AdFormatEnum.REWARDED)
                ?.takeIf { it.platform.isRewardedLoaded(it.placementId) }
        } else {
            rewardedAdPlatforms.firstOrNull { it.platform.isRewardedLoaded(it.placementId) }
        }

        if (placementToShow != null) {
            return showLoadedRewarded(placementToShow)
        }

        failShow(
            AdManagerError(
                AdFormatEnum.REWARDED,
                placementGroupCode,
                platformType?.let(::listOf) ?: rewardedAdPlatforms.map { it.type },
                message = "There is no loaded rewarded to show"
            )
        )
        return false
    }


    // ################################################################################
    // ################################### BANNER ######################################
    // ################################################################################
    @JvmOverloads
    fun showBanner(
        activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener? = null,
        placementGroupCode: String = DEFAULT_PLACEMENT_GROUPCODE, platformType: AdPlatformTypeEnum? = null
    ) {
        if (!showAds) return notifyShowDisabled(AdFormatEnum.BANNER, placementGroupCode, null, listener)
        validatePlacementGroup(placementGroupCode)

        if (platformType != null) {
            val placement = placementOf(placementGroupCode, platformType, AdFormatEnum.BANNER)
            if (placement == null) {
                listener?.onError(missingPlacementError(AdFormatEnum.BANNER, placementGroupCode, platformType))
                return
            }
            _showBanner(activity, placementGroupCode, placement, containerView, listener)
            return
        }

        val bannerAdPlatforms = _getAdPlatformsWithSortedByAdFormat(placementGroupCode, AdFormatEnum.BANNER)
        if (bannerAdPlatforms.isEmpty()) {
            listener?.onError(noPlatformError(AdFormatEnum.BANNER, placementGroupCode))
            return
        }

        // if already banner loaded, start from this platform
        val startFrom = bannerAdPlatforms.indexOfFirst { it.platform.isBannerLoaded(it.placementId) }.coerceAtLeast(0)
        _showBannerFromFirstAvailable(activity, placementGroupCode, containerView, listener, startFrom)
    }

    private fun _showBannerFromFirstAvailable(
        activity: Activity, placementGroupCode: String = DEFAULT_PLACEMENT_GROUPCODE, containerView: RelativeLayout,
        listener: AdPlatformShowListener? = null, platformIndex: Int = 0,
        platformErrors: MutableList<AdPlatformError> = mutableListOf()
    ) {
        val bannerAdPlatforms = _getAdPlatformsWithSortedByAdFormat(placementGroupCode, AdFormatEnum.BANNER)
        if (platformIndex >= bannerAdPlatforms.size) {
            return
        }

        val _listener = object : AdPlatformShowListener() {
            override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum) {
                saveLastShowDate(AdFormatEnum.BANNER)
                listener?.onDisplayed(adPlatformEnum)
            }

            override fun onClicked(adPlatformEnum: AdPlatformTypeEnum) {
                listener?.onClicked(adPlatformEnum)
            }

            override fun onPlatformError(error: AdPlatformError) {
                platformErrors.add(error)
                listener?.onPlatformError(error)

                if ((platformIndex + 1) < bannerAdPlatforms.size) {
                    activity.runOnUiThread {
                        _showBannerFromFirstAvailable(activity, placementGroupCode, containerView, listener, platformIndex + 1, platformErrors)
                    }
                } else {
                    listener?.onError(
                        AdManagerError(
                            AdFormatEnum.BANNER,
                            placementGroupCode,
                            bannerAdPlatforms.map { it.type },
                            platformErrors.toList(),
                            "No banner found in all platforms"
                        )
                    )
                }
            }
        }

        val placement = bannerAdPlatforms[platformIndex]
        placement.platform.showBanner(activity, placement.placementId, containerView, _listener)
    }

    private fun _showBanner(
        activity: Activity, placementGroupCode: String, placement: PlatformPlacement,
        containerView: RelativeLayout, listener: AdPlatformShowListener? = null
    ) {
        placement.platform.showBanner(activity, placement.placementId, containerView, object : AdPlatformShowListener() {
            override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum) {
                saveLastShowDate(AdFormatEnum.BANNER)
                listener?.onDisplayed(adPlatformEnum)
            }

            override fun onClicked(adPlatformEnum: AdPlatformTypeEnum) {
                listener?.onClicked(adPlatformEnum)
            }

            override fun onPlatformError(error: AdPlatformError) {
                listener?.onPlatformError(error)
                listener?.onError(
                    AdManagerError(
                        AdFormatEnum.BANNER,
                        placementGroupCode,
                        listOf(placement.type),
                        listOf(error),
                        "Banner show failed for ${error.platform.name}"
                    )
                )
            }
        })
    }

    // ################################################################################
    // ################################### MREC ######################################
    // ################################################################################
    @JvmOverloads
    fun showMrec(
        activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener? = null,
        placementGroupCode: String = DEFAULT_PLACEMENT_GROUPCODE, platformType: AdPlatformTypeEnum? = null
    ) {
        if (!showAds) return notifyShowDisabled(AdFormatEnum.MREC, placementGroupCode, null, listener)
        validatePlacementGroup(placementGroupCode)

        if (platformType != null) {
            val placement = placementOf(placementGroupCode, platformType, AdFormatEnum.MREC)
            if (placement == null) {
                listener?.onError(missingPlacementError(AdFormatEnum.MREC, placementGroupCode, platformType))
                return
            }
            _showMrec(activity, placementGroupCode, placement, containerView, listener)
            return
        }

        val mrecAdPlatforms = _getAdPlatformsWithSortedByAdFormat(placementGroupCode, AdFormatEnum.MREC)
        if (mrecAdPlatforms.isEmpty()) {
            listener?.onError(noPlatformError(AdFormatEnum.MREC, placementGroupCode))
            return
        }

        // if already mrec loaded, start from this platform
        val startFrom = mrecAdPlatforms.indexOfFirst { it.platform.isMrecLoaded(it.placementId) }.coerceAtLeast(0)
        _showMrecFromFirstAvailable(activity, placementGroupCode, containerView, listener, startFrom)
    }

    private fun _showMrecFromFirstAvailable(
        activity: Activity, placementGroupCode: String = DEFAULT_PLACEMENT_GROUPCODE, containerView: RelativeLayout,
        listener: AdPlatformShowListener? = null, platformIndex: Int = 0,
        platformErrors: MutableList<AdPlatformError> = mutableListOf()
    ) {
        val mrecAdPlatforms = _getAdPlatformsWithSortedByAdFormat(placementGroupCode, AdFormatEnum.MREC)
        if (platformIndex >= mrecAdPlatforms.size) {
            return
        }

        val _listener = object : AdPlatformShowListener() {
            override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum) {
                saveLastShowDate(AdFormatEnum.MREC)
                listener?.onDisplayed(adPlatformEnum)
            }

            override fun onClicked(adPlatformEnum: AdPlatformTypeEnum) {
                listener?.onClicked(adPlatformEnum)
            }

            override fun onPlatformError(error: AdPlatformError) {
                platformErrors.add(error)
                listener?.onPlatformError(error)

                if ((platformIndex + 1) < mrecAdPlatforms.size) {
                    activity.runOnUiThread {
                        _showMrecFromFirstAvailable(activity, placementGroupCode, containerView, listener, platformIndex + 1, platformErrors)
                    }
                } else {
                    listener?.onError(
                        AdManagerError(
                            AdFormatEnum.MREC,
                            placementGroupCode,
                            mrecAdPlatforms.map { it.type },
                            platformErrors.toList(),
                            "No MREC found in all platforms"
                        )
                    )
                }
            }
        }

        val placement = mrecAdPlatforms[platformIndex]
        placement.platform.showMrec(activity, placement.placementId, containerView, _listener)
    }

    private fun _showMrec(
        activity: Activity, placementGroupCode: String, placement: PlatformPlacement,
        containerView: RelativeLayout, listener: AdPlatformShowListener? = null
    ) {
        placement.platform.showMrec(activity, placement.placementId, containerView, object : AdPlatformShowListener() {
            override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum) {
                saveLastShowDate(AdFormatEnum.MREC)
                listener?.onDisplayed(adPlatformEnum)
            }

            override fun onClicked(adPlatformEnum: AdPlatformTypeEnum) {
                listener?.onClicked(adPlatformEnum)
            }

            override fun onPlatformError(error: AdPlatformError) {
                listener?.onPlatformError(error)
                listener?.onError(
                    AdManagerError(
                        AdFormatEnum.MREC,
                        placementGroupCode,
                        listOf(placement.type),
                        listOf(error),
                        "MREC show failed for ${error.platform.name}"
                    )
                )
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

    /** Call from Activity.onDestroy: releases what this manager still holds for the activity. */
    fun onDestroyActivity(activity: Activity) {
        releaseFullScreenDisplayLeases(activity)
        removeLoadingViewFromActivity(activity)
    }


    private fun saveLastShowDate(adFormatEnum: AdFormatEnum) {
        lastShowDateByAdFormat.put(adFormatEnum, Date())
    }

    fun randInt(min: Int, max: Int): Int {
        return Random().nextInt(max - min + 1) + min
    }

    private var lastPostDelayedSetTimeForInterstitialLoad: Date? = null
    private fun _autoloadInterstitialByHandler(activity: Activity, placementGroupCode: String = DEFAULT_PLACEMENT_GROUPCODE, listener: AdPlatformLoadListener? = null, platformType: AdPlatformTypeEnum? = null) {
        if (hasWorkingAutoloadInterstitialHandler) {
            if (lastPostDelayedSetTimeForInterstitialLoad != null) {
                val diffSeconds = TimeUnit.SECONDS.convert(Date().time - lastPostDelayedSetTimeForInterstitialLoad!!.time, TimeUnit.MILLISECONDS)
                if (diffSeconds < (autoLoadDelay + 2)) {
                    return
                }
            }
        }

        initHandlers()

        hasWorkingAutoloadInterstitialHandler = true
        lastPostDelayedSetTimeForInterstitialLoad = Date()

        autoloadInterstitialHandler?.postDelayed({
            try {
                activity.runOnUiThread {
                    loadInterstitial(activity, placementGroupCode, listener, platformType, false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            hasWorkingAutoloadInterstitialHandler = false
        }, autoLoadDelay * 1000)
    }

    private var lastPostDelayedSetTimeForRewardedLoad: Date? = null
    private fun _autoloadRewardedByHandler(activity: Activity, placementGroupCode: String = DEFAULT_PLACEMENT_GROUPCODE, listener: AdPlatformLoadListener? = null, platformType: AdPlatformTypeEnum? = null) {
        if (hasWorkingAutoloadRewardedHandler) {
            if (lastPostDelayedSetTimeForRewardedLoad != null) {
                val diffSeconds = TimeUnit.SECONDS.convert(Date().time - lastPostDelayedSetTimeForRewardedLoad!!.time, TimeUnit.MILLISECONDS)
                if (diffSeconds < (autoLoadDelay + 2)) {
                    return
                }
            }
        }

        initHandlers()

        hasWorkingAutoloadRewardedHandler = true
        lastPostDelayedSetTimeForRewardedLoad = Date()

        autoloadRewardedHandler?.postDelayed({
            try {
                activity.runOnUiThread {
                    loadRewarded(activity, placementGroupCode, listener, platformType, false)
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
        releaseAllFullScreenDisplayLeases()
        clearLoadingView()

        adPlatforms.forEach {
            it.destroy(activity)
        }

        stopAutoloadInterstitialHandler()
        stopAutoloadRewardedHandler()
        handlerThread?.quit()
        handlerThread = null
    }


    // AD SHOWING VIEW
    var isEnableShowLoadingViewForInterstitial: Boolean = true
    private var loadingView: AdLoadingCustomView? = null

    private fun clearLoadingView(activity: Activity? = null) {
        val view = loadingView ?: return
        if (activity != null && view.context !== activity) return

        (view.parent as? ViewGroup)?.removeView(view)
        loadingView = null
    }

    fun initLoadingView(activity: Activity, rootView: ViewGroup? = null) {
        // Never reuse a View created with another Activity context.
        clearLoadingView()
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
        } catch (e: Exception) {
            clearLoadingView(activity)
            e.printStackTrace()
        }
    }

    fun removeLoadingViewFromActivity(activity: Activity) {
        try {
            clearLoadingView(activity)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}
