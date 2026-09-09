package buildsrc.convention

import buildsrc.kotlinDefaults

plugins {
    kotlin("jvm")
    kotlin("plugin.compose")
}

kotlinDefaults()

// The reports name which composables are skippable and which parameters are unstable.
composeCompiler {
    reportsDestination.set(layout.buildDirectory.dir("compose-reports"))
    metricsDestination.set(layout.buildDirectory.dir("compose-metrics"))
}
