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

package com.atlarge.opendc.compute.virt.service

import java.util.UUID

/**
 * An internal event emitted by the [VirtProvisioningService] for the allocation policies to keep track of the
 * available hypervisors.
 */
public sealed class VirtProvisioningServiceEvent {
    /**
     * This event is emitted when a hypervisor is added to the pool of schedulable hypervisors.
     */
    public data class HypervisorAdded(val uid: UUID) : VirtProvisioningServiceEvent()

    /**
     * This event is emitted when a hypervisor is removed from the pool of schedulable hypervisors.
     */
    public data class HypervisorRemoved(val uid: UUID) : VirtProvisioningServiceEvent()
}
