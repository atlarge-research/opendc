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

import com.atlarge.opendc.simulator.Bootstrap
import com.atlarge.opendc.simulator.Context
import com.atlarge.opendc.simulator.Process
import com.atlarge.opendc.simulator.instrumentation.Instrument
import com.atlarge.opendc.simulator.kernel.Kernel
import com.atlarge.opendc.simulator.kernel.Simulation
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * This test suite checks for smoke when running a large amount of simulations.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
internal class SmokeTest {
    class EchoProcess : Process<Unit, Unit> {
        override val initialState = Unit
        override suspend fun Context<Unit, Unit>.run() {
            while (true) {
                val msg = receive()
                sender?.send(msg)
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
        val simulation = OmegaKernel.create(bootstrap)
        simulation.run()
    }

    object NullProcess : Process<Unit, Unit> {
        override val initialState = Unit
        override suspend fun Context<Unit, Unit>.run() {}
    }

    /**
     * Test if the kernel allows sending messages to [Context] instances that have already stopped.
     */
    @Test
    fun `sending message to process that has gracefully stopped`() {
        val process = NullProcess
        val bootstrap: Bootstrap<Unit> = Bootstrap.create { ctx ->
            process.also {
                ctx.register(it)
                ctx.schedule(0, it)
            }
        }

        val simulation = OmegaKernel.create(bootstrap)
        simulation.run()
    }

    object CrashProcess : Process<Unit, Unit> {
        override val initialState = Unit
        override suspend fun Context<Unit, Unit>.run() {
            TODO("This process should crash")
        }
    }

    /**
     * Test if the kernel allows sending messages to [Context] instances that have crashed.
     */
    @Test
    fun `sending message to process that has crashed`() {
        val process = CrashProcess
        val bootstrap: Bootstrap<Unit> = Bootstrap.create { ctx ->
            process.also {
                ctx.register(it)
                ctx.schedule(0, it)
            }
        }

        val simulation = OmegaKernel.create(bootstrap)
        simulation.run()
    }

    class ModelProcess(private val value: Int) : Process<Boolean, Int> {
        override val initialState = false
        override suspend fun Context<Boolean, Int>.run() {
            assertEquals(value, model)
            state = true
            hold(10)
        }
    }

    /**
     * Test if the kernel allows access to the simulation model object.
     */
    @Test
    fun `access simulation model`() {
        val value = 1
        val process = ModelProcess(value)
        val bootstrap: Bootstrap<Int> = Bootstrap.create { ctx ->
            ctx.register(process)
            value
        }

        val kernel = OmegaKernel.create(bootstrap)
        kernel.run(5)
    }


    @Test
    fun `instrumentation works`() {
        val instrument: Instrument<Int, Unit> = {
            var value = 0

            for (i in 1..10) {
                send(value)
                value += 10
                hold(20)
            }
        }

        val simulation: Simulation<Unit> = OmegaKernel.create(Bootstrap.create { Unit })
        val stream = simulation.install(instrument)

        val res = async(Unconfined) {
            stream.consumeEach { println(it) }
        }

        simulation.run(100)
    }
}
