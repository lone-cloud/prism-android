pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

include(":app")
rootProject.name = "Sunup"

// includeBuild("../android-distributor") {
//     dependencySubstitution {
//         substitute(
//                 module("org.unifiedpush.android:distributor")
//         ).using(project(":distributor"))
//         substitute(
//             module("org.unifiedpush.android:distributor-ui")
//         ).using(project(":distributor_ui"))
//     }
// }
