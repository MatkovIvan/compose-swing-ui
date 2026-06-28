import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    id("buildsrc.convention.kotlin-jvm")
    // Publish this module to Maven Local / GitHub Packages.
    id("buildsrc.convention.publishing")
    // Per-module test coverage (self-contained; no cross-module aggregation).
    jacoco
}

// This is a verbatim AOSP fork of Compose animation-core, so it is intentionally exempt from the shared
// ktlint/detekt/lint quality gates; compile, test, and ABI validation still run via kotlin-jvm + abiValidation.

kotlin {
    explicitApi()

    // The whole public surface is gated behind @ExperimentalSwingAnimationApi so external consumers
    // must opt in. The module's own production code and tests build against that surface, so opt in
    // module-wide here: this applies to every compilation (main + test) without weakening the
    // requirement for downstream consumers and without touching the generated public ABI.
    //
    // The vendored animation-core also annotates interfaces with @JvmDefaultWithCompatibility, which
    // the compiler only accepts when default methods are generated for all JVM targets. Match upstream.
    compilerOptions {
        optIn.add("org.jetbrains.compose.swing.animation.core.ExperimentalSwingAnimationApi")
        freeCompilerArgs.add("-jvm-default=no-compatibility")
    }

    // Lock the public ABI with the Kotlin Gradle plugin's built-in validation.
    @OptIn(ExperimentalAbiValidation::class)
    abiValidation {
        enabled.set(true)
    }
}

dependencies {
    // withFrameNanos / @Composable / snapshot state appear in public signatures.
    api(libs.composeRuntime)
    // Range / nullability annotations (@FloatRange, @IntRange, @RestrictTo) on the vendored engine's
    // public declarations, mirroring upstream animation-core's api dependency.
    api(libs.androidxAnnotation)
    // Primitive collections (IntList, IntObjectMap, ...) used internally by the keyframe/spline specs.
    implementation(libs.androidxCollection)
    // Suspend animation primitives (Job, CancellationException, Mutex). The Swing dispatcher and
    // frame clock live in :swing-ui; the engine only needs the common coroutine primitives.
    implementation(libs.kotlinxCoroutinesCore)

    testImplementation(kotlin("test"))
    // runTest + virtual time drive the engine tests off a controllable frame clock.
    testImplementation(libs.kotlinxCoroutinesTest)
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

// Regression ratchet: fail the build if line coverage of the engine drops below the floor. The floor
// sits a few points under the currently achieved ratio so ordinary noise never breaks the build while a
// real coverage regression does.
tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.named("jacocoTestReport"))
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.20".toBigDecimal()
            }
        }
    }
}
tasks.named("check") {
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}

publishing {
    publications.named<MavenPublication>("maven") {
        pom {
            name.set("compose-swing-ui :: swing-ui-animation")
            description.set(
                "Vendored Compose animation-core engine (animate*AsState, Animatable, Transition, " +
                    "easing, spring/tween) for the Compose-over-Swing runtime.",
            )
        }
    }
}
