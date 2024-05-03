/*
 * Copyright (c) 2024 AtLarge Research
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

package org.opendc.compute.failure.models

import kotlinx.coroutines.delay
import org.apache.commons.math3.distribution.RealDistribution
import org.opendc.compute.service.ComputeService
import java.time.InstantSource
import java.util.random.RandomGenerator
import kotlin.coroutines.CoroutineContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * Sample based failure model
 *
 * @property context
 * @property clock
 * @property service
 * @property random
 * @property iatSampler A distribution from which the time until the next fault is sampled in ms
 * @property durationSampler A distribution from which the duration of a fault is sampled in s
 * @property nohSampler A distribution from which the number of hosts that fault is sampled.
 */
public class SampleBasedFailureModel(
    context: CoroutineContext,
    clock: InstantSource,
    service: ComputeService,
    random: RandomGenerator,
    private val iatSampler: RealDistribution,
    private val durationSampler: RealDistribution,
    private val nohSampler: RealDistribution,
) : FailureModel(context, clock, service, random) {
    override suspend fun runInjector() {
        while (true) {
            val iatSample = max(0.0, iatSampler.sample())
            val intervalDuration = (iatSample * 3.6e6).roundToLong()

            // Handle long overflow
            if (clock.millis() + intervalDuration <= 0) {
                return
            }

            delay(intervalDuration)

            val numberOfHosts = min(1.0, max(0.0, nohSampler.sample()))
            val victims = victimSelector.select(hosts, numberOfHosts)

            val durationSample = max(0.0, durationSampler.sample())
            val faultDuration = (durationSample * 3.6e6).toLong()
            fault.apply(victims, faultDuration)

            break
        }
    }
}
