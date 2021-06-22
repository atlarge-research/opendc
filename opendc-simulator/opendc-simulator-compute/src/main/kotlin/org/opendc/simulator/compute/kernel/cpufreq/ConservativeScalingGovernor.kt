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
 * A CPUFreq [ScalingGovernor] that models the conservative scaling governor in the Linux kernel.
 */
public class ConservativeScalingGovernor(public val threshold: Double = 0.8, private val stepSize: Double = -1.0) :
    ScalingGovernor {
    override fun createLogic(policy: ScalingPolicy): ScalingGovernor.Logic = object : ScalingGovernor.Logic {
        /**
         * The step size to use.
         */
        private val stepSize = if (this@ConservativeScalingGovernor.stepSize < 0) {
            // https://github.com/torvalds/linux/blob/master/drivers/cpufreq/cpufreq_conservative.c#L33
            policy.max * 0.05
        } else {
            this@ConservativeScalingGovernor.stepSize.coerceAtMost(policy.max)
        }

        /**
         * The previous load of the CPU.
         */
        private var previousLoad = threshold

        override fun onStart() {
            policy.target = policy.min
        }

        override fun onLimit(load: Double) {
            val currentTarget = policy.target
            if (load > threshold) {
                // Check for load increase (see: https://github.com/torvalds/linux/blob/master/drivers/cpufreq/cpufreq_conservative.c#L102)
                val step = when {
                    load > previousLoad -> stepSize
                    load < previousLoad -> -stepSize
                    else -> 0.0
                }
                policy.target = (currentTarget + step).coerceIn(policy.min, policy.max)
            }
            previousLoad = load
        }
    }

    override fun toString(): String = "ConservativeScalingGovernor[threshold=$threshold,stepSize=$stepSize]"
}
