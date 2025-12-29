pluginManagement {
    repositories {
        google()
        maven { url = uri("https://repo1.maven.org/maven2/") }
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven { url = uri("https://repo1.maven.org/maven2/") }
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "RakutenMonitorAndroid"
include(":app")
