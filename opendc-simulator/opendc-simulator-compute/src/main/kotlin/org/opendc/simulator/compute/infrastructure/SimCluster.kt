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

package org.opendc.simulator.compute.infrastructure

import org.opendc.simulator.compute.cluster.ClusterDistributor
import org.opendc.simulator.compute.telemetry.ClusterSystemStats
import java.time.InstantSource

/**
 * A [SimCluster] implementation that simulates virtual machines on a physical machine.
 *
 * @param id Identifies the cluster: clusters are equal when their ids are, so ids must be unique within a simulation.
 * @param name The name of the cluster.
 * @param clock The (virtual) clock used to track time.
 * @constructor Create empty Sim host
 */
public class SimCluster(
    public val id: Int,
    private val name: String,
    private val dataCenterName: String,
    private val clock: InstantSource,
    private val powerDistributor: ClusterDistributor,
) : AutoCloseable {
    private var lastReport = clock.millis()

    private val hosts: ArrayList<SimHost> = arrayListOf()

    init {
        launch()
    }

    /**
     * Launch the hypervisor.
     */
    private fun launch() {
    }

    public fun getName(): String {
        return name
    }

    public fun getDataCenterName(): String {
        return dataCenterName
    }

    public fun addHost(host: SimHost) {
        hosts.add(host)
    }

    public fun removeHost(host: SimHost) {
        hosts.remove(host)
    }

    public fun getSystemStats(): ClusterSystemStats {
        val now = clock.millis()
        this.lastReport = now
        this.powerDistributor.updateCounters(now)

        // TODO: Add support for embodied Carbon
        return ClusterSystemStats(
            this.powerDistributor.powerDraw,
            this.powerDistributor.energyUsage,
            this.powerDistributor.carbonIntensity,
            this.powerDistributor.carbonEmission,
            0.0,
        )
    }

    override fun hashCode(): Int = id

    override fun equals(other: Any?): Boolean = other is SimCluster && id == other.id

    override fun toString(): String = "SimCluster[id=$id,name=$name]"

    override fun close() {
        TODO("Not yet implemented")
    }
}
