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
            version = "1.3.4"

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
    implementation("com.applovin:applovin-sdk:+")
    implementation("com.applovin.mediation:fyber-adapter:+")
    implementation("com.applovin.mediation:google-ad-manager-adapter:+")
    implementation("com.applovin.mediation:google-adapter:+")
    implementation("com.applovin.mediation:vungle-adapter:+")
    implementation("com.applovin.mediation:facebook-adapter:+")
    implementation("com.applovin.mediation:mintegral-adapter:+")
    implementation("com.applovin.mediation:bytedance-adapter:+")
    implementation("com.applovin.mediation:unityads-adapter:+")

}