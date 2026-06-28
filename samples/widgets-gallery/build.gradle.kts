plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/buildsrc/convention/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")
    // Apply the shared ktlint + detekt quality gates.
    id("buildsrc.convention.kotlin-quality")

    // Apply the Application plugin to add support for building an executable JVM application.
    application
}

dependencies {
    implementation(project(":swing-ui"))

    // The Compose-over-Swing animation engine (animate*AsState driving a Swing property). The gallery's
    // animation section eases a real ProgressBar value toward a reactive target across the frame clock.
    implementation(project(":swing-ui-animation"))

    // The Swing-Compose test harness (SwingUiTest + the SwingMatcher/interaction API). This is the
    // canonical way a consumer headlessly tests a Swing-Compose app: the kotlin-jvm convention already
    // configures the JUnit Platform test task under -Djava.awt.headless=true.
    testImplementation(project(":swing-ui-test"))

    // kotlin-test, declared here so the Kotlin Gradle plugin aligns its framework binding to this
    // module's JUnit Platform test task (the convention's `useJUnitPlatform()`); the transitive `api`
    // dependency from :swing-ui-test brings the common assertions but not the consumer-side binding.
    testImplementation(kotlin("test"))
}

application {
    // Fully qualified name of the application main class (WidgetsGalleryMain.kt compiles to ...Kt).
    mainClass = "org.jetbrains.compose.swing.samples.widgets.WidgetsGalleryMainKt"
}
