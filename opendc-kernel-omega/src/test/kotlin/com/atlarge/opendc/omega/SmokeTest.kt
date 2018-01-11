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

package com.atlarge.opendc.omega

import com.atlarge.opendc.simulator.Context
import com.atlarge.opendc.simulator.Process
import com.atlarge.opendc.simulator.Bootstrap
import org.junit.jupiter.api.Test

/**
 * This test suite checks for smoke when running a large amount of simulations.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
internal class SmokeTest {
	class EchoProcess : Process<Unit, Unit> {
		override val initialState = Unit
		suspend override fun Context<Unit, Unit>.run() {
			while (true) {
				receive {
					sender?.send(message)
				}
			}
		}
	}

	/**
	 * Run a large amount of simulations and test if any exceptions occur.
	 */
	@Test
	fun smoke() {
		val n = 1000
		val messages = 100
		val bootstrap: Bootstrap<Unit> = Bootstrap.create { ctx ->
			repeat(n) {
				EchoProcess().also {
					ctx.register(it)

					for (i in 1 until messages) {
						ctx.schedule(i, it, delay = i.toLong())
					}
				}
			}
		}
		val kernel = OmegaKernelFactory.create(bootstrap)
		kernel.run()
	}

	class NullProcess : Process<Unit, Unit> {
		override val initialState = Unit
		suspend override fun Context<Unit, Unit>.run() {}
	}

	/**
	 * Test if the kernel allows sending messages to [Context] instances that have already stopped.
	 */
	@Test
	fun `sending message to process that has gracefully stopped`() {
		val process = NullProcess()
		val bootstrap: Bootstrap<Unit> = Bootstrap.create { ctx ->
			process.also {
				ctx.register(it)
				ctx.schedule(0, it)
			}
		}

		val kernel = OmegaKernelFactory.create(bootstrap)
		kernel.run()
	}

	class CrashProcess : Process<Unit, Unit> {
		override val initialState = Unit
		suspend override fun Context<Unit, Unit>.run() {
			TODO("This process should crash")
		}
	}

	/**
	 * Test if the kernel allows sending messages to [Context] instances that have crashed.
	 */
	@Test
	fun `sending message to process that has crashed`() {
		val process = CrashProcess()
		val bootstrap: Bootstrap<Unit> = Bootstrap.create { ctx ->
			process.also {
				ctx.register(it)
				ctx.schedule(0, it)
			}
		}

		val kernel = OmegaKernelFactory.create(bootstrap)
		kernel.run()
	}
}
