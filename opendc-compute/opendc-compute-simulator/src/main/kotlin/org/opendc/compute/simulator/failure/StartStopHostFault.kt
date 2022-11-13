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

package org.opendc.compute.simulator.failure

import kotlinx.coroutines.delay
import org.apache.commons.math3.distribution.RealDistribution
import org.opendc.compute.simulator.SimHost
import java.time.InstantSource
import kotlin.math.roundToLong

/**
 * A type of [HostFault] where the hosts are stopped and recover after some random amount of time.
 */
public class StartStopHostFault(private val duration: RealDistribution) : HostFault {
    override suspend fun apply(clock: InstantSource, victims: List<SimHost>) {
        for (host in victims) {
            host.fail()
        }

        val df = (duration.sample() * 1000).roundToLong() // seconds to milliseconds

        // Handle long overflow
        if (clock.millis() + df <= 0) {
            return
        }

        delay(df)

        for (host in victims) {
            host.recover()
        }
    }

    override fun toString(): String = "StartStopHostFault[$duration]"
}
