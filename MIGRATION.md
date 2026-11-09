# SimpleAdManager 1.4.0 geçiş rehberi

1.4.0 geriye uyumlu değildir. Değişiklikler `admanager` kütüphanesindeki 2026 yazı değişiklikleriyle aynı mantığı izler.

## Kütüphaneyi kullanan uygulama gereksinimleri

- Kotlin 2.3 veya üstü (kütüphane Kotlin 2.4 ile derleniyor).
- `compileSdk` 37 veya üstü (AGP 9 ve `androidx.core` 1.19 bunu zorunlu kılıyor).

## Native reklamlar kaldırıldı

`loadNativeAds`, `loadSmallNativeAds`, `loadMediumNativeAds`, `showNative`, `hasLoadedNativeAds`, `getLoadedNativeAdsCount`,
`AdFormatEnum.NATIVE_SMALL` / `NATIVE_MEDIUM` ve `AdPlacementGroup.nativeSmall` / `nativeMedium` artık yok.

## Hata callback API'si

`AdErrorMode` kaldırıldı. Platform hatası ile manager'ın terminal hatası ayrı callback'lere bölündü:

```kotlin
object : AdPlatformLoadListener() {
    override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum) {}
    override fun onPlatformError(error: AdPlatformError) {}   // tek bir platform başarısız (sıradaki denenebilir)
    override fun onError(error: AdManagerError) {}            // terminal: istek tamamlanamadı
}
```

- Platform parametreleri artık nullable değil: `AdPlatformTypeEnum?` yerine `AdPlatformTypeEnum`.
- `AdPlatformError`: `format`, `platform`, `placementId`, `message`, `cause`.
- `AdManagerError`: `format`, `placementGroupCode` (App Open için boş), `attemptedPlatforms`, `platformErrors`, `message`.
- Aynı nesne hem global hem lokal listener ise callback bir kez çağrılır.
- `showAds == false` iken load/show çağrıları sessizce dönmez, `onError` ("Ads are disabled...") gelir.
- Platform listesi boşsa veya ilgili formatta placement yoksa `onError` gelir (eskiden callback gelmiyor ya da exception atılıyordu).

## Kurulum

`adPlatforms`, `placementGroups`, `adPlatformSortByAdFormat`, `lastShowDateByAdFormat`, `loadingView` dışarıya kapatıldı:

```kotlin
SimpleAdManager().apply {
    setPlacementGroups(listOf(AdPlacementGroup(...), AdPlacementGroup(...)))
    setAdPlatforms(listOf(AdmobAds(...), ApplovinAds(...)))
}
```

- Bilinmeyen `placementGroupCode` ile yapılan çağrı `IllegalArgumentException` atar (yazım hatası erken yakalansın diye).
- `autoLoadDelay`, `randomInterval`, `interstitialMinElapsedSecondsToNextShow`, `rewardedMinElapsedSecondsToNextShow` negatif olamaz.
- `setAdPlatformSortByAdFormatStr(...)` artık `AdPlatformOrderResult` döner; bilinmeyen format/platform, tekrar eden platform,
  eklenmemiş platform veya o grupta placement'ı olmayan platform reddedilir. `APP_OPEN` için sıralama desteklenmez
  (`AppOpenAdManager` kendi sırasını kullanır). Yeni `setAdPlatformOrder(groupCode, format, vararg platforms)` de var.
- Sıralama verilmemiş bir format için, ilgili grupta placement'ı olan tüm platformlar eklenme sırasıyla denenir.

## Başlatma

```kotlin
adManager.initializePlatforms(
    applicationContext,
    onInitializeComplete = { result -> /* SUCCESS, PARTIAL_SUCCESS, FAILURE, DISABLED */ },
    onPlatformInitializeComplete = { result -> /* result.platform, result.isSuccessful */ }
)
adManager.isAllPlatformsSdksInitialized()
adManager.isPlatformSdkInitialized(AdPlatformTypeEnum.ADMOB)
```

`AdPlatformWrapper.initialize(...)` imzasına `onInitializeComplete: ((Boolean) -> Unit)?` parametresi eklendi.

## Diğer davranış değişiklikleri

- `showInterstitialForTimeStrategy(...)` artık `AdShowRequestResult` döner (`SHOW_REQUESTED`, `THROTTLED`, `DISABLED`).
- `autoLoadDelay` artık gerçekten uygulanıyor: kapanan reklamdan sonraki otomatik yükleme bu kadar saniye sonra başlar.
- Activity'nin `onDestroy` metodunda `adManager.onDestroyActivity(this)` çağrılması önerilir.
- Yükleniyor görünümü başka bir Activity'nin context'iyle tekrar kullanılmaz.

## App Open

`AppOpenAdManager` yeniden yazıldı ve artık uygulamanın yaşam döngüsünü kendisi dinlemiyor (`@OnLifecycleEvent` kaldırıldı).
Uygulama ön plana geldiğinde gösterim için örnek uygulamadaki `AppOpenLifecycleController` sınıfını projenize kopyalayın
(`androidx.lifecycle:lifecycle-process` bağımlılığı gerekir):

```kotlin
appOpenAdManager = AppOpenAdManager(application, placements, "admob,applovin", showListener, loadListener)
AppOpenLifecycleController(application) { appOpenAdManager }.register()
// SDK başlatıldıktan sonra:
appOpenAdManager.load()
```

- `isEnable` kaldırıldı: `enable()`, `disable()` (bekleyen yüklemeleri hata ile tamamlar), `pauseShowing()`, `resumeShowing()`.
- `show(activity)`, `show(showOrder, activity)` ve `showIntervalElapsed(activity)` artık `AppOpenAdShowResult` döner.
- Interstitial/rewarded açıkken ya da kapandıktan sonraki `minElapsedSecondsAfterFullScreenAd` saniye içinde App Open gösterilmez.
- Reklam geçerlilik süresi `adValidityDurationMillis` constructor parametresiyle ayarlanır (varsayılan 4 saat).
- `loadAdmob`, `showAdmob`, `loadApplovin`, `showApplovin`, `onStart`, `AppOpenAdBaseLifeCycle` kaldırıldı.
