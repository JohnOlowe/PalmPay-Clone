plugins {
    alias(libs.plugins.android.application)
}

import java.util.Properties

val localProps = Properties()
rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(localProps::load)
val paystackApiKey: String =
    localProps.getProperty("PAYSTACK_API_KEY")
        ?: System.getenv("PAYSTACK_API_KEY")
        ?: ""

android {
    namespace = "damjay.palmpay.clone"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "damjay.palmpay.clone"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "PAYSTACK_API_KEY", "\"" + paystackApiKey + "\"")
    }

    // Pin every build to one keystore so the APK signature never changes between
    // builds. Credentials live in gradle.properties (not hard-coded here).
    signingConfigs {
        create("stable") {
            val storeFilePath = (project.findProperty("PALMPAY_CLONE_STORE_FILE") as? String)
                ?: "keystore/damjay_debug.keystore"
            storeFile = rootProject.file(storeFilePath)
            storePassword = project.findProperty("PALMPAY_CLONE_STORE_PASSWORD") as? String
            keyAlias = project.findProperty("PALMPAY_CLONE_KEY_ALIAS") as? String
            keyPassword = project.findProperty("PALMPAY_CLONE_KEY_PASSWORD") as? String
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("stable")
        }
        // To also sign release builds with the same stable key, uncomment:
        // getByName("release") {
        //     signingConfig = signingConfigs.getByName("stable")
        // }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { it.testLogging.showStandardStreams = true }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
