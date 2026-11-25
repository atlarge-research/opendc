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

package org.opendc.cli.run

import org.opendc.cli.readExperiment
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Running on a server: submitting, following progress, unpacking results, and every way that ends short. */
class RemoteBackendTest {
    private val out: Path = createTempDirectory("opendc-cli-remote")

    @AfterTest
    fun clean() {
        out.toFile().deleteRecursively()
    }

    @Test
    fun `runs on the server and unpacks the results where a local run would write them`() {
        FakeOpendcServer().use { server ->
            val session = backend(server, Credentials.Bearer("odc_pat_secret")).prepare(request())
            assertEquals(Parallelism.Server, session.overview.parallelism)
            assertEquals(FakeOpendcServer.TOTAL_TASKS.toLong(), session.overview.totalTasks)

            val outcome = session.run()

            assertEquals(1, outcome.outputs.runCount)
            assertTrue(out.resolve("tiny/raw-output/0/seed=0/host.parquet").exists())
            assertEquals(FakeOpendcServer.TOTAL_TASKS.toLong(), session.progress.snapshot.completedTasks)
            assertTrue(server.authorizations.all { it == "Bearer odc_pat_secret" }, server.authorizations.toString())
        }
    }

    @Test
    fun `gathers runs in one project, created only the first time`() {
        FakeOpendcServer(runningPolls = 0).use { server ->
            backend(server).prepare(request()).run()
            backend(server).prepare(request()).run()

            assertEquals(listOf("opendc-cli"), server.projects)
        }
    }

    @Test
    fun `surfaces the server's refusal with its issues, and leaves no draft behind`() {
        FakeOpendcServer(submit = SubmitAnswer.Refuse("workloads[0]", "no trace called nightly")).use { server ->
            val failure = assertFailsWith<ApiFailure> { backend(server).prepare(request()) }

            assertEquals(400, failure.status)
            assertEquals(listOf("workloads[0]: no trace called nightly"), failure.issues)
            assertEquals(listOf("x-1"), server.deleted)
        }
    }

    @Test
    fun `reports why each scenario of a failed run failed`() {
        FakeOpendcServer(finalState = "failed").use { server ->
            val failure = assertFailsWith<RemoteRunFailed> { backend(server).prepare(request()).run() }

            assertEquals(RemoteState.FAILED, failure.state)
            assertEquals(listOf("scenario 0: simulationError (threw)"), failure.failures)
            assertEquals(0, failure.outputs.runCount)
        }
    }

    @Test
    fun `keeps what landed of a partial run and still reports it as a failure`() {
        FakeOpendcServer(finalState = "partial").use { server ->
            val failure = assertFailsWith<RemoteRunFailed> { backend(server).prepare(request()).run() }

            assertEquals(1, failure.outputs.runCount)
            assertTrue(out.resolve("tiny/raw-output/0/seed=0/host.parquet").exists())
        }
    }

    @Test
    fun `rides out a server that stops answering for a moment`() {
        FakeOpendcServer(runningPolls = 1, unavailablePolls = 3).use { server ->
            val outcome = backend(server).prepare(request()).run()

            assertEquals(1, outcome.outputs.runCount)
            assertTrue(server.cancelled.isEmpty())
        }
    }

    @Test
    fun `cancels the run when the server stays unreachable, rather than leave it running unwatched`() {
        FakeOpendcServer(unavailablePolls = Int.MAX_VALUE).use { server ->
            val impatient =
                RemoteBackend(
                    OpendcApi(server.url, Credentials.Anonymous),
                    ProjectChoice.Named("opendc-cli"),
                    Duration.ofMillis(10),
                    Duration.ZERO,
                )

            val failure = assertFailsWith<ApiFailure> { impatient.prepare(request()).run() }

            assertEquals(503, failure.status)
            assertEquals(listOf("x-1"), server.cancelled)
        }
    }

    @Test
    fun `refuses an archive entry that would land outside the output directory`() {
        val zip = ByteArrayOutputStream()
        ZipOutputStream(zip).use {
            it.putNextEntry(ZipEntry("../escaped.txt"))
            it.write(1)
            it.closeEntry()
        }

        assertFailsWith<IllegalArgumentException> { unpack(zip.toByteArray().inputStream(), out) }
        assertFalse(Files.exists(out.parent.resolve("escaped.txt")))
    }

    private fun backend(
        server: FakeOpendcServer,
        credentials: Credentials = Credentials.Anonymous,
    ) = RemoteBackend(OpendcApi(server.url, credentials), ProjectChoice.Named("opendc-cli"), Duration.ofMillis(10))

    private fun request(): RunRequest {
        val file = File(checkNotNull(javaClass.classLoader.getResource("experiments/tiny-experiment.json")).toURI())
        return RunRequest(readExperiment(file, file.parentFile), file.parentFile.toPath(), out, 1, writerThreads = 1, wantSummary = false)
    }
}
