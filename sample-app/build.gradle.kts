plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("buildsrc.convention.kotlin-quality")
    application
}

dependencies {
    implementation(project(":swing-ui"))
    implementation(project(":swing-ui-animation"))
}

application {
    mainClass = "org.jetbrains.compose.swing.app.AppKt"
}
