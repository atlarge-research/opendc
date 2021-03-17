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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.ArrayDeque
import kotlin.coroutines.CoroutineContext

/**
 * A [SimResourceSwitch] implementation that allocates outputs to the inputs of the switch exclusively. This means that
 * a single output is directly connected to an input and that the switch can only support as much outputs as inputs.
 */
public class SimResourceSwitchExclusive<R : SimResource>(context: CoroutineContext) : SimResourceSwitch<R> {
    /**
     * The [CoroutineScope] of the service bounded by the lifecycle of the service.
     */
    private val scope = CoroutineScope(context + Job())

    private val _outputs = mutableSetOf<SimResourceProvider<R>>()
    override val outputs: Set<SimResourceProvider<R>>
        get() = _outputs

    private val availableResources = ArrayDeque<SimResourceForwarder<R>>()
    private val _inputs = mutableSetOf<SimResourceProvider<R>>()
    override val inputs: Set<SimResourceProvider<R>>
        get() = _inputs

    override fun addOutput(resource: R): SimResourceProvider<R> {
        check(availableResources.isNotEmpty()) { "No capacity to serve request" }
        val forwarder = availableResources.poll()
        val output = Provider(resource, forwarder)
        _outputs += output
        return output
    }

    override fun addInput(input: SimResourceProvider<R>) {
        if (input in inputs) {
            return
        }

        val forwarder = SimResourceForwarder(input.resource)

        scope.launch { input.consume(forwarder) }

        _inputs += input
        availableResources += forwarder
    }

    override fun close() {
        scope.cancel()
    }

    private inner class Provider(
        override val resource: R,
        private val forwarder: SimResourceForwarder<R>
    ) : SimResourceProvider<R> {

        override suspend fun consume(consumer: SimResourceConsumer<R>) = forwarder.consume(consumer)

        override fun interrupt() {
            forwarder.interrupt()
        }

        override fun close() {
            _outputs -= this
            availableResources += forwarder
        }
    }
}
