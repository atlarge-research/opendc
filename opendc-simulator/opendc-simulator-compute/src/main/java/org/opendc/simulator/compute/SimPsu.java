/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.simulator.compute;

import org.opendc.simulator.compute.model.ProcessingUnit;
import org.opendc.simulator.flow2.InPort;
import org.opendc.simulator.power.SimPowerInlet;

/**
 * A power supply unit in a {@link SimBareMetalMachine}.
 *
 * <p>
 * This class manages the computation of power usage for a {@link SimBareMetalMachine} based on the resource usage.
 */
public abstract class SimPsu extends SimPowerInlet {
    /**
     * Return the power demand of the machine (in W) measured in the PSU.
     * <p>
     * This method provides access to the power consumption of the machine before PSU losses are applied.
     */
    public abstract double getPowerDemand();

    /**
     * Return the instantaneous power usage of the machine (in W) measured at the inlet of the power supply.
     */
    public abstract double getPowerUsage();

    /**
     * Return the cumulated energy usage of the machine (in J) measured at the inlet of the powers supply.
     */
    public abstract double getEnergyUsage();

    /**
     * Return an {@link InPort} that converts processing demand (in MHz) into energy demand (J) for the specified CPU
     * <code>model</code>.
     *
     * @param id The unique identifier of the CPU for this machine.
     * @param model The details of the processing unit.
     */
    abstract InPort getCpuPower(int id, ProcessingUnit model);

    /**
     * This method is invoked when the CPU frequency is changed for the specified <code>port</code>.
     *
     * @param port The {@link InPort} for which the capacity is changed.
     * @param capacity The capacity to change to.
     */
    void setCpuFrequency(InPort port, double capacity) {
        port.pull((float) capacity);
    }
}
