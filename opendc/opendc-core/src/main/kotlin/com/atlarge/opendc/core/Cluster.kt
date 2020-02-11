/*
 * MIT License
 *
 * Copyright (c) 2019 atlarge-research
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

package com.atlarge.opendc.core

import com.atlarge.odcsim.Behavior
import com.atlarge.odcsim.ProcessContext
import com.atlarge.odcsim.SendRef
import com.atlarge.opendc.core.resources.compute.MachineMessage
import com.atlarge.opendc.core.resources.compute.host.Host
import com.atlarge.opendc.core.resources.compute.supervision.MachineSupervisionEvent
import java.util.UUID

/**
 * A logical grouping of heterogeneous hosts and primary storage within a zone.
 *
 * @property uid The unique identifier of the cluster.
 * @property name The name of this cluster.
 * @property hosts The machines in this cluster.
 */
data class Cluster(override val uid: UUID, override val name: String, val hosts: List<Host>) : Identity {
    /**
     * Build the runtime [Behavior] of this cluster of hosts.
     *
     * @param manager The manager of the cluster.
     */
    suspend operator fun invoke(ctx: ProcessContext, manager: SendRef<MachineSupervisionEvent>) {
        // Launch all hosts in the cluster
        for (host in hosts) {
            val channel = ctx.open<MachineMessage>()
            ctx.spawn({ host(it, manager, channel) }, name = host.name)
        }
    }
}
