package com.helikanonlibsample.simpleadmanager

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.helikanonlib.simpleadmanager.AdManagerError
import com.helikanonlib.simpleadmanager.AdPlatformError
import com.helikanonlib.simpleadmanager.AdPlatformShowListener
import com.helikanonlib.simpleadmanager.AdPlatformTypeEnum
import com.helikanonlibsample.simple_admanager.MyApplication
import com.helikanonlibsample.simpleadmanager.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initAds()
        initViews()


        MyApplication.adManager.showBanner(this, binding.bannerContainer, object : AdPlatformShowListener() {
            override fun onPlatformError(error: AdPlatformError) {
                Log.d("MyApplication.adManager", "[BANNER] platform error ${error.platform.name} > ${error.message}")
            }

            override fun onError(error: AdManagerError) {
                Log.d("MyApplication.adManager", "[BANNER] no banner > ${error.message}")
            }
        })
        Handler(Looper.getMainLooper()).postDelayed({
            MyApplication.adManager.showMrec(this, binding.mrecContainer, object : AdPlatformShowListener() {
                override fun onPlatformError(error: AdPlatformError) {
                    Log.d("MyApplication.adManager", "[MREC] platform error ${error.platform.name} > ${error.message}")
                }

                override fun onError(error: AdManagerError) {
                    Log.d("MyApplication.adManager", "[MREC] no mrec > ${error.message}")
                }
            })
        }, 3000)

    }

    fun initViews() {
        binding.btnShowInterstitial.setOnClickListener {
            MyApplication.adManager.showInterstitial(this, "defaultPlace", "default", object : AdPlatformShowListener() {
                override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum) {
                    super.onDisplayed(adPlatformEnum)
                }

                override fun onClicked(adPlatformEnum: AdPlatformTypeEnum) {
                    super.onClicked(adPlatformEnum)
                }

                override fun onClosed(adPlatformEnum: AdPlatformTypeEnum) {
                    super.onClosed(adPlatformEnum)
                }

                override fun onPlatformError(error: AdPlatformError) {
                    super.onPlatformError(error)
                }

                override fun onError(error: AdManagerError) {
                    super.onError(error)
                }

            }) // if autoload mode is false it will load and show
        }

        binding.btnShowRewarded.setOnClickListener {
            MyApplication.adManager.showRewarded(this, "defaultPlace", "default", object : AdPlatformShowListener() {
                override fun onRewarded(type: String?, amount: Int?, adPlatformEnum: AdPlatformTypeEnum) {
                    super.onRewarded(type, amount, adPlatformEnum)
                }
            }) // if autoload mode is false it will load and show
        }

        binding.btnShowInterstitialForTimeStrategy.setOnClickListener {
            val result = MyApplication.adManager.showInterstitialForTimeStrategy(this)
            Log.d("MyApplication.adManager", "time strategy result: $result")
        }


        binding.btnLoadAndShowInterstitial.setOnClickListener {
            MyApplication.adManager.loadAndShowInterstitial(this)
        }

        binding.btnLoadAndShowRewarded.setOnClickListener {
            MyApplication.adManager.loadAndShowRewarded(this)
        }

        binding.btnLoadAppOpenAd.setOnClickListener {
            val result = MyApplication.admobAppOpenAdManager?.show("admob", this)
            Log.d("MyApplication.adManager", "app open show result: $result")
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

    override fun onDestroy() {
        MyApplication.adManager.onDestroyActivity(this)
        super.onDestroy()
    }


    private fun initAds() {
        MyApplication.adManager.initializePlatformsWithActivity(this)
        MyApplication.adManager.loadInterstitial(this)
    }

}
