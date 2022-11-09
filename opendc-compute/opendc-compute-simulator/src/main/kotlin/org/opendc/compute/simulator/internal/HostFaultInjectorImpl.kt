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

package org.opendc.compute.simulator.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.commons.math3.distribution.RealDistribution
import org.opendc.compute.simulator.SimHost
import org.opendc.compute.simulator.failure.HostFault
import org.opendc.compute.simulator.failure.HostFaultInjector
import org.opendc.compute.simulator.failure.VictimSelector
import java.time.InstantSource
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToLong

/**
 * Internal implementation of the [HostFaultInjector] interface.
 *
 * @param context The scope to run the fault injector in.
 * @param clock The [InstantSource] to keep track of simulation time.
 * @param hosts The set of hosts to inject faults into.
 * @param iat The inter-arrival time distribution of the failures (in hours).
 * @param selector The [VictimSelector] to select the host victims.
 * @param fault The type of [HostFault] to inject.
 */
internal class HostFaultInjectorImpl(
    private val context: CoroutineContext,
    private val clock: InstantSource,
    private val hosts: Set<SimHost>,
    private val iat: RealDistribution,
    private val selector: VictimSelector,
    private val fault: HostFault
) : HostFaultInjector {
    /**
     * The scope in which the injector runs.
     */
    private val scope = CoroutineScope(context + Job())

    /**
     * The [Job] that awaits the nearest fault in the system.
     */
    private var job: Job? = null

    /**
     * Start the fault injection into the system.
     */
    override fun start() {
        if (job != null) {
            return
        }

        job = scope.launch {
            runInjector()
            job = null
        }
    }

    /**
     * Converge the injection process.
     */
    private suspend fun runInjector() {
        while (true) {
            // Make sure to convert delay from hours to milliseconds
            val d = (iat.sample() * 3.6e6).roundToLong()

            // Handle long overflow
            if (clock.millis() + d <= 0) {
                return
            }

            delay(d)

            val victims = selector.select(hosts)
            fault.apply(clock, victims)
        }
    }

    /**
     * Stop the fault injector.
     */
    public override fun close() {
        scope.cancel()
    }
}
