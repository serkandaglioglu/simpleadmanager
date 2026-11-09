package com.helikanonlib.simpleadmanager

abstract class AdPlatformLoadListener() {
    open fun onLoaded(adPlatformEnum: AdPlatformTypeEnum) {}

    /** One platform failed. The manager may still try the next platform. */
    open fun onPlatformError(error: AdPlatformError) {}

    /** Terminal: the manager could not complete the request (all platforms failed, nothing configured, ads disabled...). */
    open fun onError(error: AdManagerError) {}
}


abstract class AdPlatformShowListener() {
    open fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum) {}
    open fun onClicked(adPlatformEnum: AdPlatformTypeEnum) {}
    open fun onClosed(adPlatformEnum: AdPlatformTypeEnum) {}
    open fun onRewarded(type: String? = null, amount: Int? = null, adPlatformEnum: AdPlatformTypeEnum) {}
    open fun onPlatformError(error: AdPlatformError) {}
    open fun onError(error: AdManagerError) {}
}


enum class AdPlatformTypeEnum {
    FACEBOOK,
    ADMOB,
    STARTAPP,
    ADINCUBE,
    IRONSOURCE,
    MOPUB,
    UNITYADS,
    ADMOST,
    APPLOVIN
}

enum class AdFormatEnum {
    INTERSTITIAL,
    BANNER,
    REWARDED,
    MREC,
    APP_OPEN
}

enum class AdInitializationStatus {
    SUCCESS,
    PARTIAL_SUCCESS,
    FAILURE,
    DISABLED
}

enum class AdShowRequestResult {
    SHOW_REQUESTED,
    THROTTLED,
    DISABLED
}

data class AdPlatformInitializationResult(
    val platform: AdPlatformTypeEnum,
    val isSuccessful: Boolean
)

data class AdInitializationResult(
    val status: AdInitializationStatus,
    val platformResults: List<AdPlatformInitializationResult> = emptyList(),
    val message: String? = null
) {
    val initializedPlatforms: List<AdPlatformTypeEnum>
        get() = platformResults.filter { it.isSuccessful }.map { it.platform }

    val failedPlatforms: List<AdPlatformTypeEnum>
        get() = platformResults.filterNot { it.isSuccessful }.map { it.platform }
}

data class AdPlatformOrderResult(
    val isSuccessful: Boolean,
    val message: String? = null
)

data class AdPlatformError @JvmOverloads constructor(
    val format: AdFormatEnum,
    val platform: AdPlatformTypeEnum,
    val placementId: String,
    val message: String,
    val cause: Throwable? = null
)

/** placementGroupCode is empty for app open ads, which are not tied to a placement group. */
data class AdManagerError @JvmOverloads constructor(
    val format: AdFormatEnum,
    val placementGroupCode: String,
    val attemptedPlatforms: List<AdPlatformTypeEnum> = emptyList(),
    val platformErrors: List<AdPlatformError> = emptyList(),
    val message: String
)

data class PlatformAdInstance @JvmOverloads constructor(
    var format: AdFormatEnum,
    var placementId: String,
    var instance: Any?,

    )


data class AdPlacementGroup @JvmOverloads constructor(
    var groupCode: String = "default",
    var platformType: AdPlatformTypeEnum,

    var interstitial: String = "",
    var rewarded: String = "",
    var banner: String = "",
    var mrec: String = "",
    var appOpenAd: String = ""

)
