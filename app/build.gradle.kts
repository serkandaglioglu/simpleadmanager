plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("kotlin-kapt")
    id("applovin-quality-service")
}

applovin {
    apiKey = "yOGLuhTmu0KZ0hihHeBgrZlHF622HDt9VSgLv1WrypvzRupBc5ZH141KQvOmP_4oXHtUG2DSFDaB5utqcD_XyT"
}

android {
    namespace = "com.helikanonlibsample.simpleadmanager"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.helikanonlibsample.simpleadmanager"
        minSdk = 24
        targetSdk = 34
        versionCode = 26
        versionName = "1.2.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
    }


    buildTypes {
        release {
            isShrinkResources = false
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.google.android.gms:play-services-ads:22.6.0")
    implementation(project(":simpleadmanager"))
}