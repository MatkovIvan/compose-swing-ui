// Convention plugin that publishes a single JVM library to Maven Local and GitHub Packages.
// Apply it alongside `kotlin-jvm` in a module build script, then set the publication name/description
// for that module. Coordinate and credential resolution lives in PublishingCoordinates.kt.
//
// group/version are wired here so every module that publishes picks up the same coordinates:
//   group   defaults to `dev.matkov.compose.swing`, overridable with -Pgroup=...
//   version defaults to `0.1.0-SNAPSHOT`, overridable with -Pversion=X.Y.Z; pass -PversionSuffix=...
//           (for example alpha.1, beta.2, rc.1) to append a pre-release suffix.
//
// publishToMavenLocal works with no GitHub credentials: the GitHubPackages repository block reads
// credentials as nullable providers, which maven-publish only validates when a remote publish task
// actually executes.
package buildsrc.convention

import org.gradle.api.plugins.JavaPluginExtension

plugins {
    `maven-publish`
}

private val publishGroup: String =
    providers.gradleProperty("group").orNull?.takeIf { it.isNotBlank() }
        ?: "dev.matkov.compose.swing"
private val publishVersion: String =
    run {
        val base = providers.gradleProperty("version").orNull?.takeIf { it.isNotBlank() } ?: "0.1.0-SNAPSHOT"
        val suffix = providers.gradleProperty("versionSuffix").orNull?.takeIf { it.isNotBlank() }
        if (suffix == null) base else "$base-$suffix"
    }

group = publishGroup
version = publishVersion

private val coordinates = resolveRepositoryCoordinates()
private val publishUsername = publishUsername()
private val publishToken = publishToken()

// kotlin-jvm applies the Java plugin, which provides this extension; the sources/javadoc jars it adds
// here are wired into the `java` software component picked up by the MavenPublication below.
extensions.configure<JavaPluginExtension> {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            from(components["java"])

            pom {
                url.set(coordinates.webUrl)

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set(coordinates.owner)
                    }
                }

                scm {
                    connection.set("scm:git:${coordinates.webUrl}.git")
                    developerConnection.set(
                        "scm:git:ssh://git@${coordinates.host}/" +
                            "${coordinates.owner}/${coordinates.name}.git",
                    )
                    url.set(coordinates.webUrl)
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri(coordinates.packagesUrl)
            credentials {
                username = publishUsername.orNull
                password = publishToken.orNull
            }
        }
    }
}
