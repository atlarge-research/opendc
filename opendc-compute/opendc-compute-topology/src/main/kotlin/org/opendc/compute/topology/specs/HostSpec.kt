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

package org.opendc.compute.topology.specs

import org.opendc.simulator.compute.cpu.CpuPowerModel
import org.opendc.simulator.compute.gpu.GpuPowerModel
import org.opendc.simulator.compute.models.MachineModel

/**
 * Description of a physical host that will be simulated by OpenDC and host the virtual machines.
 *
 * @param name The name of the host.
 * @param model The physical model of the machine.
 * @param cpuPowerModel The [cpuPowerModel] that determines the power draw based on cpu utilization
 */
public data class HostSpec(
    val name: String,
    val clusterName: String,
    val model: MachineModel,
    val cpuPowerModel: CpuPowerModel,
    val gpuPowerModel: GpuPowerModel?,
    val embodiedCarbon: Double = 1000.0,
    val expectedLifetime: Double = 5.0,
)
