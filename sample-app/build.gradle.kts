plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("buildsrc.convention.kotlin-quality")
    application
}

dependencies {
    implementation(project(":swing-ui"))
}

application {
    mainClass = "org.jetbrains.compose.swing.app.AppKt"
}
