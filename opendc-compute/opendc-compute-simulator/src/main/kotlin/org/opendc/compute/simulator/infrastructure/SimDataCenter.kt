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

package org.opendc.compute.simulator.datacenter

import org.opendc.compute.simulator.cluster.SimCluster
import org.opendc.compute.simulator.infrastructure.SimHost
import org.opendc.compute.simulator.telemetry.DataCenterSystemStats
import org.opendc.simulator.compute.carbon.CarbonModel
import org.opendc.simulator.compute.power.SimPowerSource
import java.time.InstantSource

/**
 * A [SimDataCenter] implementation that simulates virtual machines on a physical machine.
 *
 * @param name The name of the host.
 * @param clock The (virtual) clock used to track time.
 * @constructor Create empty Sim host
 */
public class SimDataCenter(
    private val name: String,
    private val clock: InstantSource,
    private val powerSource: SimPowerSource,
    private val carbonModel: CarbonModel?,
) : AutoCloseable {
    private var lastReport = clock.millis()

    private val clusters: ArrayList<SimCluster> = arrayListOf()
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

    public fun addCluster(cluster: SimCluster) {
        this.clusters.add(cluster)
    }

    public fun removeCluster(cluster: SimCluster) {
        this.clusters.remove(cluster)
    }

    public fun addHost(host: SimHost) {
        this.hosts.add(host)
    }

    public fun removeHost(host: SimHost) {
        this.hosts.remove(host)
    }

    public fun getSystemStats(): DataCenterSystemStats {
        val now = clock.millis()
        this.lastReport = now
        this.powerSource.updateCounters(now)

        // TODO: Add support for embodied Carbon
        return DataCenterSystemStats(
            this.powerSource.powerDraw,
            this.powerSource.energyUsage,
            this.powerSource.carbonIntensity,
            this.powerSource.carbonEmission,
            0.0,
        )
    }

    override fun hashCode(): Int = name.hashCode()

    override fun equals(other: Any?): Boolean {
        return other is SimDataCenter && name == other.name
    }

    override fun toString(): String = "SimHost[uid=$name,name=$name]"

    override fun close() {
        TODO("Not yet implemented")
    }
}
