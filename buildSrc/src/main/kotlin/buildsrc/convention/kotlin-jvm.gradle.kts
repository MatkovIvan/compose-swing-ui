package buildsrc.convention

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.compose")
}

kotlin {
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

// Target Java 11 bytecode (without provisioning a JDK toolchain) so the published binaries run on
// any JDK >= 11, while the build itself runs on the JDK it is invoked with (>= 21 in development).
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
}
tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    // Swing-Compose tests run off-screen on the EDT. Forcing headless mode keeps CI deterministic
    // and guarantees no real AWT window is ever realized (which would flash UI or throw on a
    // display-less machine). The test harness is designed to work fully under headless: it lays out
    // the root to a fixed size so components still get real bounds without an on-screen peer.
    systemProperty("java.awt.headless", "true")
}
