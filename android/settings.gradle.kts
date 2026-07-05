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
        // Beberapa web3 SDK (Web3Auth, dll.) dipublish via JitPack
        maven("https://jitpack.io")
    }
}

rootProject.name = "OriginTag"
include(":app")
