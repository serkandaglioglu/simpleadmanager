package com.helikanonlibsample.simple_admanager

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.helikanonlib.simpleadmanager.*
import com.helikanonlibsample.simple_admanager.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initAds()
        initViews()

        Handler(Looper.getMainLooper()).postDelayed({
            MyApplication.adManager?.showNative(this@MainActivity, AdFormatEnum.NATIVE_SMALL, binding.nativeContainer)
        }, 5000)

        Handler(Looper.getMainLooper()).postDelayed({
            MyApplication.admobAppOpenAdManager?.isEnable = true
        }, 2000)



        MyApplication.adManager.showBanner(this, binding.bannerContainer, object : AdPlatformShowListener() {
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                Log.d("MyApplication.adManager", "[BANNER] AdErrorMode.PLATFORM showBanner>> $errorMode $errorMessage ${adPlatformEnum?.name}")
            }
        })
        Handler(Looper.getMainLooper()).postDelayed({
            MyApplication.adManager.showMrec(this, binding.mrecContainer, object : AdPlatformShowListener() {
                override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                    Log.d("MyApplication.adManager", "[MREC] AdErrorMode.PLATFORM showMrec>> $errorMode $errorMessage ${adPlatformEnum?.name}")
                }
            })
        }, 3000)

    }

    fun initViews() {
        binding.btnShowInterstitial.setOnClickListener {
            MyApplication.adManager.showInterstitial(this, "defaultPlace", "default", object : AdPlatformShowListener() {
                override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum?) {
                    super.onDisplayed(adPlatformEnum)
                }

                override fun onClicked(adPlatformEnum: AdPlatformTypeEnum?) {
                    super.onClicked(adPlatformEnum)
                }

                override fun onClosed(adPlatformEnum: AdPlatformTypeEnum?) {
                    super.onClosed(adPlatformEnum)
                }

                override fun onRewarded(type: String?, amount: Int?, adPlatformEnum: AdPlatformTypeEnum?) {
                    super.onRewarded(type, amount, adPlatformEnum)
                }

                override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                    super.onError(errorMode, errorMessage, adPlatformEnum)
                }

            }) // if autoload mode is false it will load and show
        }

        binding.btnShowRewarded.setOnClickListener {
            MyApplication.adManager.showRewarded(this, "defaultPlace", "default", object : AdPlatformShowListener() {
                override fun onRewarded(type: String?, amount: Int?, adPlatformEnum: AdPlatformTypeEnum?) {
                    super.onRewarded(type, amount, adPlatformEnum)
                }
            }) // if autoload mode is false it will load and show
        }

        binding.btnShowInterstitialForTimeStrategy.setOnClickListener {
            MyApplication.adManager.showInterstitialForTimeStrategy(this)
        }


        binding.btnLoadAndShowInterstitial.setOnClickListener {
            MyApplication.adManager.loadAndShowInterstitial(this)
        }

        binding.btnLoadAndShowRewarded.setOnClickListener {
            MyApplication.adManager.loadAndShowRewarded(this)
        }

        binding.btnLoadAppOpenAd.setOnClickListener {
            MyApplication.admobAppOpenAdManager?.show("admob", this, null)

            //MyApplication.admobAppOpenAdManager?.disable()
        }
    }


    override fun onResume() {
        super.onResume()

        MyApplication.adManager.onResume(this)

    }

    override fun onPause() {
        super.onPause()
        MyApplication.adManager.onPause(this)
    }


    private fun initAds() {
        MyApplication.adManager.initializePlatformsWithActivity(this)
        MyApplication.adManager.loadInterstitial(this)
        MyApplication.adManager.loadNativeAds(this, AdFormatEnum.NATIVE_SMALL, 3, object : AdPlatformLoadListener() {
            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum?) {
                super.onLoaded(adPlatformEnum)
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                super.onError(errorMode, errorMessage, adPlatformEnum)
            }

        })

        MyApplication.adManager.loadNativeAds(this, AdFormatEnum.NATIVE_MEDIUM, 3, object : AdPlatformLoadListener() {
            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum?) {
                super.onLoaded(adPlatformEnum)
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                super.onError(errorMode, errorMessage, adPlatformEnum)
            }

        })
    }


}