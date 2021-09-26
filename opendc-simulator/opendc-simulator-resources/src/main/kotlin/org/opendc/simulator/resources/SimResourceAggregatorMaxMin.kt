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
 * A [SimResourceAggregator] that distributes the load equally across the input resources.
 */
public class SimResourceAggregatorMaxMin(
    interpreter: SimResourceInterpreter,
    parent: SimResourceSystem? = null
) : SimAbstractResourceAggregator(interpreter, parent) {
    private val consumers = mutableListOf<Input>()

    override fun doConsume(limit: Double, duration: Long) {
        // Sort all consumers by their capacity
        consumers.sortWith(compareBy { it.ctx.capacity })

        // Divide the requests over the available capacity of the input resources fairly
        for (input in consumers) {
            val inputCapacity = input.ctx.capacity
            val fraction = inputCapacity / capacity
            val grantedSpeed = limit * fraction

            val command = if (grantedSpeed > 0.0)
                SimResourceCommand.Consume(grantedSpeed, duration)
            else
                SimResourceCommand.Consume(0.0, duration)
            input.push(command)
        }
    }

    override fun doFinish() {
        val iterator = consumers.iterator()
        for (input in iterator) {
            iterator.remove()
            input.push(SimResourceCommand.Exit)
        }
    }

    override fun onInputStarted(input: Input) {
        consumers.add(input)
    }

    override fun onInputFinished(input: Input) {
        consumers.remove(input)
    }
}
