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

package org.opendc.web.server.results

import io.quarkus.narayana.jta.QuarkusTransaction
import io.quarkus.test.junit.QuarkusTest
import io.restassured.path.json.JsonPath
import jakarta.inject.Inject
import org.apache.parquet.example.data.simple.SimpleGroup
import org.apache.parquet.hadoop.example.ExampleParquetWriter
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.MessageTypeParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.opendc.sdk.model.telemetry.MetaFile
import org.opendc.sdk.model.telemetry.OutputFileSpec
import org.opendc.trace.parquet.LocalOutputFile
import org.opendc.web.dispatcher.ExitReason
import org.opendc.web.launcher.ResultMetric
import org.opendc.web.launcher.TelemetryTarget
import org.opendc.web.launcher.UnitOutcome
import org.opendc.web.server.ApiTest
import org.opendc.web.server.execution.ExecutionLoop
import org.opendc.web.server.execution.RecordingDispatcher
import org.opendc.web.server.execution.ended
import org.opendc.web.server.model.Execution
import org.opendc.web.server.model.Experiment
import org.opendc.web.server.storage.ObjectStore
import org.opendc.web.server.storage.runKey
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

/** A stopped run is charted from its parquet, so the numbers coming out must be the ones in the files. */
@QuarkusTest
class ExperimentResultsTest {
    @Inject
    lateinit var loop: ExecutionLoop

    @Inject
    lateinit var dispatcher: RecordingDispatcher

    @Inject
    lateinit var store: ObjectStore

    @TempDir
    lateinit var scratch: Path

    private lateinit var projectId: String

    @BeforeEach
    fun seedProject() {
        dispatcher.forget()
        projectId =
            ApiTest.requestJson()
                .body("""{"name":"Results ${UUID.randomUUID()}"}""")
                .post("/api/v1/projects")
                .then()
                .statusCode(201)
                .extract()
                .path("id")
    }

    @Test
    fun `a finished run is charted from its parquet rather than from what it posted`() {
        val experiment = running()
        post(experiment, ResultMetric.HOST_CPU_UTILIZATION, 0.9)
        hostTable(experiment, Row(t = 0, host = 0, usage = 100.0, capacity = 1000.0, power = 100.0))

        settle(experiment)

        assertEquals(0.1, valueOf(results(experiment), ResultMetric.HOST_CPU_UTILIZATION))
    }

    @Test
    fun `adds the hosts of an instant for power and weighs them by capacity for utilization`() {
        val experiment = running()
        hostTable(
            experiment,
            Row(t = 0, host = 0, usage = 200.0, capacity = 1000.0, power = 100.0),
            Row(t = 0, host = 1, usage = 400.0, capacity = 1000.0, power = 200.0),
        )

        settle(experiment)

        val charted = results(experiment)
        assertEquals(300.0, valueOf(charted, ResultMetric.HOST_POWER_DRAW))
        assertEquals(0.3, valueOf(charted, ResultMetric.HOST_CPU_UTILIZATION))
    }

    @Test
    fun `reports the same utilization for two fleets that got through the same work`() {
        val fewLarge = running()
        hostTable(
            fewLarge,
            Row(t = 0, host = 0, usage = 100.0, capacity = 900.0, power = 100.0),
            Row(t = 0, host = 1, usage = 8.0, capacity = 100.0, power = 100.0),
        )
        settle(fewLarge)

        val manySmall = running()
        hostTable(
            manySmall,
            Row(t = 0, host = 0, usage = 54.0, capacity = 500.0, power = 100.0),
            Row(t = 0, host = 1, usage = 54.0, capacity = 500.0, power = 100.0),
        )
        settle(manySmall)

        // Both fleets used 108 of 1000. An unweighted mean would call the first one 9.6%.
        assertEquals(0.108, valueOf(results(fewLarge), ResultMetric.HOST_CPU_UTILIZATION), 1e-9)
        assertEquals(0.108, valueOf(results(manySmall), ResultMetric.HOST_CPU_UTILIZATION), 1e-9)
    }

    // Capacity lives in the meta file, so a run whose meta never landed has no share to report, but its
    // power is still in the table and must not be lost with it.
    @Test
    fun `leaves out utilization but keeps power when the meta file has not landed`() {
        val experiment = running()
        hostTable(experiment, Row(t = 0, host = 0, usage = 100.0, capacity = 1000.0, power = 100.0), withMeta = false)

        settle(experiment)

        val metrics = results(experiment).getList<String>("scenarios[0].series.metric")
        assertEquals(listOf(ResultMetric.HOST_POWER_DRAW.id), metrics)
    }

