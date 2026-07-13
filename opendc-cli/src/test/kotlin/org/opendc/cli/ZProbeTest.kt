package org.opendc.cli

import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.testing.test
import java.io.File
import kotlin.test.Test

class ZProbeTest {
    private fun opendc() = OpendcCommand().subcommands(RunCommand(), ValidateCommand(), ShowCommand())

    private fun fixture(dir: String): String {
        val root = File(dir)
        root.deleteRecursively()
        root.mkdirs()
        File(root, "topology.json").writeText(
            """
            {"clusters":[{"name":"C01","hosts":[{"name":"H01","count":1,
            "cpu":{"coreCount":8,"coreSpeed":2100},"memory":{"memorySize":100000},
            "cpuPowerModel":{"modelType":"linear","idlePower":32.0,"maxPower":180.0}}]}]}
            """.trimIndent(),
        )
        File(root, "experiment.json").writeText(
            """
            {"name":"legacy",
             "topologies":[{"pathToFile":"$dir/topology.json"}],
             "workloads":[{"pathToFile":"workload_traces/surf_week","type":"ComputeWorkload"}],
             "exportModels":[{"exportInterval":3600}]}
            """.trimIndent(),
        )
        return "$dir/experiment.json"
    }

    @Test
    fun `probe legacy validate`() {
        val exp = fixture("build/tmp/probe-a")
        val r = opendc().test(listOf("--legacy", "validate", exp))
        println("PROBE-validate status=${r.statusCode} out=<<${r.output}>>")
    }

    @Test
    fun `probe legacy flag after subcommand`() {
        val exp = fixture("build/tmp/probe-b")
        val r = opendc().test(listOf("run", "--legacy", exp, "--no-progress"))
        println("PROBE-flag-after status=${r.statusCode} out=<<${r.output}>>")
    }

    @Test
    fun `probe legacy run reveals trace root`() {
        val exp = fixture("build/tmp/probe-c")
        val r =
            try {
                val res = opendc().test(listOf("--legacy", "run", exp, "-o", "build/tmp/probe-c-out", "--no-progress"))
                "status=${res.statusCode} out=<<${res.output}>>"
            } catch (e: Throwable) {
                "ESCAPED ${e::class.simpleName}: ${e.message}"
            }
        println("PROBE-run $r")
    }

    @Test
    fun `probe legacy run with input-root`() {
        val exp = fixture("build/tmp/probe-d")
        // --input-root points at the fixture dir; the topology path is CWD-relative, so if the root is
        // honoured for topology inlining the topology must NOT be found there.
        val r =
            try {
                val res =
                    opendc().test(
                        listOf(
                            "--legacy", "run", exp, "--input-root", "build/tmp/probe-d",
                            "-o", "build/tmp/probe-d-out", "--no-progress",
                        ),
                    )
                "status=${res.statusCode} out=<<${res.output}>>"
            } catch (e: Throwable) {
                "ESCAPED ${e::class.simpleName}: ${e.message}"
            }
        println("PROBE-input-root $r")
    }
}
