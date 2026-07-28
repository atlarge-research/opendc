description = "OpenDC SDK runner: executes the SDK simulation model on the OpenDC simulator"

plugins {
    `kotlin-library-conventions`
    `testing-conventions`
    kotlin("plugin.serialization") version "2.3.20"
    id("me.champeau.jmh")
}

jmh {
    resultFormat.set("JSON")

    // NOTE: The me.champeau.jmh plugin joins all `includes` entries into a single
    // comma-separated string and passes it to JMH as ONE regex, so multiple list
    // entries do not act as an OR. Combine the benchmarks into one alternation regex.
    val jmhBenchmarks =
        listOf(
            "TaskScalingBenchmark",
        )

    includes.add(".*(" + jmhBenchmarks.joinToString("|") + ").*")
}

// The workload traces under src/jmh/resources (~1 GB: borg, marconi, solvinity, ...)
// are read from the filesystem via relative paths at benchmark runtime -- see the
// `--experiment-path` arguments and the `pathToFile` fields in the experiment JSON,
// which are opened with FileInputStream, not loaded from the classpath. Bundling them
// into the JMH uber-jar makes the `jmhJar` task hang for minutes before any benchmark
// starts, so exclude them from JMH resource processing and packaging.
sourceSets.named("jmh") {
    resources.exclude("workloadTraces/**")
}

tasks.named<Jar>("jmhJar") {
    exclude("workloadTraces/**")
}

tasks.named("jmh") {
    doLast {
        val resultsFile = layout.buildDirectory.file("results/jmh/results.json").get().asFile
        val statsFile = layout.buildDirectory.file("memory-stats.csv").get().asFile
        if (!resultsFile.exists() || !statsFile.exists()) return@doLast

        // Columns: benchmark, heap avg/avgStd/peak/peakStd, rss avg/avgStd/peak/peakStd (all MB).
        fun Double.orNull() = if (isNaN()) null else this
        val memoryByBenchmark =
            statsFile.readLines().associate { line ->
                val cols = line.split(",")
                cols[0].trim('"') to
                    mapOf(
                        "heap" to
                            mapOf(
                                "avgMb" to cols[1].toDouble().orNull(),
                                "avgStdMb" to cols[2].toDouble().orNull(),
                                "peakMb" to cols[3].toDouble().orNull(),
                                "peakStdMb" to cols[4].toDouble().orNull(),
                            ),
                        "rss" to
                            mapOf(
                                "avgMb" to cols[5].toDouble().orNull(),
                                "avgStdMb" to cols[6].toDouble().orNull(),
                                "peakMb" to cols[7].toDouble().orNull(),
                                "peakStdMb" to cols[8].toDouble().orNull(),
                            ),
                    )
            }
        statsFile.delete()

        @Suppress("UNCHECKED_CAST")
        val results = groovy.json.JsonSlurper().parse(resultsFile) as List<MutableMap<String, Any>>
        for (entry in results) {
            val benchmark = entry["benchmark"] as String
            memoryByBenchmark[benchmark]?.let { entry["memoryMetric"] = it }
        }

        resultsFile.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(results)))
    }
}

tasks.test {
    maxHeapSize = "10g"
    jvmArgs(
        "-Xlog:gc*:file=build/gc.log:time,uptime,level,tags:filecount=5,filesize=50m",
        // optional but useful alongside gc logging:
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-XX:HeapDumpPath=build/heapdump.hprof",
    )
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

    jmhImplementation(libs.kotlinx.serialization.json)

    testImplementation(project(":opendc-trace:opendc-trace-parquet"))
    testImplementation(libs.kotlinx.serialization.json)
    testRuntimeOnly(libs.log4j.core)
    testRuntimeOnly(libs.log4j.slf4j)
}