    // The writer records the closing instant twice: on the export interval and when the last task ends.
    @Test
    fun `counts an instant the writer recorded twice only once`() {
        val experiment = running()
        hostTable(
            experiment,
            Row(t = 0, host = 0, usage = 200.0, capacity = 1000.0, power = 100.0),
            Row(t = 0, host = 1, usage = 400.0, capacity = 1000.0, power = 200.0),
            Row(t = 0, host = 0, usage = 200.0, capacity = 1000.0, power = 100.0),
            Row(t = 0, host = 1, usage = 400.0, capacity = 1000.0, power = 200.0),
        )

        settle(experiment)

        assertEquals(300.0, valueOf(results(experiment), ResultMetric.HOST_POWER_DRAW))
    }

    @Test
    fun `reports a finished experiment as complete so a chart stops asking`() {
        val experiment = running()
        hostTable(experiment, Row(t = 0, host = 0, usage = 500.0, capacity = 1000.0, power = 100.0))

        settle(experiment)

        assertEquals(true, results(experiment).getBoolean("complete"))
    }

    @Test
    fun `forgets what a scenario measured once it has been run again`() {
        val experiment = running()
        hostTable(experiment, Row(t = 0, host = 0, usage = 100.0, capacity = 1000.0, power = 100.0))
        settle(experiment)
        assertEquals(0.1, valueOf(results(experiment), ResultMetric.HOST_CPU_UTILIZATION))

        ApiTest.requestJson().post("/api/v1/experiments/$experiment/scenarios/0/retry").then().statusCode(200)
        hostTable(experiment, Row(t = 0, host = 0, usage = 800.0, capacity = 1000.0, power = 100.0))
        loop.drain()
        settle(experiment)

        assertEquals(0.8, valueOf(results(experiment), ResultMetric.HOST_CPU_UTILIZATION))
    }

    // A failure model can make a run twice as long as the same run without one, and both are drawn on
    // one chart, whose rows join the scenarios on their instants.
    @Test
    fun `folds every scenario onto the same instants however long each ran`() {
        val experiment = running(TWO_SCENARIOS)
        hostTable(experiment, *everyExport(count = 40), scenario = 0)
        hostTable(experiment, *everyExport(count = 75), scenario = 1)
        settleAll(experiment)

        val charted =
            ApiTest.requestJson().get("/api/v1/experiments/$experiment/results?buckets=8").then().statusCode(200).extract().jsonPath()
        val bucketMs = charted.getLong("bucketMs")
        val short = instantsOf(charted, scenario = 0)
        val long = instantsOf(charted, scenario = 1)

        assertEquals(short, long.take(short.size), "the shorter run's points sit on the longer run's instants")
        assertTrue(long.size <= 8, "no more points than asked for: $long")
        assertTrue(long.zipWithNext().all { (first, next) -> next - first == bucketMs }, "every point is one bucket apart: $long")
    }

    @Test
    fun `falls back to what a failed run managed to post`() {
        val experiment = running()
        post(experiment, ResultMetric.HOST_CPU_UTILIZATION, 0.62)

        dispatcher.finish(executionOf(experiment), ended(ExitReason.SIMULATION_ERROR, 21, "it failed"))

        assertEquals(0.62, valueOf(results(experiment), ResultMetric.HOST_CPU_UTILIZATION))
    }

    /** An experiment of [spec] whose units the platform is carrying. */
    private fun running(spec: String = SPEC): String {
        val publicId =
            ApiTest.requestJson()
                .body("""{"projectId":"$projectId","name":"Results","spec":$spec}""")
                .post("/api/v1/experiments")
                .then()
                .statusCode(201)
                .extract()
                .path<String>("id")
        ApiTest.requestJson().post("/api/v1/experiments/$publicId/submit").then().statusCode(200)
        loop.drain()
        return publicId
    }

    /** The platform reports the run ended, after its launcher certified it. */
    private fun settle(experiment: String) {
        val execution = executionOf(experiment)
        dispatcher.certify(execution, 0, UnitOutcome.Succeeded(1.0))
        dispatcher.finish(execution, ended(ExitReason.OK))
    }

    /** The platform reports every execution of the experiment ended, after their launchers certified each run. */
    private fun settleAll(experiment: String) {
        for (execution in executionsOf(experiment)) {
            for (unit in dispatcher.manifestOf(execution).units) {
                dispatcher.certify(execution, unit.scenario.id, UnitOutcome.Succeeded(1.0))
            }
            dispatcher.finish(execution, ended(ExitReason.OK))
        }
    }

    private fun executionOf(experiment: String): UUID = executionsOf(experiment).last()

    private fun executionsOf(experiment: String): List<UUID> =
        QuarkusTransaction.requiringNew().call {
            val id = checkNotNull(Experiment.findByPublicId(UUID.fromString(experiment))).id
            Execution.findByExperiment(id).map { it.publicId }
        }

    private fun instantsOf(
        charted: JsonPath,
        scenario: Int,
    ): List<Long> {
        val series = "scenarios.find { it.scenarioIndex == $scenario }.series.find { it.metric == '${ResultMetric.HOST_POWER_DRAW.id}' }"
        return charted.getList<Number>("$series.points.t").map { it.toLong() }
    }

