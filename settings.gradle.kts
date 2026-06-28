@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

include(":sample-app")
include(":swing-ui")
include(":swing-ui-test")

rootProject.name = "compose-swing-ui"
