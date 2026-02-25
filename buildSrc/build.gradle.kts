plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation("com.android.tools.build:builder:8.13.2") // version Android Gradle Plugin
    implementation("com.android.tools:common:31.13.2")
    implementation("com.android.tools:sdklib:31.2.2")
    implementation("com.android.tools.build:gradle-api:8.13.2")
}