    /** One host's row at each of the first [count] five-minute exports, the interval a spec defaults to. */
    private fun everyExport(count: Int): Array<Row> =
        Array(count) { Row(t = EXPORT_INTERVAL_MS * (it + 1), host = 0, usage = 100.0, capacity = 1000.0, power = 100.0) }

    private fun results(experiment: String): JsonPath =
        ApiTest.requestJson().get("/api/v1/experiments/$experiment/results").then().statusCode(200).extract().jsonPath()

    private fun valueOf(
        charted: JsonPath,
        metric: ResultMetric,
    ): Double = charted.getDouble("scenarios[0].series.find { it.metric == '${metric.id}' }.points[0].value")

    /** Reports one sample as a launcher would, using the token written into its manifest. */
    private fun post(
        experiment: String,
        metric: ResultMetric,
        value: Double,
    ) {
        val target = dispatcher.manifestOf(executionOf(experiment)).telemetry
        check(target is TelemetryTarget.Endpoint)
        ApiTest
            .requestJson()
            .header("Authorization", "Bearer ${target.token}")
            .body(
                """{"runs":[{"scenarioIndex":0,"seed":0,"completedTasks":1,""" +
                    """"series":[{"metric":"${metric.id}","points":[{"t":0,"value":$value}]}]}]}""",
            )
            .post("/api/v1/telemetry")
            .then()
            .statusCode(204)
    }

    /**
     * Writes a host table where the launcher of [scenario]'s run would have put one, and the meta
     * file naming each host's capacity beside it unless [withMeta] is off.
     */
    private fun hostTable(
        experiment: String,
        vararg rows: Row,
        scenario: Int = 0,
        withMeta: Boolean = true,
    ) {
        val run = runKey(UUID.fromString(experiment), scenario, 0)
        // Floats, because the simulator writes its measurements as 32-bit floats.
        write("$run/${OutputFileSpec.HOST.fileName}", HOST_SCHEMA, rows.toList()) { row ->
            add("timestamp", row.t)
            add("host_id", row.host)
            add("cpu_usage", row.usage.toFloat())
            add("power_draw", row.power.toFloat())
        }
        if (withMeta) {
            write("$run/${MetaFile.HOST.path}", HOST_META_SCHEMA, rows.distinctBy { it.host }) { row ->
                add("host_id", row.host)
                add("cpu_capacity", row.capacity.toFloat())
            }
        }
    }

    private fun write(
        key: String,
        schema: MessageType,
        rows: List<Row>,
        fill: SimpleGroup.(Row) -> Unit,
    ) {
        val file = scratch.resolve("${UUID.randomUUID()}.parquet")
        ExampleParquetWriter.builder(LocalOutputFile(file)).withType(schema).build().use { writer ->
            rows.forEach { row -> writer.write(SimpleGroup(schema).apply { fill(row) }) }
        }
        Files.newInputStream(file).use { store.put(key, it) }
    }

    private data class Row(
        val t: Long,
        val host: Int,
        val usage: Double,
        val capacity: Double,
        val power: Double,
    )

    private companion object {
        /** The columns of the host table this reads, written the way the simulator writes them. */
        val HOST_SCHEMA: MessageType =
            MessageTypeParser.parseMessageType(
                """
                message host {
                  required int64 timestamp;
                  required int32 host_id;
                  required float cpu_usage;
                  required float power_draw;
                }
                """.trimIndent(),
            )

        /** The columns of the host meta file this reads. */
        val HOST_META_SCHEMA: MessageType =
            MessageTypeParser.parseMessageType(
                """
                message host {
                  required int32 host_id;
                  required float cpu_capacity;
                }
                """.trimIndent(),
            )

        const val TOPOLOGY =
            """{"datacenters":[{"clusters":[{"name":"C0","hosts":[{"name":"H0","count":1,""" +
                """"cpu":{"coreCount":4,"coreSpeed":"2.5 GHz"},"memory":{"size":"16 GiB"}}]}]}]}"""

        const val WORKLOAD =
            """{"type":"inline","tasks":[{"id":0,"submissionTime":"0 ms","duration":"10 minutes",""" +
                """"cpuCoreCount":1,"cpuCapacity":"1 GHz","memory":"1 GiB",""" +
                """"fragments":[{"duration":"10 minutes","cpuUsage":"1 GHz"}]}]}"""

        const val SPEC = """{"name":"results","topologies":[$TOPOLOGY],"workloads":[$WORKLOAD],"runs":1}"""

        const val TWO_SCENARIOS =
            """{"name":"results","topologies":[$TOPOLOGY],"workloads":[$WORKLOAD],"runs":1,""" +
                """"failureModels":[{"type":"none"},{"type":"prefab","prefabName":"G5k06Exp"}]}"""

        const val EXPORT_INTERVAL_MS = 300_000L
    }
}
