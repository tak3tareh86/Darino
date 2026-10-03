pluginManagement {
  resolutionStrategy {
    eachPlugin {
      when (requested.id.id) {
        "com.android.application" ->
          useModule("com.android.tools.build:gradle:" + requested.version)
        "com.google.devtools.ksp" ->
          useModule("com.google.devtools.ksp:symbol-processing-gradle-plugin:" + requested.version)
        "com.google.gms.google-services" ->
          useModule("com.google.gms:google-services:" + requested.version)
      }
    }
  }

  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    maven {
      url = uri("https://jitpack.io")
    }
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

  repositories {
    google()
    mavenCentral()
    maven {
      url = uri("https://jitpack.io")
    }
  }
}

rootProject.name = "Darino"

include(":app")
