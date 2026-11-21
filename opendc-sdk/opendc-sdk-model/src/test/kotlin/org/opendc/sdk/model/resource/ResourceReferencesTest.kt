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

package org.opendc.sdk.model.resource

import org.junit.jupiter.api.Test
import org.opendc.sdk.model.carbon.TraceBasedCarbonSpec
import org.opendc.sdk.model.experiment.ExperimentSpec
import org.opendc.sdk.model.experiment.expand
import org.opendc.sdk.model.failure.NoFailureSpec
import org.opendc.sdk.model.failure.TraceBasedFailureSpec
import org.opendc.sdk.model.topology.ClusterSpec
import org.opendc.sdk.model.topology.DataCenterSpec
import org.opendc.sdk.model.topology.PowerSourceSpec
import org.opendc.sdk.model.topology.TopologySpec
import org.opendc.sdk.model.validHost
import org.opendc.sdk.model.validWorkload
import org.opendc.sdk.model.workload.EfficientTraceWorkloadSpec
import org.opendc.sdk.model.workload.TraceWorkloadSpec
import kotlin.test.assertEquals

/**
 * Anything that has to find or rewrite the traces a document names, such as a server checking that
 * they exist or locating them for a launcher, walks them here. A reference the walk misses is one
 * that reaches a simulation unchecked, and a path that is wrong points a user at the wrong field.
 */
class ResourceReferencesTest {
    private val experiment =
        ExperimentSpec(
            topologies = setOf(topology(carbon = null), topology(carbon = NamedReference("grid"))),
            workloads =
                setOf(
                    validWorkload,
                    TraceWorkloadSpec(NamedReference("bitbrains")),
                    EfficientTraceWorkloadSpec(UriReference("file:///traces/marconi")),
                ),
            failureModels = setOf(NoFailureSpec, TraceBasedFailureSpec(NamedReference("outages"))),
            runs = 3,
            name = "walked",
        )

    @Test
    fun `finds every trace an experiment names, at the path validation would report it under`() {
        assertEquals(
            listOf(
                ResourceUse(ResourceRole.WORKLOAD, "workloads[1].source", NamedReference("bitbrains")),
                ResourceUse(ResourceRole.WORKLOAD, "workloads[2].source", UriReference("file:///traces/marconi")),
                ResourceUse(ResourceRole.FAILURE, "failureModels[1].source", NamedReference("outages")),
                ResourceUse(ResourceRole.CARBON, "topologies[1].datacenters[1].powerSource.carbon.source", NamedReference("grid")),
            ),
            experiment.references(),
        )
    }

    @Test
    fun `rewrites every reference and nothing else`() {
        val rewritten = experiment.mapReferences { UriReference("s3://bucket/${it.path}") }

        assertEquals(
            experiment.references().map { "s3://bucket/${it.path}" },
            rewritten.references().map { (it.reference as UriReference).uri },
        )
        val restored = rewritten.mapReferences { use -> experiment.references().single { it.path == use.path }.reference }
        assertEquals(experiment, restored)
    }

    @Test
    fun `finds the traces of one scenario at the paths of a scenario`() {
        val scenario =
            experiment.expand().single {
                it.workload is TraceWorkloadSpec && it.failureModel is TraceBasedFailureSpec && it.topology.carbon() != null &&
                    it.seed == experiment.initialSeed
            }

        assertEquals(
            listOf("workload.source", "failureModel.source", "topology.datacenters[1].powerSource.carbon.source"),
            scenario.references().map { it.path },
        )
    }

    private fun TopologySpec.carbon(): ResourceReference? =
        datacenters.orEmpty().firstNotNullOfOrNull { (it.powerSource.carbon as? TraceBasedCarbonSpec)?.source }

    private fun topology(carbon: ResourceReference?): TopologySpec =
        TopologySpec(
            listOf(
                DataCenterSpec(listOf(ClusterSpec(hosts = listOf(validHost))), name = "quiet"),
                DataCenterSpec(
                    listOf(ClusterSpec(hosts = listOf(validHost))),
                    name = "busy",
                    powerSource = PowerSourceSpec(carbon = carbon?.let(::TraceBasedCarbonSpec)),
                ),
            ),
        )
}
