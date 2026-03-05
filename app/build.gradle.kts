plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

detekt {
    config.setFrom(files("${rootProject.projectDir}/detekt.yml"))
    buildUponDefaultConfig = true
}

android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    compileSdk = 36

    val versionString = rootProject.file("VERSION").readText().trim()
    val (vMajor, vMinor, vPatch) = versionString.split(".").map { it.toInt() }

    defaultConfig {
        applicationId = "app.lonecloud.prism"
        minSdk = 31
        targetSdk = 36
        versionCode = vMajor * 10000 + vMinor * 100 + vPatch
        versionName = versionString

        buildConfigField("String", "DEFAULT_API_URL", "\"https://push.services.mozilla.com\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("ANDROID_SIGNING_STORE_FILE") ?: "non-existent.keystore")
            storePassword = System.getenv("ANDROID_SIGNING_STORE_PASSWORD") ?: ""
            keyAlias = System.getenv("ANDROID_SIGNING_KEY_ALIAS") ?: ""
            keyPassword = System.getenv("ANDROID_SIGNING_KEY_PASSWORD") ?: ""
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    buildTypes {
        getByName("release") {
            resValue("string", "app_name", "Prism")
            isMinifyEnabled = true
            isShrinkResources = true
            val signingEnvVarsPresent = listOf(
                "ANDROID_SIGNING_STORE_FILE",
                "ANDROID_SIGNING_STORE_PASSWORD",
                "ANDROID_SIGNING_KEY_ALIAS",
                "ANDROID_SIGNING_KEY_PASSWORD"
            ).all { System.getenv(it) != null }
            if (signingEnvVarsPresent) signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            resValue("string", "app_name", "Prism-dbg")
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        disable += "QueryAllPackagesPermission"
    }

    namespace = "app.lonecloud.prism"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.unifiedpush.distributor)
    implementation(libs.unifiedpush.distributor.base)
    implementation(libs.unifiedpush.distributor.ui)
    implementation(libs.tink.android)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.okhttp)
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.ui.tooling.preview.android)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.security.crypto)
    debugImplementation(libs.androidx.ui.tooling)
}
