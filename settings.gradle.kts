pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "Blokku"
include(":app")
// Saat integrasi dengan fork ZalithLauncher, tambahkan:
// include(":engine", ":jre_lwjgl3glfw")
