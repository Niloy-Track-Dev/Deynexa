pluginManagement {
  repositories {
    google()
    maven { url = uri("https://maven-central.storage-download.googleapis.com/maven2/") }
    maven { url = uri("https://maven.aliyun.com/repository/public") }
    mavenCentral()
    maven { url = uri("https://plugins.gradle.org/m2/") }
    gradlePluginPortal()
  }
}

// plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    maven { url = uri("https://maven-central.storage-download.googleapis.com/maven2/") }
    maven { url = uri("https://maven.aliyun.com/repository/public") }
    mavenCentral()
    maven { url = uri("https://plugins.gradle.org/m2/") }
  }
}

rootProject.name = "Daynexa"

include(":app")
