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

    implementation(libs.log4j.core)
    runtimeOnly(libs.log4j.slf4j)

    testImplementation(kotlin("test"))
}
