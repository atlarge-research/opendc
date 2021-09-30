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

package org.opendc.simulator.flow.mux

import org.opendc.simulator.flow.*
import org.opendc.simulator.flow.interference.InterferenceKey
import java.util.ArrayDeque

/**
 * A [FlowMultiplexer] implementation that allocates inputs to the outputs of the multiplexer exclusively. This means
 * that a single input is directly connected to an output and that the multiplexer can only support as many
 * inputs as outputs.
 *
 * @param engine The [FlowEngine] driving the simulation.
 */
public class ForwardingFlowMultiplexer(private val engine: FlowEngine) : FlowMultiplexer {
    override val inputs: Set<FlowConsumer>
        get() = _inputs
    private val _inputs = mutableSetOf<Input>()

    override val outputs: Set<FlowConsumer>
        get() = _outputs
    private val _outputs = mutableSetOf<FlowConsumer>()
    private val _availableOutputs = ArrayDeque<FlowForwarder>()

    override val counters: FlowCounters = object : FlowCounters {
        override val demand: Double
            get() = _outputs.sumOf { it.counters.demand }
        override val actual: Double
            get() = _outputs.sumOf { it.counters.actual }
        override val overcommit: Double
            get() = _outputs.sumOf { it.counters.overcommit }
        override val interference: Double
            get() = _outputs.sumOf { it.counters.interference }

        override fun reset() {
            for (input in _outputs) {
                input.counters.reset()
            }
        }

        override fun toString(): String = "FlowCounters[demand=$demand,actual=$actual,overcommit=$overcommit]"
    }

    override fun newInput(key: InterferenceKey?): FlowConsumer {
        val forwarder = checkNotNull(_availableOutputs.poll()) { "No capacity to serve request" }
        val output = Input(forwarder)
        _inputs += output
        return output
    }

    override fun removeInput(input: FlowConsumer) {
        if (!_inputs.remove(input)) {
            return
        }

        (input as Input).close()
    }

    override fun addOutput(output: FlowConsumer) {
        if (output in outputs) {
            return
        }

        val forwarder = FlowForwarder(engine)

        _outputs += output
        _availableOutputs += forwarder

        output.startConsumer(object : FlowSource by forwarder {
            override fun onStop(conn: FlowConnection, now: Long, delta: Long) {
                _outputs -= output

                forwarder.onStop(conn, now, delta)
            }
        })
    }

    override fun clear() {
        for (input in _outputs) {
            input.cancel()
        }
        _outputs.clear()

        // Inputs are implicitly cancelled by the output forwarders
        _inputs.clear()
    }

    /**
     * An input on the multiplexer.
     */
    private inner class Input(private val forwarder: FlowForwarder) : FlowConsumer by forwarder {
        /**
         * Close the input.
         */
        fun close() {
            // We explicitly do not close the forwarder here in order to re-use it across input resources.
            _inputs -= this
            _availableOutputs += forwarder
        }

        override fun toString(): String = "ForwardingFlowMultiplexer.Input"
    }
}
