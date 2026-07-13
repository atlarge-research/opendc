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

import org.opendc.common.units.DataSize
import org.opendc.common.units.Frequency
import org.opendc.common.units.Power
import org.opendc.sdk.model.experiment.Experiment
import org.opendc.sdk.model.topology.PowerModelType
import org.opendc.sdk.model.workload.InlineWorkload
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * An SDK experiment may compose itself from other files: any object in the document can name an
 * `importFrom` file to take the rest of its fields from. The `experiments/imports` fixture is the
 * worked example — an experiment whose topology and workload each live in their own file, and whose
 * host is imported in turn by the topology.
 */
class ExperimentImportsTest {
    /** The headline: a topology written in its own file, pulled into an experiment. */
    @Test
    fun `an experiment imports its topology from another file`() {
        val experiment = load("experiments/imports/experiment.json")

        val cluster = experiment.topologies.single().clusters.single()
        assertEquals("cluster", cluster.name)
        assertEquals(Power.ofWatts(10000), cluster.powerSource.maxPower)

        val host = cluster.hosts.single()
        assertEquals(8, host.cpu.coreCount)
        assertEquals(Frequency.ofGHz(3.0), host.cpu.coreSpeed)
        assertEquals(DataSize.ofGiB(16), host.memory.size)
        assertEquals(PowerModelType.LINEAR, host.cpuPowerModel.type)

        assertEquals(emptyList(), experiment.validate())
    }

    /** Any object may be imported, not just a topology — here the workload is a file of its own. */
    @Test
    fun `an experiment imports its workload from another file`() {
        val workload = load("experiments/imports/experiment.json").workloads.single()

        assertEquals(listOf("task-0", "task-1"), (workload as InlineWorkload).tasks.map { it.name })
    }

    /**
     * The import supplies the fields the object does not state itself. `big-host.json` says
     * `"count": 1`, but the topology that imports it says `"count": 4`, and the local key wins.
     */
    @Test
    fun `a key written next to the import overrides the imported one`() {
        val host = load("experiments/imports/experiment.json").topologies.single().clusters.single().hosts.single()

        assertEquals(4, host.count, "the topology's own count overrides the one in the imported host")
        assertEquals("big-host", host.name, "everything it does not override still comes from the import")
    }

    /**
     * Imports nest, and each file's paths are read against its own directory: the experiment names
     * `topology.json`, which in turn names `hosts/big-host.json` — relative to itself, not to the
     * experiment.
     */
    @Test
    fun `imports nest and resolve against the file that declares them`() {
        // Loading at all proves it: the host lives a directory deeper than the file that imports it.
        val host = load("experiments/imports/experiment.json").topologies.single().clusters.single().hosts.single()

        assertEquals(8, host.cpu.coreCount)
    }

    @Test
    fun `an experiment that imports nothing is read unchanged`() {
        val experiment = load("experiments/tiny-experiment.json")

        assertEquals("tiny", experiment.name)
        assertEquals(emptyList(), experiment.validate())
    }

    @Test
    fun `a missing import is reported against the path that named it`() {
        val root = tempDir()
        File(root, "experiment.json").writeText("""{ "importFrom": "nowhere.json" }""")

        val error = assertFailsWith<ImportException> { readExperiment(File(root, "experiment.json")) }
        assertContains(error.message.orEmpty(), "nowhere.json")
    }

    @Test
    fun `an import cycle is reported instead of hanging`() {
        val root = tempDir()
        File(root, "a.json").writeText("""{ "importFrom": "b.json" }""")
        File(root, "b.json").writeText("""{ "importFrom": "a.json" }""")

        val error = assertFailsWith<ImportException> { readExperiment(File(root, "a.json")) }
        assertContains(error.message.orEmpty(), "imports itself")
    }

    private fun load(resource: String): Experiment = readExperiment(resourceFile(resource))

    private fun resourceFile(name: String): File =
        File(checkNotNull(javaClass.classLoader.getResource(name)) { "missing fixture $name" }.toURI())

    private fun tempDir(): File = createTempDirectory("opendc-imports").toFile().apply { deleteOnExit() }
}
