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

package org.opendc.compute.simulator.failure.models

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.opendc.compute.service.ComputeService
import org.opendc.compute.simulator.SimHost
import org.opendc.compute.simulator.failure.hostfault.HostFault
import org.opendc.compute.simulator.failure.hostfault.StartStopHostFault
import org.opendc.compute.simulator.failure.victimselector.StochasticVictimSelector
import java.time.InstantSource
import java.util.random.RandomGenerator
import kotlin.coroutines.CoroutineContext

/**
 * Factory interface for constructing [HostFaultInjector] for modeling failures of compute service hosts.
 */
public abstract class FailureModel (
    context: CoroutineContext,
    protected val clock: InstantSource,
    protected val service: ComputeService,
    protected val random: RandomGenerator
): AutoCloseable {
    protected val scope: CoroutineScope = CoroutineScope(context + Job())

    // TODO: could at some point be extended to different types of faults
    protected val fault: HostFault = StartStopHostFault(service, clock)

    // TODO: could at some point be extended to different types of victim selectors
    protected val victimSelector: StochasticVictimSelector = StochasticVictimSelector(random)

    protected val hosts: Set<SimHost> = service.hosts.map { it as SimHost }.toSet()

    /**
     * The [Job] that awaits the nearest fault in the system.
     */
    private var job: Job? = null

    /**
     * Start the fault injection into the system.
     */
    public fun start() {
        if (job != null) {
            return
        }

        job =
            scope.launch {
                runInjector()
                job = null
            }
    }

    public abstract suspend fun runInjector()

    /**
     * Stop the fault injector.
     */
    public override fun close() {
        scope.cancel()
    }
}
