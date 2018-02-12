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

package com.atlarge.opendc.model.odc.topology.machine

import com.atlarge.opendc.model.odc.platform.workload.Task
import com.atlarge.opendc.model.topology.Topology
import com.atlarge.opendc.model.topology.destinations
import com.atlarge.opendc.simulator.Context
import com.atlarge.opendc.simulator.Duration
import com.atlarge.opendc.simulator.Process
import mu.KotlinLogging

/**
 * A Physical Machine (PM) inside a rack of a datacenter. It has a speed, and can be given a workload on which it will
 * work until finished or interrupted.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
open class Machine : Process<Machine.State, Topology> {
    /**
     * The logger instance to use for the simulator.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * The status of a machine.
     */
    enum class Status {
        HALT, IDLE, RUNNING
    }

    /**
     * The shape of the state of a [Machine] entity.
     *
     * @property status The status of the machine.
     * @property task The task assign to the machine.
     * @property memory The memory usage of the machine (defaults to 50mb for the kernel)
     * @property load The load on the machine (defaults to 0.0)
     * @property temperature The temperature of the machine (defaults to 23 degrees Celcius)
     */
    data class State(val status: Status,
                     val task: Task? = null,
                     val memory: Int = 50,
                     val load: Double = 0.0,
                     val temperature: Double = 23.0)

    /**
     * The initial state of a [Machine] entity.
     */
    override val initialState = State(Status.HALT)

    /**
     * Run the simulation kernel for this entity.
     */
    override suspend fun Context<State, Topology>.run() = model.run {
        state = State(Status.IDLE)

        val interval: Duration = 10
        val cpus = outgoingEdges.destinations<Cpu>("cpu")
        val speed = cpus.fold(0, { acc, cpu -> acc + cpu.clockRate * cpu.cores })

        // Halt the machine if it has not processing units (see bug #4)
        if (cpus.isEmpty()) {
            state = State(Status.HALT)
            return
        }

        var task: Task = receiveTask()
        state = State(Status.RUNNING, task, load = 1.0, memory = state.memory + 50, temperature = 30.0)

        while (true) {
            if (task.finished) {
                logger.info { "$id: Task ${task.id} finished. Machine idle at $time" }
                state = State(Status.IDLE)
                task = receiveTask()
            } else {
                task.consume(time, speed * delta)
            }

            // Check if we have received a new order in the meantime.
            val msg = receive(interval)
            if (msg is Task) {
                task = msg
                state = State(Status.RUNNING, task, load = 1.0, memory = state.memory + 50, temperature = 30.0)
            }
        }
    }

    /**
     * Wait for a [Task] to be received by the [Context] and discard all other messages received in the meantime.
     *
     * @return The task that has been received.
     */
    private suspend fun Context<State, Topology>.receiveTask(): Task {
        while (true) {
            val msg = receive()
            if (msg is Task)
                return msg
        }
    }
}
