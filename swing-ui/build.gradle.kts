import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("buildsrc.convention.kotlin-quality")
    // Publish this module to Maven Local / GitHub Packages.
    id("buildsrc.convention.publishing")
    // Per-module test coverage (self-contained; no cross-module aggregation).
    jacoco
}

kotlin {
    explicitApi()

    // Lock the public ABI with the Kotlin Gradle plugin's built-in validation.
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

// A resolvable, isolated classpath holding ONLY the Compose compiler plugin jar (no transitive deps).
// The in-process compiler harness loads this jar via -Xplugin so it runs the exact same Compose
// @Composable/target inference as the real Gradle build. Kept out of the test compile/runtime classpath
// on purpose: the harness wants the plugin as a standalone jar, not on the classpath.
val composeCompilerPluginClasspath: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    api(libs.composeRuntime)
    implementation(libs.kotlinxCoroutinesSwing)
    // @MagicConstant typed-constant annotations. CLASS/IDE-only: compileOnly so they warn consumers
    // in-IDE across the jar boundary without leaking org.jetbrains:annotations to the published runtime.
    compileOnly(libs.jetbrainsAnnotations)
    testImplementation(kotlin("test"))
    // Behavioral tests drive the library through the public test harness.
    testImplementation(project(":swing-ui-test"))

    // Official embeddable Kotlin compiler, driven in-process (test-only) to assert Compose compiler
    // diagnostics. Aligned to the Kotlin version we compile with by the resolutionStrategy block below.
    testImplementation(kotlin("compiler-embeddable"))

    // The Compose compiler plugin jar, resolved into its own configuration and handed to the harness.
    composeCompilerPluginClasspath(libs.kotlinComposeCompilerPluginEmbeddable) {
        isTransitive = false
    }
}

// Align the embeddable compiler artifacts to the Kotlin version we compile with so the in-process compiler
// and the Compose plugin jar are the same line and load each other cleanly.
val embeddableCompilerArtifacts =
    setOf("kotlin-compiler-embeddable", "kotlin-annotation-processing-embeddable")
configurations.matching { it.name.startsWith("test") }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin" && requested.name in embeddableCompilerArtifacts) {
            useVersion(libs.versions.kotlin.get())
        }
    }
}

// Generate XML + HTML coverage reports and run them as part of the test lifecycle.
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

// Class-file paths whose behavior is driven by a real display/window server (windows, tray, native
// dialogs) and therefore cannot run in the headless CI environment. They are removed from the verified
// class set so the gate measures only code that the headless test suite can actually exercise.
val coverageUntestableClassFiles =
    listOf(
        "org/jetbrains/compose/swing/window/WindowKt.class",
        "org/jetbrains/compose/swing/window/WindowKt\$*.class",
        "org/jetbrains/compose/swing/window/DialogKt.class",
        "org/jetbrains/compose/swing/window/DialogKt\$*.class",
        "org/jetbrains/compose/swing/window/ApplicationKt.class",
        "org/jetbrains/compose/swing/window/ApplicationKt\$*.class",
        "org/jetbrains/compose/swing/window/ApplicationApplier.class",
        "org/jetbrains/compose/swing/window/ApplicationApplier\$*.class",
        "org/jetbrains/compose/swing/components/TrayKt.class",
        "org/jetbrains/compose/swing/components/TrayKt\$*.class",
        "org/jetbrains/compose/swing/components/TrayMenuHost.class",
        "org/jetbrains/compose/swing/components/TrayMenuHost\$*.class",
        "org/jetbrains/compose/swing/components/ComposableSingletons\$TrayKt.class",
        "org/jetbrains/compose/swing/dialogs/DialogsKt.class",
        "org/jetbrains/compose/swing/dialogs/DialogsKt\$*.class",
    )

// Regression ratchet: fail the build if line coverage of the headless-testable code drops below the
// floor. The floor sits a few points under the currently achieved ratio so ordinary noise never breaks
// the build while a real coverage regression does.
tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.named("jacocoTestReport"))
    classDirectories.setFrom(
        files(
            classDirectories.files.map { dir ->
                fileTree(dir) { exclude(coverageUntestableClassFiles) }
            },
        ),
    )
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.89".toBigDecimal()
            }
        }
    }
}
tasks.named("check") {
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}

tasks.withType<Test>().configureEach {
    // Hand the resolved Compose compiler plugin jar path to the harness. A FileCollection (not the
    // Configuration itself) is captured so the task stays configuration-cache compatible, and it is read
    // lazily inside the provider so configuration of unrelated tasks never forces resolution.
    val pluginClasspath: FileCollection = composeCompilerPluginClasspath
    jvmArgumentProviders.add(
        CommandLineArgumentProvider {
            listOf("-Dcompose.compiler.plugin.classpath=${pluginClasspath.asPath}")
        },
    )
}

publishing {
    publications.named<MavenPublication>("maven") {
        pom {
            name.set("compose-swing-ui")
            description.set(
                "Compose runtime over Swing: declarative composable wrappers and modifiers for Swing components.",
            )
        }
    }
}
