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

package org.opendc.simulator.compute

import java.time.Clock

/**
 * A simulated execution context in which a bootable image runs. This interface represents the
 * firmware interface between the running image (e.g. operating system) and the physical or virtual firmware on
 * which the image runs.
 */
public interface SimExecutionContext {
    /**
     * The virtual clock tracking simulation time.
     */
    public val clock: Clock

    /**
     * The machine model of the machine that is running the image.
     */
    public val machine: SimMachineModel

    /**
     * Ask the host machine to interrupt the specified vCPU.
     *
     * @param cpu The id of the vCPU to interrupt.
     * @throws IllegalArgumentException if the identifier points to a non-existing vCPU.
     */
    public fun interrupt(cpu: Int)
}
