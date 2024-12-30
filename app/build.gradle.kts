plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ktlint)
}

android {
    compileOptions {
        targetCompatibility = JavaVersion.VERSION_17
        sourceCompatibility = JavaVersion.VERSION_17
    }

    compileSdk = 35

    defaultConfig {
        applicationId = "org.unifiedpush.distributor.sunup"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"

        // buildConfigField("String", "DEFAULT_API_URL", "\"http://10.0.2.2:8088\"")
        buildConfigField("String", "DEFAULT_API_URL", "\"https://push.services.mozilla.com\"")
        buildConfigField("Boolean", "URGENCY", "false")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        getByName("release") {
            resValue("string", "app_name", "Sunup")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            resValue("string", "app_name", "Sunup-dbg")
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    namespace = "org.unifiedpush.distributor.sunup"
}

if (project.hasProperty("sign")) {
    android {
        signingConfigs {
            create("release") {
                storeFile = file(System.getenv("RELEASE_STORE_FILE"))
                storePassword = System.getenv("RELEASE_STORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }

        buildTypes {
            getByName("release") {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

dependencies {
    // implementation(project(":distributor_ui"))
    // implementation(project(":distributor"))
    implementation(libs.unifiedpush.distributor)
    implementation(libs.unifiedpush.distributor.ui)
    implementation(libs.androidx.animation.android)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.appcompat)
    implementation(libs.kotlin.stdlib)
    implementation(libs.material)
    implementation(libs.okhttp)
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.ui.tooling.preview.android)
    implementation(libs.androidx.ui.tooling)
    implementation(libs.kotlinx.serialization.json)
}
