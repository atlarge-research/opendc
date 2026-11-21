/*
 * Copyright (c) 2026 AtLarge Research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

description = "Web server of OpenDC"

plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
    alias(libs.plugins.kotlin.serialization)
    `quarkus-conventions`
    application
}

application {
    applicationName = "opendc-server"
    mainClass.set("io.quarkus.bootstrap.runner.QuarkusEntryPoint")
}

allOpen {
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

// The OpenDC simulation modules route SLF4J through Log4j2 and expose the Log4j2 SLF4J binding
// (log4j-slf4j2-impl) as an api dependency; it leaks transitively via opendc-sdk-model ->
// opendc-common, leaving SLF4J with two competing providers under Quarkus' JBoss LogManager.
// Drop the Log4j2 SLF4J binding here so SLF4J binds deterministically.
configurations.all {
    exclude(group = "org.apache.logging.log4j", module = "log4j-slf4j2-impl")
}

dependencies {
    implementation(enforcedPlatform(libs.quarkus.bom))

    implementation(projects.opendcSdk.opendcSdkModel)
    implementation(projects.opendcWeb.opendcWebDispatcher)
    // For the canonical trace table names and the column each table must carry.
    implementation(projects.opendcTrace.opendcTraceApi)
    // For reading a parquet footer, which is how an uploaded table is checked without fetching it.
    implementation(projects.opendcTrace.opendcTraceParquet)

    implementation(libs.quarkus.kotlin)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.quarkus.rest)
    implementation(libs.quarkus.rest.kotlin.serialization)
    implementation(libs.quarkus.smallrye.openapi)
    implementation(libs.quarkus.oidc)
    implementation(libs.quarkus.security)
    implementation(libs.quarkus.smallrye.health)
    implementation(libs.quarkus.micrometer.prometheus)

    implementation(libs.quarkus.hibernate.orm.panache.kotlin)
    implementation(libs.quarkus.hibernate.validator)
    implementation(libs.quarkus.flyway)
    implementation(libs.quarkus.scheduler)
    // Live telemetry only, and only where a deployment asks for it. Nothing reaches for a client
    // unless opendc.telemetry.kind=redis, which is what lets dev and the test suite run without one.
    implementation(libs.quarkus.redis.client)
    implementation(libs.quarkus.jdbc.postgresql)
    implementation(libs.quarkus.jdbc.h2)

    implementation(enforcedPlatform(libs.aws.bom))
    implementation(libs.aws.s3)
    // The lightest of the SDK's HTTP clients. Uploads are streamed from a spooled file, so nothing
    // here needs connection pooling or async.
    implementation(libs.aws.url.connection.client)

    testImplementation(libs.quarkus.junit5.core)
    testImplementation(libs.quarkus.jacoco)
    testImplementation(libs.restassured.core)
    testImplementation(libs.quarkus.test.oidc.server)
}

// The local dispatcher runs the launcher as a separate program, so the launcher's own distribution
// has to exist before a development run or a test can start one.
tasks.named("quarkusDev") {
    dependsOn(":opendc-web:opendc-web-launcher:installDist")
}

tasks.test {
    dependsOn(":opendc-web:opendc-web-launcher:installDist")
    // Each test profile starts an application of its own in this one JVM, one after another, which
    // outgrows Gradle's default 512 MB heap.
    maxHeapSize = "1g"
}

// The application plugin generates the start scripts; the Quarkus fast-jar launcher carries its
// own classpath manifest, so it is the only entry the scripts need.
tasks.startScripts {
    classpath = files("lib/quarkus-run.jar")
}

val quarkusAppDir = layout.buildDirectory.dir("quarkus-app")

// A distribution is the whole self-hosted product: the server, the frontend's static export it
// serves, and the launcher its local dispatcher starts. src/dist/config points the server at both.
// Only the distribution builds the frontend, so neither a development run nor a test ever does.
evaluationDependsOn(":opendc-web:opendc-web-frontend")
evaluationDependsOn(":opendc-web:opendc-web-launcher")

val frontendExport = project(":opendc-web:opendc-web-frontend").tasks.named("nextBuild")
val launcherDistribution = project(":opendc-web:opendc-web-launcher").tasks.named("installDist")
val launcherInstallDir = project(":opendc-web:opendc-web-launcher").layout.buildDirectory.dir("install")

distributions {
    main {
        distributionBaseName.set("opendc-server")

        contents {
            from("../../LICENSE.txt")
            from(tasks.quarkusBuild) {
                into("lib")
            }
            from(frontendExport) {
                into("frontend")
            }
            from(launcherDistribution) {
                into("launcher")
            }
            // The application plugin hard-wires the module jar and the plain runtime classpath
            // into lib/ with no removal API. The Quarkus fast-jar under build/quarkus-app already
            // bundles every dependency, so those defaults would ship each jar twice. Keep only
            // jars that come out of the Quarkus build or the launcher's own distribution.
            exclude { element ->
                val file = element.file.toPath()
                element.name.endsWith(".jar") &&
                    !file.startsWith(quarkusAppDir.get().asFile.toPath()) &&
                    !file.startsWith(launcherInstallDir.get().asFile.toPath())
            }
        }
    }
}
