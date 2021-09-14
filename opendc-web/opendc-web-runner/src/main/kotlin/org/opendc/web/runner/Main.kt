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

package org.opendc.web.runner

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.long
import io.opentelemetry.api.metrics.MeterProvider
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.opendc.experiments.capelin.*
import org.opendc.experiments.capelin.env.EnvironmentReader
import org.opendc.experiments.capelin.env.MachineDef
import org.opendc.experiments.capelin.model.Workload
import org.opendc.experiments.capelin.trace.ParquetTraceReader
import org.opendc.experiments.capelin.trace.PerformanceInterferenceReader
import org.opendc.experiments.capelin.trace.RawParquetTraceReader
import org.opendc.experiments.capelin.util.ComputeServiceSimulator
import org.opendc.experiments.capelin.util.createComputeScheduler
import org.opendc.simulator.compute.kernel.interference.VmInterferenceModel
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.power.LinearPowerModel
import org.opendc.simulator.core.runBlockingSimulation
import org.opendc.telemetry.compute.ComputeMetricExporter
import org.opendc.telemetry.compute.collectServiceMetrics
import org.opendc.telemetry.sdk.metrics.export.CoroutineMetricReader
import org.opendc.telemetry.sdk.toOtelClock
import org.opendc.web.client.ApiClient
import org.opendc.web.client.AuthConfiguration
import org.opendc.web.client.model.Scenario
import org.opendc.web.client.model.Topology
import java.io.File
import java.net.URI
import java.time.Duration
import java.util.*
import org.opendc.web.client.model.Portfolio as ClientPortfolio

private val logger = KotlinLogging.logger {}

/**
 * Represents the CLI command for starting the OpenDC web runner.
 */
class RunnerCli : CliktCommand(name = "runner") {
    /**
     * The URL to the OpenDC API.
     */
    private val apiUrl by option(
        "--api-url",
        help = "url to the OpenDC API",
        envvar = "OPENDC_API_URL"
    )
        .convert { URI(it) }
        .default(URI("https://api.opendc.org/v2"))

    /**
     * The auth domain to use.
     */
    private val authDomain by option(
        "--auth-domain",
        help = "auth domain of the OpenDC API",
        envvar = "AUTH0_DOMAIN"
    )
        .required()

    /**
     * The auth client ID to use.
     */
    private val authClientId by option(
        "--auth-id",
        help = "auth client id of the OpenDC API",
        envvar = "AUTH0_CLIENT_ID"
    )
        .required()

    /**
     * The auth client secret to use.
     */
    private val authClientSecret by option(
        "--auth-secret",
        help = "auth client secret of the OpenDC API",
        envvar = "AUTH0_CLIENT_SECRET"
    )
        .required()

    /**
     * The path to the traces directory.
     */
    private val tracePath by option(
        "--traces",
        help = "path to the directory containing the traces",
        envvar = "OPENDC_TRACES"
    )
        .file(canBeFile = false)
        .defaultLazy { File("traces/") }

    /**
     * The maximum duration of a single experiment run.
     */
    private val runTimeout by option(
        "--run-timeout",
        help = "maximum duration of experiment in seconds",
        envvar = "OPENDC_RUN_TIMEOUT"
    )
        .long()
        .default(60L * 3) // Experiment may run for a maximum of three minutes

    /**
     * Run a single scenario.
     */
    private suspend fun runScenario(portfolio: ClientPortfolio, scenario: Scenario, environment: EnvironmentReader): List<WebComputeMonitor.Result> {
        val id = scenario.id

        logger.info { "Constructing performance interference model" }

        val traceDir = File(
            tracePath,
            scenario.trace.traceId
        )
        val traceReader = RawParquetTraceReader(traceDir)
        val interferenceGroups = let {
            val path = File(traceDir, "performance-interference-model.json")
            val operational = scenario.operationalPhenomena
            val enabled = operational.performanceInterferenceEnabled

            if (!enabled || !path.exists()) {
                return@let null
            }

            PerformanceInterferenceReader().read(path.inputStream())
        }

        val targets = portfolio.targets
        val results = (0 until targets.repeatsPerScenario).map { repeat ->
            logger.info { "Starting repeat $repeat" }
            withTimeout(runTimeout * 1000) {
                val interferenceModel = interferenceGroups?.let { VmInterferenceModel(it, Random(repeat.toLong())) }
                runRepeat(scenario, repeat, environment, traceReader, interferenceModel)
            }
        }

        logger.info { "Finished simulation for scenario $id" }

        return results
    }

