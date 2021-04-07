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
import kotlinx.coroutines.flow.*
import org.opendc.simulator.compute.cpufreq.ScalingDriver
import org.opendc.simulator.compute.cpufreq.ScalingGovernor
import org.opendc.simulator.compute.model.ProcessingUnit
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
    override val model: SimMachineModel,
    scalingGovernor: ScalingGovernor,
    scalingDriver: ScalingDriver
) : SimAbstractMachine(clock) {
    /**
     * The [Job] associated with this machine.
     */
    private val scope = CoroutineScope(context + Job())

    override val context: CoroutineContext = scope.coroutineContext

    /**
     * The [TimerScheduler] to use for scheduling the interrupts.
     */
    private val scheduler = TimerScheduler<Any>(this.context, clock)

    override val resources: Map<ProcessingUnit, SimResourceSource> =
        model.cpus.associateWith { SimResourceSource(it.frequency, clock, scheduler) }

    /**
     * Construct the [ScalingDriver.Logic] for this machine.
     */
    private val scalingDriver = scalingDriver.createLogic(this)

    /**
     * The scaling contexts associated with each CPU.
     */
    private val scalingGovernors = resources.map { (cpu, resource) ->
        scalingGovernor.createLogic(this.scalingDriver.createContext(cpu, resource))
    }

    init {
        scalingGovernors.forEach { it.onStart() }
    }

    /**
     * The power draw of the machine.
     */
    public var powerDraw: Double = 0.0
        private set

    override fun updateUsage(usage: Double) {
        super.updateUsage(usage)

        scalingGovernors.forEach { it.onLimit() }
        powerDraw = scalingDriver.computePower()
    }

    override fun close() {
        super.close()

        scheduler.close()
        scope.cancel()
    }
}
