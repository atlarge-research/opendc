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

import com.atlarge.odcsim.simulationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ln1p
import kotlin.math.pow
import kotlin.random.Random

/**
 * A [FaultInjector] that injects uncorrelated faults into the system, meaning that failures of the subsystems are
 * independent.
 */
public class UncorrelatedFaultInjector(private val alpha: Double, private val beta: Double, private val random: Random = Random(0)) : FaultInjector {
    /**
     * Enqueue the specified [FailureDomain] to fail some time in the future.
     */
    override fun enqueue(domain: FailureDomain) {
        domain.scope.launch {
            val d = random.weibull(alpha, beta) * 1e3 // Make sure to convert delay to milliseconds

            // Handle long overflow
            if (simulationContext.clock.millis() + d <= 0) {
                return@launch
            }

            delay(d.toLong())
            domain.fail()
        }
    }

    // XXX We should extract this in some common package later on.
    private fun Random.weibull(alpha: Double, beta: Double) = (beta * (-ln1p(-nextDouble())).pow(1.0 / alpha))
}
