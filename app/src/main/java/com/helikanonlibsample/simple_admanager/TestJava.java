package com.helikanonlibsample.simple_admanager;

import android.app.Activity;

import com.helikanonlib.simpleadmanager.SimpleAdManager;

public class TestJava {

    void testFun(Activity activity){
        SimpleAdManager adManager = new SimpleAdManager();
        adManager.setEnableShowLoadingViewForInterstitial(true);
    }

}
