pluginManagement {
  repositories {
    // No content filter on google(): the previous includeGroupByRegex list
    // silently excluded anything outside com.android / com.google / androidx,
    // which is a common cause of "plugin not found" during a first sync.
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

// Resolves a JDK 17 toolchain automatically if the machine doesn't have one,
// so the build works without the developer installing a JDK by hand.
plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "FOMO"

include(":app")
