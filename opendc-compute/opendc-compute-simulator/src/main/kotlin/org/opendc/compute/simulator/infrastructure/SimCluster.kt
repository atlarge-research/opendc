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

package org.opendc.compute.simulator.cluster

import org.opendc.compute.simulator.infrastructure.SimHost
import org.opendc.simulator.engine.graph.FlowDistributor
import java.time.InstantSource

/**
 * A [SimCluster] implementation that simulates virtual machines on a physical machine.
 *
 * @param name The name of the host.
 * @param clock The (virtual) clock used to track time.
 * @constructor Create empty Sim host
 */
public class SimCluster(
    private val name: String,
    private val dataCenterName: String,
    private val clock: InstantSource,
    private val powerDistributor: FlowDistributor,
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

    // TODO: Build
//    public fun getSystemStats(): HostSystemStats {
//        val now = clock.millis()
//        val duration = now - lastReport
//
//        return HostSystemStats()
//    }

    override fun hashCode(): Int = name.hashCode()

    override fun equals(other: Any?): Boolean {
        return other is SimCluster && name == other.name
    }

    override fun toString(): String = "SimHost[uid=$name,name=$name]"

    override fun close() {
        TODO("Not yet implemented")
    }
}
