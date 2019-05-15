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

package com.atlarge.opendc.model.resources.compute.scheduling

import com.atlarge.odcsim.ActorContext
import com.atlarge.odcsim.ActorRef
import com.atlarge.odcsim.Instant
import com.atlarge.odcsim.TimerScheduler
import com.atlarge.odcsim.unsafeCast
import com.atlarge.opendc.model.resources.compute.Machine
import com.atlarge.opendc.model.resources.compute.MachineEvent
import com.atlarge.opendc.model.resources.compute.MachineMessage
import com.atlarge.opendc.model.resources.compute.ProcessingElement
import com.atlarge.opendc.model.workload.application.Application
import com.atlarge.opendc.model.workload.application.Pid
import com.atlarge.opendc.model.workload.application.ProcessMessage
import com.atlarge.opendc.model.workload.application.ProcessSupervisor
import java.util.ArrayDeque
import java.util.UUID

/**
 * A machine scheduling policy where processes are space-shared on the machine.
 *
 * Space-sharing for machine scheduling means that all running processes will be allocated a separate set of the
 * [ProcessingElement]s in a [Machine]. Applications are scheduled on the machine in first-in-first-out (FIFO) order,
 * thus larger applications may block smaller tasks from proceeding, while space is available (no backfilling).
 *
 * @property machine The machine to create the scheduler for.
 * @property ctx The context in which the scheduler runs.
 * @property scheduler The timer scheduler to use.
 */
class SpaceSharedMachineScheduler(
    machine: Machine,
    ctx: ActorContext<MachineMessage>,
    scheduler: TimerScheduler<MachineMessage>
) : MachineSchedulerLogic(machine, ctx, scheduler), ProcessSupervisor {
    private var cores = 0
    private val available = ArrayDeque<ProcessingElement>()
    private val queue = ArrayDeque<ActorRef<ProcessMessage>>()
    private val running = LinkedHashSet<ActorRef<ProcessMessage>>()
    private val processes = HashMap<ActorRef<ProcessMessage>, ProcessView>()

    override fun updateResources(cores: List<ProcessingElement>) {
        available.addAll(cores)
        this.cores = cores.size

        // Add all running tasks in front of the queue
        running.reversed().forEach { queue.addFirst(it) }
        running.clear()

        reschedule()
    }

    override fun submit(application: Application, key: Any, handler: ActorRef<MachineEvent>) {
        // Create application instance on the machine
        val pid = ctx.spawn(application(), name = application.name + ":" + application.uid + ":" + UUID.randomUUID().toString())
        processes[pid] = ProcessView(application, handler, pid)

        // Setup the task
        ctx.send(pid, ProcessMessage.Setup(machine, ctx.self.unsafeCast()))

        // Inform the owner that the task has been submitted
        ctx.send(handler, MachineEvent.Submitted(ctx.self, application, key, pid))
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
                ctx.log.warn("Process {} will not fit in machine: dropping.", process)
                queue.remove()
                return
            } else if (process.application.cores > available.size) {
                // The task will not fit at the moment
                // Try again if resources become available
                ctx.log.debug("Application queued: not enough processing elements available [requested={}, available={}]",
                    process.application.cores, available.size)
                return
            }
            queue.remove()

            // Compute the available resources
            val resources = List(process.application.cores) {
                val pe = available.poll()
                Pair(pe, 1.0)
            }.toMap()
            process.state = ProcessState.RUNNING
            process.allocation = ProcessMessage.Allocation(resources, Instant.POSITIVE_INFINITY)
            running += pid

            ctx.send(pid, process.allocation)
        }
    }

    override fun onReady(pid: Pid) {
        val process = processes[pid]!!

        // Schedule the task if it has been setup
        queue.add(pid)
        process.state = ProcessState.READY

        reschedule()
    }

    override fun onConsume(pid: Pid, utilization: Map<ProcessingElement, Double>, until: Instant) {
        val process = processes[pid]!!
        val allocation = process.allocation

        if (until > allocation.until) {
            // Tasks are not allowed to extend allocation provided by the machine
            // TODO Fail the task
            ctx.log.warn("Task {} must not extend allocation provided by the machine", pid)
        } else if (until < allocation.until) {
            // Shrink allocation
            process.allocation = allocation.copy(until = until)
        }

        // Reschedule the process after the allocation expires
        scheduler.after(pid, process.allocation.until - ctx.time) {
            // We just extend the allocation
            process.allocation = process.allocation.copy(until = Instant.POSITIVE_INFINITY)
            ctx.send(pid, process.allocation)
        }
    }

    override fun onExit(pid: Pid, status: Int) {
        val process = processes.remove(pid)!!
        running -= pid
        scheduler.cancel(pid)
        process.allocation.resources.keys.forEach { available.add(it) }

        ctx.log.debug("Application {} terminated with status {}", pid, status)

        // Inform the owner that the task has terminated
        ctx.send(process.broker, MachineEvent.Terminated(ctx.self, pid, status))

        reschedule()
    }

    companion object : MachineScheduler {
        override fun invoke(
            machine: Machine,
            ctx: ActorContext<MachineMessage>,
            scheduler: TimerScheduler<MachineMessage>
        ): MachineSchedulerLogic {
            return SpaceSharedMachineScheduler(machine, ctx, scheduler)
        }
    }
}
