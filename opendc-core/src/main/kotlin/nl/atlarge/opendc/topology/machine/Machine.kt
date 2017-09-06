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

package nl.atlarge.opendc.topology.machine

import nl.atlarge.opendc.experiment.Task
import nl.atlarge.opendc.simulator.EntityContext
import nl.atlarge.opendc.simulator.Kernel
import nl.atlarge.opendc.topology.Entity
import nl.atlarge.opendc.topology.outgoing
import java.util.*

/**
 * A Physical Machine (PM) inside a rack of a datacenter. It has a speed, and can be given a workload on which it will
 * work until finished or interrupted.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
class Machine: Entity<Machine.State>, Kernel<EntityContext<Machine>> {
	/**
	 * The status of a machine.
	 */
	enum class Status {
		HALT, IDLE, RUNNING
	}

	/**
	 * The shape of the state of a [Machine] entity.
	 */
	data class State(val status: Status)

	/**
	 * The initial state of a [Machine] entity.
	 */
	override val initialState = State(Status.HALT)

	/**
	 * Run the simulation kernel for this entity.
	 */
	override suspend fun EntityContext<Machine>.simulate() {
		update(state.copy(status = Machine.Status.IDLE))

		val cpus = component.outgoing<Cpu>("cpu")
		val speed = cpus.fold(0, { acc, (speed, cores) -> acc + speed * cores })
		val task: Task

		val delay = Random().nextInt(1000) + 1
		wait(delay)

		loop@while (true) {
			val msg = receive()
			when (msg) {
				is Task -> {
					task = msg
					break@loop
				}
				else -> println("warning: unhandled message $msg")
			}
		}

		update(state.copy(status = Machine.Status.RUNNING))
		while (tick()) {
			task.consume(speed.toLong())
		}
		update(state.copy(status = Machine.Status.HALT))
	}
}
