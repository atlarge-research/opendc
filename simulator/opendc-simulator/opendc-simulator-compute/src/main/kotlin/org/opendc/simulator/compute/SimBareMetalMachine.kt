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

package org.opendc.simulator.compute

import kotlinx.coroutines.*
import org.opendc.simulator.compute.model.SimProcessingUnit
import org.opendc.simulator.resources.*
import org.opendc.utils.TimerScheduler
import java.time.Clock
import kotlin.coroutines.*

/**
 * A simulated bare-metal machine that is able to run a single workload.
 *
 * A [SimBareMetalMachine] is a stateful object and you should be careful when operating this object concurrently. For
 * example. the class expects only a single concurrent call to [run].
 *
 * @param context The [CoroutineContext] to run the simulated workload in.
 * @param clock The virtual clock to track the simulation time.
 * @param model The machine model to simulate.
 */
@OptIn(ExperimentalCoroutinesApi::class, InternalCoroutinesApi::class)
public class SimBareMetalMachine(
    context: CoroutineContext,
    private val clock: Clock,
    override val model: SimMachineModel
) : SimAbstractMachine(clock) {
    /**
     * The [Job] associated with this machine.
     */
    private val job = Job()

    override val context: CoroutineContext = context + job

    /**
     * The [TimerScheduler] to use for scheduling the interrupts.
     */
    private val scheduler = TimerScheduler<Any>(this.context, clock)

    override val resources: Map<SimProcessingUnit, SimResourceSource<SimProcessingUnit>> =
        model.cpus.associateWith { SimResourceSource(it, clock, scheduler) }

    override fun close() {
        super.close()
        scheduler.close()
        job.cancel()
    }
}
