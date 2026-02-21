import RunBundletoolTask.Companion.aapt2
import com.android.SdkConstants
import org.gradle.kotlin.dsl.register

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.versions)
}

detekt {
    config.setFrom(files("${rootProject.projectDir}/detekt.yml"))
    buildUponDefaultConfig = true
}

tasks.withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask> {
    rejectVersionIf {
        val v = candidate.version.lowercase()
        listOf("alpha", "beta", "-rc", "cr", "preview").any { v.contains(it) }
    }
}

kotlin {
    jvmToolchain(21)
}

android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
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
            storeFile = file(System.getenv("ANDROID_SIGNING_STORE_FILE") ?: "../prism-release.keystore")
            storePassword = System.getenv("ANDROID_SIGNING_STORE_PASSWORD") ?: "android123"
            keyAlias = System.getenv("ANDROID_SIGNING_KEY_ALIAS") ?: "prism"
            keyPassword = System.getenv("ANDROID_SIGNING_KEY_PASSWORD") ?: "android123"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        getByName("release") {
            resValue("string", "app_name", "Prism")
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
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

dependencies {
    implementation(libs.unifiedpush.distributor)
    implementation(libs.unifiedpush.distributor.base)
    implementation(libs.unifiedpush.distributor.ui)
    implementation(libs.accompanist.permissions)
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
    debugImplementation(libs.androidx.ui.tooling)
}

val buildTools = RunBundletoolTask.Companion.BuildTools(
    SdkConstants.CURRENT_BUILD_TOOLS_VERSION,
    SdkConstants.FD_BUILD_TOOLS
)

tasks.register<RunBundletoolTask>("reproduceUniversal") {
    group = "build"
    description = "Generate universal .apks from .aab with bundletool"
    dependsOn("bundleRelease")
    aabFile = project.rootDir.resolve("app/build/outputs/bundle/release/app-release.aab")
    universalApks = project.rootDir.resolve("universal.apks")
    aapt2 = project.aapt2(androidComponents, buildTools)
    signature.set(RunBundletoolTask.Signature.UnsignedOrDebug)
}

tasks.register<RunBundletoolTask>("bundletoolBuildApks") {
    group = "build"
    description = "Generate default and universal .apks from .aab with bundletool"

    val aabPath = System.getenv("AAB") ?: error("AAB not set")
    aabFile = project.rootDir.resolve(aabPath)
    universalApks = project.rootDir.resolve("universal.apks")
    defaultApks = project.rootDir.resolve("app.apks")
    aapt2 = project.aapt2(androidComponents, buildTools)

    val ks = System.getenv("KS") ?: error("KS not set")
    val ksPass = System.getenv("KS_PASS") ?: error("KS_PASS not set")
    val keyAlias = System.getenv("KEY_ALIAS") ?: error("KEY_ALIAS not set")

    signature.set(RunBundletoolTask.Signature.Signed(ks, ksPass, keyAlias))
}
