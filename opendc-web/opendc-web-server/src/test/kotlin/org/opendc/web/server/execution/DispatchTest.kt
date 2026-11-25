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

package org.opendc.web.server.execution

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opendc.web.server.storage.ObjectStoreKind

/**
 * A deployment that cannot work should refuse to start and say why, rather than start and fail every
 * run it is handed with an error that names neither the setting nor the cause.
 */
class DispatchTest {
    @Test
    fun `refuses to run on a cluster that cannot read this server's disk`() {
        val problems = deploymentProblems(DispatcherKind.KUBERNETES, ObjectStoreKind.LOCAL, "http://opendc.example/api/v1/telemetry")

        assertTrue(problems.single().contains("opendc.storage.kind=s3"), "$problems")
    }

    @Test
    fun `refuses to have pods report to an address only this machine can reach`() {
        for (url in listOf("http://localhost:8080/api/v1/telemetry", "http://127.0.0.1/t", "http://0.0.0.0:8080/t")) {
            val problems = deploymentProblems(DispatcherKind.KUBERNETES, ObjectStoreKind.S3, url)

            assertTrue(problems.single().contains("telemetry-url"), "$url: $problems")
        }
    }

    @Test
    fun `lets a cluster deployment with object storage and a reachable address start`() {
        assertEquals(
            emptyList<String>(),
            deploymentProblems(DispatcherKind.KUBERNETES, ObjectStoreKind.S3, "http://opendc.default.svc:8080/api/v1/telemetry"),
        )
    }

    @Test
    fun `lets a local deployment start on local storage, beside itself`() {
        assertEquals(
            emptyList<String>(),
            deploymentProblems(DispatcherKind.LOCAL, ObjectStoreKind.LOCAL, "http://localhost:8080/api/v1/telemetry"),
        )
    }

    // This server stages a SLURM job's inputs and collects its outputs itself, and the job reports
    // nothing, so neither the disk nor the loopback address is out of a job's reach.
    @Test
    fun `lets a SLURM deployment start on local storage behind a loopback address`() {
        assertEquals(
            emptyList<String>(),
            deploymentProblems(DispatcherKind.SLURM, ObjectStoreKind.LOCAL, "http://localhost:8080/api/v1/telemetry"),
        )
    }
}
