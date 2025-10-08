plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation("com.android.tools.build:builder:8.13.0") // version Android Gradle Plugin
    implementation("com.android.tools:common:31.2.2")
    implementation("com.android.tools.build:bundletool:1.18.2")
    implementation("com.android.tools:sdklib:31.2.2")
    implementation("com.android.tools.build:gradle-api:8.13.0")
}
