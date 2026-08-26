description = "OpenDC shared simulation data model"

plugins {
    `kotlin-library-conventions`
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(libs.jackson.annotations)
    api(projects.opendc.opendcCommon)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(kotlin("test"))
    testImplementation(libs.jackson.databind)
    testImplementation(libs.jackson.module.kotlin)

    implementation(project(mapOf("path" to ":opendc-trace:opendc-trace-api")))
    implementation(project(mapOf("path" to ":opendc-simulator:opendc-simulator-compute")))
    implementation(project(mapOf("path" to ":opendc-compute:opendc-compute-simulator")))
}
