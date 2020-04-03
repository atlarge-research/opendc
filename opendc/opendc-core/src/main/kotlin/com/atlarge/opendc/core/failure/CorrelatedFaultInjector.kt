/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
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

package com.atlarge.opendc.core.failure

import com.atlarge.odcsim.Domain
import com.atlarge.odcsim.simulationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.random.Random
import kotlin.random.asJavaRandom

/**
 * A [FaultInjector] that injects fault in the system which are correlated to each other. Failures do not occur in
 * isolation, but will trigger other faults.
 */
public class CorrelatedFaultInjector(
    private val domain: Domain,
    private val iatScale: Double,
    private val iatShape: Double,
    private val sizeScale: Double,
    private val sizeShape: Double,
    private val dScale: Double,
    private val dShape: Double,
    random: Random = Random(0)
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
     * The [Random] instance to use.
     */
    private val random: java.util.Random = random.asJavaRandom()

    /**
     * Enqueue the specified [FailureDomain] to fail some time in the future.
     */
    override fun enqueue(domain: FailureDomain) {
        active += domain

        // Clean up the domain if it finishes
        domain.scope.coroutineContext[Job]!!.invokeOnCompletion {
            this@CorrelatedFaultInjector.domain.launch {
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

        job = this.domain.launch {
            while (active.isNotEmpty()) {
                ensureActive()

                // Make sure to convert delay from hours to milliseconds
                val d = lognvariate(iatScale, iatShape) * 3.6e6

                // Handle long overflow
                if (simulationContext.clock.millis() + d <= 0) {
                    return@launch
                }

                delay(d.toLong())

                val n = lognvariate(sizeScale, sizeShape).toInt()
                val targets = active.shuffled(random).take(n)

                for (failureDomain in targets) {
                    active -= failureDomain
                    failureDomain.fail()
                }

                val df = lognvariate(dScale, dShape) * 6e4

                // Handle long overflow
                if (simulationContext.clock.millis() + df <= 0) {
                    return@launch
                }

                delay(df.toLong())

                for (failureDomain in targets) {
                    failureDomain.recover()

                    // Re-enqueue machine to be failed
                    enqueue(failureDomain)
                }
            }

            job = null
        }
    }

    // XXX We should extract this in some common package later on.
    private fun lognvariate(scale: Double, shape: Double) = exp(scale + shape * random.nextGaussian())
}
