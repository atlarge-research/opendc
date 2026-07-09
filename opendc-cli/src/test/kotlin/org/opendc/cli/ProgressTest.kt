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

package org.opendc.cli

import org.opendc.cli.progress.ExperimentProgress
import org.opendc.cli.progress.ProgressSink
import org.opendc.sdk.model.experiment.expand
import org.opendc.sdk.model.serialization.SdkJson
import org.opendc.sdk.runner.OpenDC
import org.opendc.sdk.runner.provision.FileSystemResourceProvisioner
import java.io.File
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Verifies the progress plumbing: the stable task denominator is known and every task is counted. */
class ProgressTest {
    @Test
    fun `progress counts every task in the workload`() {
        val file = File(checkNotNull(javaClass.classLoader.getResource("experiments/tiny-experiment.json")).toURI())
        val experiment = file.inputStream().use { SdkJson.decodeExperiment(it) }
        val progress = ExperimentProgress(experiment.expand().sumOf { it.runs })

        OpenDC.builder()
            .provisioner(FileSystemResourceProvisioner(Path.of(".")))
            .sink(ProgressSink(progress))
            .parallelism(1)
            .build()
            .simulate(experiment)

        val snapshot = progress.snapshot
        assertTrue(snapshot.totalTasks > 0, "the workload's task count must be discovered before the run")
        assertEquals(snapshot.totalTasks, snapshot.completedTasks, "every task should be accounted for at the end")
    }
}
