/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.experiments.radice

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.groupChoice
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import com.typesafe.config.Config
import org.opendc.experiments.radice.scenario.*
import org.opendc.experiments.radice.scenario.mapper.YamlScenarioMapper
import org.opendc.experiments.radice.scenario.topology.*
import java.io.File

/**
 * A [CliktCommand] for generating the portfolios to explore.
 */
internal class RadiceGenerateCommand(private val config: Config) : CliktCommand(name = "generate") {
    /**
     * The path to the experiment output.
     */
    private val outputPath by option("-O", "--output", help = "path to write the portfolio file to")
        .file(canBeDir = false, canBeFile = true)
        .defaultLazy { File("portfolio.yml") }

    /**
     * The portfolio to generate.
     */
    private val portfolio by option()
        .groupChoice(
            "baseline" to BaselinePortfolio(),
            "workload" to WorkloadPortfolio(),
            "scheduler" to SchedulerPortfolio(),
            "custom" to CustomPortfolio(),
        )
        .required()

    /**
     * The [YamlScenarioMapper] to use.
     */
    private val scenarioMapper = YamlScenarioMapper()

    override fun run() {
        echo("Generating portfolio: ${portfolio.groupName}")

        val scenarios = portfolio.generate(config)
        scenarioMapper.writeAll(outputPath, scenarios)

        echo("Portfolio written to $outputPath")
    }

    /**
     * A portfolio that can be generated.
     */
    sealed class Portfolio(name: String) : OptionGroup(name) {
        /**
         * Generate the scenarios of this portfolio.
         */
        abstract fun generate(config: Config): List<ScenarioSpec>
    }

    /**
     * The baseline portfolio in which we explore the baseline trace with various scales of the baseline topology.
     */
    class BaselinePortfolio : Portfolio("Baseline portfolio") {
        /**
         * The topology to explore.
         */
        private val topologies = listOf(
            BASE_TOPOLOGY,
            BASE_TOPOLOGY_25P,
            BASE_TOPOLOGY_50P,
            BASE_TOPOLOGY_75P,
            BASE_TOPOLOGY_90P,
            BASE_TOPOLOGY_200P,
            BASE_TOPOLOGY_400P,
        )

        override fun generate(config: Config): List<ScenarioSpec> {
            return topologies.map { topology ->
                ScenarioSpec.fromDefaults(config, topology = topology, partitions = mapOf("topology" to topology.id))
            }
        }
    }

    /**
     * The workload portfolio in which we explore the baseline topology against different workloads.
     */
    class WorkloadPortfolio : Portfolio("Workload portfolio") {
        /**
         * The workloads to explore.
         */
        private val workloads = listOf(
            "baseline",
            "azure",
            "bitbrains",
            "materna",
        )

        override fun generate(config: Config): List<ScenarioSpec> {
            return workloads.map { workload ->
                ScenarioSpec.fromDefaults(config, workload = WorkloadSpec(workload), partitions = mapOf("workload" to workload))
            }
        }
    }

    /**
     * The scheduler portfolio in which we explore the impact of different schedulers on the baseline workload.
     */
    class SchedulerPortfolio : Portfolio("Scheduler portfolio") {
        /**
         * A mapping from scheduler name to scheduler implementation.
         */
        private val schedulers = mapOf(
            "mem" to SCHEDULER_MEM,
            "mem-inv" to SCHEDULER_MEM_INV,
            "core-mem" to SCHEDULER_CORE_MEM,
            "core-mem-inv" to SCHEDULER_CORE_MEM_INV,
            "active-servers" to SCHEDULER_ACTIVE_SERVERS,
            "active-servers-inv" to SCHEDULER_ACTIVE_SERVERS_INV,
            "provisioned-cores" to SCHEDULER_PROVISIONED_CORES,
            "provisioned-cores-inv" to SCHEDULER_PROVISIONED_CORES_INV,
            "random" to SCHEDULER_RANDOM,
            "combo" to SCHEDULER_COMBO,
            "default" to SCHEDULER_DEFAULT,
        )

