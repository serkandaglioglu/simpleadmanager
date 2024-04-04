plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("kotlin-kapt")
    id("maven-publish")
}

publishing{
    publications{
        register<MavenPublication>("release"){
            groupId = "com.helikanonlib"
            artifactId = "simpleadmanager"
            version = "1.2.6"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}

android {
    namespace = "com.helikanonlib.simpleadmanager"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("androidx.lifecycle:lifecycle-process:2.7.0")

    // google service
    implementation ("com.google.android.gms:play-services-ads:22.6.0")
    implementation ("com.google.android.gms:play-services-appset:16.0.2")
    implementation ("com.google.android.gms:play-services-ads-identifier:18.0.1")


    // APPLOVIN
    implementation("com.applovin:applovin-sdk:12.2.0")
    // implementation("com.applovin.mediation:adcolony-adapter:4.8.0.4") // applovin 12.2.0 sürümünde kaldırılmıştı sitesinden
    implementation("com.applovin.mediation:fyber-adapter:8.2.6.0")
    implementation("com.applovin.mediation:google-adapter:22.6.0.1")
    implementation("com.applovin.mediation:google-ad-manager-adapter:22.6.0.1")
    implementation("com.applovin.mediation:ironsource-adapter:7.8.0.0.0")
    implementation("com.applovin.mediation:facebook-adapter:6.16.0.2")
    implementation("com.applovin.mediation:mintegral-adapter:16.6.51.0") // admostla beraber sorun çıkıyo
    implementation("com.applovin.mediation:bytedance-adapter:5.8.0.7.0")
    implementation("com.applovin.mediation:unityads-adapter:4.9.3.0")
    implementation("com.applovin.mediation:vungle-adapter:7.1.0.0")
}