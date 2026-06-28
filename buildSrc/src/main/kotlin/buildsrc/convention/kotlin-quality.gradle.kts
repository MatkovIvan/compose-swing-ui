// Convention plugin applying the shared code-quality gates (ktlint + detekt) to a module.
// Apply it alongside `kotlin-jvm` in module build scripts.
package buildsrc.convention

import dev.detekt.gradle.Detekt
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

plugins {
    id("org.jlleitschuh.gradle.ktlint")
    id("dev.detekt")
    // Standalone Android Lint runner (no AGP/Android SDK) hosting the Compose lint checks.
    id("com.android.lint")
}

// Precompiled script plugins do not get generated `libs` accessors, so resolve the catalog directly.
private val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

private fun requiredVersion(alias: String): String =
    libs
        .findVersion(alias)
        .orElseThrow { IllegalStateException("Missing version '$alias' in gradle/libs.versions.toml") }
        .requiredVersion

ktlint {
    version.set(requiredVersion("ktlint"))
    ignoreFailures.set(false)
}

detekt {
    buildUponDefaultConfig.set(true)
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    ignoreFailures.set(false)
    parallel.set(true)
}

// Analyze against the same JVM target the project compiles to.
tasks.withType<Detekt>().configureEach {
    jvmTarget.set("21")
}

// The default `detekt` task runs without type resolution, so rules that need the compile classpath
// (UseOrEmpty, UseCheckOrError, ImplicitDefaultLocale, …) never fire. Wire the type-resolution tasks
// detektMain/detektTest — which compile the sources first — into `check` so those rules are enforced.
tasks.named("check") {
    dependsOn(tasks.named("detektMain"), tasks.named("detektTest"))
}

// Run the Compose lint checks as part of `check`, and surface them as errors (no quiet warnings).
lint {
    warningsAsErrors = true
    // Lint configuration (which Compose checks are enabled and at what severity) lives in lint.xml.
    lintConfig = rootProject.file("config/lint/lint.xml")
}

dependencies {
    // Compose lint checks (slackhq/compose-lints) contributed to the Android Lint runtime.
    "lintChecks"(
        libs
            .findLibrary("composeLintChecks")
            .orElseThrow { IllegalStateException("Missing library 'composeLintChecks' in gradle/libs.versions.toml") },
    )
}
