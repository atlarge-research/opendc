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

import org.opendc.simulator.compute.device.SimPsu
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.power.PowerDriver
import org.opendc.simulator.flow.*
import org.opendc.simulator.flow.FlowEngine
import kotlin.math.max

/**
 * A simulated bare-metal machine that is able to run a single workload.
 *
 * A [SimBareMetalMachine] is a stateful object, and you should be careful when operating this object concurrently. For
 * example, the class expects only a single concurrent call to [run].
 *
 * @param engine The [FlowEngine] to drive the simulation.
 * @param model The machine model to simulate.
 * @param powerDriver The power driver to use.
 * @param psu The power supply of the machine.
 * @param parent The parent simulation system.
 */
public class SimBareMetalMachine(
    engine: FlowEngine,
    model: MachineModel,
    powerDriver: PowerDriver,
    public val psu: SimPsu = SimPsu(500.0, mapOf(1.0 to 1.0)),
    parent: FlowConvergenceListener? = null,
) : SimAbstractMachine(engine, parent, model) {
    /**
     * The current power usage of the machine (without PSU loss) in W.
     */
    public val powerUsage: Double
        get() = _powerUsage
    private var _powerUsage = 0.0

    /**
     * The total energy usage of the machine (without PSU loss) in Joules.
     */
    public val energyUsage: Double
        get() {
            computeEnergyUsage(engine.clock.millis())
            return _energyUsage
        }
    private var _energyUsage = 0.0
    private var _energyLastComputation = 0L

    /**
     * The processing units of the machine.
     */
    override val cpus: List<SimProcessingUnit> = model.cpus.map { cpu ->
        Cpu(FlowSink(engine, cpu.frequency, this@SimBareMetalMachine), cpu)
    }

    /**
     * The logic of the power driver.
     */
    private val powerDriverLogic = powerDriver.createLogic(this, cpus)

    private var _lastConverge = Long.MAX_VALUE

    override fun onConverge(now: Long) {
        // Update the PSU stage
        psu.update()

        val lastConverge = _lastConverge
        _lastConverge = now
        val duration = max(0, now - lastConverge)
        if (duration > 0) {
            // Compute the power and energy usage of the machine
            computeEnergyUsage(now)
        }

        _powerUsage = powerDriverLogic.computePower()
    }

    init {
        psu.connect(powerDriverLogic)
        _powerUsage = powerDriverLogic.computePower()
    }

    /**
     * Helper method to compute total energy usage.
     */
    private fun computeEnergyUsage(now: Long) {
        val duration = max(0, now - _energyLastComputation)
        _energyLastComputation = now

        // Compute the energy usage of the machine
        _energyUsage += _powerUsage * (duration / 1000.0)
    }

    /**
     * A [SimProcessingUnit] of a bare-metal machine.
     */
    private class Cpu(
        private val source: FlowSink,
        override val model: ProcessingUnit
    ) : SimProcessingUnit, FlowConsumer by source {
        override var capacity: Double
            get() = source.capacity
            set(value) {
                // Clamp the capacity of the CPU between [0.0, maxFreq]
                source.capacity = value.coerceIn(0.0, model.frequency)
            }

        override fun toString(): String = "SimBareMetalMachine.Cpu[model=$model]"
    }
}
