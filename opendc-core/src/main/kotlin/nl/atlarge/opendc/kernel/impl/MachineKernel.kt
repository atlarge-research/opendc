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

package nl.atlarge.opendc.kernel.impl

import nl.atlarge.opendc.experiment.Task
import nl.atlarge.opendc.kernel.AbstractEntityKernel
import nl.atlarge.opendc.kernel.EntityContext
import nl.atlarge.opendc.topology.machine.Cpu
import nl.atlarge.opendc.topology.machine.Machine

class MachineKernel(ctx: EntityContext<Machine>): AbstractEntityKernel<Machine>(ctx) {

	suspend override fun EntityContext<Machine>.run() {
		println("${this}: Initialising!")

		val cpus = component.outgoingEdges().filter { it.tag == "cpu" }.map { it.to.entity as Cpu }
		val speed = cpus.fold(0, { acc, (speed, cores) -> acc + speed * cores })
		val task: Task

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

		while (tick()) {
			task.consume(speed.toLong())
		}
	}

}
