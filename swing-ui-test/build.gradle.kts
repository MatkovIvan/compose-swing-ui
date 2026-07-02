import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("buildsrc.convention.kotlin-quality")
    id("buildsrc.convention.publishing")
    jacoco
}

kotlin {
    explicitApi()

    @OptIn(ExperimentalAbiValidation::class)
    abiValidation {
        enabled.set(true)
        filters {
            exclude {
                annotatedWith.add("org.jetbrains.compose.swing.annotations.InternalSwingUiApi")
            }
        }
    }

    sourceSets.configureEach {
        languageSettings.optIn("org.jetbrains.compose.swing.annotations.InternalSwingUiApi")
    }
}

dependencies {
    api(project(":swing-ui"))
    api(libs.composeRuntime)
    api(kotlin("test"))
    api(libs.kotlinxCoroutinesTest)
    api(libs.kotlinxCoroutinesSwing)
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
tasks.named("test") {
    finalizedBy(tasks.named("jacocoTestReport"))
}

publishing {
    publications.named<MavenPublication>("maven") {
        pom {
            name.set("compose-swing-ui-test")
            description.set(
                "Headless test harness for driving and asserting compose-swing-ui compositions.",
            )
        }
    }
}
