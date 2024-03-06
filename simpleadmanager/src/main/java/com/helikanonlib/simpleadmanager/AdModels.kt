package com.helikanonlib.simpleadmanager

abstract class AdPlatformLoadListener() {
    open fun onLoaded(adPlatformEnum: AdPlatformTypeEnum?) {}
    open fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {}
}


abstract class AdPlatformShowListener() {
    open fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum?) {}
    open fun onClicked(adPlatformEnum: AdPlatformTypeEnum?) {}
    open fun onClosed(adPlatformEnum: AdPlatformTypeEnum?) {}
    open fun onRewarded(type: String? = null, amount: Int? = null, adPlatformEnum: AdPlatformTypeEnum?) {}
    open fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {}
}


enum class AdErrorMode {
    MANAGER,
    PLATFORM
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
    NATIVE_SMALL,
    NATIVE_MEDIUM,
    APP_OPEN
}

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
    var nativeSmall: String = "",
    var nativeMedium: String = "",
    var appOpenAd: String = ""

)


/*
data class AdPlacementGroupModel @JvmOverloads constructor(
    var groupName: String,
    var interstitial: String = "",
    var rewarded: String = "",
    var banner: String = "",
    var mrec: String = "",
    var native: String = "",
    var appOpenAd: String = "",
    var nativeMedium: String = ""

)*/
