// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.androidLibrary) apply false

    id("com.google.gms.google-services") version "4.4.1" apply false

    // id ("com.applovin.quality.AppLovinQualityServiceGradlePlugin") version "5.1.2" apply false

}

buildscript {
    repositories {
        maven { url = uri("https://artifacts.applovin.com/android") }
    }
    dependencies {
        classpath ("com.applovin.quality:AppLovinQualityServiceGradlePlugin:+")
    }
}
