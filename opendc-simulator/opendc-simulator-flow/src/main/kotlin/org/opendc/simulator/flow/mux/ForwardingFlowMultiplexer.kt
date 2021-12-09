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
import kotlin.math.max

/**
 * A [FlowMultiplexer] implementation that allocates inputs to the outputs of the multiplexer exclusively. This means
 * that a single input is directly connected to an output and that the multiplexer can only support as many
 * inputs as outputs.
 *
 * @param engine The [FlowEngine] driving the simulation.
 * @param listener The convergence listener of the multiplexer.
 */
public class ForwardingFlowMultiplexer(
    private val engine: FlowEngine,
    private val listener: FlowConvergenceListener? = null
) : FlowMultiplexer, FlowConvergenceListener {
    override val inputs: Set<FlowConsumer>
        get() = _inputs
    private val _inputs = mutableSetOf<Input>()

    override val outputs: Set<FlowSource>
        get() = _outputs
    private val _outputs = mutableSetOf<Output>()
    private val _availableOutputs = ArrayDeque<Output>()

    override val counters: FlowCounters = object : FlowCounters {
        override val demand: Double
            get() = _outputs.sumOf { it.forwarder.counters.demand }
        override val actual: Double
            get() = _outputs.sumOf { it.forwarder.counters.actual }
        override val remaining: Double
            get() = _outputs.sumOf { it.forwarder.counters.remaining }
        override val interference: Double
            get() = _outputs.sumOf { it.forwarder.counters.interference }

        override fun reset() {
            for (output in _outputs) {
                output.forwarder.counters.reset()
            }
        }

        override fun toString(): String = "FlowCounters[demand=$demand,actual=$actual,remaining=$remaining]"
    }

    override val rate: Double
        get() = _outputs.sumOf { it.forwarder.rate }

    override val demand: Double
        get() = _outputs.sumOf { it.forwarder.demand }

    override val capacity: Double
        get() = _outputs.sumOf { it.forwarder.capacity }

    override fun newInput(key: InterferenceKey?): FlowConsumer {
        val output = checkNotNull(_availableOutputs.poll()) { "No capacity to serve request" }
        val input = Input(output)
        _inputs += input
        return input
    }

    override fun newInput(capacity: Double, key: InterferenceKey?): FlowConsumer = newInput(key)

    override fun removeInput(input: FlowConsumer) {
        if (!_inputs.remove(input)) {
            return
        }

        val output = (input as Input).output
        output.forwarder.cancel()
        _availableOutputs += output
    }

    override fun newOutput(): FlowSource {
        val forwarder = FlowForwarder(engine, this)
        val output = Output(forwarder)

        _outputs += output
        return output
    }

    override fun removeOutput(output: FlowSource) {
        if (!_outputs.remove(output)) {
            return
        }

        val forwarder = (output as Output).forwarder
        forwarder.close()
    }

    override fun clearInputs() {
        for (input in _inputs) {
            val output = input.output
            output.forwarder.cancel()
            _availableOutputs += output
        }

        _inputs.clear()
    }

    override fun clearOutputs() {
        for (output in _outputs) {
            output.forwarder.cancel()
        }
        _outputs.clear()
        _availableOutputs.clear()
    }

    override fun clear() {
        clearOutputs()
        clearInputs()
    }

    private var _lastConverge = Long.MAX_VALUE

    override fun onConverge(now: Long, delta: Long) {
        val listener = listener
        if (listener != null) {
            val lastConverge = _lastConverge
            _lastConverge = now
            val duration = max(0, now - lastConverge)
            listener.onConverge(now, duration)
        }
    }

    /**
     * An input on the multiplexer.
     */
    private inner class Input(@JvmField val output: Output) : FlowConsumer by output.forwarder {
        override fun toString(): String = "ForwardingFlowMultiplexer.Input"
    }

    /**
     * An output on the multiplexer.
     */
    private inner class Output(@JvmField val forwarder: FlowForwarder) : FlowSource by forwarder {
        override fun onStart(conn: FlowConnection, now: Long) {
            _availableOutputs += this
            forwarder.onStart(conn, now)
        }

        override fun onStop(conn: FlowConnection, now: Long, delta: Long) {
            forwarder.cancel()
            forwarder.onStop(conn, now, delta)
        }

        override fun toString(): String = "ForwardingFlowMultiplexer.Output"
    }
}
