plugins {
    alias(libs.plugins.androidLibrary)
    id("maven-publish")
}

publishing{
    publications{
        register<MavenPublication>("release"){
            groupId = "com.helikanonlib"
            artifactId = "simpleadmanager"
            version = "1.3.8"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}

android {
    namespace = "com.helikanonlib.simpleadmanager"
    compileSdk = 37

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

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    publishing {
        singleVariant("release")
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


    // implementation(libs.firebase.crashlytics.buildtools) // NativeTemplateStyle.java içinde bulunan @CanIgnoreReturnValue annotation için ekledik

    // google service
    implementation (libs.play.services.ads)
    implementation (libs.play.services.appset)
    implementation (libs.play.services.ads.identifier)


    // APPLOVIN
    implementation("com.applovin:applovin-sdk:13.7.0")
    implementation("com.applovin.mediation:fyber-adapter:8.5.1.0")
    implementation("com.applovin.mediation:google-ad-manager-adapter:25.5.0.0")
    implementation("com.applovin.mediation:google-adapter:25.5.0.0")
    implementation("com.applovin.mediation:vungle-adapter:7.8.0.0")
    implementation("com.applovin.mediation:facebook-adapter:6.22.0.1")
    implementation("com.applovin.mediation:mintegral-adapter:17.2.21.0")
    implementation("com.applovin.mediation:bytedance-adapter:8.4.0.5.0"){
        exclude(module = "tiktok-business-android-sdk-comp")
    }
    implementation("com.applovin.mediation:unityads-adapter:4.21.0.1")

    // inmobi begin
    implementation("com.applovin.mediation:inmobi-adapter:11.5.1.0")
    implementation("com.squareup.picasso:picasso:2.8")
    // implementation("androidx.recyclerview:recyclerview:1.1.0") // zaten yukarıda ekli
    // inmobi end

}