/*
 * Copyright (c) 2020 AtLarge Research
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

package org.opendc.runner.web

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import com.mongodb.MongoClientSettings
import com.mongodb.MongoCredential
import com.mongodb.ServerAddress
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import io.opentelemetry.api.metrics.MeterProvider
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.export.MetricProducer
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import mu.KotlinLogging
import org.bson.Document
import org.bson.types.ObjectId
import org.opendc.compute.service.scheduler.FilterScheduler
import org.opendc.compute.service.scheduler.filters.ComputeCapabilitiesFilter
import org.opendc.compute.service.scheduler.filters.ComputeFilter
import org.opendc.compute.service.scheduler.weights.*
import org.opendc.experiments.capelin.*
import org.opendc.experiments.capelin.model.Workload
import org.opendc.experiments.capelin.trace.ParquetTraceReader
import org.opendc.experiments.capelin.trace.RawParquetTraceReader
import org.opendc.format.environment.EnvironmentReader
import org.opendc.simulator.core.runBlockingSimulation
import org.opendc.telemetry.sdk.toOtelClock
import java.io.File
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

/**
 * Represents the CLI command for starting the OpenDC web runner.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public class RunnerCli : CliktCommand(name = "runner") {
    /**
     * The name of the database to use.
     */
    private val mongoDb by option(
        "--mongo-db",
        help = "name of the database to use",
        envvar = "OPENDC_DB"
    )
        .default("opendc")

    /**
     * The database host to connect to.
     */
    private val mongoHost by option(
        "--mongo-host",
        help = "database host to connect to",
        envvar = "OPENDC_DB_HOST"
    )
        .default("localhost")

    /**
     * The database port to connect to.
     */
    private val mongoPort by option(
        "--mongo-port",
        help = "database port to connect to",
        envvar = "OPENDC_DB_PORT"
    )
        .int()
        .default(27017)

    /**
     * The database user to connect with.
     */
    private val mongoUser by option(
        "--mongo-user",
        help = "database user to connect with",
        envvar = "OPENDC_DB_USER"
    )
        .default("opendc")

    /**
     * The database password to connect with.
     */
    private val mongoPassword by option(
        "--mongo-password",
        help = "database password to connect with",
        envvar = "OPENDC_DB_PASSWORD"
    )
        .convert { it.toCharArray() }
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
        .default(60 * 3) // Experiment may run for a maximum of three minutes

    /**
     * Connect to the user-specified database.
     */
    private fun createDatabase(): MongoDatabase {
        val credential = MongoCredential.createScramSha1Credential(
            mongoUser,
            mongoDb,
            mongoPassword
        )

        val settings = MongoClientSettings.builder()
            .credential(credential)
            .applyToClusterSettings { it.hosts(listOf(ServerAddress(mongoHost, mongoPort))) }
            .build()
        val client = MongoClients.create(settings)
        return client.getDatabase(mongoDb)
    }

    /**
     * Run a single scenario.
     */
    private suspend fun runScenario(portfolio: Document, scenario: Document, topologyParser: TopologyParser): List<WebExperimentMonitor.Result> {
        val id = scenario.getObjectId("_id")

        logger.info { "Constructing performance interference model" }

        val traceDir = File(
            tracePath,
            scenario.getEmbedded(listOf("trace", "traceId"), String::class.java)
        )
        val traceReader = RawParquetTraceReader(traceDir)
        val targets = portfolio.get("targets", Document::class.java)
        val topologyId = scenario.getEmbedded(listOf("topology", "topologyId"), ObjectId::class.java)
        val environment = topologyParser.read(topologyId)

        val results = (0 until targets.getInteger("repeatsPerScenario")).map {
            logger.info { "Starting repeat $it" }
            withTimeout(runTimeout * 1000) {
                runRepeat(scenario, it, environment, traceReader)
            }
        }

        logger.info { "Finished simulation for scenario $id" }

        return results
    }

    /**
     * Run a single repeat.
     */
    private suspend fun runRepeat(
        scenario: Document,
        repeat: Int,
        environment: EnvironmentReader,
        traceReader: RawParquetTraceReader,
    ): WebExperimentMonitor.Result {
        val monitor = WebExperimentMonitor()

        try {
            runBlockingSimulation {
                val seed = repeat
                val traceDocument = scenario.get("trace", Document::class.java)
                val workloadName = traceDocument.getString("traceId")
                val workloadFraction = traceDocument.get("loadSamplingFraction", Number::class.java).toDouble()

                val seeder = Random(seed)

                val chan = Channel<Unit>(Channel.CONFLATED)

                val meterProvider: MeterProvider = SdkMeterProvider
                    .builder()
                    .setClock(clock.toOtelClock())
                    .build()
                val metricProducer = meterProvider as MetricProducer

                val operational = scenario.get("operational", Document::class.java)
                val allocationPolicy =
                    when (val policyName = operational.getString("schedulerName")) {
                        "mem" -> FilterScheduler(
                            filters = listOf(ComputeFilter(), ComputeCapabilitiesFilter()),
                            weighers = listOf(MemoryWeigher() to -1.0)
                        )
                        "mem-inv" -> FilterScheduler(
                            filters = listOf(ComputeFilter(), ComputeCapabilitiesFilter()),
                            weighers = listOf(MemoryWeigher() to 1.0)
                        )
                        "core-mem" -> FilterScheduler(
                            filters = listOf(ComputeFilter(), ComputeCapabilitiesFilter()),
                            weighers = listOf(CoreMemoryWeigher() to -1.0)
                        )
                        "core-mem-inv" -> FilterScheduler(
                            filters = listOf(ComputeFilter(), ComputeCapabilitiesFilter()),
                            weighers = listOf(CoreMemoryWeigher() to 1.0)
                        )
                        "active-servers" -> FilterScheduler(
                            filters = listOf(ComputeFilter(), ComputeCapabilitiesFilter()),
                            weighers = listOf(ProvisionedCoresWeigher() to -1.0)
                        )
                        "active-servers-inv" -> FilterScheduler(
                            filters = listOf(ComputeFilter(), ComputeCapabilitiesFilter()),
                            weighers = listOf(InstanceCountWeigher() to 1.0)
                        )
                        "provisioned-cores" -> FilterScheduler(
                            filters = listOf(ComputeFilter(), ComputeCapabilitiesFilter()),
                            weighers = listOf(ProvisionedCoresWeigher() to -1.0)
                        )
                        "provisioned-cores-inv" -> FilterScheduler(
                            filters = listOf(ComputeFilter(), ComputeCapabilitiesFilter()),
                            weighers = listOf(ProvisionedCoresWeigher() to 1.0)
                        )
                        "random" -> FilterScheduler(
                            filters = listOf(ComputeFilter(), ComputeCapabilitiesFilter()),
                            weighers = listOf(RandomWeigher(java.util.Random(seeder.nextLong())) to 1.0)
                        )
                        else -> throw IllegalArgumentException("Unknown policy $policyName")
                    }

                val trace = ParquetTraceReader(
                    listOf(traceReader),
                    Workload(workloadName, workloadFraction),
                    seed
                )
                val failureFrequency = if (operational.getBoolean("failuresEnabled", false)) 24.0 * 7 else 0.0

                withComputeService(clock, meterProvider, environment, allocationPolicy) { scheduler ->
                    val failureDomain = if (failureFrequency > 0) {
                        logger.debug { "ENABLING failures" }
                        createFailureDomain(
                            this,
                            clock,
                            seeder.nextInt(),
                            failureFrequency,
                            scheduler,
                            chan
                        )
                    } else {
                        null
                    }

                    withMonitor(monitor, clock, meterProvider as MetricProducer, scheduler) {
                        processTrace(
                            clock,
                            trace,
                            scheduler,
                            chan,
                            monitor
                        )
                    }

                    failureDomain?.cancel()
                }

                val monitorResults = collectMetrics(metricProducer)
                logger.debug { "Finish SUBMIT=${monitorResults.submittedVms} FAIL=${monitorResults.unscheduledVms} QUEUE=${monitorResults.queuedVms} RUNNING=${monitorResults.runningVms}" }
            }
        } catch (cause: Throwable) {
            logger.warn(cause) { "Experiment failed" }
        }

        return monitor.getResult()
    }

    private val POLL_INTERVAL = 5000L // ms = 5 s
    private val HEARTBEAT_INTERVAL = 60000L // ms = 1 min

    override fun run(): Unit = runBlocking(Dispatchers.Default) {
        logger.info { "Starting OpenDC web runner" }
        logger.info { "Connecting to MongoDB instance" }
        val database = createDatabase()
        val manager = ScenarioManager(database.getCollection("scenarios"))
        val portfolios = database.getCollection("portfolios")
        val topologies = database.getCollection("topologies")
        val topologyParser = TopologyParser(topologies)

        logger.info { "Watching for queued scenarios" }

        while (true) {
            val scenario = manager.findNext()

            if (scenario == null) {
                delay(POLL_INTERVAL)
                continue
            }

            val id = scenario.getObjectId("_id")

            logger.info { "Found queued scenario $id: attempting to claim" }

            if (!manager.claim(id)) {
                logger.info { "Failed to claim scenario" }
                continue
            }

            coroutineScope {
                // Launch heartbeat process
                val heartbeat = launch {
                    while (true) {
                        delay(HEARTBEAT_INTERVAL)
                        manager.heartbeat(id)
                    }
                }

                try {
                    val portfolio = portfolios.find(Filters.eq("_id", scenario.getObjectId("portfolioId"))).first()!!
                    val results = runScenario(portfolio, scenario, topologyParser)

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
}

/**
 * Main entry point of the runner.
 */
public fun main(args: Array<String>): Unit = RunnerCli().main(args)
