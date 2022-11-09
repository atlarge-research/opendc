/*
 * Copyright (c) 2020 AtLarge Research
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

import org.apache.commons.math3.distribution.RealDistribution
import org.opendc.compute.simulator.SimHost
import org.opendc.compute.simulator.internal.HostFaultInjectorImpl
import java.time.Clock
import java.time.InstantSource
import kotlin.coroutines.CoroutineContext

/**
 * An interface for stochastically injecting faults into a set of hosts.
 */
public interface HostFaultInjector : AutoCloseable {
    /**
     * Start fault injection.
     */
    public fun start()

    /**
     * Stop fault injection into the system.
     */
    public override fun close()

    public companion object {
        /**
         * Construct a new [HostFaultInjector].
         *
         * @param context The scope to run the fault injector in.
         * @param clock The [Clock] to keep track of simulation time.
         * @param hosts The hosts to inject the faults into.
         * @param iat The inter-arrival time distribution of the failures (in hours).
         * @param selector The [VictimSelector] to select the host victims.
         * @param fault The type of [HostFault] to inject.
         */
        public operator fun invoke(
            context: CoroutineContext,
            clock: InstantSource,
            hosts: Set<SimHost>,
            iat: RealDistribution,
            selector: VictimSelector,
            fault: HostFault
        ): HostFaultInjector = HostFaultInjectorImpl(context, clock, hosts, iat, selector, fault)
    }
}
