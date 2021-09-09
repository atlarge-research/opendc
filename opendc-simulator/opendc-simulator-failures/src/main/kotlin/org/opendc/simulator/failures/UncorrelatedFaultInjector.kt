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

package org.opendc.simulator.failures

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.commons.math3.distribution.RealDistribution
import java.time.Clock
import kotlin.math.roundToLong

/**
 * A [FaultInjector] that injects uncorrelated faults into the system, meaning that failures of the subsystems are
 * independent.
 *
 * @param clock The [Clock] to keep track of simulation time.
 * @param iat The failure inter-arrival time distribution (in hours)
 * @param duration The failure duration distribution (in seconds).
 */
public class UncorrelatedFaultInjector(
    private val clock: Clock,
    private val iat: RealDistribution,
    private val duration: RealDistribution,
) : FaultInjector {
    /**
     * Enqueue the specified [FailureDomain] to fail some time in the future.
     */
    override fun enqueue(domain: FailureDomain) {
        domain.scope.launch {
            val d = (iat.sample() * 3.6e6).roundToLong() // Make sure to convert delay to milliseconds

            // Handle long overflow
            if (clock.millis() + d <= 0) {
                return@launch
            }

            delay(d)
            domain.fail()

            val df = (duration.sample() * 1000).roundToLong() // seconds to milliseconds

            // Handle long overflow
            if (clock.millis() + df <= 0) {
                return@launch
            }

            delay(df)
            domain.recover()
        }
    }
}
