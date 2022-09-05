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

package org.opendc.simulator.compute.kernel

import org.opendc.simulator.compute.SimMachine
import org.opendc.simulator.compute.kernel.interference.VmInterferenceMember
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.workload.SimWorkload

/**
 * A SimHypervisor facilitates the execution of multiple concurrent [SimWorkload]s, while acting as a single workload
 * to another [SimMachine].
 */
public interface SimHypervisor : SimWorkload {
    /**
     * The machines running on the hypervisor.
     */
    public val vms: Set<SimMachine>

    /**
     * The resource counters associated with the hypervisor.
     */
    public val counters: SimHypervisorCounters

    /**
     * The CPU usage of the hypervisor in MHz.
     */
    public val cpuUsage: Double

    /**
     * The CPU usage of the hypervisor in MHz.
     */
    public val cpuDemand: Double

    /**
     * The CPU capacity of the hypervisor in MHz.
     */
    public val cpuCapacity: Double

    /**
     * Determine whether the specified machine characterized by [model] can fit on this hypervisor at this moment.
     */
    public fun canFit(model: MachineModel): Boolean

    /**
     * Create a [SimMachine] instance on which users may run a [SimWorkload].
     *
     * @param model The machine to create.
     * @param interferenceKey The key of the machine in the interference model.
     */
    public fun newMachine(model: MachineModel, interferenceKey: VmInterferenceMember? = null): SimVirtualMachine

    /**
     * Remove the specified [machine] from the hypervisor.
     *
     * @param machine The machine to remove.
     */
    public fun removeMachine(machine: SimVirtualMachine)
}
