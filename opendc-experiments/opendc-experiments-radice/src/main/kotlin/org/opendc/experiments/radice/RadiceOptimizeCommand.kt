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
import com.github.ajalt.clikt.parameters.groups.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import com.typesafe.config.Config
import io.jenetics.*
import io.jenetics.engine.Codec
import io.jenetics.engine.Engine
import io.jenetics.engine.EvolutionResult
import io.jenetics.engine.EvolutionStatistics
import io.jenetics.engine.Limits.bySteadyFitness
import io.jenetics.ext.moea.Vec
import io.jenetics.util.IntRange
import io.jenetics.util.RandomRegistry
import org.opendc.experiments.radice.genetic.*
import org.opendc.experiments.radice.scenario.*
import org.opendc.experiments.radice.scenario.mapper.YamlScenarioMapper
import org.opendc.experiments.radice.scenario.topology.*
import org.opendc.experiments.radice.util.RadiceConcurrentEvaluator
import java.io.File
import java.util.*
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.atomic.AtomicLong
import java.util.stream.LongStream

/**
 * A [CliktCommand] for optimizing the datacenter design for risk using a genetic algorithm.
 */
internal class RadiceOptimizeCommand(private val config: Config) : CliktCommand(name = "optimize") {
    /**
     * The path to the trace directory.
     */
    private val tracePath by option("--trace-path", help = "path to trace directory")
        .file(canBeDir = true, canBeFile = false)
        .defaultLazy { File(config.getString("trace-path")) }

    /**
     * The path to the experiment output.
     */
    private val outputPath by option("-O", "--output", help = "path to experiment output")
        .file(canBeDir = true, canBeFile = false)
        .defaultLazy { File(config.getString("output-path")) }

    /**
     * Disable writing output.
     */
    private val disableOutput by option("--disable-output", help = "disable output").flag()

    /**
     * The number of threads to use for parallelism.
     */
    private val parallelism by option("-p", "--parallelism", help = "number of worker threads")
        .int()
        .default(Runtime.getRuntime().availableProcessors() - 1)

    /**
     * The number of repeats.
     */
    private val repeats by option("-r", "--repeats", help = "number of repeats")
        .int()
        .default(32)

    /**
     * The seed for seeding the random instances.
     */
    private val seed by option("-s", "--seed", help = "initial seed for randomness")
        .long()
        .default(0)

    /**
     * The population size for the genetic algorithm.
     */
    private val populationSize by option(help = "population size of the genetic algorithm")
        .int()
        .default(30)

    /**
     * The maximum number of generations to run the algorithm for.
     */
    private val maxGenerations by option(help = "maximum number of generations to evolve")
        .long()
        .default(50)

    /**
     * The optimization objective of the problem.
     */
    private val objective: GeneticObjective<*> by option("-o", "--objective", help = "optimization objective").choice(
        "total" to totalCosts()
    ).default(totalCosts())

    /**
     * The trace to run.
     */
    private val trace by option(help = "trace to run")
        .default("baseline")

    /**
     * The topology to use.
     */
    private val topology by option(help = "topologies to experiment with").groupSwitch(
        "--optimize-topology" to OptimizeTopologyOption(),
        "--static-topology" to StaticTopologyOption()
    ).defaultByName("--optimize-topology")

    /**
     * The scheduler to use.
     */
    private val scheduler by option(help = "schedulers to experiment with").groupSwitch(
        "--optimize-scheduler" to OptimizeSchedulerOption(),
        "--static-scheduler" to StaticSchedulerOption()
    ).defaultByName("--static-scheduler")

    /**
     * The base partitions to use for the invocation
     */
    private val basePartitions: Map<String, String> by option("-P", "--base-partitions").associate()

    /**
     * The [RadiceRunner] used to run the experiments.
     */
    private val runner by lazy { RadiceRunner(tracePath, null) }

    /**
     * The [YamlScenarioMapper] to use.
     */
    private val scenarioMapper = YamlScenarioMapper()

    override fun run() {
        val pool = ForkJoinPool(parallelism)

        try {
            // Create evolution statistics consumer.
            val statistics = EvolutionStatistics.ofNumber<Double>()

            val codec = createCodec()
            val decoder = codec.decoder()

            val res = RandomRegistry.with(Random(seed)) {
                val evaluator =
                    RadiceConcurrentEvaluator<DoubleGene, Double>(pool) { eval(decoder.apply(it)).data()[0] }

                val engine = Engine.Builder(evaluator, codec.encoding())
                    .minimizing()
                    .populationSize(populationSize)
                    .survivorsSelector(TournamentSelector(5))
                    .alterers(
                        UniformCrossover(),
                        Mutator(0.15),
                        GaussianMutator(0.10)
                    )
                    .build()

                engine.stream()
                    // Truncate the evolution stream after 10 "steady" generations.
                    .limit(bySteadyFitness(10))
                    // The evolution will stop after maximal 50 generations.
                    .limit(maxGenerations)
                    // Update the evaluation statistics after each generation
                    .peek { value ->
                        statistics.accept(value)
                        echo(statistics)
                    }
                    // Collect (reduce) the evolution stream to its best phenotype.
                    .collect(EvolutionResult.toBestPhenotype())
            }

            echo(statistics)

            val spec = decoder.apply(res.genotype())
            echo("Result $spec -> ${res.fitness()}")
        } finally {
            pool.shutdown()
        }
    }

