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

import kotlinx.coroutines.*
import org.apache.commons.math3.distribution.RealDistribution
import java.time.Clock
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * A [FaultInjector] that injects fault in the system which are correlated to each other. Failures do not occur in
 * isolation, but will trigger other faults.
 *
 * @param scope The scope to run the fault injector in.
 * @param clock The [Clock] to keep track of simulation time.
 * @param iat The inter-arrival time distribution of the failures (in hours).
 * @param size The failure group size distribution.
 * @param duration The failure duration (in seconds).
 */
public class CorrelatedFaultInjector(
    private val scope: CoroutineScope,
    private val clock: Clock,
    private val iat: RealDistribution,
    private val size: RealDistribution,
    private val duration: RealDistribution,
    private val random: Random = Random(0)
) : FaultInjector {
    /**
     * The active failure domains that have been registered.
     */
    private val active = mutableSetOf<FailureDomain>()

    /**
     * The [Job] that awaits the nearest fault in the system.
     */
    private var job: Job? = null

    /**
     * Enqueue the specified [FailureDomain] to fail some time in the future.
     */
    override fun enqueue(domain: FailureDomain) {
        active += domain

        // Clean up the domain if it finishes
        domain.scope.coroutineContext[Job]!!.invokeOnCompletion {
            this@CorrelatedFaultInjector.scope.launch {
                active -= domain

                if (active.isEmpty()) {
                    job?.cancel()
                    job = null
                }
            }
        }

        if (job != null) {
            return
        }

        job = this.scope.launch {
            while (active.isNotEmpty()) {
                ensureActive()

                // Make sure to convert delay from hours to milliseconds
                val d = (iat.sample() * 3.6e6).roundToLong()

                // Handle long overflow
                if (clock.millis() + d <= 0) {
                    return@launch
                }

                delay(d)

                val n = size.sample().roundToInt()
                val targets = active.shuffled(random).take(n)

                for (failureDomain in targets) {
                    active -= failureDomain
                    failureDomain.fail()
                }

                val df = (duration.sample() * 1000).roundToLong() // seconds to milliseconds

                // Handle long overflow
                if (clock.millis() + df <= 0) {
                    return@launch
                }

                delay(df)

                for (failureDomain in targets) {
                    failureDomain.recover()

                    // Re-enqueue machine to be failed
                    enqueue(failureDomain)
                }
            }

            job = null
        }
    }
}
