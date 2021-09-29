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

/**
 * A [SimResourceSwitch] enables switching of capacity of multiple resources between multiple consumers.
 */
public interface SimResourceSwitch {
    /**
     * The output resource providers to which resource consumers can be attached.
     */
    public val outputs: Set<SimResourceProvider>

    /**
     * The input resources that will be switched between the output providers.
     */
    public val inputs: Set<SimResourceProvider>

    /**
     * The resource counters to track the execution metrics of all switch resources.
     */
    public val counters: SimResourceCounters

    /**
     * Create a new output on the switch.
     *
     * @param key The key of the interference member to which the output belongs.
     */
    public fun newOutput(key: InterferenceKey? = null): SimResourceProvider

    /**
     * Remove [output] from this switch.
     */
    public fun removeOutput(output: SimResourceProvider)

    /**
     * Add the specified [input] to the switch.
     */
    public fun addInput(input: SimResourceProvider)

    /**
     * Clear all inputs and outputs from the switch.
     */
    public fun clear()
}
