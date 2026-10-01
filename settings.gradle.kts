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
    // Use Google's canonical Maven endpoint explicitly. This avoids the
    // dl.google.com HEAD behavior observed with the local JDK/Gradle HTTP client.
    maven {
      url = uri("https://maven.google.com")
    }
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

rootProject.name = "Darino"

include(":app")
