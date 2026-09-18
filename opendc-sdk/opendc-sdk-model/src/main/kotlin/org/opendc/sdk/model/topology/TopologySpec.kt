/*
 * Copyright (c) 2025 AtLarge Research
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

package org.opendc.sdk.model.topology

import kotlinx.serialization.Serializable
import mu.KotlinLogging
import org.opendc.sdk.model.validation.Validatable
import org.opendc.sdk.model.validation.ValidationIssue
import org.opendc.sdk.model.validation.validateEach

/**
 * The datacenter a scenario runs on, described as a list of clusters.
 *
 * @property datacenters Datacenters to simulate.
 */
@Serializable
public data class TopologySpec(
    public var datacenters: List<DataCenterSpec>? = null,
    public val clusters: List<ClusterSpec>? = null,
) : Validatable {
    init {
        // If user is using the old topology file structure with only clusters, move the powerSourceSpec and BatterySpec
        // into a DataCenter.
        if (datacenters == null && clusters != null) {
            logger.warn(
                "You seem to use the old topology file structure using clusters instead of datacenters.\n" +
                    "OpenDC will try to build a proper datacenter, but not all clusters can be converted fully.",
            )

            val powerSourceSpec = clusters[0].powerSource ?: PowerSourceSpec()
            val batterySpec = clusters[0].battery

            datacenters = listOf(DataCenterSpec(clusters = clusters, powerSource = powerSourceSpec, battery = batterySpec))
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }

    override fun validate(): List<ValidationIssue> =
        buildList {
            if (datacenters == null) {
                add(ValidationIssue("topology", "The topology file does not contain any datacenter or cluster specifications."))
                return@buildList
            }
            if (datacenters!!.isEmpty()) {
                add(ValidationIssue("datacenters", "must not be empty"))
            } else {
                addAll(datacenters!!.validateEach("datacenters"))
            }
        }
}
