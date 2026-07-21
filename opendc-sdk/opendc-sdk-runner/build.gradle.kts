description = "OpenDC SDK runner: executes the SDK simulation model on the OpenDC simulator"

plugins {
    `kotlin-library-conventions`
    `testing-conventions`
    kotlin("plugin.serialization") version "2.3.20"
    id("me.champeau.jmh")
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
