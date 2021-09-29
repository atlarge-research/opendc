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

import org.opendc.simulator.resources.interference.InterferenceKey
import java.util.ArrayDeque

/**
 * A [SimResourceSwitch] implementation that allocates outputs to the inputs of the switch exclusively. This means that
 * a single output is directly connected to an input and that the switch can only support as many outputs as inputs.
 */
public class SimResourceSwitchExclusive : SimResourceSwitch {
    override val outputs: Set<SimResourceProvider>
        get() = _outputs
    private val _outputs = mutableSetOf<Output>()

    private val _inputs = mutableSetOf<SimResourceProvider>()
    override val inputs: Set<SimResourceProvider>
        get() = _inputs
    private val _availableInputs = ArrayDeque<SimResourceTransformer>()

    override val counters: SimResourceCounters = object : SimResourceCounters {
        override val demand: Double
            get() = _inputs.sumOf { it.counters.demand }
        override val actual: Double
            get() = _inputs.sumOf { it.counters.actual }
        override val overcommit: Double
            get() = _inputs.sumOf { it.counters.overcommit }
        override val interference: Double
            get() = _inputs.sumOf { it.counters.interference }

        override fun reset() {
            for (input in _inputs) {
                input.counters.reset()
            }
        }

        override fun toString(): String = "SimResourceCounters[demand=$demand,actual=$actual,overcommit=$overcommit]"
    }

    /**
     * Add an output to the switch.
     */
    override fun newOutput(key: InterferenceKey?): SimResourceProvider {
        val forwarder = checkNotNull(_availableInputs.poll()) { "No capacity to serve request" }
        val output = Output(forwarder)
        _outputs += output
        return output
    }

    override fun removeOutput(output: SimResourceProvider) {
        if (!_outputs.remove(output)) {
            return
        }

        (output as Output).close()
    }

    /**
     * Add an input to the switch.
     */
    override fun addInput(input: SimResourceProvider) {
        if (input in inputs) {
            return
        }

        val forwarder = SimResourceForwarder()

        _inputs += input
        _availableInputs += forwarder

        input.startConsumer(object : SimResourceConsumer by forwarder {
            override fun onEvent(ctx: SimResourceContext, event: SimResourceEvent) {
                if (event == SimResourceEvent.Exit) {
                    // De-register the input after it has finished
                    _inputs -= input
                }

                forwarder.onEvent(ctx, event)
            }
        })
    }

    override fun clear() {
        for (input in _inputs) {
            input.cancel()
        }
        _inputs.clear()

        // Outputs are implicitly cancelled by the inputs forwarders
        _outputs.clear()
    }

    /**
     * An output of the resource switch.
     */
    private inner class Output(private val forwarder: SimResourceTransformer) : SimResourceProvider by forwarder {
        /**
         * Close the output.
         */
        fun close() {
            // We explicitly do not close the forwarder here in order to re-use it across output resources.
            _outputs -= this
            _availableInputs += forwarder
        }

        override fun toString(): String = "SimResourceSwitchExclusive.Output"
    }
}
