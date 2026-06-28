import dev.detekt.gradle.Detekt
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    // Apply the quality tooling to buildSrc itself so the convention plugins are linted too.
    // buildSrc is an included build, so root-level ktlintCheck/detekt do NOT reach it; CI invokes
    // :buildSrc:ktlintCheck and :buildSrc:detekt explicitly to enforce this.
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

// Enforce explicit visibility/return types on the convention plugin sources, matching the libraries
// this build logic publishes.
extensions.getByType<KotlinBaseExtension>().explicitApi()

dependencies {
    implementation(libs.kotlinGradlePlugin)
    implementation(libs.kotlinComposePlugin)
    // Quality tooling plugins, so the kotlin-quality convention plugin can apply and configure them.
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.ktlint.gradle.plugin)
    // The standalone Android Lint plugin, so the kotlin-quality convention plugin can apply it to JVM
    // modules and host the Compose lint checks without pulling in AGP or the Android SDK.
    implementation(libs.androidLint.gradle.plugin)
}

// buildSrc is an included build and cannot apply its own precompiled `kotlin-quality` convention
// plugin, so the same ktlint/detekt gates it applies to modules are restated here.
ktlint {
    version.set(libs.versions.ktlint.asProvider())
    ignoreFailures.set(false)
    // Do not lint Gradle's generated kotlin-dsl accessors / plugin adapters under build/.
    filter {
        exclude { it.file.path.contains("/build/generated-sources/") }
    }
}

detekt {
    buildUponDefaultConfig.set(true)
    // The config path is relative to the repository root; from buildSrc/ that is one level up.
    config.setFrom(rootDir.resolve("../config/detekt/detekt.yml"))
    ignoreFailures.set(false)
    parallel.set(true)
}

// Pin the bytecode target without provisioning a JDK toolchain, so the build runs on any JDK >= 21
// (including JDK 25) while still producing JVM 21 class files.
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
    compilerOptions.allWarningsAsErrors.set(true)
}
tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

// Analyze against the same JVM target buildSrc compiles to.
tasks.withType<Detekt>().configureEach {
    jvmTarget.set("21")
}
