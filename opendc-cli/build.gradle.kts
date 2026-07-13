description = "Command-line interface for running OpenDC simulations"

plugins {
    `kotlin-conventions`
    `testing-conventions`
    application
}

group = "org.opendc"
version = "3.0-SNAPSHOT"

application {
    applicationName = "opendc"
    mainClass.set("org.opendc.cli.MainKt")
    applicationDefaultJvmArgs = listOf("-XX:MaxRAMPercentage=90.0")
}

dependencies {
    implementation(project(":opendc-sdk:opendc-sdk-runner"))
    implementation(libs.clikt5)
    implementation(libs.mordant)

    // The legacy experiment adapter rewrites JSON trees; it declares no serializable types of its own,
    // so it needs the library but not the serialization compiler plugin.
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.log4j.core)
    runtimeOnly(libs.log4j.slf4j)

    testImplementation(kotlin("test"))
}
