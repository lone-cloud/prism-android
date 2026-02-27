
buildscript {
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}

/*
 * By listing all the plugins used throughout all subprojects in the root project build script, it
 * ensures that the build script classpath remains the same for all projects. This avoids potential
 * problems with mismatching versions of transitive plugin dependencies. A subproject that applies
 * an unlisted plugin will have that plugin and its dependencies _appended_ to the classpath, not
 * replacing pre-existing dependencies.
 */
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.version.catalog.update)
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
