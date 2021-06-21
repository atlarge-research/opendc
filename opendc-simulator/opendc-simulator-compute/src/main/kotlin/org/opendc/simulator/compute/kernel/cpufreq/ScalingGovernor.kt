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

package org.opendc.simulator.compute.kernel.cpufreq

/**
 * A [ScalingGovernor] in the CPUFreq subsystem of OpenDC is responsible for scaling the frequency of simulated CPUs
 * independent of the particular implementation of the CPU.
 *
 * Each of the scaling governors implements a single, possibly parametrized, performance scaling algorithm.
 *
 * For more information, see the documentation of the Linux CPUFreq subsystem:
 * https://www.kernel.org/doc/html/latest/admin-guide/pm/cpufreq.html
 */
public interface ScalingGovernor {
    /**
     * Create the scaling logic for the specified [policy]
     */
    public fun createLogic(policy: ScalingPolicy): Logic

    /**
     * The logic of the scaling governor.
     */
    public interface Logic {
        /**
         * This method is invoked when the governor is started.
         */
        public fun onStart() {}

        /**
         * This method is invoked when the governor should re-decide the frequency limits.
         *
         * @param load The load of the system.
         */
        public fun onLimit(load: Double) {}
    }
}
