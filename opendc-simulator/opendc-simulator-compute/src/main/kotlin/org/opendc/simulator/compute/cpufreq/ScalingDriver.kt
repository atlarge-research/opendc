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

package org.opendc.simulator.compute.cpufreq

import org.opendc.simulator.compute.SimMachine
import org.opendc.simulator.compute.SimProcessingUnit

/**
 * A [ScalingDriver] is responsible for switching the processor to the correct frequency.
 */
public interface ScalingDriver {
    /**
     * Create the scaling logic for the specified [machine]
     */
    public fun createLogic(machine: SimMachine): Logic

    /**
     * The logic of the scaling driver.
     */
    public interface Logic {
        /**
         * Create the [ScalingContext] for the specified [cpu] instance.
         */
        public fun createContext(cpu: SimProcessingUnit): ScalingContext

        /**
         * Compute the power consumption of the processor.
         *
         * @return The power consumption of the processor in W.
         */
        public fun computePower(): Double
    }
}
