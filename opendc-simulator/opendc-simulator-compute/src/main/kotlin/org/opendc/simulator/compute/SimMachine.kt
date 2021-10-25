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

import org.opendc.simulator.compute.device.SimPeripheral
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.workload.SimWorkload

/**
 * A generic machine that is able to run a [SimWorkload].
 */
public interface SimMachine {
    /**
     * The model of the machine containing its specifications.
     */
    public val model: MachineModel

    /**
     * The peripherals attached to the machine.
     */
    public val peripherals: List<SimPeripheral>

    /**
     * Start the specified [SimWorkload] on this machine.
     *
     * @param workload The workload to start on the machine.
     * @param meta The metadata to pass to the workload.
     * @return A [SimMachineContext] that represents the execution context for the workload.
     * @throws IllegalStateException if a workload is already active on the machine or if the machine is closed.
     */
    public fun startWorkload(workload: SimWorkload, meta: Map<String, Any> = emptyMap()): SimMachineContext

    /**
     * Cancel the workload that is currently running on this machine.
     *
     * If no workload is active, this operation is a no-op.
     */
    public fun cancel()
}
