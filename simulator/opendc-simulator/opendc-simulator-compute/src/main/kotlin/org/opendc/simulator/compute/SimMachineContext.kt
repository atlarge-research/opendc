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

import org.opendc.simulator.compute.model.MemoryUnit
import java.time.Clock

/**
 * A simulated execution context in which a bootable image runs. This interface represents the
 * firmware interface between the running image (e.g. operating system) and the physical or virtual firmware on
 * which the image runs.
 */
public interface SimMachineContext {
    /**
     * The virtual clock tracking simulation time.
     */
    public val clock: Clock

    /**
     * The metadata associated with the context.
     */
    public val meta: Map<String, Any>

    /**
     * The CPUs available on the machine.
     */
    public val cpus: List<SimProcessingUnit>

    /**
     * The memory available on the machine
     */
    public val memory: List<MemoryUnit>
}
