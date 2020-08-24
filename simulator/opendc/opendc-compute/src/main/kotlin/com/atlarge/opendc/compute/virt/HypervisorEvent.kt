/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
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

package com.atlarge.opendc.compute.virt

import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.virt.driver.VirtDriver

/**
 * An event that is emitted by a [VirtDriver].
 */
public sealed class HypervisorEvent {
    /**
     * The driver that emitted the event.
     */
    public abstract val driver: VirtDriver

    /**
     * This event is emitted when the number of active servers on the server managed by this driver is updated.
     *
     * @property driver The driver that emitted the event.
     * @property numberOfActiveServers The number of active servers.
     * @property availableMemory The available memory, in MB.
     */
    public data class VmsUpdated(
        override val driver: VirtDriver,
        public val numberOfActiveServers: Int,
        public val availableMemory: Long
    ) : HypervisorEvent()

    /**
     * This event is emitted when a slice is finished.
     *
     * @property driver The driver that emitted the event.
     * @property requestedBurst The total requested CPU time (can be above capacity).
     * @property grantedBurst The actual total granted capacity, which might be lower than the requested burst due to
     * the hypervisor being interrupted during a slice.
     * @property overcommissionedBurst The CPU time that the hypervisor could not grant to the virtual machine since
     * it did not have the capacity.
     * @property interferedBurst The sum of CPU time that virtual machines could not utilize due to performance
     * interference.
     * @property cpuUsage CPU use in megahertz.
     * @property cpuDemand CPU demand in megahertz.
     * @property numberOfDeployedImages The number of images deployed on this hypervisor.
     */
    public data class SliceFinished(
        override val driver: VirtDriver,
        public val requestedBurst: Long,
        public val grantedBurst: Long,
        public val overcommissionedBurst: Long,
        public val interferedBurst: Long,
        public val cpuUsage: Double,
        public val cpuDemand: Double,
        public val numberOfDeployedImages: Int,
        public val hostServer: Server
    ) : HypervisorEvent()
}
