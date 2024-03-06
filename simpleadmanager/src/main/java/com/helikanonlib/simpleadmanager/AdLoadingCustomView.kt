package com.helikanonlib.simpleadmanager

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.helikanonlib.simpleadmanager.databinding.AdLoadingInfoBinding

class AdLoadingCustomView(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
    private lateinit var binding: AdLoadingInfoBinding

    init {
        binding = AdLoadingInfoBinding.inflate(LayoutInflater.from(context))
        addView(binding.root)

        binding.textViewCloseAdsLoading.setOnClickListener {
            //binding.root.visibility = View.GONE

            (this@AdLoadingCustomView.parent as ViewGroup).removeView(this@AdLoadingCustomView)
        }

        binding.progressBarAds.setOnClickListener {
            binding.textViewCloseAdsLoading.performClick()
        }

        binding.textViewShowingAds.setOnClickListener {
            binding.textViewCloseAdsLoading.performClick()
        }

        binding.cardViewContainer.setOnClickListener {
            binding.textViewCloseAdsLoading.performClick()
        }
    }

}