        override fun generate(config: Config): List<ScenarioSpec> {
            return schedulers.map { (name, spec) ->
                ScenarioSpec.fromDefaults(config, scheduler = spec, partitions = mapOf("scheduler" to name))
            }
        }
    }

    /**
     * A customizable portfolio.
     */
    class CustomPortfolio : RadiceGenerateCommand.Portfolio("Custom portfolio") {
        /**
         * The traces to run.
         */
        private val traces by option("--trace", help = "trace to run")
            .multiple(default = listOf("baseline"))

        /**
         * The topology to use.
         */
        private val topologies by option("--topology", help = "topology to run")
            .choice(
                "base" to BASE_TOPOLOGY,
                "base-25%" to BASE_TOPOLOGY_25P,
                "base-50%" to BASE_TOPOLOGY_50P,
                "base-75%" to BASE_TOPOLOGY_75P,
                "base-90%" to BASE_TOPOLOGY_90P,
                "base-200%" to BASE_TOPOLOGY_200P,
                "base-400%" to BASE_TOPOLOGY_400P,
                "exp-vel-ver-het" to CAPELIN_EXP_VEL_VER_HET,
                "exp-vel-ver-hom" to CAPELIN_EXP_VEL_VER_HOM,
                "exp-vol-hor-het" to CAPELIN_EXP_VOL_HOR_HET,
                "exp-vol-hor-hom" to CAPELIN_EXP_VOL_HOR_HOM,
                "exp-vol-ver-het" to CAPELIN_EXP_VOL_VER_HET,
                "exp-vol-ver-hom" to CAPELIN_EXP_VOL_VER_HOM,
                "rep-vel-ver-het" to CAPELIN_REP_VEL_VER_HET,
                "rep-vel-ver-hom" to CAPELIN_REP_VEL_VER_HOM,
                "rep-vol-hor-het" to CAPELIN_REP_VOL_HOR_HET,
                "rep-vol-hor-hom" to CAPELIN_REP_VOL_HOR_HOM,
                "rep-vol-ver-com" to CAPELIN_REP_VOL_VER_COM,
                "rep-vol-ver-het" to CAPELIN_REP_VOL_VER_HET,
                "rep-vol-ver-hom" to CAPELIN_REP_VOL_VER_HOM,
            )
            .multiple(listOf(BASE_TOPOLOGY))

        /**
         * The scheduler to use.
         */
        private val schedulers by option("--scheduler", help = "scheduler to use")
            .multiple(listOf("default"))

        /**
         * A mapping from scheduler name to scheduler implementation.
         */
        private val schedulerMap = mapOf(
            "mem" to SCHEDULER_MEM,
            "mem-inv" to SCHEDULER_MEM_INV,
            "core-mem" to SCHEDULER_CORE_MEM,
            "core-mem-inv" to SCHEDULER_CORE_MEM_INV,
            "active-servers" to SCHEDULER_ACTIVE_SERVERS,
            "active-servers-inv" to SCHEDULER_ACTIVE_SERVERS_INV,
            "provisioned-cores" to SCHEDULER_PROVISIONED_CORES,
            "provisioned-cores-inv" to SCHEDULER_PROVISIONED_CORES_INV,
            "random" to SCHEDULER_RANDOM,
            "combo" to SCHEDULER_COMBO,
            "default" to SCHEDULER_DEFAULT,
        )

        override fun generate(config: Config): List<ScenarioSpec> {
            val res = mutableListOf<ScenarioSpec>()

            for (trace in traces) {
                val tracePart = if (traces.size > 1) mapOf("trace" to trace) else emptyMap()
                for (topology in topologies) {
                    val topologyPart = if (topologies.size > 1) mapOf("topology" to topology.id) else emptyMap()

                    for (scheduler in schedulers) {
                        val schedulerPart = if (schedulers.size > 1) mapOf("scheduler" to scheduler) else emptyMap()

                        res += ScenarioSpec.fromDefaults(
                            config,
                            WorkloadSpec(trace),
                            topology,
                            requireNotNull(schedulerMap[scheduler]),
                            partitions = tracePart + topologyPart + schedulerPart
                        )
                    }
                }
            }

            return res
        }
    }
}
