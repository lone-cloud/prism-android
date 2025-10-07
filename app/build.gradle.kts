import com.android.SdkConstants
import com.android.repository.Revision
import com.android.sdklib.BuildToolInfo
import org.gradle.kotlin.dsl.register
import java.nio.file.Paths

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
    dependenciesInfo {
        // Disables dependency metadata when building APKs and Bundles.
        includeInApk = false
        includeInBundle = false
    }

    compileSdk = 36

    defaultConfig {
        applicationId = "org.unifiedpush.distributor.sunup"
        minSdk = 24
        targetSdk = 36
        versionCode = 11
        versionName = "1.2.0"

        // buildConfigField("String", "DEFAULT_API_URL", "\"http://10.0.2.2:8088\"")
        buildConfigField("String", "DEFAULT_API_URL", "\"https://push.services.mozilla.com\"")
        buildConfigField("Boolean", "URGENCY", "false")
        buildConfigField("Boolean", "SUPPORT_MIGRATIONS", "false")
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

dependencies {
    // implementation(project(":distributor_ui"))
    // implementation(project(":distributor"))
    implementation(libs.unifiedpush.distributor)
    implementation(libs.unifiedpush.distributor.ui)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.appcompat)
    implementation(libs.okhttp)
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.ui.tooling.preview.android)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)
}

tasks.register<RunBundletoolTask>("reproduceUniversal") {
    group = "build"
    description = "Generate universal .apks from .aab avec bundletool"
    dependsOn("bundleRelease")
    aabFile = project.rootDir.resolve("app/build/outputs/bundle/release/app-release.aab")
    universalApks = project.rootDir.resolve("universal.apks")
    signature.set(RunBundletoolTask.Signature.UnsignedOrDebug)
    val buildToolsVersion = SdkConstants.CURRENT_BUILD_TOOLS_VERSION
    val buildToolsDir = Paths.get(
        androidComponents.sdkComponents.sdkDirectory.get().toString(),
        SdkConstants.FD_BUILD_TOOLS,
        SdkConstants.CURRENT_BUILD_TOOLS_VERSION,
    )
    buildToolInfo.set(
        BuildToolInfo.fromStandardDirectoryLayout(
            Revision.parseRevision(buildToolsVersion),
            buildToolsDir
        )
    )
}

tasks.register<RunBundletoolTask>("bundletoolBuildApks") {
    group = "build"
    description = "Generate default and universal .apks from .aab avec bundletool"

    val aabPath = System.getenv("AAB") ?: error("AAB not set")
    aabFile = project.rootDir.resolve(aabPath)
    universalApks = project.rootDir.resolve("universal.apks")
    defaultApks = project.rootDir.resolve("app.apks")

    val ks = System.getenv("KS") ?: error("KS not set")
    val ksPass = System.getenv("KS_PASS") ?: error("KS_PASS not set")
    val keyAlias = System.getenv("KEY_ALIAS") ?: error("KEY_ALIAS not set")

    signature.set(RunBundletoolTask.Signature.Signed(ks, ksPass, keyAlias))

    val buildToolsVersion = SdkConstants.CURRENT_BUILD_TOOLS_VERSION
    val buildToolsDir = Paths.get(
        androidComponents.sdkComponents.sdkDirectory.get().toString(),
        SdkConstants.FD_BUILD_TOOLS,
        SdkConstants.CURRENT_BUILD_TOOLS_VERSION,
    )
    buildToolInfo.set(
        BuildToolInfo.fromStandardDirectoryLayout(
            Revision.parseRevision(buildToolsVersion),
            buildToolsDir
        )
    )
}
