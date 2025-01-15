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
            version = "1.3.2"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}

android {
    namespace = "com.helikanonlib.simpleadmanager"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        manifestPlaceholders["admobAppId"] = "\${admobAppId}"
    }

    buildTypes {
        release {
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.androidx.lifecycle.process)

    // google service
    implementation (libs.play.services.ads)
    implementation (libs.play.services.appset)
    implementation (libs.play.services.ads.identifier)


    // APPLOVIN
    implementation("com.applovin:applovin-sdk:13.0.1")
    // implementation("com.applovin.mediation:adcolony-adapter:4.8.0.4") // applovin 12.2.0 sürümünde kaldırılmıştı sitesinden
    implementation("com.applovin.mediation:fyber-adapter:8.3.5.0")
    implementation("com.applovin.mediation:google-adapter:23.6.0.1")
    implementation("com.applovin.mediation:google-ad-manager-adapter:23.6.0.1")
    implementation("com.applovin.mediation:ironsource-adapter:8.6.1.0.0")
    implementation("com.applovin.mediation:facebook-adapter:6.18.0.1")
    implementation("com.applovin.mediation:mintegral-adapter:16.9.31.0") // admostla beraber sorun çıkıyo
    implementation("com.applovin.mediation:bytedance-adapter:6.4.0.6.0")
    implementation("com.applovin.mediation:unityads-adapter:4.13.0.0")
    implementation("com.applovin.mediation:vungle-adapter:7.4.2.2")
}