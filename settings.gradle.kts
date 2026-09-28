pluginManagement {
  resolutionStrategy {
    eachPlugin {
      if (requested.id.id == "com.google.gms.google-services") {
        useModule("com.google.gms:google-services:${requested.version}")
      }
    }
  }
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    maven { url = uri("https://maven.aliyun.com/repository/google") }
    maven { url = uri("https://maven.aliyun.com/repository/public") }
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

  repositories {
    maven { url = uri("https://maven.aliyun.com/repository/google") }
    maven { url = uri("https://maven.aliyun.com/repository/public") }
    maven { url = uri("https://jitpack.io") }
    google()
    mavenCentral()
  }
}

rootProject.name = "Darino"

include(":app")