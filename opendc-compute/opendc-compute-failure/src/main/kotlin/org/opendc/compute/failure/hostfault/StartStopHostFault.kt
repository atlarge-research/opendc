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

package org.opendc.compute.failure.hostfault

import kotlinx.coroutines.delay
import org.opendc.compute.simulator.host.SimHost
import org.opendc.compute.simulator.service.ComputeService

/**
 * A type of [HostFault] where the hosts are stopped and recover after a given amount of time.
 */
public class StartStopHostFault(
    private val service: ComputeService,
) : HostFault(service) {
    override suspend fun apply(
        victims: List<SimHost>,
        faultDuration: Long,
    ) {
        val client: ComputeService.ComputeClient = service.newClient()

        for (host in victims) {
            val guests = host.getGuests()

            val snapshots = guests.map { it.virtualMachine!!.getActiveWorkload().getSnapshot() }
            val tasks = guests.map { it.task }
            host.fail()

            for ((task, snapshot) in tasks.zip(snapshots)) {
                client.rescheduleTask(task, snapshot)
            }
        }

        delay(faultDuration)

        for (host in victims) {
            host.recover()
        }
    }

    override fun toString(): String = "StartStopHostFault"
}
