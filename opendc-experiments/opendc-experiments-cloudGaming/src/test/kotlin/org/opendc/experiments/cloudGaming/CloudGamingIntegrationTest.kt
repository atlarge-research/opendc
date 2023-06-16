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

package org.opendc.experiments.cloudGaming

import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.dataformat.csv.CsvFactory
import com.fasterxml.jackson.dataformat.csv.CsvParser
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opendc.compute.service.ComputeService
import org.opendc.compute.service.scheduler.FilterScheduler
import org.opendc.compute.service.scheduler.filters.ComputeFilter
import org.opendc.compute.service.scheduler.filters.RamFilter
import org.opendc.compute.service.scheduler.filters.VCpuFilter
import org.opendc.compute.service.scheduler.weights.CoreRamWeigher
import org.opendc.experiments.cloudGaming.topology.clusterTopology
import org.opendc.experiments.compute.ComputeWorkloadLoader
import org.opendc.experiments.compute.VirtualMachine
import org.opendc.experiments.compute.registerComputeMonitor
import org.opendc.experiments.compute.replay
import org.opendc.experiments.compute.setupComputeService
import org.opendc.experiments.compute.setupHosts
import org.opendc.experiments.compute.telemetry.ComputeMonitor
import org.opendc.experiments.compute.telemetry.table.HostTableReader
import org.opendc.experiments.compute.telemetry.table.ServiceTableReader
import org.opendc.experiments.compute.topology.HostSpec
import org.opendc.experiments.provisioner.Provisioner
import org.opendc.simulator.compute.workload.SimTrace
import org.opendc.simulator.kotlin.runSimulation
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * An integration test suite for the Cloud Gaming experiments.
 */
class CloudGamingIntegrationTest {
    /**
     * The monitor used to keep track of the metrics.
     */
    private lateinit var monitor: TestComputeMonitor

    /**
     * The [FilterScheduler] to use for all experiments.
     */
    private lateinit var computeScheduler: FilterScheduler

    /**
     * The [ComputeWorkloadLoader] responsible for loading the traces.
     */
//    private lateinit var workloadLoader: ComputeWorkloadLoader

    /**
     * Set up the experimental environment.
     */
    @BeforeEach
    fun setUp() {
        monitor = TestComputeMonitor()
        computeScheduler = FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(16.0), RamFilter(1.0)),
            weighers = listOf(CoreRamWeigher(multiplier = 1.0))
        )
