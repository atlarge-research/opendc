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
}

dependencies {
    testImplementation(kotlin("test"))
}
