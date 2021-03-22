/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.simulator.resources

import kotlinx.coroutines.*
import java.time.Clock

/**
 * A [SimResourceSwitch] implementation that switches resource consumptions over the available resources using max-min
 * fair sharing.
 */
public class SimResourceSwitchMaxMin(
    clock: Clock,
    private val listener: Listener? = null
) : SimResourceSwitch {
    private val _outputs = mutableSetOf<SimResourceProvider>()
    override val outputs: Set<SimResourceProvider>
        get() = _outputs

    private val _inputs = mutableSetOf<SimResourceProvider>()
    override val inputs: Set<SimResourceProvider>
        get() = _inputs

    /**
     * A flag to indicate that the switch was closed.
     */
    private var isClosed = false

    /**
     * The aggregator to aggregate the resources.
     */
    private val aggregator = SimResourceAggregatorMaxMin(clock)

    /**
     * The distributor to distribute the aggregated resources.
     */
    private val distributor = SimResourceDistributorMaxMin(
        aggregator.output, clock,
        object : SimResourceDistributorMaxMin.Listener {
            override fun onSliceFinish(
                switch: SimResourceDistributor,
                requestedWork: Long,
                grantedWork: Long,
                overcommittedWork: Long,
                interferedWork: Long,
                cpuUsage: Double,
                cpuDemand: Double
            ) {
                listener?.onSliceFinish(this@SimResourceSwitchMaxMin, requestedWork, grantedWork, overcommittedWork, interferedWork, cpuUsage, cpuDemand)
            }
        }
    )

    /**
     * Add an output to the switch represented by [resource].
     */
    override fun addOutput(capacity: Double): SimResourceProvider {
        check(!isClosed) { "Switch has been closed" }

        val provider = distributor.addOutput(capacity)
        _outputs.add(provider)
        return provider
    }

    /**
     * Add the specified [input] to the switch.
     */
    override fun addInput(input: SimResourceProvider) {
        check(!isClosed) { "Switch has been closed" }

        aggregator.addInput(input)
    }

    override fun close() {
        if (!isClosed) {
            isClosed = true
            distributor.close()
            aggregator.close()
        }
    }

    /**
     * Event listener for hypervisor events.
     */
    public interface Listener {
        /**
         * This method is invoked when a slice is finished.
         */
        public fun onSliceFinish(
            switch: SimResourceSwitchMaxMin,
            requestedWork: Long,
            grantedWork: Long,
            overcommittedWork: Long,
            interferedWork: Long,
            cpuUsage: Double,
            cpuDemand: Double
        )
    }
}