//        workloadLoader = ComputeWorkloadLoader(File("src/test/resources/trace")) // Maybe I should use this instead?
    }

    /**
     * Test a small simulation setup.
     */
    @Test
    fun testSmall() = runSimulation {
        val seed = 1L
        val workload = getWorkload("myTraces")
        val topology = createTopology("small")
        val monitor = monitor

        Provisioner(dispatcher, seed).use { provisioner ->
            provisioner.runSteps(
                setupComputeService(serviceDomain = "compute.opendc.org", { computeScheduler }),
                registerComputeMonitor(serviceDomain = "compute.opendc.org", monitor),
                setupHosts(serviceDomain = "compute.opendc.org", topology)
            )

            val service = provisioner.registry.resolve("compute.opendc.org", ComputeService::class.java)!!
            service.replay(timeSource, workload, seed)
        }

        println(
            "Scheduler " +
                "Success=${monitor.attemptsSuccess} " +
                "Failure=${monitor.attemptsFailure} " +
                "Error=${monitor.attemptsError} " +
                "Pending=${monitor.serversPending} " +
                "Active=${monitor.serversActive}"
        )

        println(
            "Results:" +
                "idleTime=${monitor.idleTime} " +
                "activeTime=${monitor.activeTime} " +
                "stealTime=${monitor.stealTime} " +
                "lostTime=${monitor.lostTime} " +
                "energyUse=${monitor.energyUsage} " +
                "upTime=${monitor.uptime}"
        )
    }

    /**
     * Test a generated csv.
     */
    @Test
    fun testGenCsv() = runSimulation {

        val tracesDir = "genTraces"
        val usersPerHour = listOf(10, 15, 12, 10)

        // generate new trace
        CloudGamingTraceGenerator.generateTraceCsv(4, usersPerHour, 1, 1500.0, 3500.0, 8000, tracesDir)

        val seed = 1L
        val workload = getWorkload(tracesDir)
        val topology = createTopology("genTopo")
        val monitor = monitor

        Provisioner(dispatcher, seed).use { provisioner ->
            provisioner.runSteps(
                setupComputeService(serviceDomain = "compute.opendc.org", { computeScheduler }),
                registerComputeMonitor(serviceDomain = "compute.opendc.org", monitor),
                setupHosts(serviceDomain = "compute.opendc.org", topology)
            )

            val service = provisioner.registry.resolve("compute.opendc.org", ComputeService::class.java)!!
            service.replay(timeSource, workload, seed)
        }

        println(
            "Scheduler " +
                "Success=${monitor.attemptsSuccess} " +
                "Failure=${monitor.attemptsFailure} " +
                "Error=${monitor.attemptsError} " +
                "Pending=${monitor.serversPending} " +
                "Active=${monitor.serversActive}"
        )

        println(
            "Results:" +
                "idleTime=${monitor.idleTime} " +
                "activeTime=${monitor.activeTime} " +
                "stealTime=${monitor.stealTime} " +
                "lostTime=${monitor.lostTime} " +
                "energyUse=${monitor.energyUsage} " +
                "upTime=${monitor.uptime}"
        )
    }

    private val baseDir: File = File("src/test/resources/trace")

    private fun getWorkload(workloadDir: String) : List<VirtualMachine> {
        val traceFile = baseDir.resolve("$workloadDir/trace.csv")
        val metaFile = baseDir.resolve("$workloadDir/meta.csv")
        val fragments = parseFragments(traceFile) // takes traces and sticks them together (:
        return parseMeta(metaFile, fragments)
    }

    private val factory = CsvFactory()
        .enable(CsvParser.Feature.ALLOW_COMMENTS)
        .enable(CsvParser.Feature.TRIM_SPACES)

    private fun parseFragments(path: File): Map<Int, FragmentBuilder> {
        val fragments = mutableMapOf<Int, FragmentBuilder>()

        val parser = factory.createParser(path)
        parser.schema = fragmentsSchema

        var id = 0
        var timestamp = 0L
        var duration = 0L
        var cpuCount = 0
        var cpuUsage = 0.0

        while (!parser.isClosed) {
            val token = parser.nextValue()
            if (token == JsonToken.END_OBJECT) {
                val builder = fragments.computeIfAbsent(id) { FragmentBuilder() }
                val deadlineMs = timestamp
                val timeMs = (timestamp - duration)
                builder.add(timeMs, deadlineMs, cpuUsage, cpuCount)

                println("FRAGMENTS\n" +
                    "id $id\n" +
                    "timestamp $timestamp\n" +
                    "duration $duration\n" +
                    "cpu_count $cpuCount\n" +
                    "cpu_usage $cpuUsage\n"+
                    "timeMs $timeMs\n"+
                    "deadlineMs $deadlineMs\n")
                id = 0
                timestamp = 0L
                duration = 0
                cpuCount = 0
                cpuUsage = 0.0

                continue
            }

            when (parser.currentName) {
                "id" -> id = parser.valueAsInt
                "timestamp" -> timestamp = parser.valueAsLong
                "duration" -> duration = parser.valueAsLong
                "cpu_count" -> cpuCount = parser.valueAsInt
                "cpu_usage" -> cpuUsage = parser.valueAsDouble
            }
        }

        return fragments
    }
    private fun parseMeta(path: File, fragments: Map<Int, FragmentBuilder>): List<VirtualMachine> {
        val vms = mutableListOf<VirtualMachine>()
        var counter = 0

        val parser = factory.createParser(path)
        parser.schema = metaSchema

        var id = 0
        var startTime = 0L
        var stopTime = 0L
        var cpuCount = 0
        var cpuCapacity = 0.0
        var memCapacity = 0.0

        while (!parser.isClosed) {
            val token = parser.nextValue()
            if (token == JsonToken.END_OBJECT) {
                if (!fragments.containsKey(id)) {
                    continue
                }
                val builder = fragments.getValue(id)
                val totalLoad = builder.totalLoad
                val uid = UUID.nameUUIDFromBytes("$id-${counter++}".toByteArray())
//                println("adding VM:\n" +
//                    "UID $uid\n" +
//                    "ID $id\n" +
//                    "cpuCount $cpuCount\n" +
//                    "cpuCapacity $cpuCapacity\n" +
//                    "memCapacity $memCapacity\n" +
//                    "totalLoad $totalLoad\n" +
//                    "startTime $startTime\n" +
//                    "stopTime $stopTime\n")
                vms.add(
                    VirtualMachine(
                        uid,
                        id.toString(),
                        cpuCount,
                        cpuCapacity,
                        memCapacity.roundToLong(),
                        totalLoad,
                        Instant.ofEpochMilli(startTime),
                        Instant.ofEpochMilli(stopTime),
                        builder.build(),
                        null
                    )
                )

//                println("META\n" +
//                    "id $id\n" +
//                    "startTime $startTime\n" +
//                    "stopTime $stopTime\n" +
//                    "cpuCount $cpuCount\n" +
//                    "cpuCapacity $cpuCapacity\n"+
//                    "memCapacity $memCapacity\n")
                id = 0
                startTime = 0L
                stopTime = 0
                cpuCount = 0
                cpuCapacity = 0.0
                memCapacity = 0.0

                continue
            }

            when (parser.currentName) {
                "id" -> id = parser.valueAsInt
                "start_time" -> startTime = parser.valueAsLong
                "stop_time" -> stopTime = parser.valueAsLong
                "cpu_count" -> cpuCount = parser.valueAsInt
                "cpu_capacity" -> cpuCapacity = parser.valueAsDouble
                "mem_capacity" -> memCapacity = parser.valueAsDouble
            }
        }

        return vms
    }

    private class FragmentBuilder {
        /**
         * The total load of the trace.
         */
        @JvmField var totalLoad: Double = 0.0

        /**
         * The internal builder for the trace.
         */
        private val builder = SimTrace.builder()

        /**
         * The deadline of the previous fragment.
         */
        private var previousDeadline = Long.MIN_VALUE

        /**
         * Add a fragment to the trace.
         *
         * @param timestamp Timestamp at which the fragment starts (in epoch millis).
         * @param deadline Timestamp at which the fragment ends (in epoch millis).
         * @param usage CPU usage of this fragment.
         * @param cores Number of cores used.
         */
        //TODO: change to fit my schema
        fun add(timestamp: Long, deadline: Long, usage: Double, cores: Int) {
            val duration = max(0, deadline - timestamp)
            totalLoad += (usage * duration) / 1000.0 // avg MHz * duration = MFLOPs

            if (timestamp != previousDeadline) {
                // There is a gap between the previous and current fragment; fill the gap
                builder.add(timestamp, 0.0, cores)
            }

            builder.add(deadline, usage, cores)
            previousDeadline = deadline
        }

        /**
         * Build the trace.
         */
        fun build(): SimTrace = builder.build()
    }

    /**
     * Obtain the topology factory for the test.
     */
    private fun createTopology(name: String = "topology"): List<HostSpec> {
        val stream = checkNotNull(object {}.javaClass.getResourceAsStream("/env/$name.txt"))
        return stream.use { clusterTopology(stream) }
    }

    class TestComputeMonitor : ComputeMonitor {
        var attemptsSuccess = 0
        var attemptsFailure = 0
        var attemptsError = 0
        var serversPending = 0
        var serversActive = 0

        override fun record(reader: ServiceTableReader) {
            attemptsSuccess = reader.attemptsSuccess
            attemptsFailure = reader.attemptsFailure
            attemptsError = reader.attemptsError
            serversPending = reader.serversPending
            serversActive = reader.serversActive
        }

        var idleTime = 0L
        var activeTime = 0L
        var stealTime = 0L
        var lostTime = 0L
        var energyUsage = 0.0
        var uptime = 0L

        override fun record(reader: HostTableReader) {
            idleTime += reader.cpuIdleTime
            activeTime += reader.cpuActiveTime
            stealTime += reader.cpuStealTime
            lostTime += reader.cpuLostTime
            energyUsage += reader.powerTotal
            uptime += reader.uptime
        }
    }

    /**
     * The [CsvSchema] that is used to parse the trace file.
     */
    private val fragmentsSchema = CsvSchema.builder()
        .addColumn("id", CsvSchema.ColumnType.NUMBER)
        .addColumn("timestamp", CsvSchema.ColumnType.NUMBER)
        .addColumn("duration", CsvSchema.ColumnType.NUMBER)
        .addColumn("cpu_count", CsvSchema.ColumnType.NUMBER)
        .addColumn("cpu_usage", CsvSchema.ColumnType.NUMBER)
        .setAllowComments(true)
        .setUseHeader(true)
        .build()

    /**
     * The [CsvSchema] that is used to parse the meta file.
     */
    private val metaSchema = CsvSchema.builder()
        .addColumn("id", CsvSchema.ColumnType.NUMBER)
        .addColumn("start_time", CsvSchema.ColumnType.NUMBER)
        .addColumn("stop_time", CsvSchema.ColumnType.NUMBER)
        .addColumn("cpu_count", CsvSchema.ColumnType.NUMBER)
        .addColumn("cpu_capacity", CsvSchema.ColumnType.NUMBER)
        .addColumn("mem_capacity", CsvSchema.ColumnType.NUMBER)
        .setAllowComments(true)
        .setUseHeader(true)
        .build()

}
