/*
 * MIT License
 *
 * Copyright (c) 2017 atlarge-research
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

package com.atlarge.opendc.model.odc.platform.scheduler

import com.atlarge.opendc.model.odc.platform.workload.Task
import com.atlarge.opendc.model.odc.topology.machine.Machine
import com.atlarge.opendc.simulator.Context
import com.atlarge.opendc.simulator.Entity

/**
 * A task scheduler that is coupled to an [Entity] in the topology of the cloud network.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface Scheduler {
    /**
     * The name of this scheduler.
     */
    val name: String

    /**
     * (Re)schedule the tasks submitted to the scheduler over the specified set of machines.
     *
     * This method should be invoked at some interval to allow the scheduler to reschedule existing tasks and schedule
     * new tasks.
     */
    suspend fun <S, M> Context<S, M>.schedule()

    /**
     * Submit a [Task] to this scheduler.
     *
     * @param task The task to submit to the scheduler.
     */
    fun submit(task: Task)

    /**
     * Register a [Machine] to this scheduler.
     *
     * @param machine The machine to register.
     */
    fun register(machine: Machine)

    /**
     * Deregister a [Machine] from this scheduler.
     *
     * @param machine The machine to deregister.
     */
    fun deregister(machine: Machine)
}
