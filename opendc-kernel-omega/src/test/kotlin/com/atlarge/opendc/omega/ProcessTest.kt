/*
 * MIT License
 *
 * Copyright (c) 2018 atlarge-research
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
import org.junit.jupiter.api.Test
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * A test suite for processes in simulation.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
internal class ProcessTest {
    object FreezeProcess : Process<Unit, Unit> {
        override val initialState = Unit
        override suspend fun Context<Unit, Unit>.run() {
            receive()
            suspendCoroutine<Unit> {}
        }
    }

    /**
     * Test whether the simulation will not resume an already resumed continuation
     * of a process.
     */
    @Test
    fun `simulation will not resume frozen process`() {
        val bootstrap: Bootstrap<Unit> = Bootstrap.create { ctx ->
            ctx.register(FreezeProcess)
            ctx.schedule("Hello", destination = FreezeProcess, delay = 1)
            ctx.schedule("Hello", destination = FreezeProcess, delay = 1)
        }
        val simulation = OmegaKernel.create(bootstrap)
        simulation.run()
    }
}