    /**
     * Helper function to evaluate the specified [Genotype].
     */
    private fun eval(scenario: ScenarioSpec): Vec<DoubleArray> {
        val runner = runner
        val seed = seed

        @Suppress("UNCHECKED_CAST")
        val objective = objective as GeneticObjective<Any>
        val acc = objective.createAccumulator()

        LongStream.range(0, repeats.toLong())
            .parallel()
            .forEach { repeat ->
                val costs = runner.runScenario(scenario, seed + repeat)
                objective.add(acc, costs)
            }

        val fitness = objective.fitness(acc)
        if (!disableOutput) {
            scenarioMapper.write(File(outputPath, "scenarios"), scenario, fitness.data().toList())
        }
        return fitness
    }

    /**
     * Helper function to construct the [Codec] based on the parameters.
     */
    private fun createCodec(): Codec<ScenarioSpec, DoubleGene> {
        val topology = topology
        val scheduler = scheduler
        return when {
            topology is OptimizeTopologyOption && scheduler is OptimizeSchedulerOption ->
                Codec.of(topology.createCodec(), scheduler.createCodec()) { topologySpec, schedulerSpec -> createScenario(topologySpec, schedulerSpec) }
            topology is OptimizeTopologyOption && scheduler is StaticSchedulerOption ->
                topology.createCodec().map { spec -> createScenario(spec, scheduler.spec) }
            topology is StaticTopologyOption && scheduler is OptimizeSchedulerOption ->
                scheduler.createCodec().map { spec -> createScenario(topology.spec, spec) }
            else -> throw IllegalArgumentException("Must optimize scheduler or topology")
        }
    }

    /**
     * The next identifier to allocate for a scenario.
     */
    private val nextPhenotype = AtomicLong()

    /**
     * Construct a [ScenarioSpec] for the specified [topology] and [scheduler].
     */
    private fun createScenario(topology: TopologySpec, scheduler: SchedulerSpec): ScenarioSpec {
        val phenotype = nextPhenotype.getAndIncrement()
        return ScenarioSpec.fromDefaults(
            config,
            WorkloadSpec(trace),
            topology,
            scheduler,
            partitions = basePartitions + ("phenotype" to phenotype.toString())
        )
    }

    /**
     * The way the topology is selected.
     */
    private sealed class TopologyOption(name: String) : OptionGroup(name)

    /**
     * A [TopologyOption] where the topology is optimized.
     */
    private class OptimizeTopologyOption : TopologyOption("Optimize the topology") {
        /**
         * The range for the number of clusters to consider.
         */
        private val clusterCount by option(
            "--topology-cluster-count",
            help = "range for the number of hosts per cluster"
        )
            .int()
            .pair()
            .default(1 to 32)

        /**
         * The range for the number of hosts in a cluster to consider.
         */
        private val hostCount by option("--topology-host-count", help = "range for the number of hosts per cluster")
            .int()
            .pair()
            .default(1 to 128)

        /**
         * List of available machine models to experiment with.
         */
        private val machineModels = listOf(
            DELL_R620,
            DELL_R620_FAST,
            // DELL_R620_28_CORE,
            // DELL_R620_128_CORE,
            DELL_R710,
            DELL_R820,
            // DELL_R820_128_CORE,
            DELL_R740,
            DELL_R240,
            DELL_R6515_FASTER,
            DELL_R7515_FASTER,
            DELL_R7525_FASTER,
            HPE_DL345_GEN10_PLUS_FASTER,
            HPE_DL380_GEN10_PLUS_FASTER,
            HPE_DL110_GEN10_PLUS_FASTER
        )

        /**
         * Construct a [Codec] for representing a [TopologySpec] as [DoubleGene]s.
         */
        fun createCodec(): Codec<TopologySpec, DoubleGene> {
            val clusterCount = clusterCount.let { IntRange.of(it.first, it.second) }
            val hostCount = hostCount.let { IntRange.of(it.first, it.second) }
            return createTopologyCodec(clusterCount, hostCount, machineModels)
        }
    }

    /**
     * A [TopologyOption] where the topology is static.
     */
    private class StaticTopologyOption : TopologyOption("Use a static topology") {
        /**
         * The static topology to use.
         */
        val spec by option("--topology", help = "topology to run")
            .choice(
                "base" to BASE_TOPOLOGY,
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
            .default(BASE_TOPOLOGY)
    }

    /**
     * The way the scheduler is selected.
     */
    private sealed class SchedulerOption(name: String) : OptionGroup(name)

    /**
     * A [SchedulerOption] where the scheduler is optimized.
     */
    private class OptimizeSchedulerOption : SchedulerOption("Optimize the scheduler") {
        /**
         * The scheduler subset size to use.
         */
        private val subsetSize by option("--scheduler-subset-size", help = "scheduler subset size")
            .int()
            .pair()
            .default(1 to 32)

        /**
         * Construct a [Codec] for representing a [SchedulerSpec] as [DoubleGene]s.
         */
        fun createCodec(): Codec<SchedulerSpec, DoubleGene> {
            val subsetSize = subsetSize.let { IntRange.of(it.first, it.second) }
            return createSchedulerCodec(subsetSize)
        }
    }

    /**
     * A [SchedulerOption] where the scheduler is static.
     */
    private class StaticSchedulerOption : SchedulerOption("Use a static scheduler") {
        /**
         * The scheduler to use.
         */
        val spec by option("--scheduler", help = "scheduler to use")
            .choice(
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
                "default" to SCHEDULER_DEFAULT
            )
            .default(SCHEDULER_DEFAULT)
    }
}
