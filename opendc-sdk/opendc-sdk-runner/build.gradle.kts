description = "OpenDC SDK runner: executes the SDK simulation model on the OpenDC simulator"

plugins {
    `kotlin-library-conventions`
    `testing-conventions`
}

dependencies {
    api(project(":opendc-sdk:opendc-sdk-model"))
    api(project(":opendc-compute:opendc-compute-simulator"))

    implementation(project(":opendc-common"))
    implementation(project(":opendc-simulator:opendc-simulator-core"))
    implementation(project(":opendc-simulator:opendc-simulator-compute"))
    implementation(project(":opendc-simulator:opendc-simulator-flow"))
    implementation(project(":opendc-compute:opendc-compute-topology"))
    implementation(project(":opendc-compute:opendc-compute-workload"))
    implementation(project(":opendc-compute:opendc-compute-carbon"))
    implementation(project(":opendc-compute:opendc-compute-failure"))
    implementation(libs.commons.math3)
    implementation(libs.kotlinx.coroutines)

    testImplementation(project(":opendc-trace:opendc-trace-parquet"))
    testImplementation(libs.kotlinx.serialization.json)
    testRuntimeOnly(libs.log4j.core)
    testRuntimeOnly(libs.log4j.slf4j)
}
