description = "Command-line interface for running OpenDC simulations"

plugins {
    `kotlin-conventions`
    `testing-conventions`
    id("me.champeau.jmh")
    distribution
}

jmh {
    resultFormat.set("JSON")
    val jmhIncludes = findProperty("jmhIncludes") as? String ?: ".*CIBenchmark.*"
    includes.add(jmhIncludes)
}

tasks.named("jmh") {
    doLast {
        val resultsFile = layout.buildDirectory.file("results/jmh/results.json").get().asFile
        val heapFile = layout.buildDirectory.file("heap-stats.csv").get().asFile
        if (!resultsFile.exists() || !heapFile.exists()) return@doLast

        val heapByBenchmark =
            heapFile.readLines().associate { line ->
                val cols = line.split(",")
                cols[0].trim('"') to
                    mapOf(
                        "avgMb" to cols[1].toDouble(),
                        "avgStdMb" to cols[2].toDouble(),
                        "maxMb" to cols[3].toDouble(),
                        "maxStdMb" to cols[4].toDouble(),
                    )
            }
        heapFile.delete()

        @Suppress("UNCHECKED_CAST")
        val results = groovy.json.JsonSlurper().parse(resultsFile) as List<MutableMap<String, Any>>
        for (entry in results) {
            val benchmark = entry["benchmark"] as String
            heapByBenchmark[benchmark]?.let { entry["heapMetric"] = it }
        }

        resultsFile.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(results)))
    }
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

val createScenarioApp by tasks.creating(CreateStartScripts::class) {
    dependsOn(tasks.jar)

    applicationName = "opendc"
    mainClass.set("org.opendc.cli.MainKt")
    defaultJvmOpts = listOf("-XX:MaxRAMPercentage=90.0")
    classpath = tasks.jar.get().outputs.files + configurations["runtimeClasspath"]
    outputDir = layout.buildDirectory.dir("scenarioScripts").get().asFile
}

// Create custom Scenario distribution
distributions {
    main {
        distributionBaseName.set("OpenDCExperimentRunner")

        contents {
            from("README.md")
            from("../LICENSE.txt")

            into("bin") {
                from(createScenarioApp) {
                    include("opendc", "opendc.bat")
                }
            }

            into("lib") {
                from(tasks.jar)
                from(configurations["runtimeClasspath"])
            }
        }
    }
}
