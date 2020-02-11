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

package com.atlarge.opendc.core.resources.compute.scheduling

import com.atlarge.odcsim.ProcessContext
import com.atlarge.odcsim.SendRef
import com.atlarge.odcsim.sendOnce
import com.atlarge.opendc.core.resources.compute.Machine
import com.atlarge.opendc.core.resources.compute.MachineEvent
import com.atlarge.opendc.core.resources.compute.MachineRef
import com.atlarge.opendc.core.resources.compute.ProcessingElement
import com.atlarge.opendc.core.workload.application.Application
import com.atlarge.opendc.core.workload.application.Pid
import com.atlarge.opendc.core.workload.application.ProcessEvent
import com.atlarge.opendc.core.workload.application.ProcessMessage
import com.atlarge.opendc.core.workload.application.ProcessSupervisor
import java.util.ArrayDeque
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A machine scheduling policy where processes are space-shared on the machine.
 *
 * Space-sharing for machine scheduling means that all running processes will be allocated a separate set of the
 * [ProcessingElement]s in a [Machine]. Applications are scheduled on the machine in first-in-first-out (FIFO) order,
 * thus larger applications may block smaller tasks from proceeding, while space is available (no backfilling).
 *
 * @property ctx The context in which the scheduler runs.
 * @property machine The machine to create the scheduler for.
 */
class SpaceSharedMachineScheduler(
    ctx: ProcessContext,
    coroutineScope: CoroutineScope,
    machine: Machine,
    machineRef: MachineRef
) : MachineSchedulerLogic(ctx, coroutineScope, machine, machineRef), ProcessSupervisor {
    private var cores = 0
    private val available = ArrayDeque<ProcessingElement>()
    private val queue = ArrayDeque<SendRef<ProcessMessage>>()
    private val running = LinkedHashSet<SendRef<ProcessMessage>>()
    private val processes = HashMap<SendRef<ProcessMessage>, ProcessView>()
    private val jobs = HashMap<SendRef<ProcessMessage>, Job>()
    private val channel = ctx.open<ProcessEvent>()

    init {
        coroutineScope.launch {
            ProcessSupervisor(ctx, this@SpaceSharedMachineScheduler, channel.receive)
        }
    }

    override suspend fun updateResources(cores: List<ProcessingElement>) {
        available.addAll(cores)
        this.cores = cores.size

        // Add all running tasks in front of the queue
        running.reversed().forEach { queue.addFirst(it) }
        running.clear()

        reschedule()
    }

    override suspend fun submit(application: Application, key: Any, handler: SendRef<MachineEvent>) {
        val channel = ctx.open<ProcessMessage>()
        val pid = channel.send
        // Create application instance on the machine
        ctx.spawn({ application(it, pid, channel.receive) }, name = application.name + ":" + application.uid + ":" + UUID.randomUUID().toString())
        processes[pid] = ProcessView(application, handler, pid)

        // Inform the owner that the task has been submitted
        handler.sendOnce(MachineEvent.Submitted(machineRef, application, key, pid))

        // Setup the task
        pid.sendOnce(ProcessMessage.Setup(machine, this@SpaceSharedMachineScheduler.channel.send))
    }

    /**
     * Reschedule the tasks on this machine.
     */
    private fun reschedule() {
        while (queue.isNotEmpty()) {
            val pid = queue.peek()
            val process = processes[pid]!!

            if (process.application.cores >= cores) {
                // The task will never fit on the machine
                // TODO Fail task
                println("Process $process will not fit in machine: dropping.")
                queue.remove()
                return
            } else if (process.application.cores > available.size) {
                // The task will not fit at the moment
                // Try again if resources become available
                // ctx.log.debug("Application queued: not enough processing elements available [requested={}, available={}]",
                //    process.application.cores, available.size)
                return
            }
            queue.remove()

            // Compute the available resources
            val resources = List(process.application.cores) {
                val pe = available.poll()
                Pair(pe, 1.0)
            }.toMap()
            process.state = ProcessState.RUNNING
            process.allocation = ProcessMessage.Allocation(resources, Long.MAX_VALUE)
            running += pid

            coroutineScope.launch(Dispatchers.Unconfined) {
                pid.sendOnce(process.allocation)
            }
        }
    }

    override fun onReady(pid: Pid) {
        val process = processes[pid]!!

        // Schedule the task if it has been setup
        queue.add(pid)
        process.state = ProcessState.READY

        reschedule()
    }

    override fun onConsume(pid: Pid, utilization: Map<ProcessingElement, Double>, until: Long) {
        val process = processes[pid]!!
        val allocation = process.allocation

        if (until > allocation.until) {
            // Tasks are not allowed to extend allocation provided by the machine
            // TODO Fail the task
            println("Task $pid must not extend allocation provided by the machine")
        } else if (until < allocation.until) {
            // Shrink allocation
            process.allocation = allocation.copy(until = until)
        }

        // Reschedule the process after the allocation expires
        jobs[pid] = coroutineScope.launch {
            delay(process.allocation.until - ctx.clock.millis())
            // We just extend the allocation
            process.allocation = process.allocation.copy(until = Long.MAX_VALUE)
            pid.sendOnce(process.allocation)
        }
    }

    override fun onExit(pid: Pid, status: Int) {
        val process = processes.remove(pid)!!
        running -= pid
        jobs[pid]?.cancel()
        process.allocation.resources.keys.forEach { available.add(it) }

        // Inform the owner that the task has terminated
        coroutineScope.launch {
            process.broker.sendOnce(MachineEvent.Terminated(machineRef, pid, status))
        }
    }

    companion object : MachineScheduler {
        override fun invoke(ctx: ProcessContext, coroutineScope: CoroutineScope, machine: Machine, machineRef: MachineRef): MachineSchedulerLogic {
            return SpaceSharedMachineScheduler(ctx, coroutineScope, machine, machineRef)
        }
    }
}
