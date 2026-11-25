description = "OpenDC shared simulation data model"

plugins {
    `kotlin-library-conventions`
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(projects.opendc.opendcCommon)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(kotlin("test"))
    testImplementation(libs.jackson.databind)
    testImplementation(libs.jackson.module.kotlin)
    testImplementation(project(":opendc-trace:opendc-trace-parquet"))

    implementation(project(mapOf("path" to ":opendc-trace:opendc-trace-api")))
    api(project(mapOf("path" to ":opendc-simulator:opendc-simulator-compute")))
    api(project(mapOf("path" to ":opendc-simulator:opendc-simulator-failure")))
}
