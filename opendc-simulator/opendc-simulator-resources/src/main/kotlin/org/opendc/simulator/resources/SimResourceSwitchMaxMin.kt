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

/**
 * A [SimResourceSwitch] implementation that switches resource consumptions over the available resources using max-min
 * fair sharing.
 */
public class SimResourceSwitchMaxMin(
    interpreter: SimResourceInterpreter,
    parent: SimResourceSystem? = null
) : SimResourceSwitch {
    /**
     * The output resource providers to which resource consumers can be attached.
     */
    override val outputs: Set<SimResourceProvider>
        get() = distributor.outputs

    /**
     * The input resources that will be switched between the output providers.
     */
    override val inputs: Set<SimResourceProvider>
        get() = aggregator.inputs

    /**
     * The resource counters to track the execution metrics of all switch resources.
     */
    override val counters: SimResourceCounters
        get() = aggregator.counters

    /**
     * A flag to indicate that the switch was closed.
     */
    private var isClosed = false

    /**
     * The aggregator to aggregate the resources.
     */
    private val aggregator = SimResourceAggregatorMaxMin(interpreter, parent)

    /**
     * The distributor to distribute the aggregated resources.
     */
    private val distributor = SimResourceDistributorMaxMin(interpreter, parent)

    init {
        aggregator.startConsumer(distributor)
    }

    /**
     * Add an output to the switch.
     */
    override fun newOutput(): SimResourceProvider {
        check(!isClosed) { "Switch has been closed" }

        return distributor.newOutput()
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
            aggregator.close()
        }
    }
}
