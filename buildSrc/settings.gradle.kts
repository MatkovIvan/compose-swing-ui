@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        // Hosts the standalone Android Lint plugin and lint-* runtime artifacts.
        google()
        mavenCentral()
        // The ktlint-gradle and detekt plugin marker artifacts are published to the Gradle Plugin Portal.
        gradlePluginPortal()
    }

    // Reuse the version catalog from the main build.
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "buildSrc"
