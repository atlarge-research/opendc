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

package org.opendc.compute.simulator.failure

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import org.apache.commons.math3.distribution.LogNormalDistribution
import org.apache.commons.math3.random.Well19937c
import org.junit.jupiter.api.Test
import org.opendc.compute.simulator.SimHost
import org.opendc.simulator.kotlin.runSimulation
import java.time.Duration
import java.time.InstantSource
import kotlin.coroutines.CoroutineContext
import kotlin.math.ln

/**
 * Test suite for [HostFaultInjector] class.
 */
class HostFaultInjectorTest {
    /**
     * Simple test case to test that nothing happens when the injector is not started.
     */
    @Test
    fun testInjectorNotStarted() = runSimulation {
        val host = mockk<SimHost>(relaxUnitFun = true)

        val injector = createSimpleInjector(coroutineContext, timeSource, setOf(host))

        coVerify(exactly = 0) { host.fail() }
        coVerify(exactly = 0) { host.recover() }

        injector.close()
    }

    /**
     * Simple test case to test a start stop fault where the machine is stopped and started after some time.
     */
    @Test
    fun testInjectorStopsMachine() = runSimulation {
        val host = mockk<SimHost>(relaxUnitFun = true)

        val injector = createSimpleInjector(coroutineContext, timeSource, setOf(host))

        injector.start()

        delay(Duration.ofDays(55).toMillis())

        injector.close()

        coVerify(exactly = 1) { host.fail() }
        coVerify(exactly = 1) { host.recover() }
    }

    /**
     * Simple test case to test a start stop fault where multiple machines are stopped.
     */
    @Test
    fun testInjectorStopsMultipleMachines() = runSimulation {
        val hosts = listOf<SimHost>(
            mockk(relaxUnitFun = true),
            mockk(relaxUnitFun = true)
        )

        val injector = createSimpleInjector(coroutineContext, timeSource, hosts.toSet())

        injector.start()

        delay(Duration.ofDays(55).toMillis())

        injector.close()

        coVerify(exactly = 1) { hosts[0].fail() }
        coVerify(exactly = 1) { hosts[1].fail() }
        coVerify(exactly = 1) { hosts[0].recover() }
        coVerify(exactly = 1) { hosts[1].recover() }
    }

    /**
     * Create a simple start stop fault injector.
     */
    private fun createSimpleInjector(context: CoroutineContext, clock: InstantSource, hosts: Set<SimHost>): HostFaultInjector {
        val rng = Well19937c(0)
        val iat = LogNormalDistribution(rng, ln(24 * 7.0), 1.03)
        val selector = StochasticVictimSelector(LogNormalDistribution(rng, 1.88, 1.25))
        val fault = StartStopHostFault(LogNormalDistribution(rng, 8.89, 2.71))

        return HostFaultInjector(context, clock, hosts, iat, selector, fault)
    }
}
