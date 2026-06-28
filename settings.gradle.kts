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

include(":samples:todo-app")
include(":swing-ui")
include(":swing-ui-animation")
include(":swing-ui-test")

rootProject.name = "compose-swing-ui"