    /**
     * Run a single repeat.
     */
    private suspend fun runRepeat(
        scenario: Scenario,
        repeat: Int,
        environment: EnvironmentReader,
        traceReader: RawParquetTraceReader,
        interferenceModel: VmInterferenceModel?
    ): WebComputeMonitor.Result {
        val monitor = WebComputeMonitor()

        try {
            runBlockingSimulation {
                val workloadName = scenario.trace.traceId
                val workloadFraction = scenario.trace.loadSamplingFraction

                val seeder = Random(repeat.toLong())

                val meterProvider: MeterProvider = SdkMeterProvider
                    .builder()
                    .setClock(clock.toOtelClock())
                    .build()

                val operational = scenario.operationalPhenomena
                val computeScheduler = createComputeScheduler(operational.schedulerName, seeder)

                val trace = ParquetTraceReader(
                    listOf(traceReader),
                    Workload(workloadName, workloadFraction),
                    repeat
                )
                val failureModel =
                    if (operational.failuresEnabled)
                        grid5000(Duration.ofDays(7), repeat)
                    else
                        null

                val simulator = ComputeServiceSimulator(
                    coroutineContext,
                    clock,
                    computeScheduler,
                    environment.read(),
                    failureModel,
                    interferenceModel.takeIf { operational.performanceInterferenceEnabled }
                )

                val metricReader = CoroutineMetricReader(this, simulator.producers, ComputeMetricExporter(clock, monitor))

                try {
                    simulator.run(trace)
                } finally {
                    simulator.close()
                    metricReader.close()
                }

                val serviceMetrics = collectServiceMetrics(clock.millis(), simulator.producers[0])
                logger.debug {
                    "Finish " +
                        "SUBMIT=${serviceMetrics.instanceCount} " +
                        "FAIL=${serviceMetrics.failedInstanceCount} " +
                        "QUEUE=${serviceMetrics.queuedInstanceCount} " +
                        "RUNNING=${serviceMetrics.runningInstanceCount}"
                }
            }
        } catch (cause: Throwable) {
            logger.warn(cause) { "Experiment failed" }
        }

        return monitor.getResult()
    }

    private val POLL_INTERVAL = 30000L // ms = 30 s
    private val HEARTBEAT_INTERVAL = 60000L // ms = 1 min

    override fun run(): Unit = runBlocking(Dispatchers.Default) {
        logger.info { "Starting OpenDC web runner" }

        val client = ApiClient(baseUrl = apiUrl, AuthConfiguration(authDomain, authClientId, authClientSecret))
        val manager = ScenarioManager(client)

        logger.info { "Watching for queued scenarios" }

        while (true) {
            val scenario = manager.findNext()

            if (scenario == null) {
                delay(POLL_INTERVAL)
                continue
            }

            val id = scenario.id

            logger.info { "Found queued scenario $id: attempting to claim" }

            if (!manager.claim(id)) {
                logger.info { "Failed to claim scenario" }
                continue
            }

            coroutineScope {
                // Launch heartbeat process
                val heartbeat = launch {
                    while (true) {
                        manager.heartbeat(id)
                        delay(HEARTBEAT_INTERVAL)
                    }
                }

                try {
                    val scenarioModel = client.getScenario(id)!!
                    val portfolio = client.getPortfolio(scenarioModel.portfolioId)!!
                    val environment = convert(client.getTopology(scenarioModel.topology.topologyId)!!)
                    val results = runScenario(portfolio, scenarioModel, environment)

                    logger.info { "Writing results to database" }

                    manager.finish(id, results)

                    logger.info { "Successfully finished scenario $id" }
                } catch (e: Exception) {
                    logger.error(e) { "Scenario failed to finish" }
                    manager.fail(id)
                } finally {
                    heartbeat.cancel()
                }
            }
        }
    }

    /**
     * Convert the specified [topology] into an [EnvironmentReader] understood by Capelin.
     */
    private fun convert(topology: Topology): EnvironmentReader {
        val nodes = mutableListOf<MachineDef>()
        val random = Random(0)

        val machines = topology.rooms.asSequence()
            .flatMap { room ->
                room.tiles.flatMap { tile ->
                    tile.rack?.machines?.map { machine -> tile.rack to machine } ?: emptyList()
                }
            }
        for ((rack, machine) in machines) {
            val clusterId = rack.id
            val position = machine.position

            val processors = machine.cpus.flatMap { cpu ->
                val cores = cpu.numberOfCores
                val speed = cpu.clockRateMhz
                // TODO Remove hard coding of vendor
                val node = ProcessingNode("Intel", "amd64", cpu.name, cores)
                List(cores) { coreId ->
                    ProcessingUnit(node, coreId, speed)
                }
            }
            val memoryUnits = machine.memory.map { memory ->
                MemoryUnit(
                    "Samsung",
                    memory.name,
                    memory.speedMbPerS,
                    memory.sizeMb.toLong()
                )
            }

            val energyConsumptionW = machine.cpus.sumOf { it.energyConsumptionW }

            nodes.add(
                MachineDef(
                    UUID(random.nextLong(), random.nextLong()),
                    "node-$clusterId-$position",
                    mapOf("cluster" to clusterId),
                    MachineModel(processors, memoryUnits),
                    LinearPowerModel(2 * energyConsumptionW, energyConsumptionW * 0.5)
                )
            )
        }

        return object : EnvironmentReader {
            override fun read(): List<MachineDef> = nodes
            override fun close() {}
        }
    }
}

/**
 * Main entry point of the runner.
 */
fun main(args: Array<String>): Unit = RunnerCli().main(args)
