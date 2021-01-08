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

package org.opendc.simulator.compute.workload

/**
 * A command that is sent to the host machine.
 */
public sealed class SimResourceCommand {
    /**
     * A request to the host to process the specified amount of [work] on a vCPU before the specified [deadline].
     *
     * @param work The amount of work to process on the CPU.
     * @param limit The maximum amount of work to be processed per second.
     * @param deadline The instant at which the work needs to be fulfilled.
     */
    public data class Consume(val work: Double, val limit: Double, val deadline: Long = Long.MAX_VALUE) : SimResourceCommand() {
        init {
            require(work > 0) { "The amount of work must be positive." }
            require(limit > 0) { "Limit must be positive." }
        }
    }

    /**
     * An indication to the host that the vCPU will idle until the specified [deadline] or is interrupted.
     */
    public data class Idle(val deadline: Long = Long.MAX_VALUE) : SimResourceCommand()

    /**
     * An indication to the host that the vCPU has finished processing.
     */
    public object Exit : SimResourceCommand()
}
