pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "Blokku"
// jre_lwjgl3glfw tidak perlu: jar-nya sudah jadi di app/src/main/assets/components/lwjgl3/
include(":app")
