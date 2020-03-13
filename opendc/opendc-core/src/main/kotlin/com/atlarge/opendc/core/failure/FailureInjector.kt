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

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Random
import kotlin.coroutines.coroutineContext
import kotlin.math.ln
import kotlin.random.asKotlinRandom

/**
 * An entity that injects failures into a system.
 *
 * @param failureDomains The failure domains to be included.
 */
public class FailureInjector(private val failureDomains: List<FailureDomain>) {
    /**
     * The [Random] instance to generate the failures.
     */
    private val random = Random()

    /**
     * Start the failure injector process.
     */
    public suspend operator fun invoke() {
        val targets = HashSet(failureDomains)
        val mu = 20.0
        while (targets.isNotEmpty() && coroutineContext.isActive) {
            delay(random.expovariate(mu))
            val target = targets.random(random.asKotlinRandom())
            targets -= target
            target.fail()
        }
    }

    private fun Random.expovariate(mu: Double) = (-mu * ln(1 - nextDouble())).toLong()
}
